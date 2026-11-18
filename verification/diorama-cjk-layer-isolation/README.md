# Frozen CJK full-layer failure isolation

Baseline13b8518, fixed Folia cc6226e4, 2026-11-18. One EXISTING original
cjk-800-active-1100.png,800×600/DPR1, recorded CJK font and original material
logic. Exact browser151.0.7922.34/Three0.185.1/Motion13.4.3/dom13.4.2/utils13.3.0,
font/lock/source/host hashes and actual21 mesh inputs are retained in
active-cjk-layers-v1/scene.json. This is the existing isolated analytic media
Float32 envelope injection/delta0 stage, NOT mounted CameraRig or actual RAF.

Original full-frame SHA256 remains
73fdfcc849581230bc4ba7d4693d735b1ded55d6b724d4c86c1463a05bc54592
before passive read/after/restoration and cloned-all. Diagnostic clones share
actual geometry/material/texture inputs, without changing original refs/time/
materials. Original matrices/versions/visibility/opacity remain exact. Base-only
source also EXACTLY matches the existing original active-base800/1100ms PNG.
Dedicated browser/server close in finally; explicit stage disposal leaves
geometry0/texture0. No original fixture or expected frame is overwritten.

Native independently prepares its own full-shaped text and analytic states.
Diagnostic full ordinary and sampled renders EXACTLY match current complete
stage captures, then visibility-only masks isolate base/glow/soul. The ordinary
and sampled additive-layer PNGs match exactly, as expected: no sampler runs
there. Pause/display-time advance does not prepare more glyphs or mips.

| Layer | Mean ordinary→sampled | Worst64 ordinary→sampled |
| --- | ---: | ---: |
|all|2.693813→2.140196|4.418929→5.198953|
|base|3.418336→2.659451|5.831325→5.198953|
|glow|.334105 unchanged|.481215 unchanged|
|soul|3.410248 unchanged|4.475429 unchanged|

Each row has its OWN original/ordinary/sampled union; do not add or compare
their means to infer contribution. The failure tile(384,320)–(448,384) was
selected by the EXISTING full-stage matrix, not searched to favor this control.
Within its fixed573-pixel full-stage union, every original/ordinary/sampled
all pixel equals its base pixel. ALL glow/soul pixels there are background.
Thus the local3.979058→5.198953 failure is entirely unsung base ink, not
additive compositing/halo intensity or a reason to alter the foreground mask.
Full-base maximum improves elsewhere, but does not waive this worse tile.

One JVM exact-replay/pause/no-preparation test passes; no production source,
API, font, clock or renderer change. Fixed contact inspected; no new Android
build is needed for this test-only host change. No device/window/performance/
formal theme conclusion. Next isolate source/native texture versus placement/
canonical fractional phase for this exact base layer before another candidate.

```powershell
# Refuses fixture overwrite; only justified distinct capture may be added later.
node verification/classic-browser/capture-diorama-cjk-layers.mjs
.\gradlew.bat :library:jvmTest --tests '*DioramaCjkLayerIsolationTest'
python verification/compare-diorama-cjk-layers.py
```
