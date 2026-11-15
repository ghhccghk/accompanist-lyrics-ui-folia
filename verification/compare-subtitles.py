"""Subtitle contact sheets and descriptive foreground error; no automatic visual pass."""
from pathlib import Path
from PIL import Image, ImageChops, ImageDraw
import csv
import argparse

root = Path(__file__).resolve().parents[1] / 'build/verification'
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--grayscale',action='store_true')
args=parser.parse_args()
reference='subtitle-grayscale-upstream' if args.grayscale else 'subtitle-upstream'
output = root / ('subtitle-grayscale-comparison' if args.grayscale else 'subtitle-comparison')
output.mkdir(parents=True, exist_ok=True)
report = []
times = [750, 850, 1500, 2500, 3500, 5150, 5500]
for width in (800, 380):
    for layout in ('translation', 'current+translation+romanization'):
        sheet = Image.new('RGB', (width * 2, 220 * len(times)), '#eef1f5')
        for i, ms in enumerate(times):
            images = [Image.open(root / f'{side}/{width}-{layout}-{ms}ms.png').convert('RGB') for side in (reference, 'subtitle-kotlin')]
            for j, (label, image) in enumerate(zip(('Original Folia', 'Kotlin Compose'), images)):
                sheet.paste(image.crop((0, 400, width, 600)), (width*j, 220*i+20))
                ImageDraw.Draw(sheet).text((width*j+4,220*i+4), f'{label} | {ms}ms', fill='black')
            background = Image.new('RGB', images[0].size, (22, 36, 53))
            masks = [ImageChops.difference(image, background) for image in images]
            bbox = ImageChops.lighter(*masks).getbbox()
            selected = []
            if bbox:
                a, b = [list(image.crop(bbox).getdata()) for image in images]
                for p, q in zip(a,b):
                    if max(abs(p[k]-(22,36,53)[k]) for k in range(3)) > 8 or max(abs(q[k]-(22,36,53)[k]) for k in range(3)) > 8:
                        selected.append(sum(abs(p[k]-q[k]) for k in range(3))/3)
            report.append((width,layout,ms,len(selected),sum(selected)/len(selected) if selected else 0))
        sheet.save(output / f'{width}-{layout}-contact.png')
with (output / 'pixel-errors.csv').open('w',newline='') as f:
    writer=csv.writer(f);writer.writerow(('width','layout','media_ms','union_foreground_pixels','mean_abs_rgb_error_0_255'));writer.writerows(report)
print(output)
