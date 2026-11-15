"""Compare every available before/after frame; retain regressions, not a pass gate.

Run after the JVM capture tasks have completed. No alignment, resampling or
pixel tolerance is applied to images. MAE uses the union foreground (>8 RGB).
"""
import argparse
import csv
import hashlib
import json
import subprocess
from collections import defaultdict
from pathlib import Path
from statistics import mean

from PIL import Image, ImageChops, ImageDraw, ImageStat

root = Path(__file__).resolve().parents[1] / 'build/verification'
sets = {
    'classic-intensity-kotlin': 'classic-intensity-upstream',
    'classic-kotlin': 'classic-manual-upstream',
    'classic-continuous-kotlin': 'classic-continuous-upstream',
    'classic-matrix-kotlin': 'classic-manual-upstream',
    'line-centers-kotlin': 'line-centers-upstream',
    'short-stage-kotlin': 'short-stage-upstream',
    'layout-phase-public-kotlin': 'layout-phase-public-upstream',
}
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--before-directory',default='line-center-before')
parser.add_argument('--before-revision',default='25d241b')
parser.add_argument('--output',default='line-center-comparison')
parser.add_argument('--no-intermediates',action='store_true')
parser.add_argument('--datasets',nargs='+',choices=list(sets),default=list(sets)[:5])
args=parser.parse_args()
out = root / args.output
out.mkdir(parents=True, exist_ok=True)
sets={k:sets[k] for k in args.datasets}
versions = {
    'before': root / args.before_directory,
    'fractional-only': root / 'line-center-fractional-only',
    'cumulative-only': root / 'line-center-cumulative-only',
    'candidate': root,
}
if args.no_intermediates:
    versions={k:v for k,v in versions.items() if k in ('before','candidate')}


def error(a, b):
    assert a.size == b.size
    mask = Image.new('L', a.size)
    for im in (a, b):
        for channel in ImageChops.difference(im, Image.new('RGB', im.size, (22, 36, 53))).split():
            mask = ImageChops.lighter(mask, channel)
    mask = mask.point(lambda x: 255 if x > 8 else 0)
    return mean(ImageStat.Stat(ImageChops.difference(a, b), mask).mean) if mask.getbbox() else 0


rows = []
for dataset, reference in sets.items():
    for path in sorted((root / dataset).glob('*.png')):
        ref = root / reference / path.name
        assert ref.exists(), f'Missing original: {ref}'
        with Image.open(ref) as raw:
            a = raw.convert('RGB')
        for version, folder in versions.items():
            candidate = folder / dataset / path.name
            if not candidate.exists():
                continue
            with Image.open(candidate) as raw:
                b = raw.convert('RGB')
            rows.append((dataset, path.name, version, error(a, b)))

with (out / 'pixel-errors.csv').open('w', newline='', encoding='utf8') as f:
    writer = csv.writer(f)
    writer.writerow(('dataset', 'frame', 'version', 'union_foreground_rgb_mae'))
    writer.writerows(rows)
groups = defaultdict(list)
lookup = {}
for dataset, name, version, value in rows:
    group = name.split('-')[0] if dataset == 'classic-intensity-kotlin' else 'all'
    groups[(dataset, group, version)].append(value)
    lookup[(dataset, name, version)] = value
summary = [dict(dataset=k[0], group=k[1], version=k[2], count=len(v), mean=mean(v), maximum=max(v))
           for k, v in sorted(groups.items())]
(out / 'summary.json').write_text(json.dumps(summary, indent=2), encoding='utf8')
(out / 'conditions.json').write_text(json.dumps({
    'upstreamRevision': 'cc6226e464050a456a2913e062374f23d3844cc5',
    'baselineRevision': args.before_revision,
    'candidateHead': subprocess.run(['git', 'rev-parse', 'HEAD'], cwd=root.parents[1],
                                    capture_output=True, text=True, check=True).stdout.strip(),
    'candidateSourceSha256': {p.relative_to(root.parents[1]).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest()
                             for source in ('commonMain','jvmMain')
                             for p in sorted((root.parents[1] / f'library/src/{source}').rglob('*.kt'))},
    'note': 'Candidate can be uncommitted. Before/intermediate evidence directories must be preserved. '
            'Pixel errors are descriptive, not a visual acceptance threshold.',
}, indent=2), encoding='utf8')
changes = []
for dataset, name, version, value in rows:
    if version == 'candidate' and (dataset, name, 'before') in lookup:
        before = lookup[(dataset, name, 'before')]
        changes.append((dataset, name, before, value, value-before))
changes.sort(key=lambda row: row[-1], reverse=True)
with (out / 'changes.csv').open('w', newline='', encoding='utf8') as f:
    writer = csv.writer(f)
    writer.writerow(('dataset', 'frame', 'before_mae', 'candidate_mae', 'increase'))
    writer.writerows(changes)
with (out / 'png-identity.csv').open('w', newline='', encoding='utf8') as f:
    writer=csv.writer(f)
    writer.writerow(('dataset','frame','before_png_sha256','candidate_png_sha256','encoded_png_identical'))
    for dataset,name,*_ in changes:
        before=hashlib.sha256((versions['before']/dataset/name).read_bytes()).hexdigest()
        after=hashlib.sha256((root/dataset/name).read_bytes()).hexdigest()
        writer.writerow((dataset,name,before,after,before==after))
# Full resolution contact sheets for strongest regression/improvement, plus the
# specific entry/filter-transition frames that motivated the investigation.
selected = changes[:4] + changes[-4:]
for dataset, name in [('classic-continuous-kotlin', f'avoid-{ms}ms.png') for ms in (960, 3440, 3480)]:
    selected += [r for r in changes if r[:2] == (dataset, name)]
selected += [r for r in changes if r[1] in ('fast-avoid-850ms.png', 'calm-wrap-avoid-2800ms.png')]
for dataset, name, before, after, delta in dict.fromkeys(selected):
    images = [Image.open(folder / filename).convert('RGB') for folder, filename in (
        (root / sets[dataset], name), (versions['before'] / dataset, name), (root / dataset, name))]
    w, h = images[0].size
    sheet = Image.new('RGB', (w*3, h+28), '#eef1f5')
    for col, (label, im) in enumerate(zip(('Original', f'Before MAE {before:.4f}', f'Candidate MAE {after:.4f}'), images)):
        sheet.paste(im, (col*w, 28))
        ImageDraw.Draw(sheet).text((col*w+4, 6), label, fill='black')
    sheet.save(out / f'{dataset}-{name}')
for row in summary:
    print(row)
print(out)
