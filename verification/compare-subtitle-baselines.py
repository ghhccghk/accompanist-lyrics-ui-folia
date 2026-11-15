"""Compare complete fixed subtitle frames; errors describe residuals, never grant visual acceptance."""
from pathlib import Path
import csv
import hashlib
import json
import argparse
from PIL import Image, ImageChops, ImageDraw

project = Path(__file__).resolve().parents[1]
root = project / 'build/verification'
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--before',default='subtitle-baseline-before/subtitle-cjk-kotlin')
parser.add_argument('--output',default='subtitle-baseline-comparison')
args=parser.parse_args()
out = root / args.output
out.mkdir(parents=True, exist_ok=True)
background = (22, 36, 53)


def error(reference, candidate):
    delta = ImageChops.difference(reference, candidate)
    foreground = ImageChops.lighter(
        ImageChops.difference(reference, Image.new('RGB', reference.size, background)),
        ImageChops.difference(candidate, Image.new('RGB', candidate.size, background)))
    values = [sum(d) / 3 for d, p in zip(delta.getdata(), foreground.getdata()) if max(p) > 8]
    return len(values), sum(values) / len(values) if values else 0


rows = []
hashes = []
for source in sorted((root / 'subtitle-cjk-upstream').glob('*.png')):
    before = root / args.before / source.name
    after = root / 'subtitle-cjk-kotlin' / source.name
    frames = [Image.open(p).convert('RGB') for p in (source, before, after)]
    count, old = error(frames[0], frames[1])
    _, new = error(frames[0], frames[2])
    rows.append((source.name, count, old, new, new - old, before.read_bytes() == after.read_bytes()))
    hashes.append({str(p.relative_to(root)): hashlib.sha256(p.read_bytes()).hexdigest() for p in (source, before, after)})
    if any(source.name.endswith(f'-{ms}ms.png') for ms in (750, 1500, 5500)):
        width = frames[0].width
        sheet = Image.new('RGB', (width * 3, 320), '#eef1f5')
        for i, (label, frame) in enumerate(zip(('Original Folia', 'Before', 'Corrected paint baseline'), frames)):
            ImageDraw.Draw(sheet).text((i*width+4, 4), f'{label} | {source.stem}', fill='black')
            sheet.paste(frame.crop((0, 300, width, 600)), (i*width, 20))
        sheet.save(out / source.name)
with (out / 'pixel-errors.csv').open('w', newline='', encoding='utf8') as f:
    writer = csv.writer(f)
    writer.writerow(('frame', 'before_foreground_pixels', 'before_rgb_mae', 'after_rgb_mae', 'delta_mae', 'identical_png'))
    writer.writerows(rows)
sources = ['library/src/jvmMain/kotlin/top/ghhccghk/accompanist/lyrics/folia/internal/text/FontMetrics.jvm.kt',
           'verification/assets/FoliaParityCJK-Bold.otf', 'verification/classic-fixtures/subtitle-cjk-scenes.tsv']
summary = {'frames': len(rows), 'changed': sum(not r[5] for r in rows),
           'before_mean_mae': sum(r[2] for r in rows)/len(rows),
           'after_mean_mae': sum(r[3] for r in rows)/len(rows),
           'before_max_mae': max(r[2] for r in rows), 'after_max_mae': max(r[3] for r in rows),
           'source_sha256': {p: hashlib.sha256((project/p).read_bytes()).hexdigest() for p in sources},
           'png_sha256': hashes,
           'note': 'Fixed upstream revision and browser/font conditions are recorded with original captures. Full frames retained; contact sheets show bottom 300px without alignment or scaling. No MAE acceptance threshold.'}
(out / 'conditions.json').write_text(json.dumps(summary, indent=2), encoding='utf8')
print(json.dumps({k:v for k,v in summary.items() if k not in ('source_sha256','png_sha256')}, indent=2))
for row in rows:
    if not row[5]:
        print(row[:5])
