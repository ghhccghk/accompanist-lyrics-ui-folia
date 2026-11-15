"""Describe full-frame native-ink candidate versus original Classic and prior white-mask path."""
from pathlib import Path
import argparse, csv, hashlib, json
import numpy as np
from PIL import Image, ImageDraw

p=argparse.ArgumentParser(description=__doc__)
mode=p.add_mutually_exclusive_group()
mode.add_argument('--body-only',action='store_true')
mode.add_argument('--default-palette',action='store_true')
args=p.parse_args()
root=Path(__file__).resolve().parents[1];base=root/'build/verification'
prefix='classic-colors-body-' if args.body_only else 'classic-colors-'
reference=base/(prefix+'browser')
tinted=base/(prefix+'tinted');native=base/(prefix+'native')
if args.default_palette:
    prefix='classic-body-default-';reference=base/'classic-manual-upstream'
    tinted=base/'classic-white-kotlin';native=base/'classic-kotlin'
    original=[json.loads((reference/f'{m}-frames.json').read_text(encoding='utf8')) for m in ['default','avoid']]
    conditions={'original':original,'frames':[{'scene':m,'palette':'default','time':f['mediaMs'],
        'width':800,'file':f'{m}-{f["mediaMs"]}ms.png'} for m,c in zip(['default','avoid'],original) for f in c['frames']]}
else:
    conditions=json.loads((reference/'conditions.json').read_text(encoding='utf8'))
out=base/(prefix+'comparison');out.mkdir(exist_ok=True)
bg=np.array([22,36,53]);rows=[];hashes={}
def read(path):
    hashes[str(path.relative_to(root))]=hashlib.sha256(path.read_bytes()).hexdigest()
    return np.asarray(Image.open(path).convert('RGB')).astype(np.int16)
for frame in conditions['frames']:
    a,b,c=[read(folder/frame['file']) for folder in [reference,tinted,native]]
    assert a.shape==b.shape==c.shape==(600,frame['width'],3)
    mask=np.any(np.abs(a-bg)>8,axis=2)|np.any(np.abs(b-bg)>8,axis=2)|np.any(np.abs(c-bg)>8,axis=2)
    before=float(np.abs(a-b)[mask].mean()) if mask.any() else 0
    after=float(np.abs(a-c)[mask].mean()) if mask.any() else 0
    rows.append({'scene':frame['scene'],'palette':frame['palette'],'time_ms':frame['time'],'file':frame['file'],
        'foreground_pixels':int(mask.sum()),'tinted_mae':before,'native_mae':after,'delta':after-before})
with (out/'errors.csv').open('w',newline='',encoding='utf8') as f:
    writer=csv.DictWriter(f,fieldnames=list(rows[0]));writer.writeheader();writer.writerows(rows)
summary=[]
for scene in (['default','avoid','all'] if args.default_palette else ['baseline','arabic','cjk','all']):
    selected=[r for r in rows if scene=='all' or r['scene']==scene]
    summary.append({'scene':scene,'frames':len(selected),
        'tinted_mean':sum(r['tinted_mae'] for r in selected)/len(selected),
        'native_mean':sum(r['native_mae'] for r in selected)/len(selected),
        'tinted_max':max(r['tinted_mae'] for r in selected),'native_max':max(r['native_mae'] for r in selected),
        'improved':sum(r['delta']<0 for r in selected),'regressed':sum(r['delta']>0 for r in selected),
        'unchanged':sum(r['delta']==0 for r in selected)})
sheet=Image.new('RGB',(1200,3*222),(30,30,30));draw=ImageDraw.Draw(sheet)
for row,(scene,time) in enumerate([('default',1000),('default',1500),('avoid',2100)] if args.default_palette else [('baseline',1000),('arabic',1500),('cjk',2100)]):
    name=f'{scene}-{time}ms.png' if args.default_palette else f'{scene}-warm-{time}ms.png'
    for col,(label,folder) in enumerate([('Original pinned Folia',reference),('White mask tint',tinted),('Native ink candidate',native)]):
        draw.text((col*400+4,row*222+3),f'{label} | {scene} {time}ms',fill='white')
        image=Image.open(folder/name).convert('RGB')
        # Whole frame thumbnail, same scale on all sides. Never used for the metric.
        image.thumbnail((400,200));sheet.paste(image,(col*400+(400-image.width)//2,row*222+20))
sheet.save(out/'contact.png')
(out/'conditions.json').write_text(json.dumps({'reference':conditions,'summary':summary,'png_sha256':hashes,
    'source_sha256':{p:hashlib.sha256((root/p).read_bytes()).hexdigest() for p in [
        'library/src/commonMain/kotlin/top/ghhccghk/accompanist/lyrics/folia/internal/rendering/BodyParagraphRaster.kt',
        'library/src/commonMain/kotlin/top/ghhccghk/accompanist/lyrics/folia/internal/rendering/ParagraphRaster.kt',
        'library/src/commonMain/kotlin/top/ghhccghk/accompanist/lyrics/folia/internal/styles/classic/ClassicLine.kt',
        'library/src/jvmMain/kotlin/top/ghhccghk/accompanist/lyrics/folia/internal/rendering/BodyParagraphRaster.jvm.kt',
        'library/src/androidMain/kotlin/top/ghhccghk/accompanist/lyrics/folia/internal/rendering/BodyParagraphRaster.android.kt',
        'library/src/jvmTest/kotlin/top/ghhccghk/accompanist/lyrics/folia/ClassicColorParityTest.kt',
        'library/src/jvmTest/kotlin/top/ghhccghk/accompanist/lyrics/folia/ClassicParityTest.kt',
        'verification/compare-classic-colors.py',
        'verification/classic-browser/main.tsx','verification/classic-browser/capture-classic-colors.mjs']},
    'limits':'Descriptive same-viewport metrics; foreground union is shared by all three; no fitted registration/scale; not a declaration of full classic acceptance.'},indent=2),encoding='utf8')
print(json.dumps(summary,indent=2))
