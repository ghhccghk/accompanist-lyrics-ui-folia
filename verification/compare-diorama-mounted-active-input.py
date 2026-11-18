"""Actual original worst-frame active planes versus own input and stage."""
from pathlib import Path
import hashlib,json
import numpy as np
from PIL import Image,ImageDraw

root=Path(__file__).resolve().parents[1]
fixture=root/'verification/diorama-fixtures/mounted-active-input-v1/scene.json'
scene=json.loads(fixture.read_text(encoding='utf8'))
source=root/'build/verification/diorama-mounted-active-input-browser'
native=root/'build/verification/diorama-mounted-active-input-kotlin'
coverage=root/'build/verification/diorama-active-coverage-kotlin'
out=root/'verification/diorama-mounted-active-input';out.mkdir(exist_ok=True)
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
bg=[22,36,53]
for x in scene['inputs']+scene['layers']:assert sha(source/x['file'])==x['sha256']
assert sha(source/'original-before.png')==sha(source/'original-restored.png')==scene['expectedPngSha256']
load=lambda p:np.array(Image.open(p).convert('RGB'),dtype=np.int32)
layers={x['kind']:load(source/x['file']) for x in scene['layers']}
assert np.array_equal(layers['all'],layers['base'])
assert all(np.all(layers[k]==bg) for k in ['glow','soul'])
assert all(not x['visible'] for x in scene['inputs'] if x['order'] in [17,18])
assert all(x['opacity']==.5 for x in scene['inputs'] if x['order']==19)
n=load(native/'active-all.png');m=np.any(layers['all']!=bg,axis=2)|np.any(n!=bg,axis=2)
d=abs(layers['all']-n)
local=[dict(x=x,y=y,mae=float(d[y:y+64,x:x+64][m[y:y+64,x:x+64]].mean()))
       for y in range(0,600,64) for x in range(0,380,64) if m[y:y+64,x:x+64].any()]
alpha_rows=[]
for item in [x for x in scene['inputs'] if x['order']==19]:
    i=item['unit'];files=dict(original=source/item['file'],default=coverage/f'default-unit-{i}-body.png',foreground=coverage/f'foreground-unit-{i}-body.png')
    assert sha(files['default'])==sha(native/f'unit-{i}-body.png'),'Input-only default must match actual internal stage raster'
    raw={k:np.array(Image.open(p).convert('RGBA'))[:,:,3].astype(np.int32) for k,p in files.items()}
    shape=tuple(max(a.shape[axis] for a in raw.values()) for axis in [0,1])
    padded={}
    for k,a in raw.items():
        p=np.zeros(shape,dtype=np.int32);p[:a.shape[0],:a.shape[1]]=a;padded[k]=p
    union=np.logical_or.reduce([a!=0 for a in padded.values()])
    metrics={}
    for k,a in padded.items():
        e=abs(a-padded['original'])
        metrics[k]=dict(rawSize=list(raw[k].shape[::-1]),alphaUnionMae=float(e[union].mean()),alphaPeak=int(e.max()),alphaMass=int(a.sum()))
    alpha_rows.append(dict(unit=i,comparison='Raw texture coordinates at same zero origin; unequal extents zero-extended only, no shift/registration. Not a world-space shaping equivalence oracle.',metrics=metrics,hashes={k:sha(p) for k,p in files.items()}))
report=dict(baseline='bb84afd',sourceRevision=scene['revision'],browser=scene['browser'],motion=scene['motion'],
            condition='Actual mounted380x600/DPR1/144Hz/incoming3500ms, EXACT original1494 callback prefix; own core/finite camera/material. No original pose/raster drives native stage.',
            sourceOriginalFullFrameExact=True,sourceStateTextureVersionsMatricesExact=True,sourceCallbacks=scene['callbacks'],teardown=scene['teardown'],
            sourceActiveAllEqualsBase=True,sourceGlowSoulBackgroundOnly=True,sourceBodyOpacity=.5,
            activeOnly=dict(sourcePixels=int(np.any(layers['all']!=bg,axis=2).sum()),nativePixels=int(np.any(n!=bg,axis=2).sum()),unionPixels=int(m.sum()),mae=float(d[m].mean()),peak=int(d.max()),worst64=max(local,key=lambda x:x['mae'])),
            nativeFullShapeUnitInputs=(native/'inputs.tsv').read_text(),rawAlphaControls=alpha_rows,
            foregroundPolicyAdopted=False,
            conclusion='The worst finite frame has no visible original glow/soul: do not tune halos to repair it. Base alpha is already correct. Existing Windows foreground policy improves raw unit0 but mean-regresses units1..3; do not adopt it globally on that input-only result. Test active base fragment sampling against this exact layer, then fixed48 full frames; preserve full shaping/fractional input differences.',
            productionChanged=False,formalVisualAcceptance=False,deviceValidated=False)
(out/'comparison.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf8')
sheet=Image.new('RGB',(1000,650),'#191919');draw=ImageDraw.Draw(sheet)
for j,(label,a) in enumerate([('original actual active',layers['all']),('native actual active',n)]):
    draw.text((j*500+5,5),label,fill='white')
    im=Image.fromarray(a.astype(np.uint8)).crop((0,310,180,365)).resize((360,110),Image.Resampling.NEAREST)
    sheet.paste(im,(j*500+5,25))
for row,i in enumerate([0,3]):
    item=next(x for x in scene['inputs'] if x['unit']==i and x['order']==19)
    for j,(label,path) in enumerate([('original',source/item['file']),('default',coverage/f'default-unit-{i}-body.png'),('foreground diagnostic',coverage/f'foreground-unit-{i}-body.png')]):
        im=Image.open(path).convert('RGBA').getchannel('A').convert('RGB')
        # Same raw crop, no alignment; presentation only, never metrics.
        im=im.crop((80,100,340,270))
        x=j*330;y=170+row*240
        draw.text((x+5,y),f'unit{i} '+label,fill='white');sheet.paste(im,(x+5,y+20))
sheet.save(out/'contact.png')
print(json.dumps({k:report[k] for k in ['sourceCallbacks','sourceActiveAllEqualsBase','sourceGlowSoulBackgroundOnly','activeOnly','rawAlphaControls']},indent=2))
