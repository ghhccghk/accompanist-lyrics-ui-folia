"""Describe isolated body raster settings against unchanged original GlowWord PNGs.

Run BodyRasterTest first. This probe does not choose a setting or define acceptance.
"""
from pathlib import Path
from PIL import Image, ImageChops, ImageStat
import csv

root=Path(__file__).resolve().parents[1]/'build/verification'
rows=[]
for reference in sorted((root/'raster-upstream').glob('*.png')):
    if reference.stem=='original':continue
    a=Image.open(reference).getchannel('A')
    for suffix in ['', '-no-hint', '-lcd']:
        candidate=root/'raster-kotlin'/f'{reference.stem}{suffix}.png'
        if not candidate.exists():continue
        b=Image.open(candidate).getchannel('A')
        mask=ImageChops.lighter(a,b).point(lambda x:255 if x else 0)
        rows.append((reference.stem,suffix or 'platform-default',sum(a.getdata())/255,
            sum(b.getdata())/255,ImageStat.Stat(ImageChops.difference(a,b),mask).mean[0]))
output=root/'raster-kotlin/settings.csv'
with output.open('w',newline='') as f:
    writer=csv.writer(f);writer.writerow(['scene_pose','setting','original_alpha_area_px2','kotlin_alpha_area_px2','foreground_alpha_mae_0_255']);writer.writerows(rows)
print(output)
