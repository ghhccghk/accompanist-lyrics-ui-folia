"""Current-line BASE fragment sampler; same fixed originals and shared unions."""
from pathlib import Path
import hashlib,json
import numpy as np
from PIL import Image,ImageDraw

root=Path(__file__).resolve().parents[1]
out=root/'verification/diorama-active-base-sampler';out.mkdir(exist_ok=True)
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
frozen=json.loads((root/'.work/diorama-active-before-baeef5a/hashes.json').read_text())
for file,h in frozen.items():assert sha(root/file)==h,'Ordinary/control image changed: '+file
previous=json.loads((root/'verification/diorama-plain-sampler/comparison.json').read_text())
source=json.loads((root/'verification/diorama-fixtures/mounted-cut-v1/scene.json').read_text())
old_base=json.loads((root/'verification/diorama-active-base/comparison.json').read_text())
shared_inputs=json.loads((root/'verification/diorama-fixtures/active-base-sampler-input-v1/conditions.json').read_text())
for file,h in shared_inputs['files'].items():assert sha(root/file)==h,'Original shared input changed: '+file
bg=[22,36,53]

def compare(files):
    images={k:np.array(Image.open(p).convert('RGB'),dtype=np.int32) for k,p in files.items()}
    union=np.logical_or.reduce([np.any(a!=bg,axis=2) for a in images.values()])
    metrics={}
    for k,a in images.items():
        d=abs(a-images['original']);mask=np.any(a!=bg,axis=2);ys,xs=np.where(mask)
        local=[dict(x=x,y=y,mae=float(d[y:y+64,x:x+64][union[y:y+64,x:x+64]].mean()))
               for y in range(0,a.shape[0],64) for x in range(0,a.shape[1],64) if union[y:y+64,x:x+64].any()]
        metrics[k]=dict(mae=float(d[union].mean()) if union.any() else 0,peak=int(d.max()),
                        worst64=max(local,key=lambda r:r['mae']) if local else dict(x=0,y=0,mae=0),pixels=int(xs.size),
                        bounds=[int(xs.min()),int(ys.min()),int(xs.max()+1),int(ys.max()+1)] if xs.size else None,
                        touchesEdge=bool(mask[0].any() or mask[-1].any() or mask[:,0].any() or mask[:,-1].any()))
    return dict(unionPixels=int(union.sum()),metrics=metrics,hashes={k:sha(p) for k,p in files.items()})

rows=[]
for item in source['captures']:
    name=item['file'];old=next(r for r in previous['rows'] if r['file']==name)
    files=dict(original=root/'build/verification/diorama-mounted-browser'/name,
               ordinaryFinite=root/'build/verification/diorama-mounted-kotlin'/('material-'+name),
               beforeFinite=root/'build/verification/diorama-mounted-sampler-kotlin'/name,
               afterFinite=root/'build/verification/diorama-mounted-active-base-kotlin'/name,
               ordinaryMedia=root/'build/verification/diorama-mounted-kotlin'/('media-'+name),
               beforeMedia=root/'build/verification/diorama-mounted-sampler-kotlin'/('media-'+name),
               afterMedia=root/'build/verification/diorama-mounted-active-base-kotlin'/('media-'+name))
    for k,old_key in [('original','original'),('beforeFinite','afterFinite'),('beforeMedia','afterMedia')]:
        assert sha(files[k])==old['hashes'][old_key],name+' previous input changed'
    rows.append(dict(file=name,**compare(files)))

base=[]
shared=[]
for item in old_base['frames']:
    name=item['file']
    files=dict(original=root/'build/verification/diorama-active-base-browser'/name,
               before=root/'build/verification/diorama-active-base-kotlin'/name,
               after=root/'build/verification/diorama-active-base-sampler-kotlin'/name)
    expected=next(v for k,v in item['png_sha256'].items() if 'browser' in k)
    assert sha(files['original'])==expected,'Original isolated frame changed'
    base.append(dict(file=name,**compare(files)))
    shared.append(dict(file=name,**compare(dict(original=files['original'],sharedSource=root/'build/verification/diorama-active-base-shared-kotlin'/name))))

