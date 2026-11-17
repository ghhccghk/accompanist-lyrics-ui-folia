"""Actual original component vs independently prepared native text, no registration."""
from pathlib import Path
import hashlib,json
import numpy as np
from PIL import Image,ImageDraw

root=Path(__file__).resolve().parents[1]
source=root/'build/verification/pendolo-chorus-sweep-browser'
native=root/'build/verification/pendolo-chorus-sweep-kotlin'
out=root/'verification/pendolo-chorus-sweep'
out.mkdir(exist_ok=True)
conditions=json.loads((root/'verification/pendolo-fixtures/chorus-sweep-conditions.json').read_text())
frames=[];hashes={}
for file in sorted(source.glob('*.png')):
    other=native/file.name
    assert other.exists(),other
    a,b=[np.asarray(Image.open(p).convert('RGB')).astype(np.int16) for p in (file,other)]
    assert a.shape==b.shape
    mask=np.any(abs(a-[22,36,53])>8,axis=2)|np.any(abs(b-[22,36,53])>8,axis=2)
    difference=abs(a-b)
    frames.append(dict(file=file.name,foreground_mae=float(difference[mask].mean()) if mask.any() else 0,full_mae=float(difference.mean())))
    for p in (file,other):hashes[p.relative_to(root).as_posix()]=hashlib.sha256(p.read_bytes()).hexdigest()
assert len(frames)==conditions['frames']
residual=[r.split('\t') for p in native.glob('*-residual.tsv') for r in p.read_text(encoding='utf8').splitlines() if r and not r.startswith('#')]
assert all(not p.read_text(encoding='utf8').strip() for p in native.glob('*-mismatches.txt'))
report=dict(conditions=conditions,status='Isolated explicit-chorus masked sweep, sequential filters and keyword colors; no halo/wheel/theme/device acceptance.',
    maximum_width_or_prefix_error=max(float(r[-1]) for r in residual),
    mean_foreground_mae=sum(r['foreground_mae'] for r in frames)/len(frames),
    max_foreground_mae=max(r['foreground_mae'] for r in frames),frames=frames,png_sha256=hashes)
software=[]
for file in sorted((root/'build/verification/pendolo-chorus-sweep-software').glob('*.png')):
    a,b=[np.asarray(Image.open(p).convert('RGB')).astype(np.int16) for p in (source/file.name,file)]
    mask=np.any(abs(a-[22,36,53])>8,axis=2)|np.any(abs(b-[22,36,53])>8,axis=2)
    software.append(dict(file=file.name,foreground_mae=float(abs(a-b)[mask].mean()) if mask.any() else 0))
    hashes[file.relative_to(root).as_posix()]=hashlib.sha256(file.read_bytes()).hexdigest()
assert len(software)==132
report['software']=dict(frames=software,mean_foreground_mae=sum(r['foreground_mae'] for r in software)/len(software),
    max_foreground_mae=max(r['foreground_mae'] for r in software))
unchanged=[]
for name in ('pendolo-sweep','pendolo-stage','pendolo-tick-stage','pendolo-color-sweep'):
    previous=json.loads((root/'verification'/name/'comparison.json').read_text(encoding='utf8'))
    if name=='pendolo-tick-stage': previous=previous['pendolo-tick-stage-kotlin']
    for file,expected in previous['png_sha256'].items():
        if '-kotlin/' in file:
            assert hashlib.sha256((root/file).read_bytes()).hexdigest()==expected,file
            unchanged.append(file)
report['unchanged_prior_native_pngs']=len(unchanged)
(out/'comparison.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf8')
names=['calm-latin-150-1600.png','normal-latin-300-2400.png','chaotic-latin-150-4000.png','calm-cjk-150-1600.png','normal-cjk-300-2400.png','chaotic-cjk-150-4000.png']
sheet=Image.new('RGB',(800,len(names)*205),(30,30,30));draw=ImageDraw.Draw(sheet)
for i,name in enumerate(names):
    for j,folder in enumerate((source,native)):
        draw.text((j*400+5,i*205+3),f'{"Original" if j==0 else "Kotlin"} | {name}',fill='white')
        pic=Image.open(folder/name).convert('RGB').crop((0,0,380,180))
        sheet.paste(pic,(j*400,i*205+23))
sheet.save(out/'contact.png')
print(json.dumps({k:report[k] for k in ('maximum_width_or_prefix_error','mean_foreground_mae','max_foreground_mae')},indent=2))
print(json.dumps(dict(software_mean=report['software']['mean_foreground_mae'],software_peak=report['software']['max_foreground_mae'],unchanged=len(unchanged))))
