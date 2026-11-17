"""Full mounted original playback, measured against independently prepared native media state.
No phase alignment, shared source geometry, screenshot registration or tolerance fitting.
"""
from pathlib import Path
import json,hashlib,sys
import numpy as np
from PIL import Image,ImageDraw
root=Path(__file__).resolve().parents[1]
matrix='public'if'--public'in sys.argv else'presence'if'--presence'in sys.argv else'playback'
fixture='playback'if matrix=='public'else matrix
source=root/f'build/verification/cappella-{fixture}-browser';native=root/f'build/verification/cappella-{matrix}-kotlin'
out=root/f'verification/cappella-{matrix}';out.mkdir(exist_ok=True)
background=np.array([22,36,53]);frames=[];hashes={};numeric=[];mismatches=[]
for f in sorted(source.glob('*.png')):
    other=native/f.name
    assert other.exists(),f'Missing native {f.name}'
    a,b=[np.asarray(Image.open(p).convert('RGB')).astype(np.int16)for p in[f,other]]
    assert a.shape==b.shape
    cutoff=0 if matrix=='public'else 8
    mask=np.any(abs(a-background)>cutoff,axis=2)|np.any(abs(b-background)>cutoff,axis=2)
    diff=abs(a-b)
    frames.append(dict(file=f.name,foreground_mae=float(diff[mask].mean())if mask.any()else 0,full_mae=float(diff.mean()),
        rgb_peak=int(diff.max()),worst_64px_tile_mae=max(float(diff[y:y+64,x:x+64].mean())for y in range(0,a.shape[0],64)for x in range(0,a.shape[1],64))))
    for p in[f,other]:hashes[p.relative_to(root).as_posix()]=hashlib.sha256(p.read_bytes()).hexdigest()
expected=sum(1 for r in(root/f'verification/cappella-fixtures/{fixture}-frames.tsv').read_text().splitlines()if r and not r.startswith('#'))
assert len(frames)==expected,(len(frames),expected)
def rows(path):return[r.split('\t')for r in path.read_text(encoding='utf-8').splitlines()if r and not r.startswith('#')]
original=rows(root/f'verification/cappella-fixtures/{fixture}-observations.tsv')
actual=[r for p in native.glob('*-geometry.tsv')for r in rows(p)]
for file in sorted(set(r[0]for r in original)):
    a=[r for r in original if r[0]==file];b=[r for r in actual if r[0]==file]
    if[r[2]for r in a]!=[r[2]for r in b]:mismatches.append(dict(file=file,original_ids=[r[2]for r in a],native_ids=[r[2]for r in b]))
    for r in a:
        n=next((v for v in b if v[2]==r[2]),None)
        if n is None:continue
        source_values=[float(x)for x in (r[4]+','+r[5]+','+r[6]).split(',')]
        native_values=[float(x)for x in (n[3]+','+n[4]+','+n[5]).split(',')]
        for i,(s,v)in enumerate(zip(source_values,native_values)):numeric.append([file,r[2],i,s,v,abs(s-v)])
(out/'residual.tsv').write_text('# file message field original native error\n'+'\n'.join('\t'.join(map(str,r))for r in numeric)+'\n',encoding='utf-8')
report=dict(conditions=json.loads((source/'conditions.json').read_text(encoding='utf-8')),
    status=('Published core/public FoliaLyricsView calibration preview; public/internal pixels checked exactly. All nonbackground pixels including low alpha; not comparable to prior >8 foreground metric. Not final visual acceptance.'if matrix=='public'else'Internal moving-stage calibration, not final Cappella acceptance. Full mounted transition/layout residuals below remain required work.'),
    mean_foreground_mae=sum(r['foreground_mae']for r in frames)/len(frames),
    maximum_geometry_error=max(r[-1]for r in numeric if r[2]!=8),maximum_opacity_error=max(r[-1]for r in numeric if r[2]==8),
    presence_mismatches=mismatches,frames=frames,png_sha256=hashes)
(out/'comparison.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
names=['latin-380-480-normal-800.png','latin-380-480-chaotic-3200.png','latin-380-480-normal-8800.png',
    'wrap-800-600-normal-9100.png','arabic-800-600-normal-12750.png','cjk-1200-600-normal-14000.png']
if matrix=='presence':
    names=[next(f.name for f in sorted(source.glob(pattern)))for pattern in[
        'rapid-normal-*-857000.png','rapid-normal-*-937000.png','rapid-chaotic-*-1300000.png',
        'overlap-calm-*-1190000.png','overlap-normal-*-1367000.png','overlap-chaotic-*-2000000.png']]
sheet=Image.new('RGB',(1200,len(names)*330),(30,30,30));draw=ImageDraw.Draw(sheet)
for i,name in enumerate(names):
    for j,folder in enumerate([source,native]):
        draw.text((j*600+5,i*330+4),f'{"Original live stage"if j==0 else"Kotlin media stage"} | {name}',fill='white')
        pic=Image.open(folder/name).convert('RGB');pic.thumbnail((590,295));sheet.paste(pic,(j*600,i*330+25))
sheet.save(out/'contact.png')
print(json.dumps({k:(len(report[k])if k=='presence_mismatches'else report[k])for k in['mean_foreground_mae','maximum_geometry_error','maximum_opacity_error','presence_mismatches']},indent=2))
