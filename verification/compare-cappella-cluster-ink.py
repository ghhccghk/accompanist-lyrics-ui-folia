"""Independent original span ownership and unchanged 30-frame active-text matrix."""
from pathlib import Path
from PIL import Image, ImageDraw
import argparse, base64, csv, hashlib, json
import numpy as np

root=Path(__file__).resolve().parents[1]
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--export-source', action='store_true')
args=parser.parse_args()
fixture=root/'verification/cappella-fixtures/cluster-ink-v1'
conditions=json.loads((fixture/'conditions.json').read_text(encoding='utf-8'))
source=root/'build/verification/cappella-cluster-browser'
def pixels(path): return np.asarray(Image.open(path).convert('RGBA'))
def sha(path): return hashlib.sha256(path.read_bytes()).hexdigest()
if args.export_source:
    target=fixture/'paint.tsv'
    if target.exists(): raise RuntimeError('Preserve original ownership table; refusing overwrite')
    with target.open('w',encoding='utf-8',newline='') as f:
        writer=csv.writer(f, delimiter='\t',lineterminator='\n')
        writer.writerow(['file','case','owner','font','italic','base64Text','entireRow','blank','pixels'])
        for r in conditions['observations']:
            p=source/r['file'];assert sha(p)==r['sha256']
            a=pixels(p);whole=pixels(source/(r['name']+'-all.png'))
            writer.writerow([r['file'],r['name'],r['owner'],r['font'],str(r['italic']).lower(),
                base64.b64encode(r['text'].encode()).decode(),str(np.array_equal(a,whole)).lower(),
                str(not a[:,:,3].any()).lower(),int((a[:,:,3]>0).sum())])
    print('Original span-paint equality/blank controls exported; no native data used')
    raise SystemExit
out=root/'verification/cappella-cluster-ink';out.mkdir(exist_ok=True)
native=root/'build/verification/cappella-cluster-ink'
old=root/'.work/cappella-cluster-controls-before'
def metric(paths):
    arrays=[pixels(p).astype(np.float64) for p in paths]
    mask=np.logical_or.reduce([a[:,:,3]>0 for a in arrays])
    premul=[np.concatenate([a[:,:,:3]*a[:,:,3:]/255,a[:,:,3:]],axis=2) for a in arrays]
    result=[]
    for a in premul[1:]:
        d=abs(premul[0]-a)
        result.append({'MAE':float(d[mask].mean()) if mask.any() else 0,
            'worst64':max((float(d[y:y+64,x:x+64][mask[y:y+64,x:x+64]].mean())
                for y in range(0,300,64) for x in range(0,800,64) if mask[y:y+64,x:x+64].any()),default=0)})
    return {'hashes':[sha(p) for p in paths],'unionPixels':int(mask.sum()),'before':result[0],'after':result[1]}
records=[]
for r in conditions['observations']:
    file=r['file'];records.append({'file':file,**metric([source/file,old/file,native/file])})
activeSource=root/'build/verification/cappella-active-browser'
activeBefore=root/'.work/cappella-cluster-ink-before'
activeAfter=root/'build/verification/cappella-active-ink'
active=json.loads((root/'verification/cappella-fixtures/active-ink-v1/conditions.json').read_text(encoding='utf-8'))
activeRecords=[{'file':r['file'],**metric([activeSource/r['file'],activeBefore/r['file'],activeAfter/r['file']])} for r in active['observations']]
report={'sourceConditions':conditions,'activeSourceConditionsSHA256':sha(root/'verification/cappella-fixtures/active-ink-v1/conditions.json'),
    'comparison':'Same original/before/after union of every nonzero-alpha pixel; premultiplied RGBA; no fitting, discarded pixels or phase changes.',
    'controls':records,'active30':activeRecords,'scope':'Original active text and span paint, not full-stage Motion projection/Android-device/performance acceptance.'}
for label,rows in [('controls',records),('active30',activeRecords)]:
    report[label+'Summary']={phase:{'MAE':sum(r[phase]['MAE'] for r in rows)/len(rows),
        'worst64':max(r[phase]['worst64'] for r in rows)} for phase in ['before','after']}
(out/'comparison.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
names=[('fi-italic-0.png',source,old,native),('ffi-0.png',source,old,native),('lam-alef-0.png',source,old,native),
       ('italic-stagger-100.png',activeSource,activeBefore,activeAfter),('latin-stagger-350.png',activeSource,activeBefore,activeAfter),
       ('mixed-stagger-650.png',activeSource,activeBefore,activeAfter)]
sheet=Image.new('RGB',(1200,len(names)*160),'#162435');draw=ImageDraw.Draw(sheet)
for i,(name,*folders) in enumerate(names):
    for j,(label,folder) in enumerate(zip(['Original','Before','After'],folders)):
        a=Image.open(folder/name).convert('RGBA');bg=Image.new('RGBA',a.size,'#162435')
        a=Image.alpha_composite(bg,a).convert('RGB').crop((0,0,800,240));a.thumbnail((400,120))
        sheet.paste(a,(j*400,i*160+24));draw.text((j*400+3,i*160+3),f'{label} | {name}',fill='white')
sheet.save(out/'contact.png')
print(json.dumps({k:v for k,v in report.items() if k.endswith('Summary')}))
