"""Original formula-driven RTL word registration, not pixel alignment."""
from pathlib import Path
import hashlib,json
import numpy as np
from PIL import Image,ImageDraw

root=Path(__file__).resolve().parents[1]
old=root/'.work/diorama-registration-before-3bcc28d'
rejected=root/'.work/diorama-registration-rejected-all-words'
out=root/'verification/diorama-rtl-registration';out.mkdir(exist_ok=True)
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
frozen=json.loads((old/'hashes.json').read_text())
for file,h in frozen.items():assert sha(old/file)==h,'Preserved baseline changed: '+file
base=json.loads((root/'verification/diorama-active-base-sampler/comparison.json').read_text())
layers=json.loads((root/'verification/diorama-active-layers/comparison.json').read_text())
bg=[22,36,53]

def compare(files):
    a={k:np.array(Image.open(p).convert('RGB'),dtype=np.int32) for k,p in files.items()}
    u=np.logical_or.reduce([np.any(x!=bg,axis=2) for x in a.values()]);metrics={}
    for k,x in a.items():
        d=abs(x-a['original']);occupied=np.any(x!=bg,axis=2);ys,xs=np.where(occupied)
        local=[dict(x=c,y=r,mae=float(d[r:r+64,c:c+64][u[r:r+64,c:c+64]].mean()))
               for r in range(0,x.shape[0],64) for c in range(0,x.shape[1],64) if u[r:r+64,c:c+64].any()]
        metrics[k]=dict(mae=float(d[u].mean()) if u.any() else 0,peak=int(d.max()),worst64=max(local,key=lambda r:r['mae']),
                        pixels=int(xs.size),bounds=[int(xs.min()),int(ys.min()),int(xs.max()+1),int(ys.max()+1)] if xs.size else None)
    return dict(unionPixels=int(u.sum()),metrics=metrics,hashes={k:sha(p) for k,p in files.items()})

rows=[]
for item in base['ownShapedRows']:
    name=item['file']
    files=dict(original=root/'build/verification/diorama-active-base-browser'/name,
               beforeOrdinary=old/'diorama-active-base-kotlin'/name,
               beforeSampler=old/'diorama-active-base-sampler-kotlin'/name,
               rejectedAllWordsOrdinary=rejected/'diorama-active-base-kotlin'/name,
               rejectedAllWordsSampler=rejected/'diorama-active-base-sampler-kotlin'/name,
               afterOrdinary=root/'build/verification/diorama-active-base-kotlin'/name,
               afterSampler=root/'build/verification/diorama-active-base-sampler-kotlin'/name)
    assert sha(files['original'])==item['hashes']['original']
    if not name.startswith('arabic'):
        for kind in ['Ordinary','Sampler']:assert sha(files['before'+kind])==sha(files['after'+kind]),'Non-RTL path changed: '+name
    rows.append(dict(file=name,**compare(files)))

layer_rows=[]
for item in layers['frames']:
    name=item['file'];files=dict(original=root/'build/verification/diorama-active-layers-browser'/name,
        beforeOrdinary=old/'diorama-active-layers-kotlin'/name,
        afterOrdinary=root/'build/verification/diorama-active-layers-kotlin'/name,
        afterSampler=root/'build/verification/diorama-active-layers-sampler-kotlin'/name)
    assert sha(files['original'])==next(v for k,v in item['png_sha256'].items() if 'browser' in k)
    if not name.startswith('arabic'):assert sha(files['beforeOrdinary'])==sha(files['afterOrdinary'])
    layer_rows.append(dict(file=name,**compare(files)))

def summary(items,before,after):
    return dict(means={k:float(np.mean([r['metrics'][k]['mae'] for r in items])) for k in [before,after]},
        worst64={k:max(r['metrics'][k]['worst64']['mae'] for r in items) for k in [before,after]},
        meanRegressions=[dict(file=r['file'],before=r['metrics'][before]['mae'],after=r['metrics'][after]['mae']) for r in items if r['metrics'][after]['mae']>r['metrics'][before]['mae']],
        localRegressions=[dict(file=r['file'],before=r['metrics'][before]['worst64'],after=r['metrics'][after]['worst64']) for r in items if r['metrics'][after]['worst64']['mae']>r['metrics'][before]['worst64']['mae']])

