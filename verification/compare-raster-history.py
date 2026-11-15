"""Same original final DOM pose with different real layer histories; no parity threshold."""
from pathlib import Path
from PIL import Image, ImageChops, ImageStat, ImageDraw
import json,csv

root=Path(__file__).resolve().parents[1]/'build/verification'
p=root/'raster-history-upstream'
c=json.loads((p/'conditions.json').read_text(encoding='utf8'))
sampling=json.loads((root/'sampling-upstream/conditions.json').read_text(encoding='utf8'))
assert c['revision']==sampling['revision'] and c['fontSha256']==sampling['fontSha256']
out=root/'raster-history-comparison';out.mkdir(parents=True,exist_ok=True)
rows=[]
for dpr in [1,2]:
    for pose in ['identity','transform']:
        records=[r for r in c['frames'] if r['dpr']==dpr and r['pose']==pose]
        static=next(r for r in records if r['history'] is None)
        samples=[]
        for r in records:
            assert r['metrics']==static['metrics'],f'Final DOM pose differs: {r["file"]}'
            a=Image.open(p/r['file']).getchannel('A')
            assert ImageChops.difference(a,Image.open(p/r['repeat']).getchannel('A')).getbbox() is None, f'Unsettled original: {r["file"]}'
            samples.append((f'Original {r["history"] if r["history"] is not None else "static"}',a))
            for mode in ['cached','direct']:
                b=Image.open(root/'sampling-kotlin'/f'dpr{dpr}-layout-0-{pose}-{mode}.png').getchannel('A')
                mask=ImageChops.lighter(a,b).point(lambda x:255 if x else 0)
                rows.append((dpr,pose,str(r['history']),mode,ImageStat.Stat(ImageChops.difference(a,b),mask).mean[0]))
        for mode in ['cached','direct']:
            samples.append((mode,Image.open(root/'sampling-kotlin'/f'dpr{dpr}-layout-0-{pose}-{mode}.png').getchannel('A')))
        sheet=Image.new('RGB',(300*len(samples),182),'#eef1f5')
        for i,(label,im) in enumerate(samples):
            crop=im.crop((80*dpr,100*dpr,380*dpr,260*dpr)).convert('RGB')
            if dpr!=1:crop=crop.resize((300,160),Image.Resampling.BOX)
            sheet.paste(crop,(i*300,22));ImageDraw.Draw(sheet).text((i*300+4,4),f'{label} | DPR{dpr} | {pose}',fill='black')
        sheet.save(out/f'dpr{dpr}-{pose}.png')
with (out/'pixel-errors.csv').open('w',newline='') as f:
    w=csv.writer(f);w.writerow(['dpr','pose','original_initial_scale','kotlin_mode','foreground_alpha_mae']);w.writerows(rows)
print(out)
