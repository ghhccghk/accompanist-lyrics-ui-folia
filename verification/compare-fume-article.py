"""Unregistered original static Fume snapshots vs native article diagnostic; NOT final theme acceptance."""
from pathlib import Path
import csv, hashlib, json
import numpy as np
from PIL import Image, ImageDraw

root = Path(__file__).resolve().parents[1]
base = root / 'build/verification'
source, native = base / 'fume-article-browser', base / 'fume-article-kotlin'
out = root / 'verification/fume-article'
out.mkdir(exist_ok=True)
rows, hashes = [], {}
background = np.array([22, 36, 53])
for path in sorted(source.glob('*.png')):
    other = native / path.name
    a = np.asarray(Image.open(path).convert('RGB')).astype(np.int16)
    b = np.asarray(Image.open(other).convert('RGB')).astype(np.int16)
    assert a.shape == b.shape
    mask = np.any(abs(a-background) > 8, axis=2) | np.any(abs(b-background) > 8, axis=2)
    rows.append(dict(file=path.name, foreground_pixels=int(mask.sum()),
                     foreground_mae=float(abs(a-b)[mask].mean()) if mask.any() else 0))
    for p in (path, other):
        hashes[p.relative_to(root).as_posix()] = hashlib.sha256(p.read_bytes()).hexdigest()
font = root / 'verification/assets/FoliaParityLatin-Bold.ttf'
geometry = []
for path in sorted(native.glob('*.csv')):
    if len(path.stem.split('-')) != 2 or not path.stem[0].isdigit(): continue
    with path.open(encoding='utf8') as handle:
        for row in csv.reader(handle):
            if row[0].startswith('#'): continue
            geometry.append(max(abs(float(row[a])-float(row[b])) for a,b in [(3,4),(5,6),(7,8),(9,10)]))
line_errors = []
for path in sorted(native.glob('*-rows.csv')):
    with path.open(encoding='utf8') as handle:
        for row in csv.reader(handle):
            if row[0].startswith('#'): continue
            line_errors.append((abs(float(row[0])-float(row[1])), abs(float(row[2])-float(row[3]))))
report = dict(reference='cc6226e464050a456a2913e062374f23d3844cc5',
              conditions=json.loads((source/'conditions.json').read_text(encoding='utf8')),
              source='actual buildArticleLayout + createStaticBlockSnapshot; no glow or passed alpha, diagnostic block outlines',
              native='FumeArticle + FumeTextMeasure + FumeStaticText; complete shaped row snapshots; no public theme renderer',
              font_sha256=hashlib.sha256(font.read_bytes()).hexdigest(),
              measured_blocks=len(geometry), max_recorded_geometry_error=max(geometry),
              measured_rows=len(line_errors), max_row_width_error=max(e[0] for e in line_errors),
              max_hero_inset_error=max(e[1] for e in line_errors),
              frames=rows, mean_foreground_mae=sum(r['foreground_mae'] for r in rows)/len(rows), png_sha256=hashes,
              initial_six_mean_foreground_mae=sum(r['foreground_mae'] for r in rows if not r['file'].startswith('4-')) / 6,
              limitations='Latin geometry/rows/baseline grid pass; raster residual remains. Native hhea fallback has not been validated against differing OS/2 typo metrics. No live stamps/glow, animated camera, other scripts or Android runtime claim.')
(out/'comparison.json').write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n',encoding='utf8')
names = ['0-800.png', '2-380.png', '2-800.png', '4-800.png']
sheet = Image.new('RGB',(1000,len(names)*325),(30,30,30))
draw = ImageDraw.Draw(sheet)
for i,name in enumerate(names):
    for j,folder in enumerate((source,native)):
        draw.text((j*500+5,i*325+3),f'{"Original static snapshot" if j==0 else "Native static snapshot"} | {name}',fill='white')
        picture = Image.open(folder/name).convert('RGB');picture.thumbnail((500,300))
        sheet.paste(picture,(j*500+(500-picture.width)//2,i*325+22))
sheet.save(out/'contact.png')
print(json.dumps({k:v for k,v in report.items() if k!='png_sha256'},ensure_ascii=False,indent=2))
