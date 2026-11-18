"""Frozen mounted48: own finite camera + own finite material vs media paths."""
from pathlib import Path
import hashlib
import json
import numpy as np
from PIL import Image, ImageDraw

root = Path(__file__).resolve().parents[1]
fixture = root/'verification/diorama-fixtures/mounted-cut-v1/scene.json'
scene = json.loads(fixture.read_text(encoding='utf8'))
source = root/'build/verification/diorama-mounted-browser'
native = root/'build/verification/diorama-mounted-kotlin'
out = root/'verification/diorama-mounted-materials'
out.mkdir(parents=True, exist_ok=True)
bg = np.array([22, 36, 53])
modes = ['finite', 'material', 'media']
rows = []
original_hashes = json.loads((root/'.work/diorama-aces-before/original-hashes.json').read_text())
before_hashes = json.loads((root/'.work/diorama-mounted-material-before/native-hashes.json').read_text())
assert len(original_hashes) == 48 and len(before_hashes) == 96
assert all(hashlib.sha256((root/p).read_bytes()).hexdigest() == h for p, h in before_hashes.items())


def paths(name):
    return [source/name]+[native/(m+'-'+name) for m in modes]


for c in scene['captures']:
    files = paths(c['file'])
    assert hashlib.sha256(files[0].read_bytes()).hexdigest() == original_hashes[c['file']]
    images = [np.asarray(Image.open(p).convert('RGB')).astype(np.int16) for p in files]
    mask = np.any(images[0] != bg, axis=2)
    for im in images[1:]:
        assert im.shape == images[0].shape
        mask |= np.any(im != bg, axis=2)
    entry = dict(file=c['file'], width=c['width'], fps=c['fps'], time=c['time'],
                 hashes={str(p.relative_to(root)): hashlib.sha256(p.read_bytes()).hexdigest() for p in files}, bounds={})
    for mode, im in zip(['source']+modes, images):
        y, x = np.where(np.any(im != bg, axis=2))
        entry['bounds'][mode] = [int(x.min()), int(y.min()), int(x.max()+1), int(y.max()+1)] if x.size else None
    for mode, im in zip(modes, images[1:]):
        d = abs(images[0]-im)
        local = [float(d[y:y+64, x:x+64][mask[y:y+64, x:x+64]].mean())
                 for y in range(0, im.shape[0], 64) for x in range(0, im.shape[1], 64)
                 if mask[y:y+64, x:x+64].any()]
        entry[mode] = dict(foreground_mae=float(d[mask].mean()) if mask.any() else 0.0,
                           worst64=max(local, default=0.0), channel_peak=int(d.max()))
    rows.append(entry)
summary = {m: dict(mean_foreground_mae=float(np.mean([r[m]['foreground_mae'] for r in rows])),
                   worst_image=max(r[m]['foreground_mae'] for r in rows),
                   worst64=max(r[m]['worst64'] for r in rows)) for m in modes}
changes = dict(improved=sum(r['material']['foreground_mae'] < r['finite']['foreground_mae'] for r in rows),
               regressed=[r['file'] for r in rows if r['material']['foreground_mae'] > r['finite']['foreground_mae']],
               local_regressed=[r['file'] for r in rows if r['material']['worst64'] > r['finite']['worst64']])
report = dict(frames=48, originalFramesUnchanged=48, productionNativeFramesUnchanged=96,
              conditions_sha256=hashlib.sha256(fixture.read_bytes()).hexdigest(),
              mask='One shared original/own-finite-camera-analytic-material/own-finite-camera-finite-material/own-media ALL-nonbackground union. No registration/filter/delay/excluded frame.',
              comparisons=summary, finiteMaterialChange=changes,
              numeric={f'material-{w}.txt': (native/f'material-{w}.txt').read_text() for w in [380, 800]},
              status='Finite material decomposition only, not production media envelope equivalence or final visual/device acceptance.', frames_detail=rows)
(out/'comparison.json').write_text(json.dumps(report, indent=2)+'\n', encoding='utf8')
names = ['mounted-380-60-0.png', 'mounted-800-60-700.png', 'mounted-800-144-1600.png',
         'mounted-380-60-2099.png', 'mounted-800-60-3200.png', 'mounted-380-144-3500.png']
sheet = Image.new('RGB', (1280, len(names)*280), (30, 30, 30))
draw = ImageDraw.Draw(sheet)
for i, name in enumerate(names):
    for j, (label, p) in enumerate(zip(['Original mounted', 'Finite camera / analytic material', 'Finite camera / finite material', 'Media camera / analytic material'], paths(name))):
        draw.text((j*320+3, i*280+3), label, fill='white')
        draw.text((j*320+3, i*280+16), name, fill='white')
        im = Image.open(p).convert('RGB'); im.thumbnail((320, 240))
        sheet.paste(im, (j*320, i*280+35))
sheet.save(out/'contact.png')
print(json.dumps(dict(comparisons=summary, finiteMaterialChange=changes), indent=2))
