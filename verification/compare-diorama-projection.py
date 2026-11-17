"""Isolated same-texture projection capability. No registration/alpha filtering."""
from pathlib import Path
import json,hashlib
import numpy as np
from PIL import Image,ImageDraw
root=Path(__file__).resolve().parents[1]
source=root/'build/verification/diorama-projection-browser'
native=root/'build/verification/diorama-projection-kotlin'
out=root/'verification/diorama-projection';out.mkdir(parents=True,exist_ok=True)
rows=[];bg=np.array([22,36,53])
for p in sorted(source.glob('*.png')):
    a,b=[np.asarray(Image.open(f).convert('RGB')).astype(np.int16) for f in [p,native/p.name]]
    assert a.shape==b.shape
    mask=np.any(a!=bg,axis=2)|np.any(b!=bg,axis=2);d=abs(a-b)
    local=[float(d[y:y+64,x:x+64][mask[y:y+64,x:x+64]].mean()) for y in range(0,a.shape[0],64)
           for x in range(0,a.shape[1],64) if mask[y:y+64,x:x+64].any()]
    def bounds(image):
        yy,xx=np.where(np.any(image!=bg,axis=2))
        return [int(xx.min()),int(yy.min()),int(xx.max()+1),int(yy.max()+1)] if xx.size else None
    rows.append(dict(file=p.name,foreground_mae=float(d[mask].mean()),full_mae=float(d.mean()),
        channel_peak=int(d.max()),worst_64px_tile_mae=max(local),source_bounds=bounds(a),native_bounds=bounds(b),
        source_sha256=hashlib.sha256(p.read_bytes()).hexdigest(),native_sha256=hashlib.sha256((native/p.name).read_bytes()).hexdigest()))
assert len(rows)==9
report=dict(scope='Same original raster and explicit frozen Three.js view; Compose perspective matrix. NOT text shaping, CameraRig, fog, scene depth or theme acceptance.',
    foreground='all nonbackground RGB union, including low alpha',frames=rows,mean_foreground_mae=float(np.mean([r['foreground_mae']for r in rows])))
(out/'comparison.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
names=['800-1.7-0.8.png','380--3.6-1.9.png','801-4.4--1.2.png']
sheet=Image.new('RGB',(1000,3*310),(30,30,30));draw=ImageDraw.Draw(sheet)
for i,name in enumerate(names):
    for j,folder in enumerate([source,native]):
        draw.text((j*500+5,i*310+3),f'{"Three.js"if j==0 else "Compose"} | {name}',fill='white')
        im=Image.open(folder/name).convert('RGB');im.thumbnail((500,280));sheet.paste(im,(j*500,i*310+22))
sheet.save(out/'contact.png')
print(json.dumps(report,indent=2))
