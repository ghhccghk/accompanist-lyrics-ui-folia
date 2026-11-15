"""Compose a sampled original/Kotlin animation; report error without declaring visual pass."""
from pathlib import Path
from PIL import Image, ImageChops, ImageDraw, ImageStat
import csv
import hashlib
import json

root=Path(__file__).resolve().parents[1]/'build/verification'
upstream=root/'classic-continuous-upstream'
kotlin=root/'classic-continuous-kotlin'
conditions=json.loads((upstream/'conditions.json').read_text(encoding='utf8'))
times=[int(t) for t in (kotlin/'times.txt').read_text().splitlines()]
assert times==conditions['times'], 'Both sides must use exactly the same media timestamps'
output=root/'classic-continuous-comparison';output.mkdir(parents=True,exist_ok=True)
rows=[]
for mode in ['default','avoid']:
    frames=[]
    for ms in times:
        name=f'{mode}-{ms}ms.png'
        a,b=[Image.open(folder/name).convert('RGB') for folder in [upstream,kotlin]]
        assert a.size==b.size==(800,600)
        mask=Image.new('L',a.size,0)
        for im in [a,b]:
            for channel in ImageChops.difference(im,Image.new('RGB',im.size,(22,36,53))).split():
                mask=ImageChops.lighter(mask,channel)
        mask=mask.point(lambda x:255 if x>8 else 0)
        count=mask.histogram()[255]
        mae=sum(ImageStat.Stat(ImageChops.difference(a,b),mask).mean)/3 if count else 0
        rows.append((mode,ms,count,mae,hashlib.sha256(b.tobytes()).hexdigest()))
        frame=Image.new('RGB',(1200,472),'#eef1f5')
        for i,(label,im) in enumerate([('Original Folia',a),('Kotlin Compose',b)]):
            frame.paste(im.resize((600,450)),(i*600,22))
            ImageDraw.Draw(frame).text((i*600+5,5),f'{label} | {mode} | {ms}ms',fill='black')
        frames.append(frame)
    durations=[b-a for a,b in zip(times,times[1:])]+[40]
    frames[0].save(output/f'{mode}.gif',save_all=True,append_images=frames[1:],duration=durations,loop=0,disposal=2)
with (output/'pixel-errors.csv').open('w',newline='') as f:
    writer=csv.writer(f);writer.writerow(['mode','media_ms','union_foreground_pixels','mean_abs_rgb_error_0_255','kotlin_pixel_sha256']);writer.writerows(rows)
(output/'README.txt').write_text('Original / Kotlin; 800x600 native PNG, 600x450 GIF preview each side. GIF uses indexed color. Media timestamps and original grid are identical. No audio. This is an offline sampled sequence, not a real-window recording, throughput measurement or automatic visual pass.\n',encoding='utf8')
print(output)
