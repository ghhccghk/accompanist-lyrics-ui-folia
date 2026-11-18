"""Actual original Tilt+subtitle overlay versus public core stage; no alignment."""
from pathlib import Path
import json, hashlib
import numpy as np
from PIL import Image, ImageDraw

root=Path(__file__).resolve().parents[1]
base=root/'build/verification'
out=root/'verification/tilt-subtitles'
out.mkdir(exist_ok=True)
original=json.loads((root/'verification/tilt-fixtures/subtitles-v2/conditions.json').read_text(encoding='utf-8'))
numeric={}
for row in (base/'tilt-subtitles-kotlin/rows.tsv').read_text(encoding='utf-8').splitlines():
    if not row or row.startswith('#'):continue
    name,role,index,text,*values=row.split('\t')
    numeric[name,role,int(index)]=dict(zip(['font','width','height','top'],map(float,values)))

rows=[]
for capture in original['captures']:
    name,width=capture['file'],capture['width']
    paths=[base/f'tilt-subtitles-{p}'/name for p in ['browser','kotlin']]
    pictures=[Image.open(path).convert('RGB').crop((0,0,width,600)) for path in paths]
    a,b=[np.array(im,dtype=np.int16) for im in pictures]
    mask=np.any(a!=[22,36,53],axis=2)|np.any(b!=[22,36,53],axis=2)
    error=np.abs(a-b)
    local=[error[y:y+64,x:x+64][mask[y:y+64,x:x+64]].mean()
        for y in range(0,600,64) for x in range(0,width,64) if mask[y:y+64,x:x+64].any()]
    entry=dict(file=name,foregroundPixels=int(mask.sum()),mae=float(error[mask].mean()),
        channelPeak=int(error.max()),worst64px=float(max(local)),hashes=[hashlib.sha256(p.read_bytes()).hexdigest() for p in paths])
    if capture['mediaMs']==2400:
        geometry=[]
        for role in ['main','subtitle']:
            for i,r in enumerate(capture['main' if role=='main' else 'subtitles']):
                expected=dict(font=r['font'],width=r.get('width',r['rect']['width']),height=r.get('height',r['rect']['height']),
                    top=r['rect']['top']-(r.get('motion',{}).get('y',0) if role=='main' else 0))
                actual=numeric[name,role,i]
                geometry.append(dict(role=role,index=i,expected=expected,actual=actual,difference={k:actual[k]-expected[k] for k in expected}))
        entry['settledGeometry']=geometry
        # These are actual original current-transform boxes, not an assertion
        # of ink intersection nor a permission to call the theme readable.
        subtitle=capture['subtitles'][0]['rect']
        entry['originalMainSubtitleBoxIntersections']=[dict(row=i,verticalPixels=max(0,min(r['rect']['bottom'],subtitle['bottom'])-max(r['rect']['top'],subtitle['top'])))
            for i,r in enumerate(capture['main'])]
    rows.append(entry)

report=dict(scope='Same original/core public inputs,30 frames,all non-background pixels including low alpha. No pixel threshold, registration or collision suppression. Known unused test-root right margin only cropped to viewport.',
    originalFixtureSha256=hashlib.sha256((root/'verification/tilt-fixtures/subtitles-v2/conditions.json').read_bytes()).hexdigest(),
    frames=len(rows),meanMAE=sum(r['mae'] for r in rows)/len(rows),worst64px=max(r['worst64px'] for r in rows),
    geometryPeaks={key:max(abs(g['difference'][key]) for r in rows if 'settledGeometry'in r for g in r['settledGeometry'])for key in ['font','width','height','top']},rows=rows)
(out/'comparison.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
names=[f'{width}-{scale}-2400.png' for width in [380,800] for scale in [1,1.4,2]]
sheet=Image.new('RGB',(800,6*320),'#202020');draw=ImageDraw.Draw(sheet)
for i,name in enumerate(names):
    for j,label in enumerate(['browser','kotlin']):
        im=Image.open(base/f'tilt-subtitles-{label}'/name).convert('RGB')
        width=int(name.split('-')[0]);im=im.crop((0,0,width,600));im.thumbnail((400,290))
        draw.text((j*400+5,i*320+5),f'{label} | {name}',fill='white')
        sheet.paste(im,(j*400+(400-im.width)//2,i*320+25))
sheet.save(out/'contact.png')
print(json.dumps({k:v for k,v in report.items() if k!='rows'},indent=2))
print('Original box intersections:',[(r['file'],max(i['verticalPixels'] for i in r['originalMainSubtitleBoxIntersections'])) for r in rows if 'settledGeometry'in r])
