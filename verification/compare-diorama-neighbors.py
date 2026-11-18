"""Complete core-stage comparison; same all-alpha union for before and after."""
from pathlib import Path
import hashlib
import json
import numpy as np
from PIL import Image, ImageDraw

root=Path(__file__).resolve().parents[1]
source=root/'build/verification/diorama-neighbors-browser'
native=root/'build/verification/diorama-neighbors-kotlin'
out=root/'verification/diorama-neighbors'
out.mkdir(parents=True,exist_ok=True)
conditions=json.loads((root/'verification/diorama-fixtures/neighbors-v1/scene.json').read_text(encoding='utf8'))
assert len(conditions['captures'])==54
for width in [380,800]:
    assert (native/f'complete-{width}.txt').is_file(), 'Wait for both successful native captures'
bg=np.array([22,36,53])
rows=[]

def bounds(a):
    y,x=np.where(np.any(a!=bg,axis=2))
    return [int(x.min()),int(y.min()),int(x.max()+1),int(y.max()+1)] if x.size else None

for c in conditions['captures']:
    files=[source/c['file'],native/('before-'+c['file']),native/('after-'+c['file'])]
    a,b,d=[np.asarray(Image.open(p).convert('RGB')).astype(np.int16) for p in files]
    assert a.shape==b.shape==d.shape
    mask=np.any(a!=bg,axis=2)|np.any(b!=bg,axis=2)|np.any(d!=bg,axis=2)
    r=dict(file=c['file'],source_bounds=bounds(a),before_bounds=bounds(b),after_bounds=bounds(d),
           source_pixels=int(np.any(a!=bg,axis=2).sum()),after_pixels=int(np.any(d!=bg,axis=2).sum()),
           png_sha256={str(p.relative_to(root)):hashlib.sha256(p.read_bytes()).hexdigest() for p in files})
    for mode,im in [('before',b),('after',d)]:
        diff=abs(a-im)
        local=[float(diff[y:y+64,x:x+64][mask[y:y+64,x:x+64]].mean())
               for y in range(0,a.shape[0],64) for x in range(0,a.shape[1],64) if mask[y:y+64,x:x+64].any()]
        r[mode]=dict(foreground_mae=float(diff[mask].mean()) if mask.any() else 0.0,full_mae=float(diff.mean()),
                     worst64=max(local,default=0.0),channel_peak=int(diff.max()))
    rows.append(r)
summary={mode:dict(mean_foreground_mae=float(np.mean([r[mode]['foreground_mae'] for r in rows])),
                   worst_frame=max(r[mode]['foreground_mae'] for r in rows),local_worst=max(r[mode]['worst64'] for r in rows))
         for mode in ['before','after']}
report=dict(scope=conditions['scope'],conditions_sha256=hashlib.sha256((root/'verification/diorama-fixtures/neighbors-v1/scene.json').read_bytes()).hexdigest(),
            mask='Identical source/before/after union, every nonbackground pixel including lowest-alpha halo. No registration or cutoff.',
            summary=summary,frames=rows,
            status='Core text corridor window/life integration; fixed original camera isolates text. Raster/foreground and full follower residuals remain; NOT full scene, fog, device or public theme acceptance.')
(out/'comparison.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf8')
# Predetermined ordinary/narrow/near-end/distant conditions, not chosen by score.
names=['latin-800-3-14.png','arabic-800-3-14.png','cjk-380-0-5.2.png','cjk-800-7-36.png']
sheet=Image.new('RGB',(1260,len(names)*338),(30,30,30));draw=ImageDraw.Draw(sheet)
for i,name in enumerate(names):
    for j,(label,p) in enumerate([('Original Three text stage',source/name),('Former own active-only stage',native/('before-'+name)),('Own core window + distance life',native/('after-'+name))]):
        draw.text((j*420+4,i*338+3),label,fill='white');draw.text((j*420+4,i*338+17),name,fill='white')
        im=Image.open(p).convert('RGB');im.thumbnail((420,300));sheet.paste(im,(j*420,i*338+35))
sheet.save(out/'contact.png')
print(json.dumps(summary,indent=2))
