# Mounted active-body input isolation — 2026-11-18

Baseline bb84afd, fixed Folia cc6226e464050a456a2913e062374f23d3844cc5.
The frozen finite-camera worst region is mounted-380-144-3500.png:380×600,
DPR1, incoming3.5s, wall10.3s, same synthetic core lyrics and parity font.
Actual Chromium151.0.7922.34, exact Motion13.4.3/dom13.4.2/utils13.3.0 and
rendering/font/source/host hashes are in mounted-active-input-v1/scene.json.

The capture replays EXACTLY the existing1494-callback prefix from steps.tsv,
including each actual delta, not a newly generated nominal144Hz schedule.
Passive reads collect all12 actual active mesh textures/materials/matrices.
Labelled layer diagnostics clone existing meshes into a temporary scene;
no source ref, material, texture, clock, logic or matrix is changed. Full PNG
before/after/restoration stays SHA256
8640dfda46253c7ee53d976f003b3a256ac6996804af388134109256201e1bf3;
state, texture/source versions and matrices remain exact. Unmount leaves
roots0/queuedRafs0; dedicated browser/server close in finally.

**Original active-all equals base-only. All original glow/soul meshes are
invisible and their diagnostics are background-only. Body opacity is0.5 for
all4 units.** This frame cannot be repaired by tuning halo strength or blur.
The independently prepared real-core/finite-camera native active-only frame
has418 nonbackground pixels versus original808, union808, MAE19.792492,
channel peak74, worst64 tile(128,320)20.624204. This active-only union differs
from the full-stage five-image union; do not compare those means as a benefit.

An input-only ablation tries the EXISTING Windows foreground raster policy;
production remains unchanged. The default unit textures exactly hash-match
the real native stage. Geometry, full shaping, pivots, ownership and clocks
stay identical between ablations. Raw-alpha same-origin comparison uses only
transparent zero-extension for unequal extents, no registration:

| Unit | Default MAE/peak | Foreground diagnostic MAE/peak |
| --- | ---: | ---: |
|0|3.590156/130|1.591278/20|
|1|7.299843/210|7.459169/130|
|2|14.220997/228|15.920083/188|
|3|11.065450/228|12.507095/166|

The policy regresses3 means and is NOT adopted. Fractional full-paragraph
image offsets and one-pixel crop-size differences remain recorded, not shifted
away or repaired by breaking shaping. Contact inspected; complete hashes,
mass/extent/peak and failures stay in comparison.json. Two targeted JVM tests
pass, including frozen media while display time advances. No production
change, device/window/performance or formal visual acceptance is claimed.

Next isolate active BASE fragment sampling with the already verified sampler,
then compare the SAME48 mounted frames if justified. Do not change camera,
timing, expected source frames or global glyph policy to chase this region.

```powershell
# Capture refuses to overwrite the versioned source fixture; use a distinct
# diagnostic location for a justified future source run, preserving old data.
node verification/classic-browser/capture-diorama-mounted-active-input.mjs
.\gradlew.bat :library:jvmTest --tests '*DioramaMountedStageTest.narrow'
.\gradlew.bat :library:jvmTest --tests '*DioramaActiveInputCoverageTest'
python verification/compare-diorama-mounted-active-input.py
```
