"""Keep failed mask controls and paired, unchanged upstream stage matrices."""
from pathlib import Path
import hashlib,json
import numpy as np
from PIL import Image,ImageDraw

root=Path(__file__).resolve().parents[1]
out=root/'verification/diorama-shaped-mask';out.mkdir(parents=True,exist_ok=True)
native=root/'build/verification/diorama-shaped-mask'
conditions=json.loads((root/'verification/diorama-fixtures/shaped-mask-v1/conditions.json').read_text(encoding='utf8'))
bg=np.array([22,36,53])

def load(path):
    im=Image.open(path).convert('RGBA')
    back=Image.new('RGBA',im.size,(22,36,53,255));back.alpha_composite(im)
    return np.asarray(back.convert('RGB')).astype(np.int16)

def compare(files):
    a,b,c=[load(p) for p in files]
    assert a.shape==b.shape==c.shape
    mask=np.any(a!=bg,axis=2)|np.any(b!=bg,axis=2)|np.any(c!=bg,axis=2)
    result=dict(png_sha256={str(p.relative_to(root)):hashlib.sha256(p.read_bytes()).hexdigest() for p in files})
    for mode,im in [('before',b),('after',c)]:
        d=abs(a-im)
        local=[float(d[y:y+64,x:x+64][mask[y:y+64,x:x+64]].mean())
               for y in range(0,a.shape[0],64) for x in range(0,a.shape[1],64) if mask[y:y+64,x:x+64].any()]
        result[mode]=dict(foreground_mae=float(d[mask].mean()) if mask.any() else 0.0,full_mae=float(d.mean()),
                          worst64=max(local,default=0.0),channel_peak=int(d.max()))
    return result

ownership=[]
for line in (native/'ownership.tsv').read_text().splitlines():
    if not line or line.startswith('#'):continue
    fields=line.split('\t');name=fields[0];values=list(map(int,fields[1:]))
    assert values[-3:]==[0,0,0], 'Exact full-shaped coverage gate must pass'
    files=[root/'build/verification/diorama-shaped-mask-browser'/f'{name}.png',native/f'{name}-old.png',native/f'{name}-production.png']
    ownership.append(dict(case=name,advance_clip_control=dict(different=values[0],lost_alpha=values[1],extra_alpha=values[2]),
                          rejected_colour_span=dict(different=values[3],lost_alpha=values[4],extra_alpha=values[5]),
                          product_vs_complete_own_run=dict(different=values[6],lost_alpha=values[7],extra_alpha=values[8]),
                          independent_original_complete_canvas=compare(files)))
assert len(ownership)==4

matrices={}
for kind,count in [('active-base',90),('active-layers',180),('neighbors',54)]:
    before=root/f'.work/diorama-shaping-before/{kind}'
    after=root/f'build/verification/diorama-{kind}-kotlin'
    source=root/f'build/verification/diorama-{kind}-browser'
    if kind=='neighbors':
        for width in [380,800]:assert (after/f'complete-{width}.txt').is_file()
        captures=json.loads((root/'verification/diorama-fixtures/neighbors-v1/scene.json').read_text(encoding='utf8'))['captures']
        names=[c['file'] for c in captures];prefix='after-'
    else:
        captures=json.loads((root/f'verification/diorama-fixtures/{kind}.json').read_text(encoding='utf8'))['observations']
        names=[c['file'] for c in captures];prefix=''
    assert len(names)==count
    records=[dict(file=name,**compare([source/name,before/(prefix+name),after/(prefix+name)])) for name in names]
    matrices[kind]=dict(frames=records,
        mean={mode:float(np.mean([r[mode]['foreground_mae']for r in records])) for mode in ['before','after']},
        worst_frame={mode:max(r[mode]['foreground_mae'] for r in records) for mode in ['before','after']},
        worst64={mode:max(r[mode]['worst64'] for r in records) for mode in ['before','after']},
        regressed_frames=[r['file']for r in records if r['after']['foreground_mae']>r['before']['foreground_mae']])

report=dict(scope=conditions['scope'],ownership=ownership,matrices=matrices,
            mask='Same source/before/after all-nonbackground union per image, including low alpha. No registration, cutoff, delay or selection.',
            status='Coverage/shaping repair and preserved source-matrix regression, not full original per-unit mixed-bidi parity or complete Diorama/Android acceptance.')
(out/'comparison.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf8')
sheet=Image.new('RGB',(1260,4*250),(30,30,30));draw=ImageDraw.Draw(sheet)
for i,c in enumerate(ownership):
    name=c['case']
    for j,(label,p)in enumerate([('Original complete Canvas run',root/'build/verification/diorama-shaped-mask-browser'/f'{name}.png'),
                                 ('Advance-rectangle clip control',native/f'{name}-old.png'),('Own exact coverage partition',native/f'{name}-production.png')]):
        draw.text((j*420+4,i*250+3),label+' | '+name,fill='white')
        im=Image.fromarray(load(p).astype('uint8'));im.thumbnail((420,220));sheet.paste(im,(j*420,i*250+25))
sheet.save(out/'contact.png')
print(json.dumps({k:{p:v for p,v in m.items()if p!='frames'} for k,m in matrices.items()},indent=2))
