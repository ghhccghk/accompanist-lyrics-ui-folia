"""Pinned original wheel subtree vs native targets; no image registration/fitting."""
from pathlib import Path
import hashlib
import json
import numpy as np
from PIL import Image, ImageDraw

root = Path(__file__).resolve().parents[1]
source = root / 'build/verification/pendolo-stage-browser'
native = root / 'build/verification/pendolo-stage-kotlin'
out = root / 'verification/pendolo-stage'
out.mkdir(exist_ok=True)
conditions = json.loads((root / 'verification/pendolo-fixtures/stage-conditions.json').read_text(encoding='utf8'))
frames, hashes = [], {}
for file in sorted(source.glob('*.png')):
    other = native / file.name
    assert other.exists(), other
    a, b = [np.asarray(Image.open(p).convert('RGB')).astype(np.int16) for p in (file, other)]
    assert a.shape == b.shape
    mask = np.any(abs(a - [22, 36, 53]) > 8, axis=2) | np.any(abs(b - [22, 36, 53]) > 8, axis=2)
    diff = abs(a - b)
    frames.append(dict(file=file.name, foreground_mae=float(diff[mask].mean()) if mask.any() else 0,
                       full_mae=float(diff.mean())))
    for p in (file, other):
        hashes[p.relative_to(root).as_posix()] = hashlib.sha256(p.read_bytes()).hexdigest()
assert len(frames) == conditions['frames'] == 80
residual = [r.split('\t') for p in native.glob('*-residual.tsv')
            for r in p.read_text(encoding='utf8').splitlines() if r and not r.startswith('#')]
assert len(residual) == 5808
mismatches = list(native.glob('*-mismatches.txt'))
assert len(mismatches) == 4
assert all(not p.read_text(encoding='utf8').strip() for p in mismatches)
report = dict(conditions=conditions,
              status='Target-only lyric wheel/subtitle calibration; tick/chorus/whole-theme/device acceptance pending.',
              numeric_fields=len(residual),
              maximum_numeric_error=max(float(r[-1]) for r in residual),
              maximum_math_or_reserved_height_error=max(float(r[-1]) for r in residual if int(r[2]) <= 5),
              mean_foreground_mae=sum(r['foreground_mae'] for r in frames) / len(frames),
              max_foreground_mae=max(r['foreground_mae'] for r in frames),
              frames=frames, png_sha256=hashes)
(out / 'comparison.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf8')
names = ['latin-380-both-1600.png', 'wrap-380-both-6000.png', 'arabic-380-both-1600.png',
         'cjk-380-both-1600.png', 'latin-800-both-6000.png', 'cjk-800-both-7000.png']
worst = max(frames, key=lambda r: r['foreground_mae'])['file']
if worst not in names:
    names.append(worst)
sheet = Image.new('RGB', (1000, len(names) * 400), (30, 30, 30))
draw = ImageDraw.Draw(sheet)
for i, name in enumerate(names):
    for j, folder in enumerate((source, native)):
        draw.text((j * 500 + 5, i * 400 + 3), f'{"Original" if j == 0 else "Kotlin"} | {name}', fill='white')
        pic = Image.open(folder / name).convert('RGB')
        pic.thumbnail((490, 370))
        sheet.paste(pic, (j * 500, i * 400 + 23))
sheet.save(out / 'contact.png')
print(json.dumps({k: report[k] for k in ('numeric_fields', 'maximum_numeric_error',
    'maximum_math_or_reserved_height_error', 'mean_foreground_mae', 'max_foreground_mae')}, indent=2))
