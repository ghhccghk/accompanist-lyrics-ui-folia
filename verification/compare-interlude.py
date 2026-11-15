"""Compare actual interlude frames; preserve old implementation and unchanged oracle."""
from pathlib import Path
from PIL import Image, ImageChops, ImageDraw, ImageStat
import csv

root = Path(__file__).resolve().parents[1] / 'build/verification'
out = root / 'classic-interlude-comparison'; out.mkdir(parents=True, exist_ok=True)
candidates = [('Legacy', 'classic-interlude-legacy'), ('Kotlin', 'classic-interlude-kotlin')]
for api in [30, 34]:
    folder = f'classic-android-api{api}-interlude'
    if (root / folder / 'conditions.json').exists(): candidates.append((f'API{api}', folder))
rows = []
for scene in ['interlude', 'interlude-single']:
    for width in [800, 380]:
        contact = Image.new('RGB', (width * (len(candidates) + 1), 242 * 6), '#eef1f5')
        for m, mode in enumerate(['default', 'avoid']):
            for ms in [750, 1000, 1500, 1900, 2300, 2500]:
                name = f'{scene}-{width}-{mode}-{ms}ms.png'
                a = Image.open(root / 'classic-interlude-upstream' / name).convert('RGB')
                images = [('Original', a)]
                for label, folder in candidates:
                    b = Image.open(root / folder / name).convert('RGB')
                    assert a.size == b.size == (width, 600)
                    mask = Image.new('L', a.size)
                    for image in [a, b]:
                        for channel in ImageChops.difference(image, Image.new('RGB', image.size, (22, 36, 53))).split():
                            mask = ImageChops.lighter(mask, channel)
                    mask = mask.point(lambda x: 255 if x > 8 else 0)
                    count = mask.histogram()[255]
                    mae = sum(ImageStat.Stat(ImageChops.difference(a, b), mask).mean) / 3 if count else 0
                    rows.append((label, scene, width, mode, ms, count, mae))
                    images.append((label, b))
                if ms in [750, 1500, 2300]:
                    y = (m * 3 + [750, 1500, 2300].index(ms)) * 242
                    for col, (label, image) in enumerate(images):
                        contact.paste(image.crop((0, 180, width, 400)), (col * width, y + 22))
                        ImageDraw.Draw(contact).text((col * width + 4, y + 4), f'{label} | {mode} | {ms}ms', fill='black')
        contact.save(out / f'{scene}-{width}-contact.png')
with (out / 'pixel-errors.csv').open('w', newline='') as f:
    writer = csv.writer(f)
    writer.writerow(['renderer', 'scene', 'viewport_width', 'mode', 'media_ms', 'union_foreground_pixels', 'mean_abs_rgb_error_0_255'])
    writer.writerows(rows)
print(out)
