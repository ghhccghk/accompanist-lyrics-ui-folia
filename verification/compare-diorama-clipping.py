"""Paired locked GPU vs old/unclipped and new/clipped independent Canvas path."""
from pathlib import Path
import json, hashlib
import numpy as np
from PIL import Image, ImageDraw

root=Path(__file__).resolve().parents[1]
source=root/'build/verification/diorama-clipping-browser'
native=root/'build/verification/diorama-clipping-kotlin'
out=root/'verification/diorama-clipping';out.mkdir(parents=True,exist_ok=True)
conditions=json.loads((root/'verification/diorama-fixtures/clipping-v1/conditions.json').read_text())
for width in [380,800]: assert (native/f'complete-{width}.txt').is_file()
bg=np.array([22,36,53]);rows=[]
for c in conditions['captures']:
    files=[source/c['file'],native/('before-'+c['file']),native/('after-'+c['file'])]
    a,b,d=[np.asarray(Image.open(p).convert('RGB')).astype(np.int16) for p in files]
    mask=np.any(a!=bg,axis=2)|np.any(b!=bg,axis=2)|np.any(d!=bg,axis=2)
    r=dict(file=c['file'],source_pixels=int(np.any(a!=bg,axis=2).sum()),
           before_pixels=int(np.any(b!=bg,axis=2).sum()),after_pixels=int(np.any(d!=bg,axis=2).sum()),
           png_sha256={str(p.relative_to(root)):hashlib.sha256(p.read_bytes()).hexdigest() for p in files})
    for mode,im in [('before',b),('after',d)]:
        diff=abs(a-im)
        local=[float(diff[y:y+64,x:x+64][mask[y:y+64,x:x+64]].mean())
               for y in range(0,a.shape[0],64) for x in range(0,a.shape[1],64) if mask[y:y+64,x:x+64].any()]
        r[mode]=dict(foreground_mae=float(diff[mask].mean()) if mask.any() else 0.0,
                     full_mae=float(diff.mean()),worst64=max(local,default=0),channel_peak=int(diff.max()))
    if r['source_pixels']==0: assert r['after_pixels']==0, f'Spurious clipped/backface pixels {c["file"]}'
    if c['name']=='front': assert np.array_equal(b,d), 'Wholly visible path changed'
    rows.append(r)
assert len(rows)==18
report=dict(scope=conditions['scope'],mask='Identical source/before/after union, ALL nonbackground pixels including low alpha; no registration/filter/delay',
            frames=rows,mean_foreground={m:float(np.mean([r[m]['foreground_mae']for r in rows])) for m in ['before','after']},
            status='Bounded actual near/far/FrontSide clipping. Shared original raster isolates geometry; not own shaping, inter-plane depth buffer or complete Diorama acceptance.')
(out/'comparison.json').write_text(json.dumps(report,indent=2)+'\n')
old=json.loads((root/'.work/diorama-before-plane-clipping-hashes.json').read_text())
assert len(old)==216
new={p:hashlib.sha256((root/p).read_bytes()).hexdigest()for p in old}
assert old==new,'Forward text changed'
(out/'forward-regression.json').write_text(json.dumps(dict(count=len(old),unchanged=True,sha256=new),indent=2)+'\n')
names=['front-800.png','near-cross-800.png','eye-cross-800.png','before-near-800.png','backface-800.png','near-and-far-800.png']
sheet=Image.new('RGB',(1260,len(names)*338),(30,30,30));draw=ImageDraw.Draw(sheet)
for i,name in enumerate(names):
    for j,(label,p) in enumerate([('Original Three GPU',source/name),('Old unbounded Canvas',native/('before-'+name)),('New clipped Canvas',native/('after-'+name))]):
        draw.text((j*420+4,i*338+3),label+' | '+name,fill='white')
        im=Image.open(p).convert('RGB');im.thumbnail((420,300));sheet.paste(im,(j*420,i*338+30))
sheet.save(out/'contact.png')
print(json.dumps(dict(means=report['mean_foreground'],original_empty=[r['file']for r in rows if r['source_pixels']==0],
                     old_spurious_pixels=sum(r['before_pixels']for r in rows if r['source_pixels']==0),forward_hashes=len(new)),indent=2))
