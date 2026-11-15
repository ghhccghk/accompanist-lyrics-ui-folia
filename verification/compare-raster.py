"""Report isolated body coverage; descriptive metrics are not an acceptance pass."""
from pathlib import Path
import csv
from PIL import Image, ImageChops

root = Path(__file__).resolve().parents[1] / 'build/verification'
browser = root / 'raster-upstream'
kotlin = root / 'raster-kotlin'
assert ImageChops.difference(Image.open(browser / 'original.png'), Image.open(browser / 'identity.png')).getbbox() is None, 'Font metadata must not change the original browser paint'
rows = []
for pose in ('original', 'identity', 'transform', 'identity-raster', 'transform-raster'):
    a = Image.open(browser / (pose.replace('original', 'identity').replace('-raster','') + '.png')).getchannel('A')
    b = Image.open(kotlin / f'{pose}.png').getchannel('A')
    union = [i for i, (x, y) in enumerate(zip(a.getdata(), b.getdata())) if x or y]
    delta = list(ImageChops.difference(a, b).getdata())
    rows.append((pose, sum(a.getdata()) / 255, sum(b.getdata()) / 255, sum(delta[i] for i in union) / len(union)))
with (kotlin / 'comparison.csv').open('w', newline='') as f:
    writer = csv.writer(f)
    writer.writerow(('pose', 'browser_alpha_area_px2', 'kotlin_alpha_area_px2', 'foreground_alpha_mae_0_255'))
    writer.writerows(rows)
print(*rows, sep='\n')
