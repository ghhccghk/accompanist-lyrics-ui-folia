"""Actual upstream RAF playback vs continuous media-time native response.
No registration, rate-specific phase fit, or target convergence.
"""
from pathlib import Path
import csv,hashlib,json,sys
import numpy as np
from PIL import Image,ImageDraw

root=Path(__file__).resolve().parents[1]
public='--public'in sys.argv
kind='public'if public else'motion'
source=root/f'build/verification/cadenza-{kind}-browser'
native=root/f'build/verification/cadenza-{kind}-kotlin'
out=root/f'verification/cadenza-{kind}';out.mkdir(exist_ok=True)
rows=[];hashes={};background=np.array([22,36,53])
for file in sorted(source.glob('*.png')):
    other=native/file.name
    if not other.exists():raise RuntimeError(f'Missing native frame {file.name}')
    a,b=[np.asarray(Image.open(p).convert('RGB')).astype(np.int16)for p in[file,other]]
    assert a.shape==b.shape
    mask=np.any(abs(a-background)>8,axis=2)|np.any(abs(b-background)>8,axis=2)
    diff=abs(a-b)
    rows.append(dict(file=file.name,foreground_pixels=int(mask.sum()),foreground_mae=float(diff[mask].mean())if mask.any()else 0,full_mae=float(diff.mean())))
    for p in[file,other]:hashes[p.relative_to(root).as_posix()]=hashlib.sha256(p.read_bytes()).hexdigest()
residuals=[]
if not public:
    with(native/'source-residual.tsv').open(encoding='utf-8')as f:residuals=list(csv.DictReader(f,delimiter='\t'))
report=dict(conditions=json.loads((root/f'verification/cadenza-fixtures/{"publicMotion"if public else"motion"}-conditions.json').read_text(encoding='utf-8')),
    scope='Original unmodified RAF target updates compared with event-based continuous forcing. Native frozen/reverse queries do not replay frames.',
    status=('Public calibration preview. 'if public else'Internal calibration only. ')+'Original cadence-dependent step response and font/coverage residuals remain; no final theme acceptance.',
    mean_foreground_mae=sum(r['foreground_mae']for r in rows)/len(rows),
    by_scene={s:sum(r['foreground_mae']for r in rows if r['file'].startswith(s+'-'))/sum(r['file'].startswith(s+'-')for r in rows)for s in['latin','wrap','arabic','cjk']},
    source_trajectory_residuals=residuals,frames=rows,png_sha256=hashes)
(out/'comparison.json').write_text(json.dumps(report,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
names=[f'{s}-{w}-60-{n}.png'for s,w,n in[('latin',380,2),('latin',380,4),('wrap',380,5),('arabic',800,3),('cjk',800,2),('cjk',800,6)]]
sheet=Image.new('RGB',(1000,len(names)*330),(30,30,30));draw=ImageDraw.Draw(sheet)
for i,name in enumerate(names):
    for j,folder in enumerate([source,native]):
        draw.text((j*500+5,i*330+4),f'{"Original RAF"if j==0 else "Public Cadenza"if public else"Kotlin continuous"} | {name}',fill='white')
        pic=Image.open(folder/name).convert('RGB');pic.thumbnail((500,300));sheet.paste(pic,(j*500,i*330+24))
sheet.save(out/'contact.png')
print(json.dumps({k:report[k]for k in['mean_foreground_mae','by_scene']},indent=2))
