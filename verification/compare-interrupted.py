"""Describe interrupted classic frames; RGB errors alone do not grant visual parity."""
from pathlib import Path
from PIL import Image, ImageChops, ImageDraw, ImageStat
import csv

root = Path(__file__).resolve().parents[1] / 'build/verification'
out = root / 'classic-interrupted-comparison'
out.mkdir(parents=True, exist_ok=True)
times = [731, 761, 791, 821, 851, 881, 941, 1001, 1041, 1101, 1241, 1401, 1701, 2001]
selected = [761, 941, 1101, 1241, 1401]
rows = []
candidates = [('JVM', root / 'classic-interrupted-kotlin')]
for api in [30,34]:
    folder=root / f'classic-android-api{api}-interrupted'
    if (folder/'conditions.json').exists(): candidates.append((f'API{api}',folder))
contact = Image.new('RGB', (800 * (len(candidates)+1), 260 * len(selected) * 2), '#eef1f5')
for duration in [150, 300]:
    for ms in times:
        name = f'{duration}-{ms}ms.png'
        a = Image.open(root / 'classic-interrupted-upstream' / name).convert('RGB')
        images=[('Original',a)]
        for label,folder in candidates:
            scene='interrupted-fast' if duration==150 else 'interrupted-normal'
            candidate_name=name if label=='JVM' else f'{scene}-800-default-{ms}ms.png'
            b = Image.open(folder / candidate_name).convert('RGB')
            assert a.size == b.size == (800, 600)
            union = Image.new('L', a.size)
            for image in [a, b]:
                for channel in ImageChops.difference(image, Image.new('RGB', image.size, (22, 36, 53))).split():
                    union = ImageChops.lighter(union, channel)
            union = union.point(lambda x: 255 if x > 8 else 0)
            count = union.histogram()[255]
            mae = sum(ImageStat.Stat(ImageChops.difference(a, b), union).mean) / 3 if count else 0
            rows.append((label,duration,ms,count,mae));images.append((label,b))
        if ms in selected:
            row=([150,300].index(duration)*len(selected)+selected.index(ms))*260
            for col,(label,image) in enumerate(images):
                # Original-size crop; full original PNGs remain available.
                contact.paste(image.crop((0,180,800,418)),(800*col,row+22))
                ImageDraw.Draw(contact).text((800*col+4,row+4),f'{label} | duration {duration} | {ms}ms',fill='black')
with (out / 'pixel-errors.csv').open('w', newline='') as f:
    writer = csv.writer(f)
    writer.writerow(['renderer','duration_ms', 'media_ms', 'union_foreground_pixels', 'mean_abs_rgb_error_0_255'])
    writer.writerows(rows)
contact.save(out / 'contact.png')
print(out)
