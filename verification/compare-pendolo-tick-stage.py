"""Mounted original tick wheel vs native media response; no fitted phase."""
from pathlib import Path
import hashlib, json
import numpy as np
from PIL import Image, ImageDraw

root = Path(__file__).resolve().parents[1]
source = root / 'build/verification/pendolo-tick-stage-browser'
conditions = json.loads((root / 'verification/pendolo-fixtures/tick-stage-conditions.json').read_text(encoding='utf8'))
out = root / 'verification/pendolo-tick-stage'
out.mkdir(exist_ok=True)
report = dict(conditions=conditions, status='Mounted tick/text calibration; remaining numeric/pixel differences are not final theme acceptance.')
folders = ['pendolo-tick-stage-kotlin', 'pendolo-tick-stage-source-rotation-kotlin']
for folder in folders:
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
    assert len(frames) == conditions['frames'] == 144
    rows = [r.split('\t') for p in native.glob('*-residual.tsv') for r in p.read_text(encoding='utf8').splitlines() if r and not r.startswith('#')]
    tick = {f'tick{i}': max(float(r[-1]) for r in rows if r[1] == f'tick{i}') for i in range(4)} if folder == folders[0] else {}
    geometry = [r for r in rows if not r[1].startswith('tick')]
    entry = dict(frames=frames, png_sha256=hashes, numeric_fields=len(rows),
                 tick_maximum_errors=tick,
                 max_content_rect_error=max(float(r[-1]) for r in geometry if int(r[2]) < 4),
                 max_opacity_error=max(float(r[-1]) for r in geometry if int(r[2]) == 4),
                 mean_foreground_mae=sum(r['foreground_mae'] for r in frames) / len(frames),
                 max_foreground_mae=max(r['foreground_mae'] for r in frames))
    report[folder] = entry
    print(folder, json.dumps({k: v for k, v in entry.items() if k not in ['frames', 'png_sha256']}))
(out / 'comparison.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf8')
names = ['latin-380-normal-7200.png', 'latin-380-chaotic-8000.png', 'wrap-380-normal-28200.png',
         'arabic-800-normal-48000.png', 'cjk-800-normal-7200.png']
worst = max(report[folders[0]]['frames'], key=lambda r: r['foreground_mae'])['file']
if worst not in names: names.append(worst)
sheet = Image.new('RGB', (1500, len(names) * 400), (30, 30, 30)); draw = ImageDraw.Draw(sheet)
for i, name in enumerate(names):
    for j, (title, folder) in enumerate([('Original', source)] + [(n, root / 'build/verification' / n) for n in folders]):
        draw.text((j * 500 + 5, i * 400 + 3), f'{title.removeprefix("pendolo-tick-stage-")} | {name}', fill='white')
        pic = Image.open(folder / name).convert('RGB'); pic.thumbnail((490, 370)); sheet.paste(pic, (j * 500, i * 400 + 23))
sheet.save(out / 'contact.png')
