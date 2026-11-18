"""One fixed source/own-finite/own-media all-ink union; no alignment or alpha gate."""
from pathlib import Path
import hashlib
import json
import sys
import numpy as np
from PIL import Image, ImageDraw

root = Path(__file__).resolve().parents[1]
source = root / 'build/verification/diorama-flight-browser'
native = root / 'build/verification/diorama-flight-kotlin'
out = root / 'verification/diorama-flight'
out.mkdir(parents=True, exist_ok=True)
sequence = '--sequence' in sys.argv
stem = 'sequence' if sequence else 'scene'
conditions = json.loads((root / f'verification/diorama-fixtures/flight-v1/{stem}.json').read_text(encoding='utf-8'))
assert len(conditions['captures']) == (67 if sequence else 96)
for suffix in (['sequence'] if sequence else ['380', '800']):
    assert (native / f'numeric-{suffix}.txt').is_file(), 'Wait for completed native captures'
bg = np.array([22, 36, 53])


def bounds(im):
    y, x = np.where(np.any(im != bg, axis=2))
    return [int(x.min()), int(y.min()), int(x.max()+1), int(y.max()+1)] if x.size else None


rows = []
for c in conditions['captures']:
    name = c['file']
    files = [source/name, native/('finite-'+name), native/('media-'+name)]
    a, b, m = [np.asarray(Image.open(p).convert('RGB')).astype(np.int16) for p in files]
    assert a.shape == b.shape == m.shape
    mask = np.any(a != bg, axis=2) | np.any(b != bg, axis=2) | np.any(m != bg, axis=2)
    boxes = {mode: bounds(im) for mode, im in [('source', a), ('finite', b), ('media', m)]}
    entry = dict(file=name, scene=c['scene'], fps=c['fps'], time=c['time'],
                 empty_foreground=not bool(mask.any()), bounds=boxes,
                 edge_touch={mode: box is not None and (box[0] == 0 or box[1] == 0 or box[2] == a.shape[1] or box[3] == a.shape[0])
                             for mode, box in boxes.items()},
                 png_sha256={str(p.relative_to(root)): hashlib.sha256(p.read_bytes()).hexdigest() for p in files})
    for mode, im in [('finite', b), ('media', m)]:
        d = abs(a-im)
        local = [float(d[y:y+64, x:x+64][mask[y:y+64, x:x+64]].mean())
                 for y in range(0, a.shape[0], 64) for x in range(0, a.shape[1], 64)
                 if mask[y:y+64, x:x+64].any()]
        entry[mode] = dict(foreground_mae=float(d[mask].mean()) if mask.any() else 0.0,
                           full_mae=float(d.mean()), worst64=max(local, default=0.0), channel_peak=int(d.max()))
    rows.append(entry)

summary = {mode: dict(mean_foreground_mae=float(np.mean([r[mode]['foreground_mae'] for r in rows])),
                     peak_foreground_mae=max(r[mode]['foreground_mae'] for r in rows),
                     local_worst=max(r[mode]['worst64'] for r in rows)) for mode in ['finite', 'media']}
report = dict(scope=conditions['scope'], comparisons=summary,
              blank_frames={mode: [r['file'] for r in rows if r['bounds'][mode] is None] for mode in ['source', 'finite', 'media']},
              edge_touch={mode: [r['file'] for r in rows if r['edge_touch'][mode]] for mode in ['source', 'finite', 'media']},
              mask='Identical source/own-finite/own-media ALL nonbackground union including lowest alpha; no registration, phase shift or threshold filtering.',
              status='Internal incoming-text flight only. Finite operator gate is distinct from adaptive-media/RAF residual and raster/readability. NOT full dual-corridor CameraRig, public theme or device acceptance.',
              frames=rows)
(out/('sequence-comparison.json' if sequence else 'comparison.json')).write_text(json.dumps(report, indent=2)+'\n', encoding='utf-8')
names = ['latin-380-60-700.png', 'latin-800-60-1600.png', 'arabic-800-144-2400.png',
         'arabic-380-60-3199.png', 'cjk-800-60-3200.png', 'cjk-380-144-4200.png']
if sequence:
    names = [f'sequence-latin-800-60-{t}.png' for t in (0, 700, 1600, 2400, 3200, 4200)]
sheet = Image.new('RGB', (1260, len(names)*338), (30, 30, 30))
draw = ImageDraw.Draw(sheet)
for i, name in enumerate(names):
    for j, (label, p) in enumerate([('Original finite + Three', source/name),
                                    ('Own finite + shaped core text', native/('finite-'+name)),
                                    ('Own adaptive media + core text', native/('media-'+name))]):
        draw.text((j*420+4, i*338+3), label, fill='white')
        draw.text((j*420+4, i*338+17), name, fill='white')
        im = Image.open(p).convert('RGB'); im.thumbnail((420, 300))
        sheet.paste(im, (j*420, i*338+35))
sheet.save(out/('sequence-contact.png' if sequence else 'contact.png'))
if sequence:
    # Reproducible review artifact only; raw frames/movie stay in ignored build/.
    frames = []
    for c in conditions['captures']:
        panel = Image.new('RGB', (1200, 326), (30, 30, 30))
        pd = ImageDraw.Draw(panel)
        for j, (label, p) in enumerate([('Original finite', source/c['file']),
                                        ('Own finite', native/('finite-'+c['file'])),
                                        ('Own adaptive media', native/('media-'+c['file']))]):
            pd.text((j*400+4, 4), f"{label} {c['time']:.6f}s", fill='white')
            im = Image.open(p).convert('RGB').resize((400, 300))
            panel.paste(im, (j*400, 24))
        frames.append(panel)
    durations = [max(10, round((b-a)*1000)) for a, b in zip(conditions['times'], conditions['times'][1:])]+[67]
    frames[0].save(native/'flight-sequence.gif', save_all=True, append_images=frames[1:],
                   duration=durations, loop=0, optimize=False)
print(json.dumps(summary, indent=2))
