"""Summarize simpleperf text samples and only overlapping gfxinfo frames.

Raw traces remain local: no user lyrics/media paths or device serials in output.
Usage: python analyze-sample.py samples.txt gfxinfo.txt output.json
"""
import csv
import json
import math
from pathlib import Path
import sys


def analyze(samples_text, gfx_text):
    samples = []
    for block in samples_text.split("\nsample:\n")[1:]:
        lines = [line.strip() for line in block.splitlines()]
        timestamp = int(next(line[6:] for line in lines if line.startswith("time: ")))
        symbols = {line[8:] for line in lines if line.startswith("symbol: ")}
        samples.append((timestamp, symbols))
    start, end = min(s[0] for s in samples), max(s[0] for s in samples)
    methods = [
        "SoftwareBlurKt.blurPremultiplied", "GlowMaskKt.blurBoxCoverage",
        "GlowMaskKt.blurredMasks", "ClassicLayer.record-",
        "PartitaLineKt.", "ClassicLineKt.", "android.graphics.Bitmap.getWidth",
    ]
    inclusive = {
        method: sum(any(method in name for name in symbols) for _, symbols in samples)
        for method in methods
    }
    frames = []
    flag_counts = {}
    for section in gfx_text.split("---PROFILEDATA---")[1::2]:
        reader = csv.DictReader(section.strip().splitlines())
        for row in reader:
            if not row.get("IntendedVsync"):
                continue
            intended = int(row["IntendedVsync"])
            completed = int(row["FrameCompleted"])
            if start <= intended <= completed <= end:
                frames.append((completed-intended)/1e6)
                flag = row["Flags"]
                flag_counts[flag] = flag_counts.get(flag, 0)+1
    frames.sort()
    def quantile(q):
        return frames[max(0, math.ceil(len(frames)*q)-1)] if frames else None
    return {
        "exported_text_samples": len(samples), "first_to_last_sample_seconds": (end-start)/1e9,
        "inclusive_samples_not_additive": inclusive,
        "inclusive_percent": {k: round(100*v/len(samples), 3) for k, v in inclusive.items()},
        "overlapping_retained_frames_only": {
            "count": len(frames), "p50_ms": quantile(.5), "p95_ms": quantile(.95),
            "max_ms": max(frames) if frames else None,
            "raw_frame_flags": flag_counts,
        },
        "limits": "CPU sampling adds overhead. Text export omits some recorded samples; percentages use exported stacks. Frame ring buffer is incomplete, raw flags retained without classifying jank; not session FPS or a before/after benchmark. Theme inferred from stack, quality and Rust toggle unrecorded. No screenshots or user media captured.",
    }


if __name__ == "__main__":
    result = analyze(Path(sys.argv[1]).read_text(encoding="utf-8-sig"),
                     Path(sys.argv[2]).read_text(encoding="utf-8-sig"))
    Path(sys.argv[3]).write_text(json.dumps(result, indent=2)+"\n", encoding="utf-8")
