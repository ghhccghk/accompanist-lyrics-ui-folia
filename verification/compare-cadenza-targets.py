"""Actual mounted upstream settled targets vs independent native text stage.
No position/time registration. This deliberately does NOT test transition smoothing.
"""
from pathlib import Path
import base64,hashlib,json
import numpy as np
from PIL import Image,ImageDraw

root=Path(__file__).resolve().parents[1]
source=root/'build/verification/cadenza-target-browser'
native=root/'build/verification/cadenza-target-kotlin'
out=root/'verification/cadenza-targets';out.mkdir(exist_ok=True)
rows=[];hashes={};background=np.array([22,36,53]);geometry=[]
for file in sorted(source.glob('*.png')):
    other=native/file.name
    if not other.exists():raise RuntimeError(f'Missing native frame {file.name}')
    a,b=[np.asarray(Image.open(p).convert('RGB')).astype(np.int16) for p in [file,other]]
    assert a.shape==b.shape
    mask=np.any(abs(a-background)>8,axis=2)|np.any(abs(b-background)>8,axis=2)
    diff=abs(a-b)
    rows.append(dict(file=file.name,foreground_pixels=int(mask.sum()),foreground_mae=float(diff[mask].mean())if mask.any()else 0,full_mae=float(diff.mean())))
    for p in[file,other]:hashes[p.relative_to(root).as_posix()]=hashlib.sha256(p.read_bytes()).hexdigest()
states=json.loads((source/'source-states.json').read_text(encoding='utf-8'))
for scene in ['latin','wrap','arabic','cjk']:
    for width in[380,800]:
        p=states[f'{scene}-{width}-0']['prepared']
        n=[r.split('\t')for r in(native/f'{scene}-{width}.tsv').read_text(encoding='utf-8').splitlines()if r and not r.startswith('#')]
        size=list(map(float,n[0][0].split(',')))
        actual={base64.b64decode(r[0]).decode():list(map(float,r[3].split(',')))for r in n[1:]}
        source_ids={r['id']for r in p['placements']}
        shared=[]
        for r in p['placements']:
            if r['id']in actual:
                shared.append(dict(id=r['id'],delta=[a-b for a,b in zip(actual[r['id']],[r[k]for k in['x','y','width','height','scale']])]))
        geometry.append(dict(scene=scene,width=width,size_delta=[a-b for a,b in zip(size,[p[k]for k in['fontPx','lineHeight','maxWidth']])],
            source_only=sorted(source_ids-actual.keys()),native_only=sorted(actual.keys()-source_ids),shared=shared))
report=dict(conditions=json.loads((source/'conditions.json').read_text(encoding='utf-8')),
    scope='Actual mounted DOM/Canvas converged targets; native independently measures/wraps and paints whole shaped fragments. NO smoothing/host line-switch acceptance.',
    status='Calibration only; glyph and geometry residuals are NOT accepted theme equivalence. No public selector.',
    mean_foreground_mae=sum(r['foreground_mae']for r in rows)/len(rows),
    by_scene={s:sum(r['foreground_mae']for r in rows if r['file'].startswith(s+'-'))/sum(r['file'].startswith(s+'-')for r in rows)for s in['latin','wrap','arabic','cjk']},
    by_glow={g:sum(r['foreground_mae']for r in rows if r['file'].split('-')[2]==g)/sum(r['file'].split('-')[2]==g for r in rows)for g in['0','1']},
    frames=rows,geometry=geometry,png_sha256=hashes)
(out/'comparison.json').write_text(json.dumps(report,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
names=[f'{s}-{w}-1-{t}.png'for s,w,t in[('latin',380,750),('latin',800,1600),('wrap',380,2200),('arabic',800,1300),('cjk',380,850),('cjk',800,2800)]]
sheet=Image.new('RGB',(1000,len(names)*330),(30,30,30));draw=ImageDraw.Draw(sheet)
for i,name in enumerate(names):
    for j,folder in enumerate([source,native]):
        draw.text((j*500+5,i*330+4),f'{"Original settled"if j==0 else "Kotlin target"} | {name}',fill='white')
        pic=Image.open(folder/name).convert('RGB');pic.thumbnail((500,300));sheet.paste(pic,(j*500,i*330+24))
sheet.save(out/'contact.png')
print(json.dumps({k:v for k,v in report.items()if k not in['frames','geometry','png_sha256','conditions']},indent=2))
