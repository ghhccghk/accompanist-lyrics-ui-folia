"""Compare complete source/own-finite/own-media frames, preserving low-alpha pixels."""
from pathlib import Path
import hashlib
import json
import numpy as np
from PIL import Image, ImageDraw

root = Path(__file__).resolve().parents[1]
source = root / 'build/verification/diorama-follow-browser'
native = root / 'build/verification/diorama-follow-kotlin'
out = root / 'verification/diorama-follow'
out.mkdir(parents=True, exist_ok=True)
conditions = json.loads((root / 'verification/diorama-fixtures/follow-v1/scene.json').read_text(encoding='utf-8'))
assert len(conditions['captures']) == 96
for width in (380, 800):
    assert (native / f'numeric-{width}.txt').is_file(), 'Wait for BOTH successful native captures'
bg = np.array([22, 36, 53])
rows = []

def bounds(a):
    y, x = np.where(np.any(a != bg, axis=2))
    return [int(x.min()), int(y.min()), int(x.max()+1), int(y.max()+1)] if x.size else None

for c in conditions['captures']:
    name = c['file']
    files = [source / name, native / ('finite-' + name), native / ('media-' + name)]
    a, b, m = [np.asarray(Image.open(p).convert('RGB')).astype(np.int16) for p in files]
    assert a.shape == b.shape == m.shape
    # One identical three-way union for both comparisons, ALL nonbackground.
    mask = np.any(a != bg, axis=2) | np.any(b != bg, axis=2) | np.any(m != bg, axis=2)
    entry = dict(file=name, scene=c['scene'], fps=c['fps'], time=c['time'],
                 empty_foreground=not bool(mask.any()),
                 source_bounds=bounds(a), finite_bounds=bounds(b), media_bounds=bounds(m),
                 png_sha256={str(p.relative_to(root)): hashlib.sha256(p.read_bytes()).hexdigest() for p in files})
    for mode, im in [('finite', b), ('media', m)]:
        d = abs(a-im)
        local = [float(d[y:y+64, x:x+64][mask[y:y+64, x:x+64]].mean())
                 for y in range(0, a.shape[0], 64) for x in range(0, a.shape[1], 64)
                 if mask[y:y+64, x:x+64].any()]
        entry[mode] = dict(foreground_mae=float(d[mask].mean()) if mask.any() else 0.0, full_mae=float(d.mean()),
                           worst64=max(local, default=0.0), channel_peak=int(d.max()))
    rows.append(entry)

summary = {mode: dict(mean_foreground_mae=float(np.mean([r[mode]['foreground_mae'] for r in rows])),
                     peak_foreground_mae=max(r[mode]['foreground_mae'] for r in rows),
                     local_worst=max(r[mode]['worst64'] for r in rows)) for mode in ['finite', 'media']}
report = dict(scope=conditions['scope'], comparisons=summary,
              empty_frames=[r['file'] for r in rows if r['empty_foreground']],
              mask='Identical source/own-finite/own-media nonbackground union, including lowest alpha. No registration, delay or threshold filtering.',
              status='Finite operator correctness and bounded text follower integration. Adaptive-media RAF residual/raster/overhang remain; NOT complete CameraRig, full theme or device acceptance.',
              frames=rows)
(out / 'comparison.json').write_text(json.dumps(report, indent=2)+'\n', encoding='utf-8')
names = ['latin-380-60-700.png', 'latin-800-60-1500.png', 'arabic-800-144-2300.png',
         'arabic-380-60-3450.png', 'cjk-800-60-3100.png', 'cjk-380-144-4200.png']
sheet = Image.new('RGB', (1260, len(names)*338), (30, 30, 30))
draw = ImageDraw.Draw(sheet)
for i, name in enumerate(names):
    for j, (label, p) in enumerate([('Original finite + Three', source/name),
                                    ('Own finite + shaped text', native/('finite-'+name)),
                                    ('Own adaptive media + shaped text', native/('media-'+name))]):
        draw.text((j*420+4, i*338+3), label, fill='white')
        draw.text((j*420+4, i*338+17), name, fill='white')
        im = Image.open(p).convert('RGB'); im.thumbnail((420, 300))
        sheet.paste(im, (j*420, i*338+35))
sheet.save(out/'contact.png')
print(json.dumps(summary, indent=2))
