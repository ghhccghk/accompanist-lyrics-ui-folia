"""Report explicitly lossy render-resolution variants; no acceptance threshold."""
import csv
import hashlib
import json
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[3]
SOURCE = ROOT / "build/verification/render-resolution"
OUTPUT = SOURCE
BG = (22, 36, 53, 255)


def composite(path):
    source = Image.open(path).convert("RGBA")
    return Image.alpha_composite(Image.new("RGBA", source.size, BG), source).convert("RGB")


files = sorted(SOURCE.glob("Classic-*.png")) + sorted(SOURCE.glob("Partita-*.png"))
assert len(files) == 30, len(files)
digests = {p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in files}
rows = []
for theme in ("Classic", "Partita"):
    for time in (8000, 8150, 8600, 10400, 13000):
        full = np.asarray(composite(SOURCE / f"{theme}-Full-{time}.png"))[:, :380, :].astype(np.int16)
        for mode in ("ThreeQuarters", "Half"):
            reduced = np.asarray(composite(SOURCE / f"{theme}-{mode}-{time}.png"))[:, :380, :].astype(np.int16)
            delta = np.abs(full - reduced)
            rows.append([theme, mode, time, float(delta.mean()), int((delta != 0).any(axis=2).sum())])
with (OUTPUT / "image-differences.csv").open("w", newline="", encoding="utf-8") as file:
    writer = csv.writer(file)
    writer.writerow(["theme", "mode", "mediaMs", "fullStageRgbMae", "changedStagePixels"])
    writer.writerows(rows)
(OUTPUT / "image-digests.json").write_text(json.dumps(digests, indent=2) + "\n", encoding="utf-8")
canvas = Image.new("RGB", (1140, 1130), BG[:3])
draw = ImageDraw.Draw(canvas)
for row, theme in enumerate(("Classic", "Partita")):
    for col, mode in enumerate(("Full", "ThreeQuarters", "Half")):
        image = composite(SOURCE / f"{theme}-{mode}-8600.png").crop((0, 0, 380, 540))
        canvas.paste(image, (col * 380, row * 565 + 25))
        draw.text((col * 380 + 10, row * 565 + 7), f"{theme} {mode}", fill="white")
canvas.save(OUTPUT / "resolution-comparison.png")
print("Reported 30 PNGs, 20 lossy comparisons; alpha composited over #162435")
