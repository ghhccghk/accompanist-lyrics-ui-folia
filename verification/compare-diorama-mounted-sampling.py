"""Source-only sampler ablation; modified clone renders are diagnostics."""
from pathlib import Path
import hashlib
import json
import numpy as np
from PIL import Image, ImageDraw

root = Path(__file__).resolve().parents[1]
folder = root/'build/verification/diorama-mounted-sampling-browser'
fixture = root/'verification/diorama-fixtures/mounted-sampling-v1/scene.json'
scene = json.loads(fixture.read_text())
original = json.loads((root/'verification/diorama-fixtures/mounted-input-layers-v1/scene.json').read_text())
native = root/'build/verification/diorama-mounted-input-kotlin/source-neighbor-7.png'
sha = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
assert scene['state'] == original['state'] and scene['callbackIntervals'] == original['callbackIntervals']
assert scene['motion'] == original['motion'] and scene['rendering'] == original['rendering']
assert scene['fontSha256'] == original['fontSha256'] and scene['browser'] == original['browser']
for name in ['mounted-before.png', 'mounted-after.png', 'restored.png']:
    assert sha(folder/name) == original['originalPngSha256']
files = {s['mode']: folder/s['file'] for s in scene['samplingCaptures']}
for s in scene['samplingCaptures']:
    assert sha(folder/s['file']) == s['sha256']
assert sha(files['original']) == original['layerCaptures'][0]['sha256']
old = json.loads((root/'verification/diorama-mounted-input/comparison.json').read_text())
assert sha(native) == old['neighbor7']['hashes'][str(native.relative_to(root)).replace('\\', '/')]
files['native-default'] = native
images = {k: np.array(Image.open(p).convert('RGB'), dtype=np.int32) for k, p in files.items()}
bg = np.array([22, 36, 53])
mask = np.logical_or.reduce([np.any(a != bg, axis=2) for a in images.values()])
rows = {}
for key, a in images.items():
    ys, xs = np.where(np.any(a != bg, axis=2))
    d = abs(a-images['original'])
    rows[key] = dict(sha256=sha(files[key]), pixels=int(xs.size),
                     bounds=[int(xs.min()), int(ys.min()), int(xs.max()+1), int(ys.max()+1)],
                     positiveRgbMass=int(np.maximum(a-bg, 0).sum()),
                     originalMae=float(d[mask].mean()), nativeMae=float(abs(a-images['native-default'])[mask].mean()),
                     originalPeak=int(d.max()),
                     worst64=max(float(d[y:y+64, x:x+64][mask[y:y+64, x:x+64]].mean())
                                 for y in range(0, 600, 64) for x in range(0, 380, 64) if mask[y:y+64, x:x+64].any()))
out = root/'verification/diorama-mounted-sampling'
out.mkdir(exist_ok=True)
report = dict(baseline='5bd0613', sourceRevision=scene['revision'], originalFrameAndStateUnchanged=True,
              originalCloneUnchanged=True, callbacks=scene['actualCallbacks'],
              scope=scene['samplingScope'], mask='All-four-image nonbackground union, no low-alpha threshold, registration, fitted transform or time shift.',
              unionPixels=int(mask.sum()), measurements=rows,
              interpretation='Source-only removal of mipmaps moves coverage409 to194, near native193; native now close to the labelled no-mip control. Mipmap sampling is a demonstrated contributor in this view, not proof that every backend residual has this cause. Isotropic trilinear has its own512-pixel spread; retain it as a diagnostic, not a replacement oracle.',
              productionChanged=False, deviceValidated=False, visualAcceptance=False)
(out/'comparison.json').write_text(json.dumps(report, indent=2)+'\n')
sheet = Image.new('RGB', (840, 280), '#191919')
draw = ImageDraw.Draw(sheet)
for i, (key, path) in enumerate(files.items()):
    x=(i % 2)*420; y=(i//2)*140
    draw.text((x+5,y+4), key, fill='white')
    crop=Image.open(path).convert('RGB').crop((170,410,276,432)).resize((318,66), Image.Resampling.NEAREST)
    sheet.paste(crop, (x+5,y+24))
    draw.text((x+5,y+98), f"pixels={rows[key]['pixels']}  mass={rows[key]['positiveRgbMass']}", fill='white')
sheet.save(out/'contact.png')
print(json.dumps(report, indent=2))
