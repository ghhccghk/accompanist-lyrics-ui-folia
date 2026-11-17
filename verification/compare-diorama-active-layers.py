"""Original own raster/Three vs Kotlin own full-shaped paragraph/Compose perspective."""
from pathlib import Path
import json,hashlib
import numpy as np
from PIL import Image,ImageDraw
root=Path(__file__).resolve().parents[1]
source=root/'build/verification/diorama-active-layers-browser';native=root/'build/verification/diorama-active-layers-kotlin'
out=root/'verification/diorama-active-layers';out.mkdir(exist_ok=True,parents=True)
conditions=json.loads((root/'verification/diorama-fixtures/active-layers.json').read_text(encoding='utf-8'));bg=np.array([22,36,53]);rows=[]
for o in conditions['observations']:
    p=o['file'];a,b=[np.asarray(Image.open(f/p).convert('RGB')).astype(np.int16)for f in[source,native]]
    assert a.shape==b.shape
    mask=np.any(a!=bg,axis=2)|np.any(b!=bg,axis=2);d=abs(a-b)
    def bounds(image):
        yy,xx=np.where(np.any(image!=bg,axis=2))
        return [int(xx.min()),int(yy.min()),int(xx.max()+1),int(yy.max()+1)] if xx.size else None
    local=[float(d[y:y+64,x:x+64][mask[y:y+64,x:x+64]].mean())for y in range(0,a.shape[0],64)for x in range(0,a.shape[1],64)if mask[y:y+64,x:x+64].any()]
    rows.append(dict(file=p,scene=o['scene'],foreground_mae=float(d[mask].mean()),full_mae=float(d.mean()),worst64=max(local),channel_peak=int(d.max()),source_bounds=bounds(a),native_bounds=bounds(b),png_sha256={str(f.relative_to(root)/p):hashlib.sha256((f/p).read_bytes()).hexdigest()for f in[source,native]}))
assert len(rows)==180
report=dict(scope=conditions['scope'],foreground='all nonbackground union including low alpha; no registration, alignment or frame shift',frames=rows,
 mean_foreground_mae=float(np.mean([r['foreground_mae']for r in rows])),by_scene={name:float(np.mean([r['foreground_mae']for r in rows if r['scene']==name]))for name in ['latin','arabic','cjk']},
 status='Own shaped slices, four-pass halo and crisp ghost in original global layer order. Frozen perspective and analytic charges; NOT original RAF, CameraRig, full scene or Android acceptance.')
(out/'comparison.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
names=['latin-800-active-1100.png','latin-800-active-1900.png','arabic-800-active-1100.png','arabic-800-trail-1900.png','cjk-800-active-1100.png','cjk-380-trail-3100.png']
sheet=Image.new('RGB',(1000,len(names)*310),(30,30,30));draw=ImageDraw.Draw(sheet)
for i,name in enumerate(names):
    for j,folder in enumerate([source,native]):
        draw.text((j*500+5,i*310+3),f'{"Original own texture"if j==0 else "Kotlin own shaping"} | {name}',fill='white')
        im=Image.open(folder/name).convert('RGB');im.thumbnail((500,280));sheet.paste(im,(j*500,i*310+22))
sheet.save(out/'contact.png');print(json.dumps({k:v for k,v in report.items() if k!='frames'},indent=2))
