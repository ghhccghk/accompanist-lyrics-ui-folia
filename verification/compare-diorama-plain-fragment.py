"""Frozen24 original plain planes versus own-input anisotropic fragment control."""
from pathlib import Path
import hashlib
import json
import numpy as np
from PIL import Image, ImageDraw

root = Path(__file__).resolve().parents[1]
out = root / 'verification/diorama-plain-fragment'
out.mkdir(exist_ok=True)
sha = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
fixture = json.loads((root / 'verification/diorama-fixtures/text-planes.json').read_text(encoding='utf8'))
hashes = json.loads((root / '.work/diorama-coverage-before-c3e464c/hashes.json').read_text(encoding='utf8'))
baseline = json.loads((root / '.work/diorama-mip-before-c0e2804/hashes.json').read_text(encoding='utf8'))
bg = [22, 36, 53]
rows = []
for item in fixture['observations']:
    name = item['file']
    files = dict(original=root / 'build/verification/diorama-text-plane-browser' / name,
                 production=root / 'build/verification/diorama-text-plane-kotlin' / name,
                 fragment=root / 'build/verification/diorama-plain-fragment-kotlin' / name)
    assert sha(files['original']) == hashes['plain-source/' + name], name
    assert sha(files['production']) == baseline['plain/' + name], name
    images = {k: np.array(Image.open(p).convert('RGB'), dtype=np.int32) for k, p in files.items()}
    mask = np.logical_or.reduce([np.any(a != bg, axis=2) for a in images.values()])
    metrics = {}
    for key, a in images.items():
        d = abs(a - images['original'])
        occupied = np.any(a != bg, axis=2)
        ys, xs = np.where(occupied)
        metrics[key] = dict(mae=float(d[mask].mean()), peak=int(d.max()),
                            worst64=max(float(d[y:y+64, x:x+64][mask[y:y+64, x:x+64]].mean())
                                        for y in range(0, a.shape[0], 64) for x in range(0, a.shape[1], 64)
                                        if mask[y:y+64, x:x+64].any()),
                            pixels=int(xs.size), bounds=[int(xs.min()), int(ys.min()), int(xs.max()+1), int(ys.max()+1)] if xs.size else None,
                            touchesEdge=bool(occupied[0].any() or occupied[-1].any() or occupied[:, 0].any() or occupied[:, -1].any()))
    rows.append(dict(file=name, scene=item['scene'], viewport=[item['width'], item['height']], unionPixels=int(mask.sum()),
                     metrics=metrics, hashes={k: sha(p) for k, p in files.items()}))
regressions = lambda field: [dict(file=r['file'], production=r['metrics']['production'][field], fragment=r['metrics']['fragment'][field])
                            for r in rows if r['metrics']['fragment'][field] > r['metrics']['production'][field]]
report = dict(baseline='438176e', sourceRevision=fixture['revision'], browser=fixture['browser'], fonts=fixture['fonts'],
              conditions='Unchanged original24 Latin/Arabic/CJK/long text,380/800/801x600,DPR1,two original poses. Native independently shapes white paragraphs, full mip pyramid, per-fragment aniso4; original isolated Three has NO tone mapping/fog. No source raster/pose output drives drawing.',
              mask='All-three-image per-frame nonbackground union; no threshold, low-alpha deletion, registration, LOD fitting or time shift.',
              means={k: float(np.mean([r['metrics'][k]['mae'] for r in rows])) for k in ['production', 'fragment']},
              worst64={k: max(r['metrics'][k]['worst64'] for r in rows) for k in ['production', 'fragment']},
              meanRegressions=regressions('mae'), localRegressions=regressions('worst64'), rows=rows,
              productionChanged=False, androidRuntime=False, formalVisualAcceptance=False)
(out / 'comparison.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf8')
# Fixed representative set includes the previous candidate's near Latin/Arabic
# failures; selected before computing metrics. Full24 numeric failures retained.
names=['latin-800--3.6-1.9.png','arabic-801--3.6-1.9.png','cjk-380--3.6-1.9.png','long-800-1.7-0.8.png',
       'latin-380-1.7-0.8.png','arabic-800-1.7-0.8.png','cjk-801-1.7-0.8.png','long-801--3.6-1.9.png']
sheet = Image.new('RGB', (1200, len(names)*325), '#191919')
draw = ImageDraw.Draw(sheet)
for i, name in enumerate(names):
    for j, (label, folder) in enumerate([('original','diorama-text-plane-browser'),('production','diorama-text-plane-kotlin'),('fragment','diorama-plain-fragment-kotlin')]):
        im = Image.open(root / 'build/verification' / folder / name).convert('RGB')
        im.thumbnail((400, 300), Image.Resampling.LANCZOS)
        draw.text((j*400+3, i*325+3), label+' '+name, fill='white')
        sheet.paste(im, (j*400, i*325+23))
sheet.save(out / 'contact.png')
print(json.dumps({k: report[k] for k in ['means','worst64','meanRegressions','localRegressions']}, indent=2))
