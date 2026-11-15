"""DPR1/2 public entry vs actual original Classic; errors remain descriptive."""
from pathlib import Path
from PIL import Image, ImageChops, ImageDraw, ImageStat
import csv
from collections import defaultdict
from statistics import mean

root=Path(__file__).resolve().parents[1]/'build/verification'
out=root/'layout-phase-public-comparison';out.mkdir(parents=True,exist_ok=True)
rows=[]; summary=defaultdict(list)
for ref in sorted((root/'layout-phase-public-upstream').glob('*.png')):
    a=Image.open(ref).convert('RGB');b=Image.open(root/'layout-phase-public-kotlin'/ref.name).convert('RGB')
    assert a.size==b.size
    mask=Image.new('L',a.size)
    for im in [a,b]:
        for ch in ImageChops.difference(im,Image.new('RGB',a.size,(22,36,53))).split():mask=ImageChops.lighter(mask,ch)
    mask=mask.point(lambda x:255 if x>8 else 0)
    error=sum(ImageStat.Stat(ImageChops.difference(a,b),mask).mean)/3 if mask.getbbox() else 0
    parts=ref.stem.split('-');key='-'.join(parts[:3]);summary[key].append(error)
    rows.append((ref.name,mask.histogram()[255],error))
    if parts[-1] in ['1000ms','1500ms']:
        w,h=a.size;sheet=Image.new('RGB',(w*2,h+24),'#eef1f5')
        for i,(label,im) in enumerate([('Original',a),('Public Compose',b)]):
            sheet.paste(im,(i*w,24));ImageDraw.Draw(sheet).text((i*w+4,4),label,fill='black')
        sheet.save(out/ref.name)
with (out/'pixel-errors.csv').open('w',newline='') as f:
    writer=csv.writer(f);writer.writerow(['frame','union_foreground_physical_pixels','rgb_mae']);writer.writerows(rows)
for key,values in summary.items():print(key,'mean',round(mean(values),4),'max',round(max(values),4))
print(out)
