"""Fixed original layer versus bounded per-fragment aniso4 input control."""
from pathlib import Path
import hashlib
import json
import numpy as np
from PIL import Image, ImageDraw

root = Path(__file__).resolve().parents[1]
native = root / 'build/verification/diorama-mounted-input-kotlin'
source = root / 'build/verification/diorama-mounted-sampling-browser'
out = root / 'verification/diorama-anisotropic-control'
out.mkdir(exist_ok=True)
sha = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
files = {'original': source / 'source-original.png',
         'isotropic-source-diagnostic': source / 'source-trilinear-1.png',
         'production-shared-input': native / 'source-neighbor-7.png',
         'fragment-isotropic': native / 'source-neighbor-7-fragment.png',
         'fragment-aniso4': native / 'source-neighbor-7-fragment-aniso4.png'}
sampler = json.loads((root / 'verification/diorama-fixtures/mounted-sampling-v1/scene.json').read_text(encoding='utf8'))
for label, mode in [('original', 'original'), ('isotropic-source-diagnostic', 'trilinear-1')]:
    assert sha(files[label]) == next(s['sha256'] for s in sampler['samplingCaptures'] if s['mode'] == mode)
assert sha(files['fragment-isotropic']) == '9a91fd420158d7c52f22045b349bd47bea37315a791b6e143bffd191fe9b0e5f'
assert sha(files['production-shared-input']) == '18033bf8cb7618e873ef5f399083b5fbff3ed50f153d25f8a8a96bc54007c674'
before = root / '.work/diorama-mip-before-c0e2804'
hashes = {k: v for k, v in json.loads((before / 'hashes.json').read_text(encoding='utf8')).items()
          if k.split('/', 1)[0] in ('mounted', 'plain')}
assert len(hashes) == 168
for name, value in hashes.items():
    group, file = name.split('/', 1)
    folder = 'diorama-mounted-kotlin' if group == 'mounted' else 'diorama-text-plane-kotlin'
    assert sha(root / 'build/verification' / folder / file) == value, name
arrays = {key: np.array(Image.open(path).convert('RGB'), dtype=np.int32) for key, path in files.items()}
bg = [22, 36, 53]
mask = np.logical_or.reduce([np.any(a != bg, axis=2) for a in arrays.values()])
rows = {}
for key, a in arrays.items():
    d = abs(arrays['original'] - a)
    iso = abs(arrays['isotropic-source-diagnostic'] - a)
    rows[key] = dict(sha256=sha(files[key]), pixels=int(np.any(a != bg, axis=2).sum()),
                     positiveRgbMass=int(np.maximum(a - bg, 0).sum()),
                     originalMae=float(d[mask].mean()), originalPeak=int(d.max()),
                     isotropicMae=float(iso[mask].mean()), isotropicPeak=int(iso.max()),
                     worst64=max(float(d[y:y+64, x:x+64][mask[y:y+64, x:x+64]].mean())
                                 for y in range(0, 600, 64) for x in range(0, 380, 64)
                                 if mask[y:y+64, x:x+64].any()))
report = dict(baseline='4147b4a', sourceRevision=sampler['revision'], browser=sampler['browser'],
              condition='Unchanged real mounted380x600/DPR1/700ms mesh7/cache7; native own core/finite camera/material. Same five-image nonbackground union; no alpha deletion, registration, LOD fitting or time shift.',
              method='Inverse-Jacobian area/longest-derivative ratio capped4, rounded taps along longest derivative; trilinear LOD per fragment; independent linear RGB and alpha; existing source tint/ACES/explicit OETF/fog/blend.',
              driverMathReference=dict(project='google/swiftshader', revision='708e9e77773860bbb2ad9bb2d61b451de4b84e31',
                                       file='src/Pipeline/SamplerCore.cpp',
                                       limitation='Reference math only; not proven to match bundled Chromium driver revision or other GPUs. Independently expressed formulas, no driver implementation copied.'),
              unionPixels=int(mask.sum()), measurements=rows, preservedProductionHashes=len(hashes),
              localChannelGate=2, localChannelGatePassed=rows['fragment-aniso4']['originalPeak'] <= 2,
              interpretation='Local default-aniso4/shared-cache control now agrees within2 channel bytes. This does not validate own glyph input, whole stage, near views, other devices or a production renderer.',
              productionChanged=False, androidRuntime=False, formalVisualAcceptance=False)
(out / 'comparison.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf8')
sheet = Image.new('RGB', (650, 600), '#191919')
draw = ImageDraw.Draw(sheet)
for i, (name, path) in enumerate(files.items()):
    y = i * 120
    draw.text((5, y+3), name, fill='white')
    crop = Image.open(path).convert('RGB').crop((170, 410, 277, 432)).resize((428, 88), Image.Resampling.NEAREST)
    sheet.paste(crop, (5, y+22))
    draw.text((440, y+30), f"pixels={rows[name]['pixels']}\nmass={rows[name]['positiveRgbMass']}\npeak={rows[name]['originalPeak']}", fill='white')
sheet.save(out / 'contact.png')
assert rows['fragment-isotropic']['isotropicPeak'] <= 2
assert rows['fragment-aniso4']['originalPeak'] <= 2, 'Bounded input-control failure, not formal visual gate'
print(json.dumps(report, indent=2))
