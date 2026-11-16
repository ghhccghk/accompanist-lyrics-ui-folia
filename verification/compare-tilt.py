"""Unregistered original / Kotlin Tilt comparison; residuals are not acceptance."""
from pathlib import Path
import csv, hashlib, json
import numpy as np
from PIL import Image, ImageDraw

root = Path(__file__).resolve().parents[1]
base = root / 'build/verification'
original, kotlin = base / 'tilt-browser', base / 'tilt-kotlin'
out = base / 'tilt-kotlin-comparison'
out.mkdir(exist_ok=True)
rows, hashes = [], {}
background = np.array([22, 36, 53])
for source in sorted(original.glob('*.png')):
    target = kotlin / source.name
    if not target.exists():
        raise RuntimeError(f'Missing candidate {target}')
    a = np.asarray(Image.open(source).convert('RGB')).astype(np.int16)
    b = np.asarray(Image.open(target).convert('RGB')).astype(np.int16)
    assert a.shape == b.shape
    mask = np.any(np.abs(a-background) > 8, axis=2) | np.any(np.abs(b-background) > 8, axis=2)
    rows.append(dict(file=source.name, foreground_pixels=int(mask.sum()),
                     foreground_mae=float(np.abs(a-b)[mask].mean()) if mask.any() else 0))
    for path in (source, target):
        hashes[str(path.relative_to(root))] = hashlib.sha256(path.read_bytes()).hexdigest()
with (out / 'errors.csv').open('w', newline='', encoding='utf8') as handle:
    writer = csv.DictWriter(handle, fieldnames=list(rows[0]))
    writer.writeheader()
    writer.writerows(rows)
summary = dict(frames=len(rows), mean_foreground_mae=sum(r['foreground_mae'] for r in rows)/len(rows),
               worst=sorted(rows, key=lambda r: r['foreground_mae'], reverse=True)[:10])
(out / 'conditions.json').write_text(json.dumps(dict(reference=json.loads((original/'conditions.json').read_text()),
    summary=summary, png_sha256=hashes, limits='No registration, no reference replacement; visual calibration remains pending.'), indent=2), encoding='utf8')
names = ['tilt-wrap-800-1500ms.png', 'normal-wrap-800-1500ms.png',
         'tilt-arabic-800-1500ms.png', summary['worst'][0]['file']]
sheet = Image.new('RGB', (800, len(names)*250), (30, 30, 30))
draw = ImageDraw.Draw(sheet)
for i, name in enumerate(names):
    for j, folder in enumerate((original, kotlin)):
        draw.text((400*j+4, i*250+3), f'{folder.name} | {name}', fill='white')
        image = Image.open(folder/name).convert('RGB')
        image.thumbnail((400, 225))
        sheet.paste(image, (400*j+(400-image.width)//2, i*250+22))
sheet.save(out/'contact.png')
print(json.dumps(summary, indent=2))
