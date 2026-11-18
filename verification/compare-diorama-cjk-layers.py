"""Frozen CJK failure decomposition; unmodified source full frame retained."""
from pathlib import Path
import hashlib,json
import numpy as np
from PIL import Image,ImageDraw

root=Path(__file__).resolve().parents[1]
fixture=root/'verification/diorama-fixtures/active-cjk-layers-v1/scene.json'
scene=json.loads(fixture.read_text(encoding='utf8'))
source=root/'build/verification/diorama-cjk-layers-browser'
native=root/'build/verification/diorama-cjk-layers-kotlin'
out=root/'verification/diorama-cjk-layer-isolation';out.mkdir(exist_ok=True)
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
load=lambda p:np.array(Image.open(p).convert('RGB'),dtype=np.int32)
assert sha(source/'original.png')==scene['expectedOriginalSha256']==sha(source/'all.png')
assert sha(source/'base.png')==sha(root/'build/verification/diorama-active-base-browser/cjk-800-1100.png')
for item in scene['layers']:assert sha(source/item['file'])==item['sha256']
rows=[];bg=[22,36,53]
for layer in ['all','base','glow','soul']:
    files=dict(original=source/f'{layer}.png',ordinary=native/f'ordinary-{layer}.png',sampled=native/f'sampled-{layer}.png')
    a={k:load(p) for k,p in files.items()};u=np.logical_or.reduce([np.any(v!=bg,axis=2) for v in a.values()])
    metrics={}
    for k,v in a.items():
        d=abs(v-a['original']);mask=np.any(v!=bg,axis=2);ys,xs=np.where(mask)
        tiles=[dict(x=x,y=y,mae=float(d[y:y+64,x:x+64][u[y:y+64,x:x+64]].mean()))
               for y in range(0,600,64) for x in range(0,800,64) if u[y:y+64,x:x+64].any()]
        metrics[k]=dict(mae=float(d[u].mean()),peak=int(d.max()),worst64=max(tiles,key=lambda r:r['mae']),
            pixels=int(mask.sum()),bounds=[int(xs.min()),int(ys.min()),int(xs.max()+1),int(ys.max()+1)] if xs.size else None)
    rows.append(dict(layer=layer,unionPixels=int(u.sum()),metrics=metrics,hashes={k:sha(p) for k,p in files.items()}))

# Failure ROI selected by the existing full-stage comparator BEFORE isolating.
region=(384,320,448,384);x,y,right,bottom=region
images={layer:{kind:load(source/f'{layer}.png' if kind=='original' else native/f'{kind}-{layer}.png')
               for kind in ['original','ordinary','sampled']} for layer in ['all','base','glow','soul']}
union=np.logical_or.reduce([np.any(v[y:bottom,x:right]!=bg,axis=2) for v in images['all'].values()])
roi={}
for layer,a in images.items():
    roi[layer]={kind:float(abs(v[y:bottom,x:right]-a['original'][y:bottom,x:right])[union].mean()) for kind,v in a.items()}
    if layer in ['glow','soul']:
        assert np.array_equal(a['ordinary'],a['sampled'])
        assert all(np.all(v[y:bottom,x:right]==bg) for v in a.values()),'Failure tile has an additive contribution'
for kind in ['original','ordinary','sampled']:
    assert np.array_equal(images['all'][kind][y:bottom,x:right],images['base'][kind][y:bottom,x:right])
assert roi['base']['ordinary']<roi['base']['sampled']
report=dict(baseline='13b8518',sourceRevision=scene['revision'],browser=scene['browser'],motion=scene['motion'],three=scene['three'],
    condition='Same original CJK800x600/DPR1/1100ms/soulActive=true isolated stage, analytic Float32 envelope injection/delta0 retained. Passive current-material read and clone layers only; NOT original mounted RAF/CameraRig.',
    originalFullFrameAndInputsVersionsExact=True,sourceBaseEqualsExistingOriginalBase=True,
    nativeAllReplaysExactlyMatchExistingFullStage=True,rows=rows,
    existingFailureRoi=dict(rect=list(region),unionPixels=int(union.sum()),metrics=roi,
        allEqualsBaseForAllThreeImages=True,allGlowSoulRoiPixelsBackground=True),
    conclusion='The retained local regression is on unsung base ink, not halo/soul compositing: both additive layers have zero contribution in the preselected failing tile. Base global local maximum improves but this one base tile worsens and becomes full-stage worst. Inspect source versus native glyph phase/ownership/placement, including CJK ceil pivot; do not tune halo or change unions to hide it.',
    release=scene['release'],productionChanged=False,formalVisualAcceptance=False,deviceValidated=False)
(out/'comparison.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf8')
sheet=Image.new('RGB',(1200,4*260),'#191919');draw=ImageDraw.Draw(sheet)
for i,layer in enumerate(['all','base','glow','soul']):
    for j,kind in enumerate(['original','ordinary','sampled']):
        im=Image.fromarray(images[layer][kind].astype(np.uint8)).crop((320,275,500,365)).resize((360,180),Image.Resampling.NEAREST)
        draw.text((j*400+4,i*260+3),layer+' '+kind+' fixed crop(320,275)-(500,365)',fill='white');sheet.paste(im,(j*400,i*260+25))
sheet.save(out/'contact.png')
print(json.dumps(report,indent=2))
