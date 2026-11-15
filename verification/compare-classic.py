"""Contact sheets and descriptive pixel errors, never an automatic parity pass."""
from pathlib import Path
import csv
import argparse
from PIL import Image, ImageChops, ImageDraw

root = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--manual', action='store_true', help='Use mounted Motion manual-clock reference instead of legacy RAF captures')
parser.add_argument('--software', action='store_true', help='Compare software fallback captures to the same upstream')
args = parser.parse_args()
reference = 'classic-manual-upstream' if args.manual else 'classic-upstream'
candidate = 'classic-software-kotlin' if args.software else 'classic-kotlin'
output = root / ('build/verification/classic-software-comparison' if args.software else 'build/verification/classic-manual-comparison' if args.manual else 'build/verification/classic-comparison')
output.mkdir(parents=True, exist_ok=True)
rows = []
for mode in ('default', 'avoid'):
    checkpoints = (750, 1000, 1500, 2100, 2300, 3400, 4400)
    sheet = Image.new('RGB', (800, 322 * len(checkpoints)), '#eef1f5')
    for i, ms in enumerate(checkpoints):
        images = [Image.open(root / f'build/verification/{directory}/{mode}-{ms}ms.png').convert('RGB') for directory in (reference, candidate)]
        for col, (side, image) in enumerate(zip(('Folia browser', 'Kotlin Compose'), images)):
            sheet.paste(image.resize((400, 300)), (col * 400, i * 322 + 22))
            ImageDraw.Draw(sheet).text((col * 400 + 8, i * 322 + 4), f'{side} | {mode} | {ms} ms', fill='black')
        pixels = list(ImageChops.difference(*images).getdata())
        a, b = [list(im.getdata()) for im in images]
        bg = (22, 36, 53)
        selected = [j for j, (p, q) in enumerate(zip(a, b)) if max(abs(p[k]-bg[k]) for k in range(3)) > 8 or max(abs(q[k]-bg[k]) for k in range(3)) > 8]
        error = sum(sum(pixels[j]) for j in selected) / (3 * len(selected)) if selected else 0
        rows.append((mode, ms, len(selected), error))
    sheet.save(output / f'{mode}-contact.png')
with (output / 'pixel-errors.csv').open('w', newline='') as f:
    writer = csv.writer(f)
    writer.writerow(('mode', 'media_ms', 'union_foreground_pixels', 'mean_abs_rgb_error_0_255'))
    writer.writerows(rows)
print(output)
