"""Actual mounted mesh7 corners versus independent core/Compose homography."""
from pathlib import Path
import hashlib
import json
import math
import numpy as np

root = Path(__file__).resolve().parents[1]
fixture = root/'verification/diorama-fixtures/mounted-geometry-v1'
native = root/'build/verification/diorama-mounted-geometry-kotlin'
scene = json.loads((fixture/'scene.json').read_text())
old = json.loads((root/'verification/diorama-fixtures/mounted-input-layers-v1/scene.json').read_text())
assert scene['originalPngSha256'] == old['originalPngSha256']
assert scene['state'] == old['state'] and scene['callbackIntervals'] == old['callbackIntervals']
assert scene['motion'] == old['motion'] and scene['rendering'] == old['rendering']
assert scene['browser'] == old['browser'] and scene['fontSha256'] == old['fontSha256']
sha = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
for name in ['mounted-before.png', 'mounted-after.png']:
    assert sha(root/'build/verification/diorama-mounted-geometry-browser'/name) == scene['originalPngSha256']
assert scene['teardown'] == dict(roots=0, queuedRafs=0)
a = np.loadtxt(fixture/'vertices.tsv')
b = np.loadtxt(native/'vertices.tsv')
assert np.array_equal(a[:, :2], b[:, :2])
d = abs(a-b)
assert d[:, 2:5].max() < 1e-6 and d[:, 5:7].max() < 1e-5 and d[:, 7].max() < 1e-6
h = np.loadtxt(native/'homography.tsv').reshape(3, 3)
footprints = []
for u, v in [(0, 0), (825, 116), (1650, 232)]:
    xyw = h @ [u, v, 1]
    jacobian = (h[:2, :2]*xyw[2] - np.outer(xyw[:2], h[2, :2])) / xyw[2]**2
    screen_per_texel = np.linalg.svd(jacobian, compute_uv=False)
    footprints.append(dict(texturePoint=[u, v], screenPixelsPerTexel=screen_per_texel.tolist(),
                           textureTexelsPerScreenPixel=(1/screen_per_texel).tolist(),
                           isotropicLod=math.log2(1/screen_per_texel.min())))
out = root/'verification/diorama-mounted-geometry'
out.mkdir(exist_ok=True)
report = dict(baseline='2ccdc1a', sourceRevision=scene['revision'],
              originalFrameAndStateAndHistoryUnchanged=True, originalCallbacks=scene['actualCallbacks'],
              worldCornerPeak=float(d[:, 2:5].max()), projectedPixelPeak=float(d[:, 5:7].max()),
              depthPeak=float(d[:, 7].max()), nativeControl=(native/'control.txt').read_text().strip(),
              geometryQuantization='Original actual Float32 PlaneGeometry buffers versus native Double planes; tolerances fixed before first run: world/depth1e-6, pixel1e-5, homography1e-9.',
              texture=scene['meshGeometry']['texture'], material=scene['meshGeometry']['material'],
              renderer=scene['meshGeometry']['renderer'], footprints=footprints,
              interpretation='No measured corner displacement can explain 193 versus409 covered pixels. Heavy minification is real. This does not identify a sampling/compositing cause or prove all projection paths correct.',
              productionChanged=False, visualAcceptance=False, deviceValidated=False)
(out/'comparison.json').write_text(json.dumps(report, indent=2)+'\n')
print(json.dumps(report, indent=2))
