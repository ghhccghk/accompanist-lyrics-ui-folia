"""Fixed original 48 frames, FIVE-image union; no registration or alpha removal."""
from pathlib import Path
import hashlib, json, xml.etree.ElementTree as ET
import numpy as np
from PIL import Image

root=Path(__file__).resolve().parents[1]
b=root/'build/verification'; out=root/'verification/diorama-text-surface';out.mkdir(exist_ok=True)
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
bg=np.array([22,36,53])
old=json.loads((root/'verification/diorama-active-base-sampler/comparison.json').read_text())
scene=json.loads((root/'verification/diorama-fixtures/mounted-cut-v1/scene.json').read_text())
frozen=json.loads((root/'.work/diorama-transparent-stage-before-1103f21/hashes.json').read_text())
for f,h in frozen.items():assert sha(b/f)==h,f
rows=[]
for c in scene['captures']:
    f=c['file']; previous=next(r for r in old['rows'] if r['file']==f)
    files=dict(original=b/'diorama-mounted-browser'/f,
        finiteBefore=b/'diorama-mounted-active-base-kotlin'/f,
        finiteAfter=b/'diorama-text-surface-kotlin'/f,
        mediaBefore=b/'diorama-mounted-active-base-kotlin'/('media-'+f),
        mediaAfter=b/'diorama-text-surface-kotlin'/('media-'+f))
    for key,oldkey in [('original','original'),('finiteBefore','afterFinite'),('mediaBefore','afterMedia')]:
        assert sha(files[key])==previous['hashes'][oldkey]
    images={k:np.array(Image.open(p).convert('RGB'),dtype=np.int32) for k,p in files.items()}
    union=np.logical_or.reduce([np.any(a!=bg,axis=2) for a in images.values()]);metrics={}
    for k,a in images.items():
        d=abs(a-images['original'])
        tiles=[dict(x=x,y=y,mae=float(d[y:y+64,x:x+64][union[y:y+64,x:x+64]].mean()))
            for y in range(0,a.shape[0],64) for x in range(0,a.shape[1],64) if union[y:y+64,x:x+64].any()]
        metrics[k]=dict(mae=float(d[union].mean()) if union.any() else 0,peak=int(d.max()),
            worst64=max(tiles,key=lambda v:v['mae']) if tiles else dict(x=0,y=0,mae=0))
    rows.append(dict(file=f,unionPixels=int(union.sum()),metrics=metrics,hashes={k:sha(p) for k,p in files.items()}))
def summary(prefix):
    before=prefix+'Before';after=prefix+'After'
    return dict(means={k:float(np.mean([r['metrics'][k]['mae'] for r in rows])) for k in [before,after]},
        worst64={k:max(r['metrics'][k]['worst64']['mae'] for r in rows) for k in [before,after]},
        meanRegressions=[dict(file=r['file'],before=r['metrics'][before],after=r['metrics'][after]) for r in rows
            if r['metrics'][after]['mae']>r['metrics'][before]['mae']],
        localRegressions=[dict(file=r['file'],before=r['metrics'][before]['worst64'],after=r['metrics'][after]['worst64']) for r in rows
            if r['metrics'][after]['worst64']['mae']>r['metrics'][before]['worst64']['mae']])
report=dict(baseline='d46f46c',sourceRevision=scene['revision'],
    condition='Same 48 original PNG/state/font/viewport/time. Five-image union per frame includes every nonbackground pixel. No registration, delay, alpha suppression, camera/geometry adjustment. Other union means are not comparable.',
    finite=summary('finite'),media=summary('media'),rows=rows,oldHashesUnchanged=len(frozen),
    androidSurfaceImplemented=False,formalVisualAcceptance=False,performanceMeasured=False)
(out/'comparison.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf8')
tests=[]
for name in ['DioramaTextSurfaceTest','DioramaMountedStageTest','DioramaActiveBaseRendererTest']:
    p=root/f'library/build/test-results/jvmTest/TEST-top.ghhccghk.accompanist.lyrics.folia.{name}.xml'
    doc=ET.parse(p).getroot();assert doc.attrib['failures']=='0' and doc.attrib['errors']=='0'
    tests.append(dict(suite=name,tests=int(doc.attrib['tests']),failures=0,errors=0))
log=(root/'.work/diorama-surface-verified.log').read_text(encoding='utf8',errors='replace')
assert 'BUILD SUCCESSFUL' in log
validation=dict(suites=tests,androidLibraryCompiled=True,desktopCompiled=True,
    originalThreeOracle=(b/'diorama-text-surface-kotlin/numeric.txt').read_text().strip(),
    priorFailure=dict(test='budgetExceptionClipAndMatrixRestoration',message='expected:<1> but was:<2>',
        cause='Test sampled outer saveCount before CanvasDrawScope owned its save. Surface and outer caller now each checked against their own entry count and matrix; production restoration unchanged.'),
    deviceRun=False,realDesktopWindow=False)
(out/'validation.json').write_text(json.dumps(validation,indent=2)+'\n',encoding='utf8')
print(json.dumps({k:report[k] for k in ['finite','media','oldHashesUnchanged']},indent=2))
