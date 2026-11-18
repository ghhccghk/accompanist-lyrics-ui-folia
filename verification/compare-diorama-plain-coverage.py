"""Frozen72 views: only the independently shaped neighbour coverage changes."""
from pathlib import Path
import hashlib
import json
import argparse
import shutil
import subprocess
import xml.etree.ElementTree as ET
import numpy as np
from PIL import Image, ImageDraw

root = Path(__file__).resolve().parents[1]
before = root / '.work/diorama-coverage-before-c3e464c'
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--save-before', action='store_true', help='Save the existing c3e464c source/native output before applying the coverage change; never overwrites')
args = parser.parse_args()
if args.save_before:
    production = 'library/src/commonMain/kotlin/top/ghhccghk/accompanist/lyrics/folia/internal/styles/diorama/DioramaPlainText.kt'
    expected = subprocess.check_output(['git', 'show', 'c3e464c:'+production], cwd=root)
    assert (root/production).read_text(encoding='utf8').replace('\r\n', '\n') == expected.decode().replace('\r\n', '\n'), 'Save only the c3e464c production baseline, before changing it'
    if before.exists():
        raise SystemExit('Preserved baseline already exists; refusing overwrite')
    groups = {'mounted-source': ('build/verification/diorama-mounted-browser', 48),
              'mounted-native': ('build/verification/diorama-mounted-kotlin', 144),
              'plain-source': ('build/verification/diorama-text-plane-browser', 24),
              'plain-native': ('build/verification/diorama-text-plane-kotlin', 24),
              'raw-native': ('build/verification/diorama-mounted-raster-kotlin', 16)}
    files = {name: sorted((root/directory).glob('*.png')) for name, (directory, _) in groups.items()}
    for name, (_, count) in groups.items():
        assert len(files[name]) == count, f'Incomplete baseline group {name}'
    saved = {}
    for name, paths in files.items():
        target = before/name
        target.mkdir(parents=True)
        for path in paths:
            shutil.copy2(path, target/path.name)
            saved[f'{name}/{path.name}'] = hashlib.sha256(path.read_bytes()).hexdigest()
    (before/'hashes.json').write_text(json.dumps(saved, indent=2)+'\n', encoding='utf8')
    print(f'Preserved {len(saved)} images; apply the production change and run without --save-before')
    raise SystemExit(0)
out = root / 'verification/diorama-plain-coverage'
out.mkdir(parents=True, exist_ok=True)
hashes = json.loads((before/'hashes.json').read_text())
sha = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
assert all(sha(before/p) == value for p, value in hashes.items())
bg = np.array([22, 36, 53])
modes = ['finite', 'material', 'media']
mounted = json.loads((root/'verification/diorama-fixtures/mounted-cut-v1/scene.json').read_text())
plain = json.loads((root/'verification/diorama-fixtures/text-planes.json').read_text(encoding='utf8'))


def compare(files, labels):
    images = [np.asarray(Image.open(p).convert('RGB')).astype(np.int16) for p in files]
    assert all(im.shape == images[0].shape for im in images)
    mask = np.zeros(images[0].shape[:2], dtype=bool)
    for image in images:
        mask |= np.any(image != bg, axis=2)
    result = dict(hashes={str(p.relative_to(root)).replace('\\', '/'): sha(p) for p in files}, bounds={})
    for label, image in zip(['source']+labels, images):
        y, x = np.where(np.any(image != bg, axis=2))
        result['bounds'][label] = [int(x.min()), int(y.min()), int(x.max()+1), int(y.max()+1)] if x.size else None
    for label, image in zip(labels, images[1:]):
        delta = abs(images[0]-image)
        local = [float(delta[y:y+64, x:x+64][mask[y:y+64, x:x+64]].mean())
                 for y in range(0, image.shape[0], 64) for x in range(0, image.shape[1], 64)
                 if mask[y:y+64, x:x+64].any()]
        result[label] = dict(foreground_mae=float(delta[mask].mean()) if mask.any() else 0.0,
                             worst64=max(local, default=0.0), channel_peak=int(delta.max()))
    return result


mounted_rows = []
for capture in mounted['captures']:
    name = capture['file']
    source = root/'build/verification/diorama-mounted-browser'/name
    assert sha(source) == hashes['mounted-source/'+name]
    files = [source]
    labels = []
    for mode in modes:
        labels += [mode+'-before', mode+'-after']
        files += [before/'mounted-native'/(mode+'-'+name), root/'build/verification/diorama-mounted-kotlin'/(mode+'-'+name)]
    mounted_rows.append(dict(file=name, **compare(files, labels)))

plain_rows = []
for capture in plain['observations']:
    name = capture['file']
    source = root/'build/verification/diorama-text-plane-browser'/name
    assert sha(source) == hashes['plain-source/'+name]
    files = [source, before/'plain-native'/name, root/'build/verification/diorama-text-plane-kotlin'/name]
    plain_rows.append(dict(file=name, scene=capture['scene'], **compare(files, ['before', 'after'])))


