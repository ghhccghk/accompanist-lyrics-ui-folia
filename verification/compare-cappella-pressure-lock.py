"""Raw old/exact-lock dense source differences; no alignment or alpha threshold."""
from pathlib import Path
import json
import numpy as np
from PIL import Image, ImageDraw
root=Path(__file__).resolve().parents[1]
p=root/'verification/motion-lock-audit/cappella-pressure.json'
d=json.loads(p.read_text(encoding='utf-8'))
before=root/'.work'/d['run']/'build/verification/cappella-presence-browser'
after=root/'build/verification/cappella-presence-browser'
changed=[]
for f in d['frames']:
    if f['before']==f['exact']: continue
    a,b=[np.asarray(Image.open(folder/f['file']).convert('RGB')).astype(int) for folder in (before,after)]
    delta=abs(a-b)
    f.update(changedPixels=int(np.any(delta,axis=2).sum()),channelPeak=int(delta.max()),fullMAE=float(delta.mean()))
    changed.append(f)
differences=[]
def walk(a,b,path):
    if type(a)!=type(b): differences.append(dict(path=path,before=a,exact=b))
    elif isinstance(a,dict):
        for k in a.keys()|b.keys():
            if k not in a or k not in b: differences.append(dict(path=path+[k],before=a.get(k),exact=b.get(k)))
            else: walk(a[k],b[k],path+[k])
    elif isinstance(a,list):
        if len(a)!=len(b): differences.append(dict(path=path,before=a,exact=b))
        else:
            for i,(x,y) in enumerate(zip(a,b)):walk(x,y,path+[i])
    elif a!=b:differences.append(dict(path=path,before=a,exact=b))
walk(*[json.loads((folder/'states.json').read_text(encoding='utf-8')) for folder in (before,after)],[])
d['stateDifferences']=differences
d['note']='All numeric geometry/presence/opacity fixture bytes unchanged. Preserve raw pixel/glyph differences; cause not isolated to dependency versions.'
p.write_text(json.dumps(d,indent=2)+'\n',encoding='utf-8')
names=[f['file'] for f in changed[:3]]
sheet=Image.new('RGB',(760,len(names)*510),(30,30,30));draw=ImageDraw.Draw(sheet)
for i,name in enumerate(names):
    for j,folder in enumerate((before,after)):
        sheet.paste(Image.open(folder/name).convert('RGB'),(j*380,i*510+25))
        draw.text((j*380+3,i*510+3),('Previous | 'if j==0 else'Exact lock | ')+name,fill='white')
sheet.save(p.parent/'cappella-pressure-source-contact.png')
print(json.dumps(dict(changedFrames=len(changed),changedPixelRange=[min(f['changedPixels'] for f in changed),max(f['changedPixels'] for f in changed)],channelPeak=max(f['channelPeak'] for f in changed),fullMAEPeak=max(f['fullMAE'] for f in changed),stateChanges=len(differences)),indent=2))
