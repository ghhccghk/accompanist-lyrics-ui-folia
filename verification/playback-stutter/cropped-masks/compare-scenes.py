"""After the three documented Partita tests, compare their fresh PNG hashes."""
import hashlib
import json
import re
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
baseline = json.loads((HERE.parent / "transparent-layers/scene-comparison.json").read_text())
pattern = re.compile(r"partita-kotlin/normal-(wrap|arabic|group)-380-\d+ms\.png")
expected = {name: digest for name, digest in baseline["digests"].items() if pattern.fullmatch(name)}
assert len(expected) == 36, len(expected)
actual = {name: hashlib.sha256((ROOT / "build/verification" / name).read_bytes()).hexdigest() for name in expected}
changed = [name for name in expected if expected[name] != actual[name]]
report = {"baseline": "ca9e07c", "pngCount": len(actual), "changed": changed, "digests": actual}
output = ROOT / "build/verification/cropped-masks/scene-comparison.json"
output.parent.mkdir(parents=True, exist_ok=True)
output.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
assert not changed, changed
print(f"All {len(actual)} recaptured images match ca9e07c exactly")
