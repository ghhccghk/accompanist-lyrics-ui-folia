"""Fixed mounted48 source/before/after comparison; same five-image all-ink union.

Before images are the preserved 175dabe production outputs, never regenerated
using the patched renderer. Original frozen source frames must keep their hashes.
"""
from pathlib import Path
import hashlib
import json
import numpy as np
from PIL import Image, ImageDraw

root = Path(__file__).resolve().parents[1]
before = root / '.work/diorama-aces-before'
native = root / 'build/verification/diorama-mounted-kotlin'
source = root / 'build/verification/diorama-mounted-browser'
out = root / 'verification/diorama-tonemapping'
out.mkdir(parents=True, exist_ok=True)
conditions = root / 'verification/diorama-fixtures/mounted-cut-v1/scene.json'
captures = json.loads(conditions.read_text(encoding='utf8'))['captures']
hashes = json.loads((before / 'original-hashes.json').read_text(encoding='utf8'))
assert len(captures) == len(hashes) == 48
bg = np.array([22, 36, 53])
modes = ['finite-before', 'finite-after', 'media-before', 'media-after']


def files(name):
    return [source/name, before/'native'/('finite-'+name), native/('finite-'+name),
            before/'native'/('media-'+name), native/('media-'+name)]


def bounds(im):
    y, x = np.where(np.any(im != bg, axis=2))
    return [int(x.min()), int(y.min()), int(x.max()+1), int(y.max()+1)] if x.size else None


rows = []
for c in captures:
    paths = files(c['file'])
    assert hashlib.sha256(paths[0].read_bytes()).hexdigest() == hashes[c['file']]
    a, *images = [np.asarray(Image.open(p).convert('RGB')).astype(np.int16) for p in paths]
    assert all(im.shape == a.shape for im in images)
    mask = np.any(a != bg, axis=2)
    for im in images:
        mask |= np.any(im != bg, axis=2)
    entry = dict(file=c['file'], time=c['time'], fps=c['fps'], width=c['width'],
                 bounds={k: bounds(im) for k, im in zip(['source']+modes, [a]+images)},
                 hashes={str(p.relative_to(root)): hashlib.sha256(p.read_bytes()).hexdigest() for p in paths},
                 foreground_pixels=int(mask.sum()))
    for mode, im in zip(modes, images):
        d = abs(a-im)
        local = [float(d[y:y+64, x:x+64][mask[y:y+64, x:x+64]].mean())
                 for y in range(0, a.shape[0], 64) for x in range(0, a.shape[1], 64)
                 if mask[y:y+64, x:x+64].any()]
        entry[mode] = dict(foreground_mae=float(d[mask].mean()) if mask.any() else 0.0,
                           full_mae=float(d.mean()), worst64=max(local, default=0.0), channel_peak=int(d.max()))
    rows.append(entry)

summary = {m: dict(mean_foreground_mae=float(np.mean([r[m]['foreground_mae'] for r in rows])),
                   worst_image=max(r[m]['foreground_mae'] for r in rows),
                   worst64=max(r[m]['worst64'] for r in rows)) for m in modes}
changes = {m: dict(improved=sum(r[m+'-after']['foreground_mae'] < r[m+'-before']['foreground_mae'] for r in rows),
                   regressed=[dict(file=r['file'], before=r[m+'-before'], after=r[m+'-after'])
                              for r in rows if r[m+'-after']['foreground_mae'] > r[m+'-before']['foreground_mae']],
                   worst64_regressed=[r['file'] for r in rows if r[m+'-after']['worst64'] > r[m+'-before']['worst64']])
           for m in ['finite', 'media']}
numeric = {f'numeric-{w}.txt': (native/f'numeric-{w}.txt').read_text() for w in [380, 800]}
for name, text in numeric.items():
    assert text == (before/'native'/name).read_text(), 'Color fix cannot change camera/control geometry'
report = dict(baseline='175dabe', source_hashes_unchanged=48, frames=48,
              conditions_sha256=hashlib.sha256(conditions.read_bytes()).hexdigest(),
              mask='One identical source/before-finite/after-finite/before-media/after-media ALL-nonbackground union; lowest alpha retained, no registration/phase shift/filter/omitted frame.',
              comparisons=summary, changes=changes, unchanged_numeric_controls=numeric,
              status='Internal original mounted CameraRig+Scene ACES fix only; remaining media camera/envelope/wall-breath/depth/whole-scene/device differences are not visually accepted.',
              frames_detail=rows)
(out/'comparison.json').write_text(json.dumps(report, indent=2)+'\n', encoding='utf8')

# Same six preselected probes as the original mounted contact; never pick best frames.
names = ['mounted-380-60-0.png', 'mounted-800-60-700.png', 'mounted-800-144-1600.png',
         'mounted-380-60-2099.png', 'mounted-800-60-3200.png', 'mounted-380-144-3500.png']
sheet = Image.new('RGB', (1500, len(names)*268), (30, 30, 30))
draw = ImageDraw.Draw(sheet)
for i, name in enumerate(names):
    for j, (label, p) in enumerate(zip(['Original mounted', 'Own finite BEFORE', 'Own finite ACES', 'Own media BEFORE', 'Own media ACES'], files(name))):
        draw.text((j*300+3, i*268+3), label, fill='white')
        draw.text((j*300+3, i*268+16), name, fill='white')
        im = Image.open(p).convert('RGB'); im.thumbnail((300, 232))
        sheet.paste(im, (j*300, i*268+33))
sheet.save(out/'contact.png')
print(json.dumps(dict(comparisons=summary, changes=changes), indent=2))
