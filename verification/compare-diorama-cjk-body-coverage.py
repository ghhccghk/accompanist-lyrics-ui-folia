"""Body-only source display-glyph coverage; same matrices and unchanged halos."""
from pathlib import Path
import hashlib,json
import numpy as np
from PIL import Image,ImageDraw
root=Path(__file__).resolve().parents[1]
old=root/'.work/diorama-cjk-coverage-before-5a6bcb0'
out=root/'verification/diorama-cjk-body-coverage';out.mkdir(exist_ok=True)
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
frozen=json.loads((old/'hashes.json').read_text())
for file,h in frozen.items():assert sha(old/file)==h,'Preserved baseline changed: '+file
reference=json.loads((root/'verification/diorama-rtl-registration/comparison.json').read_text())
bg=[22,36,53]

def compare(files):
    a={k:np.array(Image.open(p).convert('RGB'),dtype=np.int32) for k,p in files.items()}
    u=np.logical_or.reduce([np.any(v!=bg,axis=2) for v in a.values()]);metrics={}
    for k,v in a.items():
        d=abs(v-a['original']);mask=np.any(v!=bg,axis=2);ys,xs=np.where(mask)
        local=[dict(x=x,y=y,mae=float(d[y:y+64,x:x+64][u[y:y+64,x:x+64]].mean()))
               for y in range(0,v.shape[0],64) for x in range(0,v.shape[1],64) if u[y:y+64,x:x+64].any()]
        metrics[k]=dict(mae=float(d[u].mean()),peak=int(d.max()),worst64=max(local,key=lambda r:r['mae']),
            pixels=int(mask.sum()),bounds=[int(xs.min()),int(ys.min()),int(xs.max()+1),int(ys.max()+1)] if xs.size else None)
    return dict(unionPixels=int(u.sum()),metrics=metrics,hashes={k:sha(p) for k,p in files.items()})

matrices={}
for matrix,items,folder in [('base',reference['rows'],'diorama-active-base'),('layers',reference['layerRows'],'diorama-active-layers')]:
    rows=[]
    for item in items:
        name=item['file'];files=dict(original=root/f'build/verification/{folder}-browser'/name,
            beforeOrdinary=old/f'{folder}-kotlin'/name,beforeSampler=old/f'{folder}-sampler-kotlin'/name,
            afterOrdinary=root/f'build/verification/{folder}-kotlin'/name,afterSampler=root/f'build/verification/{folder}-sampler-kotlin'/name)
        assert sha(files['original'])==item['hashes']['original']
        if not name.startswith('cjk'):
            for kind in ['Ordinary','Sampler']:assert sha(files['before'+kind])==sha(files['after'+kind]),'Non-CJK path changed: '+name
        rows.append(dict(file=name,**compare(files)))
    matrices[matrix]=rows

def summary(rows,before,after):
    return dict(means={k:float(np.mean([r['metrics'][k]['mae'] for r in rows])) for k in [before,after]},
        worst64={k:max(r['metrics'][k]['worst64']['mae'] for r in rows) for k in [before,after]},
        meanRegressions=[dict(file=r['file'],before=r['metrics'][before]['mae'],after=r['metrics'][after]['mae']) for r in rows if r['metrics'][after]['mae']>r['metrics'][before]['mae']],
        localRegressions=[dict(file=r['file'],before=r['metrics'][before]['worst64'],after=r['metrics'][after]['worst64']) for r in rows if r['metrics'][after]['worst64']['mae']>r['metrics'][before]['worst64']['mae']])

halo_checks=[]
for i in range(7):
    for suffix in ['base','halo']:
        file=f'diorama-active-layers-kotlin/cjk-unit{i}-{suffix}.png'
        assert sha(root/'build/verification'/file)==frozen[file], 'Grayscale/soul or halo input changed'
        halo_checks.append(dict(file=file,sha256=frozen[file]))
    assert sha(root/f'build/verification/diorama-cjk-body-coverage/0-unit-{i}-gray.png')==frozen[f'diorama-active-layers-kotlin/cjk-unit{i}-base.png']
    assert sha(root/f'build/verification/diorama-cjk-body-coverage/0-unit-{i}-halo.png')==frozen[f'diorama-active-layers-kotlin/cjk-unit{i}-halo.png']
for f in (old/'diorama-shaped-mask').glob('*.png'):
    assert sha(root/'build/verification/diorama-shaped-mask'/f.name)==sha(f),'Exact unsafe shaping input changed: '+f.name

summary_result={matrix:{kind:summary(rows,'before'+kind,'after'+kind) for kind in ['Ordinary','Sampler']} for matrix,rows in matrices.items()}
name='cjk-800-active-1100.png'
failure=next(r for r in matrices['layers'] if r['file']==name)
report=dict(baseline='5a6bcb0',sourceRevision='cc6226e464050a456a2913e062374f23d3844cc5',
    condition='Same original90 base/180 complete-layer captures, same source fonts/viewport/DPR/config/times. FIVE-image per-frame original/beforeOrdinary/beforeSampler/afterOrdinary/afterSampler foreground union. No altered original/time/geometry/registration/threshold.',
    summary=summary_result,rows=matrices,grayAndHaloInputHashes=halo_checks,
    unsafeShapingMasksExact=True,geometryAndClockUnchanged=True,
    formerFailureFrame=failure,
    allocation='Body differs only where existing Windows host raster policy applies; one extra body bitmap per independent display unit. Original gray raster/halo retained; other platforms/style overrides keep shared image. Extra allocation is not PSS/performance improvement.',
    formalVisualAcceptance=False,deviceValidated=False,performanceMeasured=False)
(out/'comparison.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf8')
names=['cjk-800-active-1100.png','cjk-801-active-1500.png','cjk-800-trail-1900.png','cjk-801-active-3450.png']
sheet=Image.new('RGB',(1600,4*300),'#191919');draw=ImageDraw.Draw(sheet)
for i,name in enumerate(names):
    for j,(label,path) in enumerate([('original',root/'build/verification/diorama-active-layers-browser'/name),
        ('old sampler',old/'diorama-active-layers-sampler-kotlin'/name),('body coverage ordinary',root/'build/verification/diorama-active-layers-kotlin'/name),
        ('body coverage sampler',root/'build/verification/diorama-active-layers-sampler-kotlin'/name)]):
        im=Image.open(path).convert('RGB').crop((300,230,560,385)).resize((390,232),Image.Resampling.NEAREST)
        draw.text((j*400+3,i*300+3),label+' '+name,fill='white');sheet.paste(im,(j*400,i*300+25))
sheet.save(out/'contact.png')
print(json.dumps(summary_result,indent=2))
