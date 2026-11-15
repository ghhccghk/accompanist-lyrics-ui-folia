"""Compare original GlowWord color coverage without aligning, fitting, or rescaling pixels."""
from pathlib import Path
import argparse, csv, hashlib, json, platform
import numpy as np
from PIL import Image, ImageDraw

p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--platforms',nargs='*',default=['jvm','api34'])
args=p.parse_args()
root=Path(__file__).resolve().parents[1]
base=root/'build/verification'
reference=base/'foreground-coverage-colors-browser'
conditions=json.loads((reference/'conditions.json').read_text(encoding='utf8'))
out=base/'foreground-coverage-colors-comparison';out.mkdir(parents=True,exist_ok=True)
rows=[];files={}
def read(path):
    files[str(path.relative_to(root))]=hashlib.sha256(path.read_bytes()).hexdigest()
    image=np.asarray(Image.open(path).convert('RGB'),dtype=np.int16)
    assert image.shape==(320,512,3),path
    return image
def metric(a,b,bg):
    mask=np.any(a!=bg,axis=2)|np.any(b!=bg,axis=2)
    assert mask.any(),'Empty control'
    difference=np.abs(a.astype(float)-b.astype(float))
    return float(difference[mask].mean()),float(difference.max())
for row in conditions['frames']:
    source=read(reference/row['file'])
    ink=np.array(list(bytes.fromhex(row['ink'][1:])))
    bg=np.array(list(bytes.fromhex(row['surface'][1:])))
    white=read(reference/f"{row['font']}-{row['size']}-ffffff-000000.png")
    predicted=np.rint(bg+white.astype(float)/255*(ink-bg)).astype(np.int16)
    mae,maximum=metric(source,predicted,bg)
    rows.append({**row,'platform':'browser-white-mask-prediction','mode':'predicted-tint','mae':mae,'max':maximum})
    for target in args.platforms:
        folder=base/f'foreground-coverage-colors-{target}'
        direct=read(folder/(Path(row['file']).stem+'-direct.png'))
        for mode in ['direct','cached','tinted']:
            actual=read(folder/(Path(row['file']).stem+f'-{mode}.png'))
            mae,maximum=metric(source,actual,bg)
            path_mae,path_max=metric(direct,actual,bg)
            rows.append({**row,'platform':target,'mode':mode,'mae':mae,'max':maximum,
                'against_direct_mae':path_mae,'against_direct_max':path_max})
with (out/'errors.csv').open('w',newline='',encoding='utf8') as f:
    fields=['font','text','asset','size','ink','surface','file','platform','mode','mae','max','against_direct_mae','against_direct_max']
    writer=csv.DictWriter(f,fieldnames=fields);writer.writeheader();writer.writerows(rows)
summary=[]
for target in ['browser-white-mask-prediction']+args.platforms:
    for mode in sorted({r['mode'] for r in rows if r['platform']==target}):
        selected=[r for r in rows if r['platform']==target and r['mode']==mode]
        summary.append({'platform':target,'mode':mode,'frames':len(selected),
            'mean_foreground_rgb_mae':sum(r['mae'] for r in selected)/len(selected),
            'max_foreground_rgb_mae':max(r['mae'] for r in selected),
            'max_channel_error':max(r['max'] for r in selected),
            'max_against_direct':max((r.get('against_direct_max',0) for r in selected))})
# Contact sheet is explicitly auxiliary: 2x nearest-neighbour crops. Metrics use full native frames.
contact=Image.new('RGB',(760,110*(1+3*len(args.platforms))),(35,35,35));draw=ImageDraw.Draw(contact)
for column,font in enumerate(['latin','arabic','cjk']):
    stem=f'{font}-19.74-f2b45d-000000'
    paths=[('original',reference/(stem+'.png'))]+[(target+' '+mode,base/f'foreground-coverage-colors-{target}'/(stem+f'-{mode}.png'))
        for target in args.platforms for mode in ['direct','cached','tinted']]
    for index,(label,path) in enumerate(paths):
        draw.text((column*253,index*110+2),font+' '+label,fill='white')
        crop=Image.open(path).convert('RGB').crop((124,162,244,205)).resize((240,86),Image.Resampling.NEAREST)
        contact.paste(crop,(column*253,index*110+20))
contact.save(out/'contact.png')
(out/'conditions.json').write_text(json.dumps({'reference':conditions,'python':platform.python_version(),'numpy':np.__version__,
    'summary':summary,'sha256':files,'script_sha256':hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
    'limits':'Static color control, no translation/registration, transforms/glow/dynamic color/full classic acceptance remain independent.'},indent=2),encoding='utf8')
print(json.dumps(summary,indent=2))
