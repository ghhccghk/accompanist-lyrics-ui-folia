"""Integrated article/camera/text comparison; preserve coordinates and residuals."""
from pathlib import Path
import argparse, json, hashlib, math
import numpy as np
from PIL import Image, ImageDraw

root = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser()
parser.add_argument('--native-dir', default='build/verification/fume-scene-kotlin')
parser.add_argument('--out', default='verification/fume-scene')
args = parser.parse_args()
source = root / 'build/verification/fume-scene-browser'
native = root / args.native_dir
out = root / args.out; out.mkdir(exist_ok=True, parents=True)
background = np.array([22,36,53]); rows=[]; hashes={}
for file in sorted(source.glob('*.png')):
    other = native / file.name
    if not other.exists(): raise RuntimeError(f'Missing native frame {file.name}')
    a,b = [np.asarray(Image.open(p).convert('RGB')).astype(np.int16) for p in [file,other]]
    assert a.shape == b.shape
    mask = np.any(abs(a-background)>8,axis=2)|np.any(abs(b-background)>8,axis=2)
    diff = abs(a-b)
    rows.append(dict(file=file.name,foreground_pixels=int(mask.sum()),foreground_mae=float(diff[mask].mean()) if mask.any() else 0,
                     full_mae=float(diff.mean())))
    for p in [file,other]: hashes[p.relative_to(root).as_posix()]=hashlib.sha256(p.read_bytes()).hexdigest()

camera=[]
raw = [r.split('\t') for r in (root/'verification/fume-fixtures/scene-camera.tsv').read_text(encoding='utf-8').splitlines() if r and not r.startswith('#')]
for row in raw:
    prefix=f'{row[0]}-{row[1]}-{row[2]}'
    reports=[r.split('\t') for r in (native/f'{prefix}.tsv').read_text(encoding='utf-8').splitlines() if r and not r.startswith('#')]
    n=next(r for r in reports if float(r[0])==float(row[3]))
    x,y,s = map(float,n[1:4]); qx,qy,qs=map(float,row[4:7])
    camera.append(dict(frame=prefix+'-'+str(round(float(row[3])*1000)),
        world_distance=math.hypot(x-qx,y-qy),screen_distance=math.hypot(x-qx,y-qy)*max(s,qs),scale_error=abs(s-qs),
        restore_error=abs(float(n[4])-float(row[7])),nodes=int(n[5])))

report=dict(conditions=json.loads((source/'conditions.json').read_text(encoding='utf-8')),
    source='untouched original article layout, stepFumeCamera60Hz, static snapshots and live draw, source text-hold duration',
    native='independently measured article and focus geometry; complete shaped-row text; '+
        ('explicit shared original camera isolates raster only' if 'source-camera' in args.native_dir else 'continuous media camera'),
    status='Integrated calibration evidence; source camera and raster residuals remain, NOT final acceptance or Android runtime',
    frames=rows, camera=camera, mean_foreground_mae=sum(r['foreground_mae']for r in rows)/len(rows),
    by_glow={str(g):sum(r['foreground_mae']for r in rows if r['file'].split('-')[1]==str(g))/sum(r['file'].split('-')[1]==str(g)for r in rows) for g in [0,1]},
    by_width={str(w):sum(r['foreground_mae']for r in rows if r['file'].split('-')[0]==str(w))/sum(r['file'].split('-')[0]==str(w)for r in rows) for w in [380,800,801]},
    maximum_screen_distance=max(r['screen_distance']for r in camera),maximum_scale_error=max(r['scale_error']for r in camera),png_sha256=hashes)
(out/'comparison.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
names=['800-1-false-750.png','800-1-false-1300.png','380-1-true-3150.png',
       '800-1-false-6300.png','800-1-false-9500.png','800-1-false-11500.png']
sheet=Image.new('RGB',(1000,len(names)*310),(30,30,30));draw=ImageDraw.Draw(sheet)
for i,name in enumerate(names):
    for j,folder in enumerate([source,native]):
        draw.text((j*500+5,i*310+3),f'{"Original"if j==0 else "Kotlin"} | {name}',fill='white')
        pic=Image.open(folder/name).convert('RGB');pic.thumbnail((500,280));sheet.paste(pic,(j*500,i*310+22))
sheet.save(out/'contact.png')
print(json.dumps({k:v for k,v in report.items()if k not in ['png_sha256','frames','camera']},indent=2))
