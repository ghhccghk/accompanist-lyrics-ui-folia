"""Labelled real-cache input control; never a production texture import."""
from pathlib import Path
import hashlib
import json
import numpy as np
from PIL import Image, ImageDraw

root = Path(__file__).resolve().parents[1]
scene = json.loads((root/'verification/diorama-fixtures/mounted-input-layers-v1/scene.json').read_text(encoding='utf8'))
native = root/'build/verification/diorama-mounted-input-kotlin'
source = root/'build/verification/diorama-mounted-input-browser'
out = root/'verification/diorama-mounted-input'
out.mkdir(parents=True, exist_ok=True)
sha = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
image = lambda p: np.asarray(Image.open(p).convert('RGB')).astype(np.int16)
bg = np.array([22, 36, 53])
assert sha(source/'mounted-before.png') == sha(source/'mounted-after.png') == sha(source/'restored.png') == scene['originalPngSha256']
assert sha(source/'source-neighbor-7.png') == scene['layerCaptures'][0]['sha256']
source_cache = root/'verification/diorama-fixtures/mounted-raster-v1/cache-7.png'
assert sha(source_cache) == next(r['sha256'] for r in scene['rasters'] if r['index'] == 7)
assert np.array_equal(image(native/'production.png'), image(native/'own-replay.png'))
frozen = root/'build/verification/diorama-mounted-kotlin/material-mounted-380-60-700.png'
assert np.array_equal(image(frozen), image(native/'production.png'))


def measure(files):
    a, b, c = [image(p) for p in files]
    mask = np.any(a != bg, axis=2) | np.any(b != bg, axis=2) | np.any(c != bg, axis=2)
    rows = {}
    for label, im in zip(['source', 'own-input', 'shared-cache7-input'], [a, b, c]):
        y, x = np.where(np.any(im != bg, axis=2))
        rows[label] = dict(nonbackground=int(x.size), bounds=[int(x.min()), int(y.min()), int(x.max()+1), int(y.max()+1)] if x.size else None,
                           positive_rgb_mass=int(np.maximum(im-bg, 0).sum()))
        if label != 'source':
            d = abs(a-im)
            local = [float(d[y:y+64, x:x+64][mask[y:y+64, x:x+64]].mean())
                     for y in range(0, a.shape[0], 64) for x in range(0, a.shape[1], 64)
                     if mask[y:y+64, x:x+64].any()]
            rows[label] |= dict(foreground_mae=float(d[mask].mean()), worst64=max(local), channel_peak=int(d.max()))
    return dict(union_pixels=int(mask.sum()), measurements=rows, hashes={str(p.relative_to(root)).replace('\\', '/'): sha(p) for p in files})


stage = measure([source/'mounted-before.png', native/'production.png', native/'source-cache7.png'])
layer = measure([source/'source-neighbor-7.png', native/'neighbor-7.png', native/'source-neighbor-7.png'])
numeric = (native/'control.txt').read_text()
report = dict(baseline='46f89e7', sourceRevision=scene['revision'], ownReplayMatchesProduction=True,
              originalFullFrameRestoredUnchanged=True, actualSourceCacheSha256=sha(source_cache),
              originalCallbacks=scene['actualCallbacks'], conditions='380x600,DPR1,60Hz original full outgoing0..6.8/incoming0...7, same mounted camera/fog/ACES/font/geometry/material state',
              sourceLayer=scene['layerScope'],
              control='Only the TEST cache7 image changes. Every camera, core geometry, width and material state is native independently reconstructed. The original clone layer is an isolated diagnostic, not an original whole-scene acceptance claim.',
              mask='All three-image nonbackground union, including lowest RGB/alpha contribution; no registration, pixel shift, threshold, filter, time shift or excluded pixel',
              stage=stage, neighbor7=layer, separateNativeLayerCounts=numeric,
              interpretation='The actual original cache7 texture does not remove the mounted/native isolated-plane residual. Texture coverage alone is insufficient. Isolated source plane covers409 pixels vs193 native with either texture; investigate minification/compositing rather than fitting font alpha or camera/time. Exact graphics-backend cause not yet established.',
              nativePublicOrRuntimeChange=False, deviceValidated=False, visualAcceptance=False)
(out/'comparison.json').write_text(json.dumps(report, indent=2)+'\n', encoding='utf8')
sheet = Image.new('RGB', (1140, 740), (25, 25, 25))
draw = ImageDraw.Draw(sheet)
files = [source/'source-neighbor-7.png', native/'neighbor-7.png', native/'source-neighbor-7.png']
for i, (label, p) in enumerate(zip(['Actual source mesh clone', 'Own glyph input', 'Same source cache7 input'], files)):
    draw.text((i*380+5, 4), label, fill='white')
    sheet.paste(Image.open(p).convert('RGB'), (i*380, 25))
    draw.text((i*380+5, 630), 'Same crop [170,410,275,430], nearest 3x', fill='white')
    im = Image.open(p).convert('RGB').crop((170, 410, 275, 430)).resize((315, 60), Image.Resampling.NEAREST)
    sheet.paste(im, (i*380, 655))
sheet.save(out/'contact.png')
print(json.dumps(dict(stage=stage['measurements'], neighbor7=layer['measurements']), indent=2))
