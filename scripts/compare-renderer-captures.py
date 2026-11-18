"""Measure unregistered pairs of paused Partita screenshots (Pillow + NumPy)."""
import argparse
import json
from pathlib import Path

import numpy as np
from PIL import Image

BACKGROUND = (22, 36, 53)
TILE = 32


def summarize(reference, native):
    difference = np.abs(reference - native)
    foreground = np.any(reference != BACKGROUND, axis=2) | np.any(native != BACKGROUND, axis=2)
    if not np.any(foreground):
        raise ValueError("Neither capture has visible content in the lyrics region")
    foreground_error = difference[foreground]
    # Background is the exact fixture color. Retain even one-code-value
    # deviations. Tile scores are based only on the foreground in each tile.
    height, width = foreground.shape
    worst = None
    for top in range(0, height, TILE):
        for left in range(0, width, TILE):
            region = np.s_[top:top + TILE, left:left + TILE]
            mask = foreground[region]
            if not mask.any():
                continue
            values = difference[region][mask]
            result = {"xy": [left, top], "foreground_pixels": int(mask.sum()),
                      "mean_rgb_error": float(values.mean()), "max_error": int(values.max())}
            if worst is None or result["mean_rgb_error"] > worst["mean_rgb_error"]:
                worst = result
    return {"union_foreground_pixels": int(foreground.sum()),
            "reference_only_pixels": int((np.any(reference != BACKGROUND, axis=2) &
                                          ~np.any(native != BACKGROUND, axis=2)).sum()),
            "native_only_pixels": int((~np.any(reference != BACKGROUND, axis=2) &
                                       np.any(native != BACKGROUND, axis=2)).sum()),
            "foreground_mean_rgb_error": float(foreground_error.mean()),
            "foreground_p99_rgb_error": float(np.percentile(foreground_error, 99)),
            "foreground_max_rgb_error": int(foreground_error.max()), "worst_32px_tile": worst}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("kotlin", type=Path)
    parser.add_argument("native", type=Path)
    parser.add_argument("--region", type=int, nargs=4, metavar=("LEFT", "TOP", "RIGHT", "BOTTOM"), required=True)
    args = parser.parse_args()
    left, top, right, bottom = args.region
    with Image.open(args.kotlin) as kotlin, Image.open(args.native) as native:
        if kotlin.size != native.size or not (0 <= left < right <= kotlin.width and 0 <= top < bottom <= kotlin.height):
            parser.error("Use equal screenshot dimensions and one in-bounds region for both captures")
        frames = [np.asarray(image.convert("RGB"), dtype=np.int16)[top:bottom, left:right] for image in (kotlin, native)]
    print(json.dumps({"region": args.region, **summarize(*frames)}, indent=2))


if __name__ == "__main__":
    main()
