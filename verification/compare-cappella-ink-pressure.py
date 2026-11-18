"""Compare a completed native240 recapture with a PRESERVED pre-repair matrix."""
from pathlib import Path
import argparse,hashlib,json
import numpy as np
from PIL import Image

root=Path(__file__).resolve().parents[1]
parser=argparse.ArgumentParser()
parser.add_argument('--baseline',required=True,help='Preserved native matrix relative to repository root')
parser.add_argument('--output',required=True,help='Report path relative to repository root')
args=parser.parse_args()
before=root/args.baseline;after=root/'build/verification/cappella-presence-kotlin';source=root/'build/verification/cappella-presence-browser'
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
geometry=[{'name':p.name,'before':sha(p),'after':sha(after/p.name)} for p in sorted(before.glob('*-geometry.tsv'))]
assert len(geometry)==6 and all(g['before']==g['after'] for g in geometry),'Unexpected geometry/order/opacity change'
files=sorted(before.glob('*.png'));assert len(files)==240
background=np.array([22,36,53]);frames=[]
for file in files:
    paths=[source/file.name,file,after/file.name]
    images=[np.asarray(Image.open(p).convert('RGB')).astype(np.int16) for p in paths]
    mask=np.logical_or.reduce([np.any(im!=background,axis=2) for im in images])
    row={'file':file.name,'hashes':[sha(p) for p in paths]}
    for label,im in zip(('before','after'),images[1:]):
        d=abs(images[0]-im)
        row[label]={'mae':float(d[mask].mean()) if mask.any() else 0,'peak':int(d.max()),'worst64':max(
            (float(d[y:y+64,x:x+64][mask[y:y+64,x:x+64]].mean()) for y in range(0,480,64) for x in range(0,380,64)
             if mask[y:y+64,x:x+64].any()),default=0)}
    row['changed']=row['hashes'][1]!=row['hashes'][2];frames.append(row)
report={'scope':'Same exact-lock original240 source and preserved before/after; all nonbackground pixels in one triple union including low alpha. No registration or discarded frames. Ink correction, not projection/whole-theme acceptance.',
    'sourceConditions':json.loads((source/'conditions.json').read_text(encoding='utf-8')),'geometry':geometry,
    'changedFrames':sum(f['changed'] for f in frames),'frames':frames}
for label in ('before','after'):
    report[label+'MAE']=sum(f[label]['mae'] for f in frames)/240
    report[label+'Worst64']=max(f[label]['worst64'] for f in frames)
output=root/args.output;output.parent.mkdir(parents=True,exist_ok=True)
output.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps({k:report[k] for k in ('changedFrames','beforeMAE','afterMAE','beforeWorst64','afterWorst64')}))
