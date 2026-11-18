"""Actual original active EGC fades; one fixed source/before/after alpha union."""
from pathlib import Path
import json,hashlib
import numpy as np
from PIL import Image,ImageDraw

root=Path(__file__).resolve().parents[1]
source=root/'build/verification/cappella-active-browser'
before=root/'.work/cappella-active-ink-before'
after=root/'build/verification/cappella-active-ink'
out=root/'verification/cappella-active-ink';out.mkdir(exist_ok=True)
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
conditions=json.loads((root/'verification/cappella-fixtures/active-ink-v1/conditions.json').read_text(encoding='utf-8'))
records=[]
for c in conditions['observations']:
    name=c['file'];paths=[source/name,before/name,after/name]
    arrays=[np.asarray(Image.open(p).convert('RGBA')).astype(np.float64) for p in paths]
    mask=np.logical_or.reduce([a[:,:,3]>0 for a in arrays])
    premul=[np.concatenate((a[:,:,:3]*(a[:,:,3:]/255),a[:,:,3:]),axis=2) for a in arrays]
    row={'file':name,'unionPixels':int(mask.sum()),'hashes':[sha(p) for p in paths]}
    for label,a in zip(('before','after'),premul[1:]):
        d=abs(premul[0]-a)
        row[label]={'foregroundMAE':float(d[mask].mean()) if mask.any() else 0,'peak':float(d.max()),
            'worst64':max((float(d[y:y+64,x:x+64][mask[y:y+64,x:x+64]].mean())
                for y in range(0,300,64) for x in range(0,800,64) if mask[y:y+64,x:x+64].any()),default=0)}
    if c['uniform'] or c['time']>1:
        actual=np.asarray(Image.open(after/name));reference=np.asarray(Image.open(after/('reference-'+name)))
        assert np.array_equal(actual,reference),f'{name}: incomplete shaped coverage'
        row['uniformDifferentPixels']=0
    records.append(row)
assert len(records)==30
report={'conditions':conditions,'scope':'Active EGC ink ownership only; source count/CSS alpha kept, complete shaping preserved. Not full theme projection/RAF/Android/device acceptance.',
    'comparison':'Premultiplied RGBA; one unchanged original/before/after all-alpha>0 union. No phase/pixel registration or discarded samples.',
    'beforeMean':sum(r['before']['foregroundMAE'] for r in records)/30,'afterMean':sum(r['after']['foregroundMAE'] for r in records)/30,
    'beforeWorst64':max(r['before']['worst64'] for r in records),'afterWorst64':max(r['after']['worst64'] for r in records),
    'beforeCoverage':(before/'coverage.tsv').read_text(),'afterCoverage':(after/'coverage.tsv').read_text(),'frames':records}
(out/'comparison.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
names=[f'{c}-{m}-{t}.png' for c in ('italic','arabic','mixed','latin','cjk') for m,t in [('stagger',650),('uniform',500)]]
sheet=Image.new('RGB',(1200,len(names)*130),'#162435');draw=ImageDraw.Draw(sheet)
for i,name in enumerate(names):
    for j,(label,folder) in enumerate([('Original ActiveCappellaText',source),('Before',before),('After',after)]):
        image=Image.open(folder/name).convert('RGBA');bg=Image.new('RGBA',image.size,'#162435')
        image=Image.alpha_composite(bg,image).convert('RGB').crop((0,0,800,200));image.thumbnail((400,100))
        sheet.paste(image,(j*400,i*130+24));draw.text((j*400+3,i*130+3),f'{label} | {name}',fill='white')
sheet.save(out/'contact.png')
print(json.dumps({k:report[k] for k in ('beforeMean','afterMean','beforeWorst64','afterWorst64')}))