def summary(items,before,after):
    return dict(means={k:float(np.mean([r['metrics'][k]['mae'] for r in items])) for k in [before,after]},
                worst64={k:max(r['metrics'][k]['worst64']['mae'] for r in items) for k in [before,after]},
                meanRegressions=[dict(file=r['file'],before=r['metrics'][before]['mae'],after=r['metrics'][after]['mae'])
                                 for r in items if r['metrics'][after]['mae']>r['metrics'][before]['mae']],
                localRegressions=[dict(file=r['file'],before=r['metrics'][before]['worst64'],after=r['metrics'][after]['worst64'])
                                  for r in items if r['metrics'][after]['worst64']['mae']>r['metrics'][before]['worst64']['mae']])

active=compare(dict(original=root/'build/verification/diorama-mounted-active-input-browser/active-all.png',
                    before=root/'build/verification/diorama-mounted-active-input-kotlin/active-all.png',
                    after=root/'build/verification/diorama-mounted-active-base-kotlin/active-only.png'))
report=dict(baseline='baeef5a',sourceRevision=source['revision'],
    condition='Same frozen48 complete mounted frames, own core/full shaping/camera/material. Current BASE sampler only; existing plain sampler unchanged, halo/soul remain ordinary additive. All7-image per-frame foreground union; isolated90 uses3-image union. No time/LOD fit, registration, alpha threshold or changed expectations.',
    finite=summary(rows,'beforeFinite','afterFinite'),media=summary(rows,'beforeMedia','afterMedia'),
    ownShapedBase=summary(base,'before','after'),activeOnlyWorst=active,rows=rows,ownShapedRows=base,
    sharedSourceDiagnostic=dict(mean=float(np.mean([r['metrics']['sharedSource']['mae'] for r in shared])),
        peak=max(r['metrics']['sharedSource']['peak'] for r in shared),
        worst64=max(r['metrics']['sharedSource']['worst64']['mae'] for r in shared),rows=shared,
        scope='Diagnostic original textures AND original captured placements/materials; NEVER used by actual library stage. Isolates sampler from own full-shaping/fractional raster phase.'),
    unchangedOldImages=len(frozen),allocation='Explicit current line only, <=32 images/32MiB estimated additional payload; real fallback, eviction and close. No preparation inside Canvas.',
    platforms=dict(jvm='Internal opt-in',android='Unavailable: ordinary real drawing'),
    formalVisualAcceptance=False,deviceValidated=False,performanceMeasured=False)
(out/'comparison.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf8')
names=['mounted-380-144-3500.png','mounted-800-60-2100.png','mounted-380-60-3200.png','mounted-800-60-3199.png','mounted-800-60-4600.png']
sheet=Image.new('RGB',(1600,len(names)*325+420),'#191919');draw=ImageDraw.Draw(sheet)
for i,name in enumerate(names):
    for j,(label,folder,file) in enumerate([('original','diorama-mounted-browser',name),('plain only','diorama-mounted-sampler-kotlin',name),('plus active BASE','diorama-mounted-active-base-kotlin',name),('media BASE','diorama-mounted-active-base-kotlin','media-'+name)]):
        im=Image.open(root/'build/verification'/folder/file).convert('RGB');im.thumbnail((400,300),Image.Resampling.LANCZOS)
        draw.text((j*400+3,i*325+3),label+' '+name,fill='white');sheet.paste(im,(j*400,i*325+23))
for j,(label,folder) in enumerate([('original','diorama-mounted-active-input-browser'),('before','diorama-mounted-active-input-kotlin'),('after BASE','diorama-mounted-active-base-kotlin')]):
    file='active-only.png' if j==2 else 'active-all.png'
    im=Image.open(root/'build/verification'/folder/file).convert('RGB').crop((0,310,190,375)).resize((760,260),Image.Resampling.NEAREST)
    im.thumbnail((520,300),Image.Resampling.NEAREST)
    y=len(names)*325;draw.text((j*530+3,y+3),label+' active-only fixed crop',fill='white');sheet.paste(im,(j*530,y+25))
sheet.save(out/'contact.png')
print(json.dumps({k:report[k] for k in ['finite','media','ownShapedBase','activeOnlyWorst','unchangedOldImages']},indent=2))
print('Shared input:',json.dumps({k:v for k,v in report['sharedSourceDiagnostic'].items() if k!='rows'}))
