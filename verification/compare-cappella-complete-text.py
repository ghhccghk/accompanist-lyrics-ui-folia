"""Fixed original/before/after union, including every nonzero-alpha pixel."""
from pathlib import Path
import hashlib,json
import numpy as np
from PIL import Image,ImageDraw

root=Path(__file__).resolve().parents[1]
source=root/'build/verification/cappella-complete-browser'
before=root/'.work/cappella-complete-text-before'
after=root/'build/verification/cappella-complete-text'
out=root/'verification/cappella-complete-text';out.mkdir(exist_ok=True)
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
conditions=json.loads((source/'conditions.json').read_text(encoding='utf-8'))
records=[]
sheet=Image.new('RGB',(1200,5*180),'#162435');draw=ImageDraw.Draw(sheet)
background=Image.new('RGBA',(800,300),'#162435')
for i,c in enumerate(conditions['observations']):
    name=c['name']
    paths=[source/f'{name}.png',before/f'{name}-passed.png',after/f'{name}-passed.png']
    images=[Image.open(p).convert('RGBA') for p in paths]
    arrays=[np.asarray(im).astype(np.float64) for im in images]
    mask=np.logical_or.reduce([a[:,:,3]>0 for a in arrays])
    # Transparent RGB is undefined; compare premultiplied color + alpha, retaining
    # ALL alpha >0. This is compositing, not a low-alpha exclusion or registration.
    premul=[np.concatenate((a[:,:,:3]*(a[:,:,3:]/255),a[:,:,3:]),axis=2) for a in arrays]
    row={'case':name,'unionPixels':int(mask.sum()),'hashes':[sha(p) for p in paths]}
    for label,a in zip(('before','after'),premul[1:]):
        diff=abs(premul[0]-a)
        worst=max(float(diff[y:y+64,x:x+64][mask[y:y+64,x:x+64]].mean())
            for y in range(0,300,64) for x in range(0,800,64) if mask[y:y+64,x:x+64].any())
        row[label]={'foregroundMAE':float(diff[mask].mean()),'peak':float(diff.max()),'worst64':worst}
    for mode in ('passed','reduced'):
        im=np.asarray(Image.open(after/f'{name}-{mode}.png'))
        ref=np.asarray(Image.open(after/f'{name}-complete.png'))
        assert np.array_equal(im,ref),f'{name}/{mode} incomplete shaped coverage'
    row['completeRowDifferentPixels']=0
    records.append(row)
    for j,(label,im) in enumerate(zip(('Original CappellaText','Before (caret clips)','After (whole row)'),images)):
        preview=Image.alpha_composite(background,im).convert('RGB').crop((0,0,800,300))
        preview.thumbnail((400,150));sheet.paste(preview,(j*400,i*180+24))
        draw.text((j*400+4,i*180+4),f'{name} | {label}',fill='white')
report={'conditions':conditions,'scope':'Whole-text coverage correction for passed/reduced rows only; active fades and normal layout-projection residuals unchanged, no full-theme/Android/device/performance pass.',
    'pixelPolicy':'One original/before/after all-nonzero-alpha union per case. Premultiplied RGBA. No registration, threshold, dropped frames or phase correction.',
    'beforeMean':sum(r['before']['foregroundMAE'] for r in records)/len(records),
    'afterMean':sum(r['after']['foregroundMAE'] for r in records)/len(records),'cases':records,
    'beforeCoverage':(before/'coverage.tsv').read_text(),'afterCoverage':(after/'coverage.tsv').read_text()}
(out/'comparison.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
sheet.save(out/'contact.png')
print(json.dumps({'before':report['beforeMean'],'after':report['afterMean'],'cases':records},ensure_ascii=False))
