"""Compare exact-lock recapture with preserved historical source frames, no alignment."""
from pathlib import Path
import json
import numpy as np
from PIL import Image,ImageDraw
root=Path(__file__).resolve().parents[1]
p=root/'verification/motion-lock-audit/comparison.json'
d=json.loads(p.read_text(encoding='utf-8'));run=d.get('run','motion-lock-audit')
assert run.replace('-','').isalnum() and '..' not in run
baseline=root/'.work'/run
for group in d['reports']:
    name=group['theme'];before=baseline/name
    after=root/('build/verification/pendolo-public-stage-browser'if name=='pendolo'else'build/verification/cappella-playback-browser')
    for f in group['pngs']:
        if f['before']==f['exactLock']:continue
        a,b=[np.asarray(Image.open(folder/f['file']).convert('RGB')).astype(int)for folder in[before,after]]
        delta=abs(a-b);mask=np.any(delta,axis=2)
        f.update(changedPixels=int(mask.sum()),channelPeak=int(delta.max()),fullMAE=float(delta.mean()))
    if name=='cappella':
        a,b=[json.loads((folder/'states.json').read_text(encoding='utf-8'))for folder in[before,after]]
        differences=[]
        def walk(x,y,path):
            if type(x)!=type(y):differences.append(dict(path=path,before=x,exactLock=y))
            elif isinstance(x,dict):
                assert x.keys()==y.keys()
                for key in x:walk(x[key],y[key],path+[key])
            elif isinstance(x,list):
                assert len(x)==len(y)
                for i,(c,e)in enumerate(zip(x,y)):walk(c,e,path+[i])
            elif x!=y:differences.append(dict(path=path,before=x,exactLock=y))
        walk(a,b,[])
        group.pop('passiveGlyphCounts',None);group['stateDifferences']=differences
        group['note']='Numeric fixture tables unchanged; source pixel/state differences preserved. Dependency attribution unproven. No repeat/cherry-pick or final visual acceptance.'
p.write_text(json.dumps(d,indent=2)+'\n',encoding='utf-8')
files=['latin-380-480-chaotic-3400.png','wrap-800-600-normal-3200.png','wrap-800-600-normal-11300.png']
sheet=Image.new('RGB',(1000,len(files)*300),(30,30,30));draw=ImageDraw.Draw(sheet)
for i,f in enumerate(files):
    for j,folder in enumerate([baseline/'cappella',root/'build/verification/cappella-playback-browser']):
        im=Image.open(folder/f).convert('RGB');im.thumbnail((500,270));sheet.paste(im,(j*500,i*300+25))
        draw.text((j*500+4,i*300+4),f'{"Previous"if j==0 else"Exact lock"} | {f}',fill='white')
sheet.save(p.parent/'source-contact.png')
print(json.dumps([{k:g[k]for k in['theme','frames','changedFrames']}for g in d['reports']]))
