"""Descriptive unregistered original Partita / Kotlin foreground comparison. Not an acceptance waiver."""
from pathlib import Path
import argparse, csv, hashlib, json
import numpy as np
from PIL import Image, ImageDraw

root=Path(__file__).resolve().parents[1]
base=root/'build/verification'
p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--candidate',default='partita-kotlin')
p.add_argument('--include-tight',action='store_true')
p.add_argument('--include-avoid',action='store_true')
args=p.parse_args()
original=base/'partita-browser';kotlin=base/args.candidate;out=base/(args.candidate+'-comparison')
out.mkdir(exist_ok=True)
rows=[]; hashes={}; bg=np.array([22,36,53])
for path in sorted(original.glob('*.png')):
    if '-tight-' in path.name and not args.include_tight:
        continue
    if '-avoid-' in path.name and not args.include_avoid:
        continue
    target=kotlin/path.name
    if not target.exists():
        raise RuntimeError(f'Missing Kotlin frame: {path.name}')
    a=np.asarray(Image.open(path).convert('RGB')).astype(np.int16)
    b=np.asarray(Image.open(target).convert('RGB')).astype(np.int16)
    assert a.shape==b.shape
    mask=np.any(np.abs(a-bg)>8,axis=2)|np.any(np.abs(b-bg)>8,axis=2)
    mae=float(np.abs(a-b)[mask].mean()) if mask.any() else 0
    rows.append({'file':path.name,'foreground_pixels':int(mask.sum()),'foreground_mae':mae})
    for f in [path,target]: hashes[str(f.relative_to(root))]=hashlib.sha256(f.read_bytes()).hexdigest()
with (out/'errors.csv').open('w',encoding='utf8',newline='') as f:
    w=csv.DictWriter(f,fieldnames=list(rows[0]));w.writeheader();w.writerows(rows)
names=['normal-wrap-800-1000ms.png','normal-wrap-800-1750ms.png','chaotic-wrap-380-1500ms.png','normal-arabic-800-1500ms.png']
sheet=Image.new('RGB',(800,len(names)*250),(30,30,30));draw=ImageDraw.Draw(sheet)
for i,name in enumerate(names):
    for j,(label,folder) in enumerate([('Original',original),('Kotlin',kotlin)]):
        draw.text((j*400+4,i*250+3),f'{label} | {name}',fill='white')
        image=Image.open(folder/name).convert('RGB');image.thumbnail((400,225))
        sheet.paste(image,(j*400+(400-image.width)//2,i*250+22))
sheet.save(out/'contact.png')
summary={'frames':len(rows),'mean_foreground_mae':sum(r['foreground_mae'] for r in rows)/len(rows),
         'worst':sorted(rows,key=lambda r:r['foreground_mae'],reverse=True)[:10]}
(out/'conditions.json').write_text(json.dumps({'reference':json.loads((original/'conditions.json').read_text()),
    'summary':summary,'png_sha256':hashes,'limits':'No alignment, rescaling or expected-image replacement. Same fonts, viewport and media timestamps. Raw residuals do not imply parity accepted.'},indent=2),encoding='utf8')
print(json.dumps(summary,indent=2))
