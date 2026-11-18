"""Actual mounted neighbour inputs, before any projection/colour/fog operation."""
from pathlib import Path
import hashlib
import json
import numpy as np
from PIL import Image, ImageDraw

root = Path(__file__).resolve().parents[1]
fixture = root / 'verification/diorama-fixtures/mounted-raster-v1/scene.json'
scene = json.loads(fixture.read_text(encoding='utf8'))
source = root / 'build/verification/diorama-mounted-raster-browser'
native = root / 'build/verification/diorama-mounted-raster-kotlin'
out = root / 'verification/diorama-mounted-raster'
out.mkdir(parents=True, exist_ok=True)
rows = []


def bounds(alpha):
    y, x = np.where(alpha > 0)
    return [int(x.min()), int(y.min()), int(x.max()+1), int(y.max()+1)] if x.size else None


for raster in scene['rasters']:
    files = [source/raster['file'], native/raster['file'], native/('body-style-'+raster['file'])]
    hashes = [hashlib.sha256(p.read_bytes()).hexdigest() for p in files]
    assert hashes[0] == raster['sha256'], 'Actual source cache texture changed'
    a, b, trial = [np.asarray(Image.open(p).convert('RGBA'))[:, :, 3].astype(np.int16) for p in files]
    assert a.shape == b.shape == trial.shape == (raster['height'], raster['width'])
    mask = (a > 0) | (b > 0) | (trial > 0)
    difference = abs(a-b)
    local = [float(difference[y:y+64, x:x+64][mask[y:y+64, x:x+64]].mean())
             for y in range(0, a.shape[0], 64) for x in range(0, a.shape[1], 64)
             if mask[y:y+64, x:x+64].any()]
    rows.append(dict(index=raster['index'], text=raster['text'], file=raster['file'],
                     width=raster['width'], height=raster['height'], fontPx=raster['fontPx'],
                     advancePx=raster['advancePx'], source_bounds=bounds(a), native_bounds=bounds(b),
                     source_alpha_sum=int(a.sum()), native_alpha_sum=int(b.sum()),
                     relative_alpha_sum=float(b.sum()/a.sum()-1),
                     source_nonzero=int((a > 0).sum()), native_nonzero=int((b > 0).sum()),
                     foreground_alpha_mae=float(difference[mask].mean()),
                     worst64_alpha_mae=max(local), alpha_peak=int(difference.max()),
                     candidate=dict(settings='Existing Windows classicBodyRasterStyle; host explicit paragraph settings retained',
                                    foreground_alpha_mae=float(abs(a-trial)[mask].mean()),
                                    alpha_peak=int(abs(a-trial).max()), alpha_sum=int(trial.sum()), bounds=bounds(trial)),
                     hashes=dict(zip(['source', 'native', 'body_style_candidate'], hashes))))

for name in ['mounted-before.png', 'mounted-after.png']:
    assert hashlib.sha256((source/name).read_bytes()).hexdigest() == scene['originalPngSha256']
numeric = (native/'rasters.tsv').read_text()
assert len(rows) == 8
report = dict(scope=scene['scope'], sourceRevision=scene['revision'],
              fixtureSha256=hashlib.sha256(fixture.read_bytes()).hexdigest(),
              originalMountedScreenshotUnchanged=True, actualOriginalCallbacks=scene['actualCallbacks'],
              nativeNumericAssertions='8 actual cache font/advance/width/height rows passed; no original pixels drive production',
              nativeMetadata=numeric, mask='All alpha>0 union, including smallest alpha; no translation, thresholding, registration or excluded row',
              mean_foreground_alpha_mae=float(np.mean([r['foreground_alpha_mae'] for r in rows])),
              candidate_mean_foreground_alpha_mae=float(np.mean([r['candidate']['foreground_alpha_mae'] for r in rows])),
              candidate_improved_rows=sum(r['candidate']['foreground_alpha_mae'] < r['foreground_alpha_mae'] for r in rows),
              alpha_sum_relative_range=[min(r['relative_alpha_sum'] for r in rows), max(r['relative_alpha_sum'] for r in rows)],
              max_raw_bounds_difference=max(abs(a-b) for r in rows for a, b in zip(r['source_bounds'], r['native_bounds'])),
              status='Input coverage differs despite identical geometry/baseline. Existing Windows body raster settings improve all8 raw rows, but complete-stage/other-font regression checks are still required before production adoption. This does not prove a specific engine cause or explain all projection/backend residual. No production behaviour changed.',
              rasters=rows)
(out/'comparison.json').write_text(json.dumps(report, indent=2)+'\n', encoding='utf8')
# Unscaled, fixed top-left crop of first and last actual cache entries. Alpha
# shown white on black; signed differences use red(source excess)/blue(native).
sheet = Image.new('RGB', (1000, 2*3*170), (25, 25, 25))
draw = ImageDraw.Draw(sheet)
for row, raster in enumerate([scene['rasters'][0], scene['rasters'][-1]]):
    alphas = [np.asarray(Image.open(p/raster['file']).convert('RGBA'))[:, :, 3].astype(np.int16)
              for p in [source, native]]
    a, b = alphas
    images = [Image.fromarray(v.astype(np.uint8)).convert('RGB') for v in alphas]
    delta = np.zeros((*a.shape, 3), dtype=np.uint8)
    delta[:, :, 0] = np.maximum(a-b, 0).astype(np.uint8)
    delta[:, :, 2] = np.maximum(b-a, 0).astype(np.uint8)
    images.append(Image.fromarray(delta))
    for i, (label, im) in enumerate(zip(['Actual original cache alpha', 'Own full-shaped native alpha', 'Unamplified signed alpha difference'], images)):
        y = (row*3+i)*170
        draw.text((5, y+3), f'{label} | {raster["file"]} | fixed raw crop [0,40,1000,190]', fill='white')
        sheet.paste(im.crop((0, 40, 1000, 190)), (0, y+20))
sheet.save(out/'contact.png')
print(json.dumps({k:v for k,v in report.items() if k not in ['rasters', 'nativeMetadata']}, indent=2))
