"""Frozen four entrance layers and48 mounted frames; no registration/threshold."""
from pathlib import Path
import hashlib,json
import numpy as np
from PIL import Image,ImageDraw

root=Path(__file__).resolve().parents[1]
out=root/'verification/diorama-mounted-entrance';out.mkdir(exist_ok=True)
b=root/'build/verification';bg=np.array([22,36,53])
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
source=json.loads((root/'verification/diorama-fixtures/mounted-entrance-v1/scene.json').read_text())
alpha=json.loads((root/'verification/diorama-fixtures/mounted-entrance-alpha-v1/scene.json').read_text())
previous=json.loads((root/'verification/diorama-active-base-sampler/comparison.json').read_text())
frozen=root/'.work/diorama-transparent-stage-before-1103f21'
hashes=json.loads((frozen/'hashes.json').read_text())
for file,h in hashes.items():assert sha(b/file)==h,'Existing production/control image changed: '+file

def compare(files):
    images={k:np.array(Image.open(p).convert('RGB'),dtype=np.int32) for k,p in files.items()}
    union=np.logical_or.reduce([np.any(a!=bg,axis=2) for a in images.values()])
    result={}
    for k,a in images.items():
        d=abs(a-images['original']);mask=np.any(a!=bg,axis=2);ys,xs=np.where(mask)
        local=[dict(x=x,y=y,mae=float(d[y:y+64,x:x+64][union[y:y+64,x:x+64]].mean()))
               for y in range(0,a.shape[0],64) for x in range(0,a.shape[1],64) if union[y:y+64,x:x+64].any()]
        result[k]=dict(mae=float(d[union].mean()) if union.any() else 0,peak=int(d.max()),
            worst64=max(local,key=lambda v:v['mae']) if local else dict(x=0,y=0,mae=0),
            pixels=int(xs.size),mass=int(abs(a-bg).sum()),
            bounds=[int(xs.min()),int(ys.min()),int(xs.max()+1),int(ys.max()+1)] if xs.size else None)
    return dict(unionPixels=int(union.sum()),metrics=result,hashes={k:sha(p) for k,p in files.items()})

def summarize(rows,before,after):
    return dict(means={k:float(np.mean([r['metrics'][k]['mae'] for r in rows])) for k in [before,after]},
        worst64={k:max(r['metrics'][k]['worst64']['mae'] for r in rows) for k in [before,after]},
        meanRegressions=[dict(file=r['file'],before=r['metrics'][before]['mae'],after=r['metrics'][after]['mae'])
                         for r in rows if r['metrics'][after]['mae']>r['metrics'][before]['mae']],
        localRegressions=[dict(file=r['file'],before=r['metrics'][before]['worst64'],after=r['metrics'][after]['worst64'])
                          for r in rows if r['metrics'][after]['worst64']['mae']>r['metrics'][before]['worst64']['mae']])

rows=[]
for c in source['captures']:
    prefix=f"{c['width']}-{c['fps']}";original=b/'diorama-mounted-entrance-browser'
    second=b/'diorama-mounted-entrance-alpha-browser';native=b/'diorama-mounted-entrance-kotlin'
    a=next(r for r in alpha['captures'] if r['file']==c['file'])
    old=next(r for r in previous['rows'] if r['file']==c['file'])
    for d in [c,a]:
        directory=original if d is c else second
        assert sha(directory/f'{prefix}-original.png')==sha(directory/f'{prefix}-restored.png')==c['expectedOriginalSha256']
        assert d['expectedOriginalSha256']==old['hashes']['original']
        for item in d['inputs']+d['layers']:assert sha(directory/item['file'])==item['sha256']
        assert d['teardown']==dict(roots=0,queuedRafs=0)
    assert c['state']==a['state'] and c['callbackHistorySha256']==a['callbackHistorySha256']
    layers=[]
    for kind in ['full','all','base','glow','soul','neighbors']:
        src=original/f'{prefix}-{"original" if kind=="full" else kind}.png'
        files={'original':src}
        for mode,tag in [('direct',''),('layer8','grouped-'),('layerF16','float-')]:
            for sample in ['before','after']:
                files[mode+sample.title()]=native/f'{prefix}-{tag}{sample}-{kind}.png'
        result=compare(files)
        if kind=='full':
            assert result['hashes']['directBefore']==old['hashes']['beforeFinite']
            assert result['hashes']['directAfter']==old['hashes']['afterFinite']
        framebuffer=None
        if kind!='full':
            original_alpha=next(r for r in a['layers'] if r['kind']==kind)['framebuffer']
            p=second/original_alpha['file'];assert sha(p)==original_alpha['sha256']
            im=Image.open(p).convert('RGBA');raw=np.array(im)
            cpu=Image.alpha_composite(Image.new('RGBA',im.size,(*bg,255)),im).convert('RGB')
            error=abs(np.array(cpu,dtype=np.int32)-np.array(Image.open(src).convert('RGB'),dtype=np.int32))
            assert original_alpha['context']['alpha'] and original_alpha['context']['premultipliedAlpha']
            assert original_alpha['clearColor']==[0,0,0,0]
            assert int(error.max())<=1,'Original raw framebuffer/CSS cross is not accounted for'
            framebuffer=dict(rgbaMax=raw.max(axis=(0,1)).tolist(),alphaMass=int(raw[:,:,3].sum()),
                cssCpuPeak=int(error.max()),cssCpuDifferentPixels=int(np.any(error!=0,axis=2).sum()),
                hash=sha(p),context=original_alpha['context'],clearColor=original_alpha['clearColor'])
        layers.append(dict(kind=kind,**result,framebuffer=framebuffer))
    # Fixed failing y0 tile from the OLD report, same complete-layer union.
    tile=old['metrics']['afterFinite']['worst64'];x,y=tile['x'],tile['y']
    assert y==0
    top={kind:np.array(Image.open(original/f'{prefix}-{kind}.png').convert('RGB'))[y:y+64,x:x+64]
         for kind in ['all','neighbors']}
    assert np.all(top['neighbors']==bg),'Cannot attribute top tile to active without this guard'
    rows.append(dict(file=c['file'],callbacks=c['callbacks'],fixedFailureTile=tile,
        topTileNeighborsBackgroundOnly=True,layers=layers))

