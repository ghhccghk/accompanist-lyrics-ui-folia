"""Isolate layout rounding and compositor sampling; no full-frame acceptance threshold."""
from pathlib import Path
from PIL import Image, ImageChops, ImageDraw, ImageStat
import csv

root = Path(__file__).resolve().parents[1] / 'build/verification'
out = root / 'sampling-comparison'; out.mkdir(parents=True, exist_ok=True)
rows = []
phase_rows = []
for dpr in [1, 2]:
    for kind in ['layout', 'motion']:
        for pose in ['identity', 'transform']:
            modes = ['cached', 'direct', 'cached-snapped', 'direct-snapped']
            sheet = Image.new('RGB', (300 * (len(modes) + 1), 182 * 4), '#eef1f5')
            base = Image.open(root / 'sampling-upstream' / f'dpr{dpr}-{kind}-0-{pose}.png').getchannel('A')
            for i, phase in enumerate(['0', '0.25', '0.5', '0.75']):
                prefix = f'dpr{dpr}-{kind}-{phase}-{pose}'
                a = Image.open(root / 'sampling-upstream' / f'{prefix}.png').getchannel('A')
                phase_rows.append((dpr, kind, pose, phase, a.getbbox(), ImageChops.difference(a, base).getbbox() is None))
                # Measured original invariant: layout snaps in CSS px even at DPR2;
                # fractional Motion remains observable. Never round the motion input.
                if kind == 'layout':
                    expected = base if phase in ['0', '0.25'] else ImageChops.offset(base, dpr, dpr)
                    assert ImageChops.difference(a, expected).getbbox() is None, f'Original layout phase changed: {prefix}'
                elif phase != '0':
                    assert ImageChops.difference(a, base).getbbox() is not None, f'Original fractional Motion lost: {prefix}'
                images = [('Original', a)]
                for mode in modes:
                    b = Image.open(root / 'sampling-kotlin' / f'{prefix}-{mode}.png').getchannel('A')
                    assert a.size == b.size == (600 * dpr, 400 * dpr)
                    mask = ImageChops.lighter(a, b).point(lambda x: 255 if x else 0)
                    diff = ImageChops.difference(a, b)
                    rows.append((dpr, kind, phase, pose, mode, sum(a.getdata()) / (255 * dpr * dpr),
                                 sum(b.getdata()) / (255 * dpr * dpr), ImageStat.Stat(diff, mask).mean[0], diff.getextrema()[1]))
                    images.append((mode, b))
                for col, (label, image) in enumerate(images):
                    crop = image.crop((80*dpr, 100*dpr, 380*dpr, 260*dpr)).convert('RGB')
                    # DPR2 shown at CSS size for overview; original PNG remains unscaled.
                    if dpr != 1: crop = crop.resize((300,160), Image.Resampling.BOX)
                    sheet.paste(crop, (col * 300, i * 182 + 22))
                    ImageDraw.Draw(sheet).text((col * 300 + 4, i * 182 + 4), f'{label} | {kind} | {pose} | {phase}', fill='black')
            sheet.save(out / f'dpr{dpr}-{kind}-{pose}-contact.png')
with (out / 'pixel-errors.csv').open('w', newline='') as f:
    writer = csv.writer(f)
    writer.writerow(['dpr', 'kind', 'phase', 'pose', 'mode', 'original_alpha_area_css_px2', 'kotlin_alpha_area_css_px2', 'foreground_alpha_mae_0_255', 'max_alpha_delta'])
    writer.writerows(rows)
with (out / 'original-phase.csv').open('w', newline='') as f:
    writer = csv.writer(f); writer.writerow(['dpr','kind','pose','phase','alpha_bbox_physical_px','identical_to_phase0']); writer.writerows(phase_rows)
print(out)
