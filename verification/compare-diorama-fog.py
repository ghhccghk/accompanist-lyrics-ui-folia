"""Locked actual Three fragment fog, unchanged all-alpha three-way union.

Supplementary solid-plane point controls use original poses and analytic pixel
rays; they do not filter, replace or register the complete-image comparison.
"""
from pathlib import Path
import hashlib
import json
import math
import numpy as np
from PIL import Image, ImageDraw

root=Path(__file__).resolve().parents[1]
out=root/'verification/diorama-fog'
out.mkdir(parents=True,exist_ok=True)
bg=np.array([22,36,53])
hashfile=lambda p: hashlib.sha256(p.read_bytes()).hexdigest()

def bounds(a):
    y,x=np.where(np.any(a!=bg,axis=2))
    return [int(x.min()),int(y.min()),int(x.max()+1),int(y.max()+1)] if x.size else None

def compare(family,count):
    fixture=root/f'verification/diorama-fixtures/{family}-v1'
    conditions_path=fixture/('conditions.json' if family=='fog-plane' else 'scene.json')
    conditions=json.loads(conditions_path.read_text(encoding='utf8'))
    source=root/f'build/verification/diorama-{family}-browser'
    native=root/f'build/verification/diorama-{family}-kotlin'
    assert len(conditions['captures'])==count
    for width in [380,800]:
        assert (native/f'complete-{width}.txt').is_file(), 'Wait for BOTH successful native captures'
    rows=[];samples=[]
    for c in conditions['captures']:
        files=[source/c['file'],native/('before-'+c['file']),native/('after-'+c['file'])]
        a,b,d=[np.asarray(Image.open(p).convert('RGB')).astype(np.int16) for p in files]
        assert a.shape==b.shape==d.shape
        mask=np.any(a!=bg,axis=2)|np.any(b!=bg,axis=2)|np.any(d!=bg,axis=2)
        r=dict(file=c['file'],source_bounds=bounds(a),before_bounds=bounds(b),after_bounds=bounds(d),
               source_pixels=int(np.any(a!=bg,axis=2).sum()),after_pixels=int(np.any(d!=bg,axis=2).sum()),
               png_sha256={str(p.relative_to(root)):hashfile(p) for p in files})
        for mode,im in [('before',b),('after',d)]:
            diff=abs(a-im)
            local=[float(diff[y:y+64,x:x+64][mask[y:y+64,x:x+64]].mean())
                   for y in range(0,a.shape[0],64) for x in range(0,a.shape[1],64) if mask[y:y+64,x:x+64].any()]
            r[mode]=dict(foreground_mae=float(diff[mask].mean()) if mask.any() else 0.0,
                         full_mae=float(diff.mean()),worst64=max(local,default=0.0),channel_peak=int(diff.max()))
        rows.append(r)
        if family=='fog-plane' and c['mask']=='solid':
            origin=np.array(c['origin']);xx=np.array(c['x']);yy=np.array(c['y'])
            w,h=c['viewportWidth'],600;tan=math.tan(math.radians(55/2));aspect=w/h
            for uf in [.25,.5,.75]:
                for vf in [.25,.5,.75]:
                    world=origin+xx*c['width']*uf+yy*c['height']*vf
                    px=int(math.floor((world[0]/(-world[2]*tan*aspect)+1)*w/2))
                    py=int(math.floor((1-world[1]/(-world[2]*tan))*h/2))
                    if not(0<=px<w and 0<=py<h):continue
                    ray=np.array([((px+.5)/w*2-1)*aspect*tan,(1-(py+.5)/h*2)*tan,-1])
                    u,v,depth=np.linalg.solve(np.column_stack([xx,yy,-ray]),-origin)
                    assert 1<u<c['width']-1 and 1<v<c['height']-1
                    t=np.clip((depth-12)/18,0,1);fog=t*t*(3-2*t)
                    rgb=np.array([223,229,236])*(1-fog)+bg*fog
                    expected=np.clip(rgb*.63+bg*(1 if c['additive'] else .37),0,255)
                    samples.append(dict(file=c['file'],pixel=[px,py],depth=float(depth),factor=float(fog),expected=expected.tolist(),
                                        original=a[py,px].tolist(),own=d[py,px].tolist(),
                                        original_error=float(abs(a[py,px]-expected).max()),own_error=float(abs(d[py,px]-expected).max()),
                                        paired_peak=int(abs(a[py,px]-d[py,px]).max())))
    summary={mode:dict(mean_foreground_mae=float(np.mean([r[mode]['foreground_mae'] for r in rows])),
                       worst_frame=max(r[mode]['foreground_mae'] for r in rows),local_worst=max(r[mode]['worst64'] for r in rows))
             for mode in ['before','after']}
    result=dict(scope=conditions['scope'],conditions_sha256=hashfile(conditions_path),
                mask='Identical source/before/after union, EVERY nonbackground pixel; no alignment, threshold, alpha removal or frame filtering.',
                summary=summary,frames=rows)
    if samples:
        result['solid_point_controls']=dict(
            method='Fixed plane uv quartiles projected to pixel; analytic ray evaluated at SAME pixel centre. Independent supplementary controls; full image mask unchanged.',
            gate='Original vs scalar <=1 byte, own vs scalar and paired <=2 bytes. Derived rounding/interpolation budget; not fitted to images.',
            samples=samples,original_peak=max(p['original_error'] for p in samples),
            own_peak=max(p['own_error'] for p in samples),paired_peak=max(p['paired_peak'] for p in samples))
    result['status']='Internal text-plane fragment fog only. Raster/coverage/continuous camera/whole-song residency/device validation unresolved; NOT full Diorama acceptance.'
    (out/f'{family}-comparison.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf8')
    print(json.dumps(dict(family=family,summary=summary,solid=result.get('solid_point_controls',{}) and {k:v for k,v in result['solid_point_controls'].items() if k!='samples'}),indent=2))
    names=(['latin-800-3-14.png','arabic-800-3-14.png','cjk-380-0-5.2.png','cjk-800-7-36.png'] if family=='fog' else
           ['whole-band-glyph-normal-800.png','far-cross-glyph-add-800.png','off-axis-solid-normal-800.png','beyond-solid-add-380.png'])
    sheet=Image.new('RGB',(1260,len(names)*338),(30,30,30));draw=ImageDraw.Draw(sheet)
    for i,name in enumerate(names):
        for j,(label,p) in enumerate([('Original Three fog',source/name),('Own without fog',native/('before-'+name)),('Own fragment fog',native/('after-'+name))]):
            draw.text((j*420+4,i*338+3),label,fill='white');draw.text((j*420+4,i*338+17),name,fill='white')
            im=Image.open(p).convert('RGB');im.thumbnail((420,300));sheet.paste(im,(j*420,i*338+35))
    sheet.save(out/f'{family}-contact.png')
    if samples:
        assert all(p['original_error']<=1 for p in samples),'Original shader/scalar discrepancy; investigate'
        assert all(p['own_error']<=2 and p['paired_peak']<=2 for p in samples),'Fog interpolation/alpha/order discrepancy; investigate'

compare('fog',54)
compare('fog-plane',56)
