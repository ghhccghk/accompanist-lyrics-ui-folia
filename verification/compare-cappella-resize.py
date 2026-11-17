"""Actual continuously mounted source vs media-reconstructible native frame.
Dynamic residuals are measured, not substituted with settled targets.
"""
from pathlib import Path
import json,hashlib
import numpy as np
from PIL import Image,ImageDraw
root=Path(__file__).resolve().parents[1]
source=root/'build/verification/cappella-resize-browser';native=root/'build/verification/cappella-resize-kotlin'
out=root/'verification/cappella-resize';out.mkdir(exist_ok=True)
background=np.array([22,36,53]);frames=[];hashes={};numeric=[]
for f in sorted(source.glob('*.png')):
    other=native/f.name
    assert other.exists(),f'Missing native {f.name}'
    a,b=[np.asarray(Image.open(p).convert('RGB')).astype(np.int16)for p in[f,other]]
    assert a.shape==b.shape
    mask=np.any(abs(a-background)>8,axis=2)|np.any(abs(b-background)>8,axis=2)
    diff=abs(a-b)
    frames.append(dict(file=f.name,foreground_mae=float(diff[mask].mean())if mask.any()else 0,full_mae=float(diff.mean())))
    for p in[f,other]:hashes[p.relative_to(root).as_posix()]=hashlib.sha256(p.read_bytes()).hexdigest()
assert len(frames)==92
for s in['latin','wrap']:
    path=native/f'{s}-residual.tsv';(out/path.name).write_bytes(path.read_bytes())
    numeric += [x.split('\t')for x in path.read_text(encoding='utf-8').splitlines()[1:]]
report=dict(conditions=json.loads((source/'conditions.json').read_text(encoding='utf-8')),
    status='Continuously mounted original frame resize comparison; not row/whole-theme acceptance. RAF quantization,layout/word wrap/shaping/raster residuals measured below.',
    mean_foreground_mae=sum(r['foreground_mae']for r in frames)/len(frames),
    maximum_width_error=max(float(r[7])for r in numeric),maximum_height_error=max(float(r[8])for r in numeric),
    frames=frames,png_sha256=hashes)
(out/'comparison.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
names=['latin-160-left-1000.png','latin-160-left-1750.png','latin-160-right-1900.png','wrap-160-left-1600.png','wrap-160-right-2200.png']
sheet=Image.new('RGB',(1000,len(names)*230),(30,30,30));draw=ImageDraw.Draw(sheet)
for i,name in enumerate(names):
    for j,folder in enumerate([source,native]):
        draw.text((j*500+5,i*230+4),f'{"Original live resize"if j==0 else"Kotlin media resize"} | {name}',fill='white')
        pic=Image.open(folder/name).convert('RGB').crop((0,0,480,200));sheet.paste(pic,(j*500,i*230+24))
sheet.save(out/'contact.png')
print(json.dumps({k:report[k]for k in['mean_foreground_mae','maximum_width_error','maximum_height_error']},indent=2))
