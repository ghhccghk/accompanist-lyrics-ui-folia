"""Describe original shadow-alpha controls and public Classic frames; never a full parity pass."""
from pathlib import Path
import csv
import hashlib
import json
import platform
import numpy as np
from PIL import Image, ImageDraw

root = Path(__file__).resolve().parents[1]
base = root / 'build/verification'
reference = root / 'verification/classic-fixtures/halo-translucent'
control = base / 'halo-translucent-kotlin'
out = base / 'halo-translucent-comparison'
out.mkdir(exist_ok=True)
hashes = {}


def read(path):
    hashes[path.relative_to(root).as_posix()] = hashlib.sha256(path.read_bytes()).hexdigest()
    return np.asarray(Image.open(path).convert('RGBA')).astype(np.float64)


rows = []
with (control / 'comparison.csv').open() as stream:
    for record in csv.DictReader(stream):
        reveal, alpha = record['reveal'], int(record['ink_alpha'])
        opacity = float(record['span_opacity'])
        name = f'{reveal}-a{alpha}-o{opacity:g}.png'
        images = [read(path) for path in [reference / name, control / ('before-' + name), control / ('after-' + name)]]
        mask = np.logical_or.reduce([image[:, :, 3] > 0 for image in images])
        # Premultiplied RGB avoids meaningless huge unpremultiplied errors at near-zero alpha.
        premultiplied = [image[:, :, :3] * image[:, :, 3:4] / 255 for image in images]
        row = {'reveal': reveal, 'alpha': alpha, 'span_opacity': opacity, 'foreground_pixels': int(mask.sum())}
        for index, label in [(1, 'before'), (2, 'after')]:
            alpha_error = np.abs(images[0][:, :, 3] - images[index][:, :, 3])
            row[label + '_alpha_mae'] = float(alpha_error[mask].mean()) if mask.any() else 0
            row[label + '_alpha_max'] = int(alpha_error.max())
            row[label + '_premultiplied_rgb_mae'] = float(np.abs(premultiplied[0] - premultiplied[index])[mask].mean()) if mask.any() else 0
            assert abs(row[label + '_alpha_mae'] - float(record[label + '_alpha_mae'])) < 1e-12
        rows.append(row)

with (out / 'controls.csv').open('w', newline='') as stream:
    writer = csv.DictWriter(stream, fieldnames=rows[0].keys())
    writer.writeheader()
    writer.writerows(rows)

partial = [row for row in rows if row['alpha'] in [64, 128, 192]]
summary = {'controls': len(rows), 'translucent_controls': len(partial),
           'before_alpha_mean': sum(row['before_alpha_mae'] for row in partial) / len(partial),
           'after_alpha_mean': sum(row['after_alpha_mae'] for row in partial) / len(partial),
           'before_alpha_max': max(row['before_alpha_max'] for row in partial),
           'after_alpha_max': max(row['after_alpha_max'] for row in partial),
           'improved': sum(row['after_alpha_mae'] < row['before_alpha_mae'] for row in partial)}

rejected_name = 'normal-a64-o0.1.png'
rejected_images = [read(path) for path in [reference / rejected_name,
    control / ('before-' + rejected_name), control / ('rejected-direct-opacity-' + rejected_name)]]
rejected_mask = np.logical_or.reduce([image[:, :, 3] > 0 for image in rejected_images])
rejected = {label: float(np.abs(rejected_images[0][:, :, 3] - rejected_images[index][:, :, 3])[rejected_mask].mean())
            for index, label in [(1, 'old_alpha_mae'), (2, 'rejected_alpha_mae')]}
assert rejected['rejected_alpha_mae'] > rejected['old_alpha_mae'], 'Rejected paint-opacity path should reproduce its failure'