def summary(rows, before_label, after_label):
    return {label: dict(mean_foreground_mae=float(np.mean([r[label]['foreground_mae'] for r in rows])),
                        worst_image=max(r[label]['foreground_mae'] for r in rows),
                        worst64=max(r[label]['worst64'] for r in rows)) for label in [before_label, after_label]} | dict(
        improved=sum(r[after_label]['foreground_mae'] < r[before_label]['foreground_mae'] for r in rows),
        unchanged=sum(r[after_label]['foreground_mae'] == r[before_label]['foreground_mae'] for r in rows),
        regressions=[dict(file=r['file'], before=r[before_label], after=r[after_label]) for r in rows
                     if r[after_label]['foreground_mae'] > r[before_label]['foreground_mae']],
        local_regressions=[dict(file=r['file'], before=r[before_label]['worst64'], after=r[after_label]['worst64']) for r in rows
                           if r[after_label]['worst64'] > r[before_label]['worst64']])


raw_scene = json.loads((root/'verification/diorama-fixtures/mounted-raster-v1/scene.json').read_text())
raw_rows = []
for raster in raw_scene['rasters']:
    name = raster['file']
    paths = [root/'build/verification/diorama-mounted-raster-browser'/name, before/'raw-native'/name,
             root/'build/verification/diorama-mounted-raster-kotlin'/name]
    assert sha(paths[0]) == raster['sha256']
    a, b, c = [np.asarray(Image.open(p).convert('RGBA'))[:, :, 3].astype(np.int16) for p in paths]
    mask = (a > 0) | (b > 0) | (c > 0)
    raw_rows.append(dict(file=name, before_alpha_mae=float(abs(a-b)[mask].mean()), after_alpha_mae=float(abs(a-c)[mask].mean()),
                         before_peak=int(abs(a-b).max()), after_peak=int(abs(a-c).max()),
                         hashes=dict(zip(['source', 'before', 'after'], map(sha, paths)))))

tests = [ET.parse(p).getroot() for p in (root/'library/build/test-results/jvmTest').glob('TEST-*.xml')]
assert tests and all(int(t.get('failures')) == 0 and int(t.get('errors')) == 0 for t in tests)
report = dict(baseline='c3e464c', change='DioramaPlainText uses existing host-preserving Windows foreground raster policy. Other OS/Android, active glyph/halo, clock/camera/material logic unchanged.',
              sourceFramesUnchanged=72,
              mask='Mounted: same seven-image source/3before/3after ALL-nonbackground union per frame. Plain: same three-image union. Raw: same three-alpha union. Lowest alpha included. No registration, fitting, frame shift, filter or omitted regression.',
              mounted={m: summary(mounted_rows, m+'-before', m+'-after') for m in modes},
              plain=summary(plain_rows, 'before', 'after'),
              plainByScene={scene: summary([r for r in plain_rows if r['scene'] == scene], 'before', 'after') for scene in ['latin', 'arabic', 'cjk', 'long']},
              raw=raw_rows, mountedFrames=mounted_rows, plainFrames=plain_rows,
              numeric={p.name: p.read_text() for p in (root/'build/verification/diorama-mounted-kotlin').glob('*.txt') if p.name.startswith(('numeric-', 'material-'))},
              validation=dict(jvmTests=sum(int(t.get('tests')) for t in tests), suites=[dict(name=t.get('name'), tests=int(t.get('tests'))) for t in tests]),
              status='Measured Windows offscreen neighbour input improvement; complete original visual acceptance, other desktop OS, Android runtime and performance remain unverified. All observed local/mean regressions retained.')
(out/'comparison.json').write_text(json.dumps(report, indent=2)+'\n', encoding='utf8')

names = ['mounted-380-60-700.png', 'mounted-800-60-700.png', 'mounted-380-144-3199.png', 'mounted-800-144-1600.png']
sheet = Image.new('RGB', (1280, len(names)*280+3*280), (25, 25, 25))
draw = ImageDraw.Draw(sheet)
for row, name in enumerate(names):
    files = [root/'build/verification/diorama-mounted-browser'/name, before/'mounted-native'/('material-'+name),
             root/'build/verification/diorama-mounted-kotlin'/('material-'+name), root/'build/verification/diorama-mounted-kotlin'/('media-'+name)]
    for column, (label, p) in enumerate(zip(['Original mounted', 'Finite material BEFORE', 'Finite material AFTER', 'Media AFTER (residual remains)'], files)):
        draw.text((column*320+4, row*280+3), label, fill='white')
        draw.text((column*320+4, row*280+16), name, fill='white')
        im = Image.open(p).convert('RGB'); im.thumbnail((320, 240))
        sheet.paste(im, (column*320, row*280+35))
for i, scene in enumerate(['latin', 'arabic', 'cjk']):
    name = f'{scene}-800-1.7-0.8.png'; row = i+len(names)
    for column, (label, p) in enumerate(zip(['Original plain', 'Own plain BEFORE', 'Own plain AFTER'],
                                          [root/'build/verification/diorama-text-plane-browser'/name, before/'plain-native'/name,
                                           root/'build/verification/diorama-text-plane-kotlin'/name])):
        draw.text((column*320+4, row*280+3), f'{label} | {scene}', fill='white')
        im = Image.open(p).convert('RGB'); im.thumbnail((320, 240)); sheet.paste(im, (column*320, row*280+30))
sheet.save(out/'contact.png')
print(json.dumps({k: v for k, v in report.items() if k in ['mounted', 'plain', 'plainByScene', 'validation']}, indent=2))