unchanged=0
for file,h in frozen.items():
    if file.split('/')[0] in ['diorama-mounted-kotlin','diorama-mounted-sampler-kotlin','diorama-mounted-active-base-kotlin','diorama-shaped-mask']:
        if file.endswith('.png'):assert sha(root/'build/verification'/file)==h;unchanged+=1

geometry=(root/'build/verification/diorama-active-registration-kotlin/geometry.tsv').read_text()
before_geometry=(old/'diorama-active-registration-kotlin/geometry.tsv').read_text()
(out/'before-geometry.tsv').write_text(before_geometry,encoding='utf8')
(out/'after-geometry.tsv').write_text(geometry,encoding='utf8')
rtl=[r.split('\t') for r in geometry.splitlines() if r.startswith('arabic\t')]
assert max(abs(float(r[8])) for r in rtl)<.001
report=dict(baseline='3bcc28d',sourceRevision='cc6226e464050a456a2913e062374f23d3844cc5',
    condition='Original fixed90 base and180 layer frames/fonts/viewport/time. SAME7-image base union including rejected broader candidate; SAME4-image layer union. No registration, pixel shift, alpha deletion, altered source frame or fitted offset.',
    sourceFormula='layoutDioramaActiveUnits: RTL center=total/2-prefix-ceil(unitAdvance)/2. Raster pad90. Complete paragraph still drawn, only safe separate RTL words register at that original anchor; cross-unit cluster/mixed-bidi retain canonical phase.',
    ordinary=summary(rows,'beforeOrdinary','afterOrdinary'),sampler=summary(rows,'beforeSampler','afterSampler'),
    rejectedAllWords=summary(rows,'beforeSampler','rejectedAllWordsSampler'),
    layersOrdinary=summary(layer_rows,'beforeOrdinary','afterOrdinary'),
    layersSamplerVsOldOrdinary=summary(layer_rows,'beforeOrdinary','afterSampler'),
    rows=rows,layerRows=layer_rows,unchangedMountedAndUnsafeCoveragePngs=unchanged,
    originalRtlPivotErrorsBefore=[float(r.split('\t')[8]) for r in before_geometry.splitlines() if r.startswith('arabic\t')],
    originalRtlPivotErrorsAfter=[float(r[8]) for r in rtl],
    originalWholeShapingConservation='Unsafe ligature/fragmented Arabic/mixed-bidi/CJK EXACT tests and original production masks unchanged',
    formalVisualAcceptance=False,deviceValidated=False,performanceMeasured=False)
(out/'comparison.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf8')
names=['arabic-800-1500.png','arabic-801-2300.png','arabic-801-3100.png','latin-380-5250.png']
sheet=Image.new('RGB',(1600,len(names)*300),'#191919');draw=ImageDraw.Draw(sheet)
for i,name in enumerate(names):
    paths=[('original',root/'build/verification/diorama-active-base-browser'/name),
           ('before',old/'diorama-active-base-sampler-kotlin'/name),
           ('rejected broad',rejected/'diorama-active-base-sampler-kotlin'/name),
           ('RTL-only',root/'build/verification/diorama-active-base-sampler-kotlin'/name)]
    for j,(label,path) in enumerate(paths):
        im=Image.open(path).convert('RGB')
        box=(180,190,450,370) if name.startswith('arabic') else (0,250,380,360)
        im=im.crop(box);im.thumbnail((395,260),Image.Resampling.NEAREST)
        if name.startswith('arabic'):im=im.resize((395,263),Image.Resampling.NEAREST)
        draw.text((j*400+3,i*300+3),label+' '+name,fill='white');sheet.paste(im,(j*400,i*300+25))
sheet.save(out/'contact.png')
print(json.dumps({k:v for k,v in report.items() if k not in ['rows','layerRows']},indent=2))
