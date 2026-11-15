"""Fixed original subtitle overflow comparisons; no pixel threshold grants acceptance."""
from pathlib import Path
import argparse
import csv
import hashlib
import json
import numpy as np
from PIL import Image, ImageDraw

parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--android-api',type=int,nargs='+',default=[])
parser.add_argument('--android-label',default='subtitle-preview')
args=parser.parse_args()
project=Path(__file__).resolve().parents[1]
root=project/'build/verification'
out=root/'subtitle-preview-final-comparison'
out.mkdir(parents=True,exist_ok=True)


def error(a,b):
    a=np.asarray(a,dtype=np.int16); b=np.asarray(b,dtype=np.int16)
    background=np.array((22,36,53))
    mask=(np.abs(a-background).max(axis=2)>8)|(np.abs(b-background).max(axis=2)>8)
    return float(np.abs(a-b)[mask].mean()) if mask.any() else 0.


rows=[]; hashes={}; summaries={}
for tag in ['cjk','cjk-repeat2','cjk-next','cjk-repeat2-next','cjk-next-backdrop','cjk-repeat2-next-backdrop']:
    frames=[]
    for source in sorted((root/f'subtitle-{tag}-upstream').glob('*ms.png')):
        current=root/f'subtitle-{tag}-kotlin'/source.name
        original=Image.open(source).convert('RGB')
        images=[original,Image.open(current).convert('RGB')]
        labels=['Original Folia','Kotlin Compose']
        width,layout,ms=source.stem.split('-'); width=int(width); time=int(ms[:-2])
        paths=[source,current]
        measurements={'jvm':error(*images)}
        if 'next' in tag:
            repeat=2 if 'repeat2' in tag else 1
            if 'backdrop' not in tag:
                native=root/'subtitle-native-window'/f'{width}-repeat{repeat}-{time}ms.png'
                if native.exists():
                    images.append(Image.open(native).convert('RGB')); labels.append('Native Desktop layer')
                    measurements['native']=error(original,images[-1]); paths.append(native)
            for api in args.android_api:
                android=root/f'classic-android-api{api}-{args.android_label}'/f'subtitle-cjk{"-repeat2" if repeat==2 else ""}-{width}-{"backdrop" if "backdrop" in tag else "transparent"}-{time}ms.png'
                if android.exists():
                    images.append(Image.open(android).convert('RGB')); labels.append(f'API{api}')
                    measurements[f'android{api}']=error(original,images[-1]); paths.append(android)
        for platform,mae in measurements.items(): rows.append((tag,source.name,platform,mae))
        for path in paths: hashes[str(path.relative_to(root))]=hashlib.sha256(path.read_bytes()).hexdigest()
        frames.append(measurements['jvm'])
        if time in [1500,5500]:
            sheet=Image.new('RGB',(width*len(images),620),'#eef1f5')
            for i,(label,im) in enumerate(zip(labels,images)):
                ImageDraw.Draw(sheet).text((i*width+4,3),f'{label} | {tag} {time}ms',fill='black')
                sheet.paste(im,(i*width,20))
            sheet.save(out/f'{tag}-{source.name}')
    summaries[tag]={'frames':len(frames),'mean_rgb_mae':sum(frames)/len(frames),'max_rgb_mae':max(frames)}
with (out/'pixel-errors.csv').open('w',newline='',encoding='utf8') as f:
    writer=csv.writer(f); writer.writerow(['tag','frame','platform','foreground_rgb_mae']);writer.writerows(rows)
for platform in ['native',*[f'android{api}' for api in args.android_api]]:
    values=[r[3] for r in rows if r[2]==platform]
    if values:summaries[platform]={'frames':len(values),'mean_rgb_mae':sum(values)/len(values),'max_rgb_mae':max(values)}
sources=[p for source_set in ['commonMain','jvmMain','androidMain'] for p in (project/'library/src'/source_set).rglob('*.kt')]
sources += [project/'verification/assets/FoliaParitySubtitle-Bold.otf',project/'verification/classic-fixtures/subtitle-cjk-scenes.tsv']
(out/'conditions.json').write_text(json.dumps({'summary':summaries,'png_sha256':hashes,
    'source_sha256':{str(p.relative_to(project)).replace('\\','/'):hashlib.sha256(p.read_bytes()).hexdigest() for p in sources},
    'note':'Full unaligned frames; same synthetic source/font/viewport/media time. Native Desktop capture is the post-draw GraphicsLayer, not an OS screenshot. Device capture conditions stored alongside PNGs. Errors describe residuals and do not grant acceptance.'},indent=2),encoding='utf8')
print(json.dumps(summaries,indent=2))
