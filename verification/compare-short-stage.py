"""Original short viewports against unchanged-before and corrected Kotlin.

Images stay at native size: do not align or resample the discrepancy away.
Pixel errors describe residuals; original geometry is checked in the JVM test.
"""
import argparse
import csv
import json
from pathlib import Path
from statistics import mean

from PIL import Image, ImageChops, ImageDraw, ImageStat

root=Path(__file__).resolve().parents[1]/'build/verification'
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--android-api',type=int)
parser.add_argument('--android-label-prefix',default='short-stage')
parser.add_argument('--heights',nargs='+',type=int,default=[100,150,180,200,240,300])
parser.add_argument('--output',default='short-stage-comparison')
args=parser.parse_args()
out=root/args.output;out.mkdir(parents=True,exist_ok=True)
rows=[]
for level in ('calm','normal','chaotic'):
    for mode in ('default','avoid'):
        heights=args.heights
        sheet=Image.new('RGB',(800*(4 if args.android_api else 3),sum(heights)+24*len(heights)),'#eef1f5')
        y=0
        for h in heights:
            name=f'{level}-800x{h}-{mode}.png'
            images=[Image.open(folder/name).convert('RGB') for folder in (
                root/'short-stage-upstream',root/'short-stage-before/short-stage-kotlin',root/'short-stage-kotlin')]
            labels=['Original','Before 80px area floor','Actual area budget']
            if args.android_api:
                folder=root/f'classic-android-api{args.android_api}-{args.android_label_prefix}-{level}-{h}'
                assert not (folder/'INVALID.txt').exists()
                conditions=json.loads((folder/'conditions.json').read_text(encoding='utf8'))
                assert conditions['viewportHeight']==h and conditions['animationIntensity']==level
                assert len(conditions['frames'])==4
                assert all(f['pausedRepeatIdentical'] for f in conditions['frames'])
                assert sum(f['seekRevisit'] for f in conditions['frames'])==2
                images.append(Image.open(folder/f'baseline-800-{mode}-1500ms.png').convert('RGB'))
                labels.append(f'API{args.android_api}')
            assert all(im.size==(800,h) for im in images)
            for col,(label,im) in enumerate(zip(labels,images)):
                sheet.paste(im,(col*800,y+24))
                ImageDraw.Draw(sheet).text((col*800+4,y+5),f'{label} | {h}px high',fill='black')
                if col:
                    a=images[0];mask=Image.new('L',a.size)
                    for image in (a,im):
                        for ch in ImageChops.difference(image,Image.new('RGB',a.size,(22,36,53))).split():
                            mask=ImageChops.lighter(mask,ch)
                    mask=mask.point(lambda x:255 if x>8 else 0)
                    error=mean(ImageStat.Stat(ImageChops.difference(a,im),mask).mean) if mask.getbbox() else 0
                    rows.append((level,h,mode,label,error))
            y+=h+24
        sheet.save(out/f'{level}-{mode}{"-android" if args.android_api else ""}.png')
with (out/f'pixel-errors{"-android" if args.android_api else ""}.csv').open('w',newline='',encoding='utf8') as f:
    writer=csv.writer(f);writer.writerow(('intensity','height','mode','candidate','union_foreground_rgb_mae'));writer.writerows(rows)
assert len(rows)==len(args.heights)*6*(3 if args.android_api else 2)
for label in sorted(set(r[3] for r in rows)):
    values=[r[-1] for r in rows if r[3]==label and r[2]=='avoid']
    print(label,'avoid mean',mean(values),'maximum',max(values))
(out/'conditions.json').write_text(json.dumps({
    'original':json.loads((root/'short-stage-upstream/conditions.json').read_text(encoding='utf8')),
    'beforeRevision':'a590f15',
    'note':'Same font, 800px width, 1500ms; before failure report retained in short-stage-before/results.xml. '
           'Native viewport crops are the full original dimensions; no registration or automatic visual pass.',
},indent=2),encoding='utf8')
print(out)
