"""Internal bounded sampler, same frozen48 mounted and24 plain matrix."""
from pathlib import Path
import hashlib
import json
import numpy as np
from PIL import Image, ImageDraw

root = Path(__file__).resolve().parents[1]
out = root / 'verification/diorama-plain-sampler'
out.mkdir(exist_ok=True)
sha = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
source_hashes = json.loads((root / '.work/diorama-coverage-before-c3e464c/hashes.json').read_text(encoding='utf8'))
before = json.loads((root / '.work/diorama-mip-before-c0e2804/hashes.json').read_text(encoding='utf8'))
mounted = json.loads((root / 'verification/diorama-fixtures/mounted-cut-v1/scene.json').read_text(encoding='utf8'))
plain = json.loads((root / 'verification/diorama-plain-fragment/comparison.json').read_text(encoding='utf8'))

def compare(files):
    images = {k: np.array(Image.open(p).convert('RGB'), dtype=np.int32) for k, p in files.items()}
    bg = [22, 36, 53]
    union = np.logical_or.reduce([np.any(a != bg, axis=2) for a in images.values()])
    metrics = {}
    for label, a in images.items():
        d = abs(a - images['original'])
        occupied = np.any(a != bg, axis=2)
        ys, xs = np.where(occupied)
        local = [float(d[y:y+64, x:x+64][union[y:y+64, x:x+64]].mean())
                 for y in range(0, a.shape[0], 64) for x in range(0, a.shape[1], 64) if union[y:y+64, x:x+64].any()]
        metrics[label] = dict(mae=float(d[union].mean()) if union.any() else 0, peak=int(d.max()), worst64=max(local, default=0),
                              pixels=int(xs.size), bounds=[int(xs.min()),int(ys.min()),int(xs.max()+1),int(ys.max()+1)] if xs.size else None,
                              touchesEdge=bool(occupied[0].any() or occupied[-1].any() or occupied[:,0].any() or occupied[:,-1].any()))
    return dict(unionPixels=int(union.sum()), metrics=metrics, hashes={k: sha(p) for k, p in files.items()})

rows=[]
for item in mounted['captures']:
    name=item['file']
    files=dict(original=root/'build/verification/diorama-mounted-browser'/name,
               beforeFinite=root/'build/verification/diorama-mounted-kotlin'/('material-'+name),
               afterFinite=root/'build/verification/diorama-mounted-sampler-kotlin'/name,
               beforeMedia=root/'build/verification/diorama-mounted-kotlin'/('media-'+name),
               afterMedia=root/'build/verification/diorama-mounted-sampler-kotlin'/('media-'+name))
    assert sha(files['original']) == source_hashes['mounted-source/'+name]
    for mode,key in [('material','beforeFinite'),('media','beforeMedia')]:
        assert sha(files[key]) == before['mounted/'+mode+'-'+name]
    rows.append(dict(file=name,**compare(files)))

def summary(before_key,after_key):
    return dict(means={k:float(np.mean([r['metrics'][k]['mae'] for r in rows])) for k in [before_key,after_key]},
                worst64={k:max(r['metrics'][k]['worst64'] for r in rows) for k in [before_key,after_key]},
                meanRegressions=[dict(file=r['file'],before=r['metrics'][before_key]['mae'],after=r['metrics'][after_key]['mae']) for r in rows if r['metrics'][after_key]['mae']>r['metrics'][before_key]['mae']],
                localRegressions=[dict(file=r['file'],before=r['metrics'][before_key]['worst64'],after=r['metrics'][after_key]['worst64']) for r in rows if r['metrics'][after_key]['worst64']>r['metrics'][before_key]['worst64']])

for row in plain['rows']:
    name=row['file']
    assert sha(root/'build/verification/diorama-plain-sampler-kotlin'/name)==row['hashes']['fragment'], 'Library sampler differs from independent fragment control: '+name

report=dict(baseline='9eaa844',sourceRevision=mounted['revision'],
            conditions='Original48 mounted frames, independent own core/text/finite or media camera, original material policies. Sampler replaces ONLY plain neighbors; active text/glow unchanged. Same five-image per-frame foreground union, no registration, alpha deletion, LOD fit or time shift.',
            finite=summary('beforeFinite','afterFinite'),media=summary('beforeMedia','afterMedia'),rows=rows,
            ownPlain24ExactlyMatchIndependentControl=True,allocationPolicy='Explicitly prepare visible paragraphs outside Canvas; max8 entries/32MiB estimated image payload, fallback for unsupported or over-budget; close eviction/unmount.',
            platforms=dict(jvm='Internal opt-in calibrated Skia path; no public Diorama',android='Explicit unavailable factory; ordinary planes, no AGSL runtime claim'),
            formalVisualAcceptance=False,deviceValidated=False)
(out/'comparison.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf8')
names=['mounted-380-60-700.png','mounted-800-60-2100.png','mounted-380-60-3200.png',
       'mounted-800-60-3199.png','mounted-380-60-3500.png','mounted-800-60-4600.png']
sheet=Image.new('RGB',(1600,len(names)*325),'#191919');draw=ImageDraw.Draw(sheet)
for i,name in enumerate(names):
    for j,(label,folder,file) in enumerate([('original','diorama-mounted-browser',name),('before finite','diorama-mounted-kotlin','material-'+name),('sampler finite','diorama-mounted-sampler-kotlin',name),('sampler media','diorama-mounted-sampler-kotlin','media-'+name)]):
        im=Image.open(root/'build/verification'/folder/file).convert('RGB');im.thumbnail((400,300),Image.Resampling.LANCZOS)
        draw.text((j*400+3,i*325+3),label+' '+name,fill='white');sheet.paste(im,(j*400,i*325+23))
sheet.save(out/'contact.png')
print(json.dumps({k:report[k] for k in ['finite','media','ownPlain24ExactlyMatchIndependentControl']},indent=2))
