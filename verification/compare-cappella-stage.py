"""Unregistered whole original JSX/native stage target pixels; phase seam in conditions.
No pass threshold is inferred from build success or average image error.
"""
from pathlib import Path
import hashlib,json
import numpy as np
from PIL import Image,ImageDraw
root=Path(__file__).resolve().parents[1]
source=root/'build/verification/cappella-stage-visual-browser'
native=root/'build/verification/cappella-stage-visual-kotlin'
out=root/'verification/cappella-stage-visual';out.mkdir(exist_ok=True)
background=np.array([22,36,53]);rows=[];hashes={}
files=sorted(source.glob('*.png'))
assert len(files)==60, f'Expected complete 60-frame matrix, got {len(files)}'
for file in files:
    other=native/file.name
    if not other.exists():raise RuntimeError(f'Missing native frame {file.name}')
    a,b=[np.asarray(Image.open(p).convert('RGB')).astype(np.int16)for p in[file,other]]
    assert a.shape==b.shape
    mask=np.any(abs(a-background)>8,axis=2)|np.any(abs(b-background)>8,axis=2)
    diff=abs(a-b)
    rows.append(dict(file=file.name,foreground_pixels=int(mask.sum()),foreground_mae=float(diff[mask].mean())if mask.any()else 0,full_mae=float(diff.mean())))
    for p in[file,other]:hashes[p.relative_to(root).as_posix()]=hashlib.sha256(p.read_bytes()).hexdigest()
report=dict(conditions=json.loads((source/'conditions.json').read_text(encoding='utf-8')),
    status='Internal complete TARGET stage, not dynamic Cappella acceptance. CSS/Skia raster, shaping, row/resize/spring/timestamp response differences remain.',
    mean_foreground_mae=sum(r['foreground_mae']for r in rows)/len(rows),
    by_scene={s:sum(r['foreground_mae']for r in rows if r['file'].startswith(s+'-'))/sum(r['file'].startswith(s+'-')for r in rows)for s in['latin','wrap','arabic','cjk']},
    frames=rows,png_sha256=hashes)
(out/'comparison.json').write_text(json.dumps(report,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
names=['latin-380-normal-1600.png','wrap-800-chaotic-6000.png','arabic-800-normal-14000.png','cjk-1200-calm-27500.png']
sheet=Image.new('RGB',(1200,len(names)*400),(30,30,30));draw=ImageDraw.Draw(sheet)
for i,name in enumerate(names):
    for j,folder in enumerate([source,native]):
        draw.text((j*600+5,i*400+4),f'{"Original JSX"if j==0 else"Kotlin target stage"} | {name}',fill='white')
        pic=Image.open(folder/name).convert('RGB');pic.thumbnail((590,365));sheet.paste(pic,(j*600,i*400+25))
sheet.save(out/'contact.png')
print(json.dumps({k:report[k]for k in['mean_foreground_mae','by_scene']},indent=2))
