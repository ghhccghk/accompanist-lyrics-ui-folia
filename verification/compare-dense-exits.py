"""Describe dense transitions against real Folia; preserve both old and new captures.

Legacy capture conditions identify the prior capped timeline and allocation shim.
No automatic visual pass is inferred from these RGB errors or contact crops.
"""
from pathlib import Path
from PIL import Image,ImageChops,ImageDraw,ImageStat
import csv

root=Path(__file__).resolve().parents[1]/'build/verification'
out=root/'classic-dense-comparison';out.mkdir(parents=True,exist_ok=True)
times=[801,901,1001,1051,1101,1201,1301]
candidates=[('legacy4','classic-dense-legacy4','{ms}ms.png'),('kotlin','classic-dense-kotlin','{ms}ms.png')]
for api in [30,34]:
    folder=next((f'classic-android-api{api}-{tag}' for tag in ['dense-token','dense']
        if (root/f'classic-android-api{api}-{tag}/conditions.json').exists()),None)
    if folder:
        candidates.append((f'android-api{api}',folder,'dense-800-default-{ms}ms.png'))
rows=[]
contact=Image.new('RGB',(380*(len(candidates)+1),242*3),'#eef1f5')
for ms in times:
    a=Image.open(root/f'classic-dense-upstream/{ms}ms.png').convert('RGB')
    images=[('Original',a)]
    for label,folder,pattern in candidates:
        b=Image.open(root/folder/pattern.format(ms=ms)).convert('RGB');assert a.size==b.size==(800,600)
        union=Image.new('L',a.size)
        for image in [a,b]:
            for channel in ImageChops.difference(image,Image.new('RGB',image.size,(22,36,53))).split():
                union=ImageChops.lighter(union,channel)
        union=union.point(lambda x:255 if x>8 else 0);count=union.histogram()[255]
        mae=sum(ImageStat.Stat(ImageChops.difference(a,b),union).mean)/3 if count else 0
        rows.append((label,ms,count,mae));images.append((label,b))
    if ms in [801,901,1001]:
        row=[801,901,1001].index(ms)
        for col,(label,image) in enumerate(images):
            contact.paste(image.crop((0,180,380,400)),(380*col,242*row+22))
            ImageDraw.Draw(contact).text((380*col+4,242*row+4),f'{label} | {ms}ms | crop x0..380 y180..400',fill='black')
with (out/'pixel-errors.csv').open('w',newline='') as f:
    writer=csv.writer(f);writer.writerow(['renderer','media_ms','union_foreground_pixels','mean_abs_rgb_error_0_255']);writer.writerows(rows)
contact.save(out/'contact.png')
print(out)
