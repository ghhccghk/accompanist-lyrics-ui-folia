"""Actual original React/canvas stage. Same frames/masks before and after entry."""
import json,hashlib,math
from pathlib import Path
import numpy as np
from PIL import Image,ImageDraw
root=Path(__file__).resolve().parents[1];bg=np.array([22,36,53])
source=root/'build/verification/fume-mounted-browser'
before=root/'build/verification/fume-mounted-kotlin-before-entry';after=root/'build/verification/fume-mounted-kotlin'
out=root/'verification/fume-mounted';out.mkdir(exist_ok=True,parents=True)
inputs=json.loads((root/'verification/fume-fixtures/mounted-observations.json').read_text(encoding='utf-8'))
rows=[]
for r in inputs['observations']:
    f=r['file'];a,b,c=[np.asarray(Image.open(p/f).convert('RGB')).astype(np.int16) for p in [source,before,after]]
    assert a.shape==b.shape==c.shape
    mask=np.any(a!=bg,axis=2)|np.any(b!=bg,axis=2)|np.any(c!=bg,axis=2)
    def metrics(x):
        d=abs(a-x)
        local=[float(d[y:y+64,z:z+64][mask[y:y+64,z:z+64]].mean()) for y in range(0,a.shape[0],64)
               for z in range(0,a.shape[1],64) if mask[y:y+64,z:z+64].any()]
        return dict(foreground_mae=float(d[mask].mean()) if mask.any() else 0,full_mae=float(d.mean()),channel_peak=int(d.max()),worst64=max(local,default=0))
    native=next(x.split('\t') for x in (after/f"{r['width']}-{str(r['stepped']).lower()}.tsv").read_text().splitlines() if x.startswith(f+'\t'))
    q=r['camera'];sx,sy,ss=map(float,native[1:4])
    rows.append(dict(file=f,time=r['time'],mask_pixels=int(mask.sum()),before=metrics(b),after=metrics(c),
        camera_screen_error=math.hypot(q['x']-sx,q['y']-sy)*max(q['scale'],ss),scale_error=abs(q['scale']-ss),
        source_parent_scale=r['parentScale'],native_parent_scale=float(native[4]),
        parent_scale_error=abs(r['parentScale']-float(native[4])),
        png_sha256={str(folder.relative_to(root)/f):hashlib.sha256((folder/f).read_bytes()).hexdigest() for folder in [source,before,after]}))
report=dict(revision=inputs['revision'],motion=inputs['motion'],scope=inputs['scope'],foreground='same all-nonbackground union of original/before/after; includes low alpha; unregistered',
    before='same current Kotlin pipeline with only stageEntryEnabled=false, reproduces missing parent scale',
    after='actual-core FoliaLyricsView/Fume with media parent entry and Canvas clipping',frames=rows,
    mean_before=float(np.mean([r['before']['foreground_mae']for r in rows])),mean_after=float(np.mean([r['after']['foreground_mae']for r in rows])),
    max_parent_scale_error=max(r['parent_scale_error']for r in rows),
    max_camera_screen_error=max(r['camera_screen_error']for r in rows),max_scale_error=max(r['scale_error']for r in rows),
    status='Entry behavior verified independently. Camera/raster differences remain; NOT final visual or Android runtime acceptance.')
report['entry_frames']={k:float(np.mean([r[k]['foreground_mae']for r in rows if r['time']<1.15])) for k in ['before','after']}
(out/'comparison.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
names=['800-1-false-0.png','800-1-false-700.png','800-1-false-800.png','800-1-false-1000.png','380-1-true-3150.png','800-1-false-10000.png']
sheet=Image.new('RGB',(1200,len(names)*270),(30,30,30));draw=ImageDraw.Draw(sheet)
for i,f in enumerate(names):
    for j,folder in enumerate([source,before,after]):
        draw.text((j*400+5,i*270+3),f'{["Original React","Before entry","Public entry"][j]} | {f}',fill='white')
        im=Image.open(folder/f).convert('RGB');im.thumbnail((400,240));sheet.paste(im,(j*400,i*270+22))
sheet.save(out/'contact.png')
print(json.dumps({k:v for k,v in report.items() if k!='frames'},indent=2))
