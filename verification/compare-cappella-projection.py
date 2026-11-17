"""Fixed original/before/after all-nonbackground union for one projection repair."""
from pathlib import Path
import hashlib
import json
import numpy as np
from PIL import Image, ImageDraw
root=Path(__file__).resolve().parents[1]
source=root/'build/verification/cappella-presence-browser'
before=root/'.work/cappella-projection-before'
after=root/'.work/cappella-rejected-snapshot-frames'
out=root/'verification/cappella-projection';out.mkdir(exist_ok=True)
def rows(p):return [line.split('\t')for line in p.read_text(encoding='utf-8').splitlines()if line and not line.startswith('#')]
original=rows(root/'verification/cappella-fixtures/presence-observations.tsv')
def numeric(folder):
    actual=[r for p in folder.glob('*-geometry.tsv')for r in rows(p)]
    observations={(r[0],r[2]):r for r in actual};errors=[];presence=[]
    for name in sorted(set(r[0]for r in original)):
        expected=[r[2]for r in original if r[0]==name];ids=[r[2]for r in actual if r[0]==name]
        if expected!=ids:presence.append(dict(file=name,source=expected,native=ids))
    for r in original:
        n=observations.get((r[0],r[2]))
        if n is None:continue
        a=list(map(float,(','.join(r[4:7])).split(',')))
        b=list(map(float,(','.join(n[3:6])).split(',')))
        errors.extend(dict(file=r[0],id=r[2],field=i,source=x,native=y,error=abs(x-y))for i,(x,y)in enumerate(zip(a,b)))
    return dict(geometryPeak=max(e['error']for e in errors if e['field']!=8),opacityPeak=max(e['error']for e in errors if e['field']==8),presence=presence),errors
summaryBefore,errorsBefore=numeric(before);summaryAfter,errorsAfter=numeric(after)
bg=np.array([22,36,53]);frames=[]
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
for f in sorted(source.glob('*.png')):
    a,b,c=[np.asarray(Image.open(p).convert('RGB')).astype(np.int16)for p in(f,before/f.name,after/f.name)]
    assert a.shape==b.shape==c.shape
    mask=np.any(a!=bg,axis=2)|np.any(b!=bg,axis=2)|np.any(c!=bg,axis=2)
    item=dict(file=f.name,unionPixels=int(mask.sum()),hashes=[sha(p)for p in(f,before/f.name,after/f.name)])
    for key,im in [('before',b),('after',c)]:
        d=abs(a-im)
        item[key]=dict(foregroundMAE=float(d[mask].mean())if mask.any()else 0,fullMAE=float(d.mean()),channelPeak=int(d.max()),
            worst64=max(float(d[y:y+64,x:x+64].mean())for y in range(0,len(d),64)for x in range(0,len(d[0]),64)))
    frames.append(item)
assert len(frames)==240
report=dict(scope='Same exact-lock source240, baseline and candidate. One union per source/before/after triplet includes EVERY nonbackground pixel, including low alpha. No registration, fitted time or metric-threshold changes.',
    before=summaryBefore,after=summaryAfter,frames=frames,
    decision='REJECTED; production restored byte-for-byte. Mean improvement does not excuse overlap/calm regression.',
    geometryWorstBefore=sorted(errorsBefore,key=lambda e:e['error'],reverse=True)[:25],
    geometryWorstAfter=sorted(errorsAfter,key=lambda e:e['error'],reverse=True)[:25],
    sourceHashes={n:sha(root/f'.work/CappellaPlaybackPreparation-{n}-snapshot.kt')for n in ['before','rejected']})
for k,summary in [('before',summaryBefore),('after',summaryAfter)]:summary['meanForegroundMAE']=sum(f[k]['foregroundMAE']for f in frames)/len(frames)
(out/'comparison.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
(root/'build/verification/cappella-projection-geometry.json').write_text(json.dumps(dict(before=errorsBefore,after=errorsAfter),indent=2)+'\n',encoding='utf-8')
names=['rapid-chaotic-18-1020000.png','rapid-normal-18-1020000.png','overlap-chaotic-20-1300000.png']
sheet=Image.new('RGB',(1140,3*510),(30,30,30));draw=ImageDraw.Draw(sheet)
for i,name in enumerate(names):
    for j,(label,folder)in enumerate([('Original',source),('Before',before),('After',after)]):
        sheet.paste(Image.open(folder/name).convert('RGB'),(j*380,i*510+25));draw.text((j*380+2,i*510+3),label+' | '+name,fill='white')
sheet.save(out/'contact.png')
print(json.dumps(dict(before=summaryBefore,after=summaryAfter),indent=2))
