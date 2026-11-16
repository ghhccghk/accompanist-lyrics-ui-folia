"""Compare two SoftwareBlurDeviceProbe logs, failing on coverage/origin changes."""
import json
from pathlib import Path
import sys


def read(path):
    rows = {}
    for line in Path(path).read_text(encoding="utf-8-sig").splitlines():
        if not line or not line[0].isdigit():
            continue
        size, *fields = line.split()
        row = dict(field.split("=", 1) for field in fields)
        rows[(size, row["sigma"])] = row
    if not rows:
        raise ValueError("No completed probe cases")
    return rows


before, after = read(sys.argv[1]), read(sys.argv[2])
assert before.keys() == after.keys(), "Incomplete/mismatched probe cases"
result = []
for (size, sigma), old in before.items():
    new = after[(size, sigma)]
    for key in ("output", "origin", "argbHash", "sha256"):
        if key in old or key in new:
            assert old.get(key) == new.get(key), f"Changed {size} sigma={sigma}: {key}"
    a, b = float(old["p50Ms"]), float(new["p50Ms"])
    result.append({"source": size, "sigma": float(sigma), "before": old, "after": new,
                   "p50_reduction_percent": round(100*(a-b)/a, 2)})
Path(sys.argv[3]).write_text(json.dumps(result, indent=2)+"\n", encoding="utf-8")
