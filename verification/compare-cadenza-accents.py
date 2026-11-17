"""Unregistered same-font/time/viewport chorus and keyword color comparison."""
from pathlib import Path
import hashlib,json
import numpy as np
from PIL import Image,ImageDraw
root=Path(__file__).resolve().parents[1]
source=root/'build/verification/cadenza-accents-browser';native=root/'build/verification/cadenza-accents-kotlin'
out=root/'verification/cadenza-accents';out.mkdir(exist_ok=True)
rows=[];hashes={};background=np.array([22,36,53])
for p in sorted(source.glob('*.png')):
    q=native/p.name
    if not q.exists():raise RuntimeError(f'Missing native frame {p.name}')
    a,b=[np.asarray(Image.open(f).convert('RGB')).astype(np.int16)for f in[p,q]]
    assert a.shape==b.shape
    mask=np.any(abs(a-background)>8,axis=2)|np.any(abs(b-background)>8,axis=2)
    rows.append(dict(file=p.name,foreground_pixels=int(mask.sum()),foreground_mae=float(abs(a-b)[mask].mean())if mask.any()else 0))
    for f in[p,q]:hashes[f.relative_to(root).as_posix()]=hashlib.sha256(f.read_bytes()).hexdigest()
report=dict(conditions=json.loads((root/'verification/cadenza-fixtures/accent-conditions.json').read_text(encoding='utf-8')),
    status='Internal calibration. Original settled Canvas chorus circles and DOM text; native whole-run shaping. Not full theme/transition acceptance.',
    mean_foreground_mae=sum(r['foreground_mae']for r in rows)/len(rows),
    by_scene={s:sum(r['foreground_mae']for r in rows if r['file'].startswith(s+'-'))/sum(r['file'].startswith(s+'-')for r in rows)for s in['latin','wrap','arabic','cjk']},
    rings=(native/'rings.txt').read_text(encoding='utf-8'),frames=rows,png_sha256=hashes)
(out/'comparison.json').write_text(json.dumps(report,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
names=[f'{s}-{w}-{t}.png'for s,w,t in[('latin',380,850),('latin',380,1600),('wrap',380,2200),('arabic',800,1300),('cjk',800,850),('cjk',800,2800)]]
sheet=Image.new('RGB',(1000,len(names)*330),(30,30,30));draw=ImageDraw.Draw(sheet)
for i,name in enumerate(names):
    for j,folder in enumerate([source,native]):
        draw.text((j*500+5,i*330+4),f'{"Original chorus"if j==0 else "Kotlin chorus"} | {name}',fill='white')
        pic=Image.open(folder/name).convert('RGB');pic.thumbnail((500,300));sheet.paste(pic,(j*500,i*330+24))
sheet.save(out/'contact.png')
print(json.dumps({k:report[k]for k in['mean_foreground_mae','by_scene','rings']},indent=2))
