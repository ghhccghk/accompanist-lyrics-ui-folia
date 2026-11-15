"""Compare native-window recorded layers with original Folia and offscreen Compose.

This is not an OS screenshot or input test. RGB errors are descriptive, not an
automatic visual acceptance threshold. Requires a complete successful capture.
"""
import csv
import hashlib
import json
from pathlib import Path
from statistics import mean

from PIL import Image, ImageChops, ImageDraw, ImageStat

root = Path(__file__).resolve().parents[1]
evidence = root / 'build/verification'
native = evidence / 'classic-native-window'
assert (native / 'SUCCESS').is_file(), 'Native capture did not complete'
with (native / 'frames.tsv').open(newline='') as f:
    frames = list(csv.DictReader(f, delimiter='\t'))
assert len(frames) == 60, f'Incomplete capture: {len(frames)} / 60'
assert all(row['paused_identical'] == 'true' for row in frames)
assert sum(row['revisit'] == 'true' for row in frames) == 40
reference = evidence / 'layout-phase-public-upstream'
conditions = json.loads((reference / 'conditions.json').read_text())
font_hash = hashlib.sha256((root / 'verification/assets/FoliaParityLatin-Bold.ttf').read_bytes()).hexdigest()
assert conditions['fontSha256'] == font_hash
assert font_hash in (native / 'conditions.txt').read_text()
out = evidence / 'classic-native-window-comparison'
out.mkdir(parents=True, exist_ok=True)
rows = []
for width in (800, 380):
    for mode in ('default', 'avoid'):
        sheet = Image.new('RGB', (width * 3, 624 * 5), '#eef1f5')
        for i, ms in enumerate((750, 1000, 1500, 1900, 2100)):
            filename = f'{width}-{mode}-{ms}ms.png'
            image_path = native / filename
            recorded = [r for r in frames if int(r['width']) == width and
                        r['avoid'] == str(mode == 'avoid').lower() and int(r['media_ms']) == ms]
            assert len(recorded) == (4 if width == 800 else 2)
            assert {r['sha256'] for r in recorded} == {hashlib.sha256(image_path.read_bytes()).hexdigest()}
            paths = (reference / f'dpr1-{filename}', evidence / 'layout-phase-public-kotlin' / f'dpr1-{filename}', image_path)
            images = [Image.open(p).convert('RGB') for p in paths]
            assert all(im.size == (width, 600) for im in images)
            for j, (label, im) in enumerate(zip(('Original Folia', 'Offscreen Compose', 'Native window recorded layer'), images)):
                sheet.paste(im, (j * width, i * 624 + 24))
                ImageDraw.Draw(sheet).text((j * width + 4, i * 624 + 4), f'{label} | {ms}ms', fill='black')
            for label, a in zip(('original', 'offscreen'), images[:2]):
                b = images[2]
                mask = Image.new('L', a.size)
                for im in (a, b):
                    for ch in ImageChops.difference(im, Image.new('RGB', im.size, (22, 36, 53))).split():
                        mask = ImageChops.lighter(mask, ch)
                mask = mask.point(lambda x: 255 if x > 8 else 0)
                assert mask.getbbox(), f'Empty frame: {filename}'
                difference = ImageChops.difference(a, b)
                error = mean(ImageStat.Stat(difference, mask).mean)
                rows.append((filename, label, mask.histogram()[255], error, difference.getbbox() is None))
        sheet.save(out / f'{width}-{mode}-contact.png')
with (out / 'pixel-errors.csv').open('w', newline='') as f:
    writer = csv.writer(f)
    writer.writerow(('frame', 'reference', 'union_foreground_pixels', 'rgb_mae', 'exact_pixels'))
    writer.writerows(rows)
for label in ('original', 'offscreen'):
    selected = [r for r in rows if r[1] == label]
    print(f'{label}: mean={mean(r[3] for r in selected):.6f}, max={max(r[3] for r in selected):.6f}, exact={sum(r[4] for r in selected)}/{len(selected)}')
print(out)