full_reference = base / 'classic-translucent-browser'
full_conditions = json.loads((full_reference / 'conditions.json').read_text())
full = []
background = np.array([22, 36, 53])
for frame in full_conditions['frames']:
    source = read(full_reference / frame['file'])[:, :, :3]
    a, b = [read(base / 'classic-translucent-live' / f'software-{software}-alpha-{frame["alpha"]}.png')[:, :, :3] for software in ['false', 'true']]
    mask = np.logical_or.reduce([np.any(np.abs(image - background) > 8, axis=2) for image in [source, a, b]])
    full.append({'alpha': frame['alpha'], 'native_layer_mae': float(np.abs(source - a)[mask].mean()),
                 'software_layer_mae': float(np.abs(source - b)[mask].mean())})

# Preserve the entire previous opaque matrix, not merely a chosen screenshot.
opaque = {}
for folder in ['classic-body-default-comparison', 'classic-colors-comparison']:
    previous = json.loads((base / folder / 'conditions.json').read_text())
    count = 0
    for name, expected in previous['png_sha256'].items():
        portable = name.replace('\\', '/')
        path = root / portable
        if 'classic-kotlin/' in portable or 'classic-colors-native/' in portable or 'classic-colors-tinted/' in portable:
            actual = hashlib.sha256(path.read_bytes()).hexdigest()
            assert actual == expected, f'Opaque regression: {name}'
            hashes[name] = actual
            count += 1
    assert count == (42 if folder == 'classic-body-default-comparison' else 108), 'Incomplete opaque controls'
    opaque[folder] = count

sheet = Image.new('RGB', (960, 688), (22, 36, 53))
draw = ImageDraw.Draw(sheet)
for row, name in enumerate(['normal-a128-o1.png', 'fast-a128-o1.png']):
    for column, (label, path) in enumerate([('Original', reference / name), ('Before', control / ('before-' + name)), ('After', control / ('after-' + name))]):
        draw.text((column * 320 + 5, row * 344 + 4), label + ' | ' + name, fill='white')
        image = Image.open(path).convert('RGBA')
        background_image = Image.new('RGBA', image.size, (22, 36, 53, 255))
        sheet.paste(Image.alpha_composite(background_image, image).convert('RGB'), (column * 320, row * 344 + 24))
sheet.save(out / 'contact.png')

sources = [
    'library/src/commonMain/kotlin/top/ghhccghk/accompanist/lyrics/folia/internal/rendering/GlowMask.kt',
    'library/src/commonMain/kotlin/top/ghhccghk/accompanist/lyrics/folia/internal/rendering/GlowMaskPaint.kt',
    'library/src/commonMain/kotlin/top/ghhccghk/accompanist/lyrics/folia/internal/styles/classic/ClassicLine.kt',
    'library/src/jvmTest/kotlin/top/ghhccghk/accompanist/lyrics/folia/TranslucentHaloPaintTest.kt',
    'library/src/jvmTest/kotlin/top/ghhccghk/accompanist/lyrics/folia/TranslucentClassicTest.kt',
    'verification/classic-browser/halo.tsx', 'verification/classic-browser/capture-translucent-halo.mjs',
    'verification/classic-browser/main.tsx', 'verification/classic-browser/capture-translucent-classic.mjs',
    'verification/compare-translucent-halo.py']
report = {'summary': summary, 'full_scene': full, 'opaque_pngs_unchanged': opaque,
          'reproduced_rejected_direct_opacity': rejected,
          'control_conditions': json.loads((reference / 'conditions.json').read_text()),
          'full_conditions': full_conditions, 'png_sha256': hashes,
          'source_sha256': {name: hashlib.sha256((root / name).read_bytes()).hexdigest() for name in sources},
          'host': platform.platform(),
          'limits': 'Same-source isolated glyph alpha controls plus a paused full-scene comparison; JVM offscreen/native-layer and software-layer modes. No Android device, OS-window capture, new performance evidence, or full classic acceptance.'}
(out / 'conditions.json').write_text(json.dumps(report, indent=2), encoding='utf8')
print(json.dumps({'summary': summary, 'full_scene': full, 'opaque_pngs_unchanged': opaque}, indent=2))
