"""Describe interrupted classic frames; RGB errors alone do not grant visual parity."""
from pathlib import Path
from PIL import Image, ImageChops, ImageDraw, ImageStat
import csv

root = Path(__file__).resolve().parents[1] / 'build/verification'
out = root / 'classic-interrupted-comparison'
out.mkdir(parents=True, exist_ok=True)
times = [731, 761, 791, 821, 851, 881, 941, 1001, 1041, 1101, 1241, 1401, 1701, 2001]
selected = [761, 941, 1101, 1401]
contact = Image.new('RGB', (800, 260 * len(selected) * 2), '#eef1f5')
rows = []
for duration in [150, 300]:
    for ms in times:
        name = f'{duration}-{ms}ms.png'
        a = Image.open(root / 'classic-interrupted-upstream' / name).convert('RGB')
        b = Image.open(root / 'classic-interrupted-kotlin' / name).convert('RGB')
        assert a.size == b.size == (800, 600)
        union = Image.new('L', a.size)
        for image in [a, b]:
            for channel in ImageChops.difference(image, Image.new('RGB', image.size, (22, 36, 53))).split():
                union = ImageChops.lighter(union, channel)
        union = union.point(lambda x: 255 if x > 8 else 0)
        count = union.histogram()[255]
        mae = sum(ImageStat.Stat(ImageChops.difference(a, b), union).mean) / 3 if count else 0
        rows.append((duration, ms, count, mae))
        if ms in selected:
            row = ([150, 300].index(duration) * len(selected) + selected.index(ms)) * 260
            for col, (label, image) in enumerate([('Original', a), ('Kotlin', b)]):
                # Original-size crop, never silently rescaled.
                contact.paste(image.crop((0, 180, 400, 418)), (400 * col, row + 22))
                ImageDraw.Draw(contact).text((400 * col + 4, row + 4), f'{label} | duration {duration} | {ms}ms', fill='black')
with (out / 'pixel-errors.csv').open('w', newline='') as f:
    writer = csv.writer(f)
    writer.writerow(['duration_ms', 'media_ms', 'union_foreground_pixels', 'mean_abs_rgb_error_0_255'])
    writer.writerows(rows)
contact.save(out / 'contact.png')
print(out)
