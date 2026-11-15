"""Actual mixed-direction classic frames; descriptive comparisons, no visual pass threshold."""
from pathlib import Path
from PIL import Image, ImageChops, ImageDraw, ImageStat
import csv

root = Path(__file__).resolve().parents[1] / 'build/verification'
out = root / 'classic-bidi-comparison'; out.mkdir(parents=True, exist_ok=True)
candidates = [('Legacy', 'classic-bidi-legacy'), ('Kotlin', 'classic-bidi-kotlin')]
for api in [30, 34]:
    folder = f'classic-android-api{api}-bidi'
    if (root / folder / 'conditions.json').exists(): candidates.append((f'API{api}', folder))
rows = []
for scene in ['bidi-rtl', 'bidi-ltr']:
    for mode in ['default', 'avoid']:
        contact = Image.new('RGB', (800 * (len(candidates) + 1), 322 * 3), '#eef1f5')
        for ms in [750, 1000, 1200, 1500, 1900, 2100, 2300, 2500]:
            a = Image.open(root / 'classic-bidi-upstream' / f'{scene}-{mode}-{ms}ms.png').convert('RGB')
            images = [('Original', a)]
            for label, folder in candidates:
                name = f'{scene}-800-{mode}-{ms}ms.png' if label.startswith('API') else f'{scene}-{mode}-{ms}ms.png'
                b = Image.open(root / folder / name).convert('RGB')
                assert a.size == b.size == (800, 600)
                mask = Image.new('L', a.size)
                for image in [a, b]:
                    for channel in ImageChops.difference(image, Image.new('RGB', image.size, (22, 36, 53))).split():
                        mask = ImageChops.lighter(mask, channel)
                mask = mask.point(lambda x: 255 if x > 8 else 0)
                count = mask.histogram()[255]
                mae = sum(ImageStat.Stat(ImageChops.difference(a, b), mask).mean) / 3 if count else 0
                rows.append((label, scene, mode, ms, count, mae)); images.append((label, b))
            if ms in [1200, 1500, 1900]:
                y = [1200, 1500, 1900].index(ms) * 322
                for col, (label, image) in enumerate(images):
                    contact.paste(image.crop((0, 150, 800, 450)), (col * 800, y + 22))
                    ImageDraw.Draw(contact).text((col * 800 + 4, y + 4), f'{label} | {scene} | {mode} | {ms}ms', fill='black')
        contact.save(out / f'{scene}-{mode}-contact.png')
with (out / 'pixel-errors.csv').open('w', newline='') as f:
    writer = csv.writer(f)
    writer.writerow(['renderer', 'scene', 'mode', 'media_ms', 'union_foreground_pixels', 'mean_abs_rgb_error_0_255']); writer.writerows(rows)
print(out)