matrix=[]
whole=json.loads((root/'verification/diorama-fixtures/mounted-cut-v1/scene.json').read_text())
for c in whole['captures']:
    f=c['file'];old=next(r for r in previous['rows'] if r['file']==f)
    files=dict(original=b/'diorama-mounted-browser'/f,
        ordinary=b/'diorama-mounted-kotlin'/('material-'+f),base=b/'diorama-mounted-active-base-kotlin'/f,
        media=b/'diorama-mounted-active-base-kotlin'/('media-'+f),
        ordinary8=b/'diorama-transparent-stage-kotlin'/('material-'+f),base8=b/'diorama-transparent-stage-kotlin'/('base-'+f),
        media8=b/'diorama-transparent-stage-kotlin'/('base-media-'+f),
        ordinaryF16=b/'diorama-transparent-float-kotlin'/('material-'+f),baseF16=b/'diorama-transparent-float-kotlin'/('base-'+f),
        mediaF16=b/'diorama-transparent-float-kotlin'/('base-media-'+f))
    assert sha(files['original'])==old['hashes']['original']
    assert sha(files['ordinary'])==old['hashes']['ordinaryFinite']
    assert sha(files['base'])==old['hashes']['afterFinite']
    assert sha(files['media'])==old['hashes']['afterMedia']
    matrix.append(dict(file=f,**compare(files)))

report=dict(baseline='1103f21',sourceRevision=source['revision'],
    condition='Frozen four700ms layers with7-image per-layer union; same48 mounted frames with10-image per-frame union. All nonbackground pixels, no alignment, alpha suppression, timing, geometry or camera changes. Means from distinct unions are not directly comparable.',
    sourceFramesStateTextureVersionsMatricesExact=True,originalCallbacks=[r['callbacks'] for r in rows],
    existingNativeHashesUnchanged=len(hashes),sourceTransparentPremultipliedCanvas=True,
    entrance=rows,matrix=matrix,
    layer8={k:summarize(matrix,k,k+'8') for k in ['ordinary','base','media']},
    layerF16={k:summarize(matrix,k,k+'F16') for k in ['ordinary','base','media']},
    productionChanged=False,formalVisualAcceptance=False,deviceValidated=False,performanceMeasured=False,
    conclusion='Original additive RGB and alpha accumulate in a transparent canvas before CSS composition; current direct Plus skips background attenuation. Ordinary8-bit saveLayer introduces quantization regressions, rejected. F16 control removes most entrance bright errors but small source/raster/rounding residuals remain; full48 failures retained. No production integration based on aggregate mean alone.',
    failedRun='Expanded wide diagnostic exceeded kotlinx-coroutines-test60s timeout after adding duplicate8-bit/F16 matrix captures. Assertion/stack retained in failed-run.json; rejected8-bit capture removed from ongoing JVM suite, source/failed comparison retained. Subsequent two-test run must pass before commit.')
(out/'comparison.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf8')
sheet=Image.new('RGB',(1200,4*170),'#191919');draw=ImageDraw.Draw(sheet)
for row,c in enumerate(source['captures']):
    prefix=f"{c['width']}-{c['fps']}"
    for col,(label,p) in enumerate([
        ('original',b/'diorama-mounted-entrance-browser'/f'{prefix}-all.png'),
        ('direct',b/'diorama-mounted-entrance-kotlin'/f'{prefix}-after-all.png'),
        ('8-bit layer rejected',b/'diorama-mounted-entrance-kotlin'/f'{prefix}-grouped-after-all.png'),
        ('F16 diagnostic',b/'diorama-mounted-entrance-kotlin'/f'{prefix}-float-after-all.png')]):
        x=128 if c['width']==380 else 320
        im=Image.open(p).convert('RGB').crop((x,0,x+160,64)).resize((300,120),Image.Resampling.NEAREST)
        draw.text((col*300+3,row*170+3),prefix+' '+label,fill='white');sheet.paste(im,(col*300,row*170+23))
sheet.save(out/'contact.png')
print(json.dumps({k:report[k] for k in ['layer8','layerF16','existingNativeHashesUnchanged']},indent=2))
