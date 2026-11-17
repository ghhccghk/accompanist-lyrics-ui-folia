"""Original mounted bubble/text primitives, target frame/media-anchored CSS fades.
No geometric registration, phase fit or palette correction. Not full-theme parity.
"""
from pathlib import Path
import hashlib,json
import numpy as np
from PIL import Image,ImageDraw
root=Path(__file__).resolve().parents[1]
source=root/'build/verification/cappella-bubble-browser'
native=root/'build/verification/cappella-bubble-kotlin'
out=root/'verification/cappella-bubble';out.mkdir(exist_ok=True)
background=np.array([22,36,53]);rows=[];hashes={}
for file in sorted(source.glob('*.png')):
    other=native/file.name
    if not other.exists():raise RuntimeError(f'Missing native frame {file.name}')
    a,b=[np.asarray(Image.open(p).convert('RGB')).astype(np.int16)for p in[file,other]]
    assert a.shape==b.shape
    mask=np.any(abs(a-background)>8,axis=2)|np.any(abs(b-background)>8,axis=2)
    diff=abs(a-b)
    rows.append(dict(file=file.name,foreground_pixels=int(mask.sum()),foreground_mae=float(diff[mask].mean())if mask.any()else 0,full_mae=float(diff.mean())))
    for p in[file,other]:hashes[p.relative_to(root).as_posix()]=hashlib.sha256(p.read_bytes()).hexdigest()
report=dict(conditions=json.loads((root/'verification/cappella-fixtures/bubble-conditions.json').read_text(encoding='utf-8')),
    status='Internal bubble calibration, NOT complete Cappella acceptance. Original DOM codepoint vs EGC, shaping and platform raster residuals remain.',
    mean_foreground_mae=sum(r['foreground_mae']for r in rows)/len(rows),
    by_scene={s:sum(r['foreground_mae']for r in rows if r['file'].startswith(s+'-'))/sum(r['file'].startswith(s+'-')for r in rows)for s in['latin','wrap','arabic','cjk']},
    frames=rows,png_sha256=hashes)
(out/'comparison.json').write_text(json.dumps(report,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
names=[f'{s}-{w}-{side}-{t}.png'for s,w,side,t in[('latin',160,'left',850),('latin',160,'right',2800),('wrap',160,'left',1600),('wrap',160,'right',3500),('arabic',300,'left',2800),('cjk',160,'right',2800)]]
sheet=Image.new('RGB',(1000,len(names)*230),(30,30,30));draw=ImageDraw.Draw(sheet)
for i,name in enumerate(names):
    for j,folder in enumerate([source,native]):
        draw.text((j*500+5,i*230+4),f'{"Original components"if j==0 else"Kotlin shaped bubble"} | {name}',fill='white')
        pic=Image.open(folder/name).convert('RGB').crop((0,0,480,200));sheet.paste(pic,(j*500,i*230+24))
sheet.save(out/'contact.png')
print(json.dumps({k:report[k]for k in['mean_foreground_mae','by_scene']},indent=2))
