"""Isolate actual text-shadow glyph input and resulting halo, without changing goldens.

Run BodyRasterTest first. Comparisons are descriptive, not a final visual pass.
Original: pinned GlowWord, M64, baseline191, viewport320x320, both20/40px shadows.
"""
from pathlib import Path
from PIL import Image, ImageChops, ImageDraw, ImageStat
import csv

root=Path(__file__).resolve().parents[1]
fixtures=root/'verification/classic-fixtures/halo'
output=root/'build/verification/raster-kotlin'
rows=[]
sheet=Image.new('RGB',(320*5,344*2),'#eef1f5')
settings=['identity','identity-no-hint','identity-lcd','identity-shadow']
for index,(kind,original) in enumerate([('source','shadow-source.png'),('halo','halo-1.png')]):
    a=Image.open(fixtures/original).getchannel('A')
    def tile(alpha,column,label):
        # Show unmodified alpha as white ink on black; do not normalize brightness.
        rgb=Image.merge('RGB',(alpha,alpha,alpha))
        sheet.paste(rgb,(column*320,index*344+24))
        ImageDraw.Draw(sheet).text((column*320+4,index*344+5),label,fill='black')
    tile(a,0,f'Original {kind}')
    for column,setting in enumerate(settings,1):
        name=f'halo64-{setting}{"-halo" if kind=="halo" else ""}.png'
        b=Image.open(output/name).getchannel('A').crop((0,0,320,320))
        difference=ImageChops.difference(a,b)
        union=ImageChops.lighter(a,b).point(lambda x:255 if x else 0)
        rows.append((kind,setting,sum(a.getdata())/255,sum(b.getdata())/255,
            ImageStat.Stat(difference,union).mean[0],difference.getextrema()[1],str(b.getbbox())))
        tile(b,column,setting)
with (output/'shadow-source.csv').open('w',newline='') as f:
    writer=csv.writer(f)
    writer.writerow(['kind','setting','original_area_px2','kotlin_area_px2','foreground_alpha_mae','max_alpha_error','kotlin_bounds'])
    writer.writerows(rows)
sheet.save(output/'shadow-source-contact.png')
print(output/'shadow-source.csv')
