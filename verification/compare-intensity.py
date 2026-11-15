"""Original classic intensity against Kotlin and optional Android captures.

Pixel errors are descriptive; contact sheets retain entry, activity, afterglow
and exit. Normal-only frames demonstrate the previously missing intensity.
"""
import argparse
import csv
import json
from collections import defaultdict
from pathlib import Path
from statistics import mean

from PIL import Image, ImageChops, ImageDraw, ImageStat

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--android-api', type=int)
parser.add_argument('--android-label-prefix', default='intensity',
                    help='Capture labels are <prefix>-calm/-normal/-chaotic')
args = parser.parse_args()
root = Path(__file__).resolve().parents[1] / 'build/verification'
out = root / 'classic-intensity-comparison'
out.mkdir(parents=True, exist_ok=True)
times = [750, 1000, 1500, 1900, 2100, 2300, 2500, 2800, 3000]
rows = []
for level in ('calm', 'normal', 'chaotic'):
    android = root / f'classic-android-api{args.android_api}-{args.android_label_prefix}-{level}' if args.android_api else None
    if android:
        assert not (android / 'INVALID.txt').exists(), f'Invalid capture: {android}'
        conditions = json.loads((android / 'conditions.json').read_text(encoding='utf8'))
        assert conditions['animationIntensity'] == level
        assert len(conditions['frames']) == 72
        assert all(f['pausedRepeatIdentical'] for f in conditions['frames'])
        assert sum(f['seekRevisit'] for f in conditions['frames']) == 36
    for scene, width in (('baseline', 800), ('wrap', 380)):
        for mode in ('default', 'avoid'):
            columns = 4 if android else 3
            sheet = Image.new('RGB', (width * columns, 624 * len(times)), '#eef1f5')
            for i, ms in enumerate(times):
                name = f'{level}-{scene}-{mode}-{ms}ms.png'
                paths = [('Original Folia', root / 'classic-intensity-upstream' / name),
                         ('Previous Normal-only', root / 'classic-intensity-before-precision' / f'normal-{scene}-{mode}-{ms}ms.png'),
                         ('Kotlin intensity', root / 'classic-intensity-kotlin' / name)]
                if android:
                    paths.append((f'API{args.android_api}', android / f'{scene}-{width}-{mode}-{ms}ms.png'))
                images = [(label, Image.open(path).convert('RGB')) for label, path in paths]
                assert all(image.size == (width, 600) for _, image in images)
                a = images[0][1]
                for j, (label, image) in enumerate(images):
                    sheet.paste(image, (j * width, i * 624 + 24))
                    ImageDraw.Draw(sheet).text((j * width + 4, i * 624 + 4), f'{label} | {ms}ms', fill='black')
                    if j == 0:
                        continue
                    mask = Image.new('L', a.size)
                    for im in (a, image):
                        for ch in ImageChops.difference(im, Image.new('RGB', im.size, (22, 36, 53))).split():
                            mask = ImageChops.lighter(mask, ch)
                    mask = mask.point(lambda x: 255 if x > 8 else 0)
                    error = mean(ImageStat.Stat(ImageChops.difference(a, image), mask).mean) if mask.getbbox() else 0
                    rows.append((level, scene, mode, ms, label, mask.histogram()[255], error))
            sheet.save(out / f'{level}-{scene}-{mode}{"-android" if android else ""}-contact.png')
with (out / f'pixel-errors{"-android" if args.android_api else ""}.csv').open('w', newline='') as f:
    writer = csv.writer(f)
    writer.writerow(('intensity', 'scene', 'mode', 'media_ms', 'candidate', 'union_foreground_pixels', 'rgb_mae'))
    writer.writerows(rows)
groups = defaultdict(list)
for row in rows:
    groups[(row[0], row[4])].append(row[-1])
for key, values in groups.items():
    print(*key, f'mean={mean(values):.6f}', f'max={max(values):.6f}')
print(out)
