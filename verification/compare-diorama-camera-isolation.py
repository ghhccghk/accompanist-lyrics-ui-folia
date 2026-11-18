"""Fixed original mounted frames; camera/material cross, with no image alignment.

Read-only original inputs. Creates a separate diagnostic report, never overwrites
the original oracle, earlier comparison or native baseline images.
"""
from pathlib import Path
import hashlib
import json
import xml.etree.ElementTree as ET

import numpy as np
from PIL import Image

root = Path(__file__).resolve().parents[1]
build = root / "build/verification"
inputs = build / "diorama-camera-isolation"
out = root / "verification/diorama-camera-isolation"
out.mkdir(exist_ok=True)
sha = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
scene_path = root / "verification/diorama-fixtures/mounted-cut-v1/scene.json"
scene = json.loads(scene_path.read_text())
previous_path = root / "verification/diorama-text-surface/comparison.json"
previous = json.loads(previous_path.read_text())
frozen = {r["file"]: r for r in previous["rows"]}
assert len(scene["captures"]) == 48
for capture in scene["captures"]:
    assert sha(build / "diorama-mounted-browser" / capture["file"]) == frozen[capture["file"]]["hashes"]["original"]

names = ["sourcePoseSourceMaterial", "mediaPoseSourceMaterial", "sourcePoseMediaMaterial", "mediaPoseMediaMaterial"]
rows = []
background = np.array([22, 36, 53])
for capture in scene["captures"]:
    if capture["time"] not in (3.201, 3.499):
        continue
    file = capture["file"]
    paths = {"original": build / "diorama-mounted-browser" / file}
    paths.update({name: inputs / (name + "-" + file) for name in names})
    images = {name: np.array(Image.open(path).convert("RGB"), dtype=np.int32) for name, path in paths.items()}
    old = build / "diorama-text-surface-kotlin" / ("media-" + file)
    assert sha(old) == frozen[file]["hashes"]["mediaAfter"]
    assert np.array_equal(images["mediaPoseMediaMaterial"], np.array(Image.open(old).convert("RGB")))
    union = np.logical_or.reduce([np.any(image != background, axis=2) for image in images.values()])
    metrics = {}
    for name in names:
        diff = np.abs(images[name] - images["original"])
        tiles = [dict(x=x, y=y, mae=float(diff[y:y+64, x:x+64][union[y:y+64, x:x+64]].mean()))
                 for y in range(0, union.shape[0], 64) for x in range(0, union.shape[1], 64)
                 if union[y:y+64, x:x+64].any()]
        metrics[name] = dict(mae=float(diff[union].mean()), peak=int(diff.max()),
                             worst64=max(tiles, key=lambda t: t["mae"]))
    rows.append(dict(file=file, unionPixels=int(union.sum()), metrics=metrics,
                     hashes={name: sha(path) for name, path in paths.items()}))
assert len(rows) == 8

for width in (380, 800):
    for kind in ("camera", "factors"):
        data = (inputs / f"{kind}-{width}.tsv").read_bytes()
        (out / f"{kind}-{width}.tsv").write_bytes(data)
    assert "captures=24" in (inputs / f"control-{width}.txt").read_text()

suite = ET.parse(root / "library/build/test-results/jvmTest/TEST-top.ghhccghk.accompanist.lyrics.folia.DioramaMountedCameraIsolationTest.xml").getroot()
assert suite.attrib["failures"] == "0" and suite.attrib["errors"] == "0" and suite.attrib["tests"] == "2"
report = dict(sourceRevision=scene["revision"], originalMotion=scene["motion"],
              frozen48OriginalImagesUnchanged=True, nativePreviousMediaExact=8,
              conditions="Unchanged original 48 matrix, subset 8 images at 3.201/3.499 seconds; own core/font/viewport/geometry. Fixed 5-image union including every non-background pixel; no registration, alpha suppression, timing shift or fitted camera. Source pose/envelope are LABELLED diagnostic controls only, never production inputs.",
              inputHashes={str(path.relative_to(root)).replace("\\", "/"): sha(path) for path in [
                  scene_path, previous_path, root / "verification/diorama-fixtures/mounted-cut-v1/steps.tsv",
                  root / "verification/diorama-fixtures/mounted-material-v1/materials.tsv",
                  root / "verification/assets/FoliaParityLatin-Bold.ttf"]},
              numericCaptures=48, baseVariantsPerCapture=6, additionalShortPostFlightControls=8, finiteSourceStateGate=1e-9,
              finiteSourcePeak=max(float((inputs / f"control-{w}.txt").read_text().split("originalFinitePeak=")[1].split()[0]) for w in (380, 800)),
              tests=dict(suite=suite.attrib["name"], tests=2, failures=0, errors=0), rows=rows,
              productionChanged=False, formalVisualAcceptance=False, deviceRun=False,
              rejectedHarness=dict(path=".work/diorama-camera-isolation-imagebitmap-rejected",
                                   log=".work/diorama-camera-material-cross.log",
                                   failure="Direct ImageBitmap Canvas differs from previous Compose capture by 12 pixels, peak1, first narrow 3.201s case; rejected. Same original Compose Canvas/capture path reproduces all eight previous media images exactly. Precision gate unchanged; low-level rounding cause not separately isolated."))
(out / "comparison.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf8")
for row in rows:
    print(row["file"], {name: dict(mae=round(value["mae"], 6), worst64=round(value["worst64"]["mae"], 6)) for name, value in row["metrics"].items()})
print("Original48 unchanged; native current8 exact; no production change or acceptance claim.")
