"""Mounted original tick wheel vs native media response; no fitted phase."""
from pathlib import Path
import hashlib, json
import numpy as np
from PIL import Image, ImageDraw

root = Path(__file__).resolve().parents[1]
source = root / 'build/verification/pendolo-typography-stage-browser'
conditions = json.loads((root / 'verification/pendolo-fixtures/typography-stage-conditions.json').read_text(encoding='utf8'))
out = root / 'verification/pendolo-typography-stage'
out.mkdir(exist_ok=True)
report = dict(conditions=conditions, status='Actual original independent font scales vs native wheel; not theme acceptance or a claim of improved raster parity.')
folders = ['pendolo-typography-stage-kotlin', 'pendolo-typography-stage-source-rotation-kotlin']
before_folders = []
for folder in folders + [n for n in before_folders if (root / 'build/verification' / n).exists()]:
    native = root / 'build/verification' / folder
    frames, hashes = [], {}
    for file in sorted(source.glob('*.png')):
        other = native / file.name
        assert other.exists(), other
        a, b = [np.asarray(Image.open(p).convert('RGB')).astype(np.int16) for p in (file, other)]
        assert a.shape == b.shape
        mask = np.any(abs(a - [22, 36, 53]) > 8, axis=2) | np.any(abs(b - [22, 36, 53]) > 8, axis=2)
        difference = abs(a - b)
        frames.append(dict(file=file.name, foreground_mae=float(difference[mask].mean()) if mask.any() else 0, full_mae=float(difference.mean())))
        for p in (file, other): hashes[p.relative_to(root).as_posix()] = hashlib.sha256(p.read_bytes()).hexdigest()
    assert len(frames) == conditions['frames'] == 16
    rows = [r.split('\t') for p in native.glob('*-residual.tsv') for r in p.read_text(encoding='utf8').splitlines() if r and not r.startswith('#')]
    geometry=rows
    entry = dict(frames=frames, png_sha256=hashes, numeric_fields=len(rows),
                 chorus_maximum_errors={str(i):max(float(r[-1]) for r in rows if int(r[2])==i) for i in range(3)},
                 max_content_rect_error=max(float(r[-1]) for r in geometry if int(r[2]) >= 3),

                 mean_foreground_mae=sum(r['foreground_mae'] for r in frames) / len(frames),
                 max_foreground_mae=max(r['foreground_mae'] for r in frames))
    widths = [r.split('\t') for p in native.glob('*-widths.tsv') for r in p.read_text(encoding='utf8').splitlines() if r and not r.startswith('#')]
    if widths: entry['maximum_untransformed_box_error'] = max(float(r[-1]) for r in widths)
    report[folder] = entry
    print(folder, json.dumps({k: v for k, v in entry.items() if k not in ['frames', 'png_sha256']}))
unchanged=[]
# Defaults are checked by re-running the pre-existing alternate-palette suite.
previous=json.loads((root/'verification/pendolo-palette-stage/comparison.json').read_text(encoding='utf8'))
for file,expected in previous['pendolo-palette-stage-kotlin']['png_sha256'].items():
    if '-kotlin/' in file:
        assert hashlib.sha256((root/file).read_bytes()).hexdigest()==expected,file
        unchanged.append(file)
report['unchanged_prior_native_pngs']=len(unchanged)
(out / 'comparison.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf8')
names=['latin-380-1.5-1-9000.png','latin-800-1.5-1-24000.png','cjk-380-1-1.75-24000.png','arabic-800-0.75-1.5-24000.png']
worst = max(report[folders[0]]['frames'], key=lambda r: r['foreground_mae'])['file']
if worst not in names: names.append(worst)
sheet = Image.new('RGB', (1500, len(names) * 400), (30, 30, 30)); draw = ImageDraw.Draw(sheet)
for i, name in enumerate(names):
    for j, (title, folder) in enumerate([('Original', source)] + [(n, root / 'build/verification' / n) for n in folders]):
        draw.text((j * 500 + 5, i * 400 + 3), f'{title.removeprefix("pendolo-chorus-stage-")} | {name}', fill='white')
        pic = Image.open(folder / name).convert('RGB'); pic.thumbnail((490, 370)); sheet.paste(pic, (j * 500, i * 400 + 23))
sheet.save(out / 'contact.png')
