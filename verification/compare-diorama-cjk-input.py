"""Fixed base input crosses; no source data enters product."""
from pathlib import Path
import hashlib,json
import numpy as np
from PIL import Image,ImageDraw
root=Path(__file__).resolve().parents[1]
native=root/'build/verification/diorama-cjk-input-kotlin'
out=root/'verification/diorama-cjk-input-isolation';out.mkdir(exist_ok=True)
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
source=root/'build/verification/diorama-cjk-layers-browser/base.png'
layer=json.loads((root/'verification/diorama-cjk-layer-isolation/comparison.json').read_text())
assert sha(source)==next(r for r in layer['rows'] if r['layer']=='base')['hashes']['original']
files=dict(original=source,**{m:native/f'{m}.png' for m in ['own-own','source-own','own-source','source-source','foreground-own']})
a={k:np.array(Image.open(p).convert('RGB'),dtype=np.int32) for k,p in files.items()}
u=np.logical_or.reduce([np.any(v!=[22,36,53],axis=2) for v in a.values()]);metrics={}
for k,v in a.items():
    d=abs(v-a['original']);tiles=[dict(x=x,y=y,mae=float(d[y:y+64,x:x+64][u[y:y+64,x:x+64]].mean()))
        for y in range(0,600,64) for x in range(0,800,64) if u[y:y+64,x:x+64].any()]
    metrics[k]=dict(mae=float(d[u].mean()),peak=int(d.max()),worst64=max(tiles,key=lambda r:r['mae']),
        existingFailureRoi=float(d[320:384,384:448][u[320:384,384:448]].mean()))
raw=[]
for i in range(7):
    p=dict(original=root/f'build/verification/diorama-active-layers-browser/cjk-unit{i}-base.png',
        own=native/f'own-unit-{i}.png',foreground=native/f'foreground-unit-{i}.png')
    alpha={k:np.array(Image.open(f).convert('RGBA'))[:,:,3].astype(np.int32) for k,f in p.items()}
    shape=tuple(max(v.shape[j] for v in alpha.values()) for j in range(2));padded={}
    for k,v in alpha.items():
        x=np.zeros(shape,dtype=np.int32);x[:v.shape[0],:v.shape[1]]=v;padded[k]=x
    union=np.logical_or.reduce([v>0 for v in padded.values()]);m={}
    for k,v in padded.items():
        d=abs(v-padded['original']);m[k]=dict(mae=float(d[union].mean()),peak=int(d.max()),mass=int(v.sum()),size=list(alpha[k].shape[::-1]))
    raw.append(dict(unit=i,metrics=m,hashes={k:sha(f) for k,f in p.items()}))
report=dict(baseline='08aaa09',sourceRevision='cc6226e464050a456a2913e062374f23d3844cc5',
    condition='Frozen original CJK800x600/DPR1/1100ms BASE. All6 framebuffer inputs share one union. Raw alpha uses same-zero-origin transparent extension only, no registration. Original placement AND texture diagnostic explicitly source-driven; never actual library input.',
    unionPixels=int(u.sum()),metrics=metrics,hashes={k:sha(p) for k,p in files.items()},rawAlpha=raw,
    geometry=(native/'inputs.tsv').read_text(),foregroundPolicyAdopted=False,
    conclusion='Pure source/source peak6 versus own75 isolates remaining own input/placement difference. Swapping only source placement worsens; changing only coverage to existing Windows foreground policy improves all7 raw alpha means/peaks and frozen stage. Keep geometry/phase intact. Test a body-only CJK coverage path across fixed matrices, retaining grayscale halo inputs and full shaping; no claim that this single diagnostic validates a production policy.',
    productionChanged=False,formalVisualAcceptance=False,deviceValidated=False)
(out/'comparison.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf8')
sheet=Image.new('RGB',(1200,600),'#191919');draw=ImageDraw.Draw(sheet)
for i,(k,v) in enumerate(a.items()):
    im=Image.fromarray(v.astype(np.uint8)).crop((320,275,500,365)).resize((360,180),Image.Resampling.NEAREST)
    x=i%3*400;y=i//3*300;draw.text((x+3,y+3),k+' same fixed crop',fill='white');sheet.paste(im,(x,y+25))
sheet.save(out/'contact.png')
print(json.dumps(report,indent=2))
