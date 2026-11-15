"""Original Folia browser / actual Android fixed-font captures. No automatic visual pass."""
from pathlib import Path
from PIL import Image, ImageChops, ImageDraw
import argparse
import csv
import json

parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--api',type=int,required=True)
parser.add_argument('--label',default='')
args=parser.parse_args()
root=Path(__file__).resolve().parents[1]/'build/verification'
if args.label and not all(c.isascii() and (c.isalnum() or c=='-') for c in args.label):parser.error('Invalid evidence label')
tag=f'classic-android-api{args.api}{"-"+args.label if args.label else ""}'
candidate=root/tag
conditions=json.loads((candidate/'conditions.json').read_text(encoding='utf8'))
output=root/f'{tag}-comparison'
output.mkdir(parents=True,exist_ok=True)
report=[]
groups={}
seen=set()
for frame in conditions['frames']:
    if frame['file'] in seen:continue  # Revisit hashes are checked by capture-android.
    seen.add(frame['file'])
    scene,width,mode,ms=frame['scene'],frame['width'],frame['mode'],frame['mediaMs']
    if scene=='subtitle':
        original=root/('subtitle-backdrop-upstream' if mode=='backdrop' else 'subtitle-upstream')/f'{width}-translation-{ms}ms.png'
    elif scene=='dense':
        original=root/'classic-dense-upstream'/f'{ms}ms.png'
    else:
        original=root/'classic-manual-upstream'/f'{"" if scene=="baseline" else scene+"-"}{mode}-{ms}ms.png'
    a,b=Image.open(original).convert('RGB'),Image.open(candidate/frame['file']).convert('RGB')
    assert a.size==b.size
    bg=(22,36,53)
    selected=[sum(abs(x[k]-y[k]) for k in range(3))/3 for x,y in zip(a.getdata(),b.getdata())
        if max(abs(x[k]-bg[k]) for k in range(3))>8 or max(abs(y[k]-bg[k]) for k in range(3))>8]
    report.append((scene,width,mode,ms,len(selected),sum(selected)/len(selected) if selected else 0))
    groups.setdefault((scene,width,mode),[]).append((ms,a,b))
for (scene,width,mode),frames in groups.items():
    selected=frames if scene=='dense' else [f for f in frames if f[0] in [750,1000,1500,2100,3400,4400,5500]]
    sheet=Image.new('RGB',(width*2,622*len(selected)),'#eef1f5')
    for i,(ms,a,b) in enumerate(selected):
        for j,(label,image) in enumerate([('Original Folia',a),(f'Android API{args.api}',b)]):
            sheet.paste(image,(width*j,622*i+22))
            ImageDraw.Draw(sheet).text((width*j+4,622*i+4),f'{label} | {scene} | {mode} | {ms}ms',fill='black')
    sheet.save(output/f'{scene}-{width}-{mode}-contact.png')
with (output/'pixel-errors.csv').open('w',newline='') as f:
    writer=csv.writer(f)
    writer.writerow(('scene','width','mode','media_ms','union_foreground_pixels','mean_abs_rgb_error_0_255'))
    writer.writerows(report)
print(output)
