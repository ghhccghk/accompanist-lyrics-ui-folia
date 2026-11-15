"""Full-frame complex-script comparison against unchanged mounted Folia Motion.

Reports descriptive error and contact sheets; does not define a passing threshold.
"""
from pathlib import Path
import csv
from PIL import Image, ImageChops, ImageDraw

root = Path(__file__).resolve().parents[1] / 'build/verification'
output = root / 'classic-matrix-comparison'
output.mkdir(parents=True, exist_ok=True)
rows = []
for scene in ('arabic', 'wrap', 'group', 'cluster', 'fast', 'instant', 'cjk', 'whitespace'):
    times = (750, 800, 1000, 1200, 1500, 1900, 2100, 2800)
    for mode in ('default', 'avoid'):
        sheet = None
        for i, ms in enumerate(times):
            name = f'{scene}-{mode}-{ms}ms.png'
            a, b = [Image.open(root / folder / name).convert('RGB') for folder in
                    ('classic-manual-upstream', 'classic-matrix-kotlin')]
            assert a.size == b.size
            width, height = a.size
            if sheet is None:
                sheet = Image.new('RGB', (width * 2, (height + 22) * len(times)), '#eef1f5')
            for col, (label, image) in enumerate((('Original Folia', a), ('Kotlin Compose', b))):
                sheet.paste(image, (col * width, i * (height + 22) + 22))
                ImageDraw.Draw(sheet).text((col * width + 4, i * (height + 22) + 4),
                    f'{label} | {scene} | {mode} | {ms}ms', fill='black')
            bg = (22, 36, 53)
            selected = [sum(abs(p[k] - q[k]) for k in range(3)) / 3
                        for p, q in zip(a.getdata(), b.getdata())
                        if max(abs(p[k] - bg[k]) for k in range(3)) > 8
                        or max(abs(q[k] - bg[k]) for k in range(3)) > 8]
            rows.append((scene, mode, ms, len(selected), sum(selected)/len(selected) if selected else 0))
        sheet.save(output / f'{scene}-{mode}-contact.png')
with (output / 'pixel-errors.csv').open('w', newline='') as f:
    writer = csv.writer(f)
    writer.writerow(('scene', 'mode', 'media_ms', 'union_foreground_pixels', 'mean_abs_rgb_error_0_255'))
    writer.writerows(rows)
print(output)
