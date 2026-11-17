"""Actual original component vs independently prepared native text, no registration."""
from pathlib import Path
import hashlib,json
import numpy as np
from PIL import Image,ImageDraw

root=Path(__file__).resolve().parents[1]
source=root/'build/verification/pendolo-sweep-browser'
native=root/'build/verification/pendolo-sweep-kotlin'
out=root/'verification/pendolo-sweep'
out.mkdir(exist_ok=True)
conditions=json.loads((root/'verification/pendolo-fixtures/sweep-conditions.json').read_text())
frames=[];hashes={}
for file in sorted(source.glob('*.png')):
    other=native/file.name
    assert other.exists(),other
    a,b=[np.asarray(Image.open(p).convert('RGB')).astype(np.int16) for p in (file,other)]
    assert a.shape==b.shape
    mask=np.any(abs(a-[22,36,53])>8,axis=2)|np.any(abs(b-[22,36,53])>8,axis=2)
    difference=abs(a-b)
    frames.append(dict(file=file.name,foreground_mae=float(difference[mask].mean()) if mask.any() else 0,full_mae=float(difference.mean())))
    for p in (file,other):hashes[p.relative_to(root).as_posix()]=hashlib.sha256(p.read_bytes()).hexdigest()
assert len(frames)==conditions['frames']
residual=[r.split('\t') for p in native.glob('*-residual.tsv') for r in p.read_text(encoding='utf8').splitlines() if r and not r.startswith('#')]
assert all(not p.read_text(encoding='utf8').strip() for p in native.glob('*-mismatches.txt'))
report=dict(conditions=conditions,status='Isolated non-chorus active-text calibration. Not whole-wheel/tick/subtitle/theme or device acceptance.',
    maximum_width_or_prefix_error=max(float(r[-1]) for r in residual),
    mean_foreground_mae=sum(r['foreground_mae'] for r in frames)/len(frames),
    max_foreground_mae=max(r['foreground_mae'] for r in frames),frames=frames,png_sha256=hashes)
(out/'comparison.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf8')
names=['latin-150-1000.png','latin-300-1600.png','wrap-150-1300.png','wrap-300-1600.png','arabic-150-1600.png','cjk-150-1600.png']
sheet=Image.new('RGB',(800,len(names)*205),(30,30,30));draw=ImageDraw.Draw(sheet)
for i,name in enumerate(names):
    for j,folder in enumerate((source,native)):
        draw.text((j*400+5,i*205+3),f'{"Original" if j==0 else "Kotlin"} | {name}',fill='white')
        pic=Image.open(folder/name).convert('RGB').crop((0,0,380,180))
        sheet.paste(pic,(j*400,i*205+23))
sheet.save(out/'contact.png')
print(json.dumps({k:report[k] for k in ('maximum_width_or_prefix_error','mean_foreground_mae','max_foreground_mae')},indent=2))
