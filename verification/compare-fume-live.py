"""Original Fume Canvas versus native whole-shaped-row clipping. No registration or fitting."""
from pathlib import Path
import json, hashlib
import numpy as np
from PIL import Image, ImageDraw
root=Path(__file__).resolve().parents[1]
source=root/'build/verification/fume-live-browser'
native=root/'build/verification/fume-live-kotlin'
out=root/'verification/fume-live'
out.mkdir(exist_ok=True)
background=np.array([22,36,53])
rows=[];hashes={}
for file in sorted(source.glob('*.png')):
    other=native/file.name
    if not other.exists(): raise RuntimeError(f'Missing native frame {file.name}')
    a=np.asarray(Image.open(file).convert('RGB')).astype(np.int16)
    b=np.asarray(Image.open(other).convert('RGB')).astype(np.int16)
    assert a.shape==b.shape
    mask=np.any(abs(a-background)>8,axis=2)|np.any(abs(b-background)>8,axis=2)
    diff=abs(a-b)
    rows.append(dict(file=file.name,foreground_pixels=int(mask.sum()),foreground_mae=float(diff[mask].mean())if mask.any()else 0,
                     full_mae=float(diff.mean())))
    for p in (file,other): hashes[p.relative_to(root).as_posix()]=hashlib.sha256(p.read_bytes()).hexdigest()
report=dict(conditions=json.loads((source/'conditions.json').read_text(encoding='utf-8')),
    source='unmodified drawFumeLiveBlock + static snapshot resolver; original article block geometry; fixed translation, no camera',
    native='FumeLiveText; same block geometry input; intact paragraph shaping + word-segment masks + per-EGC clipped paint',
    frames=rows,mean_foreground_mae=sum(r['foreground_mae']for r in rows)/len(rows),
    by_glow={str(g):sum(r['foreground_mae']for r in rows if r['file'].split('-')[2]==str(g))/len([r for r in rows if r['file'].split('-')[2]==str(g)])for g in [0,1]},
    png_sha256=hashes,status='calibration; no final theme or camera acceptance')
(out/'comparison.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
names=['0-800-0-1000.png','0-800-1-740.png','0-800-1-1550.png','1-800-1-800.png','2-800-1-2000.png','0-800-1-4500.png']
sheet=Image.new('RGB',(1000,len(names)*265),(30,30,30));draw=ImageDraw.Draw(sheet)
for i,name in enumerate(names):
    for j,folder in enumerate([source,native]):
        draw.text((j*500+5,i*265+3),f'{"Original"if j==0 else "Kotlin"} | {name}',fill='white')
        pic=Image.open(folder/name).convert('RGB');pic=pic.crop((0,80,pic.width,480));pic.thumbnail((500,240))
        sheet.paste(pic,(j*500,i*265+22))
sheet.save(out/'contact.png')
print(json.dumps({k:v for k,v in report.items()if k not in ['png_sha256','frames']},indent=2))
