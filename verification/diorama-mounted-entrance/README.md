# Frozen mounted entrance: transparent framebuffer contribution

Baseline1103f21, fixed Folia cc6226e464050a456a2913e062374f23d3844cc5,
2026-11-18. This closes the contribution investigation, not full Diorama visual
acceptance. No production renderer or public theme changes in this step.

The SAME four380/800×600/DPR1,60/144Hz700ms original failures are replayed from
their exact452/1083 callback prefixes. Both passive captures retain the old
full PNGs, source state, matrices and texture/source versions exactly. Original
CameraRig and DioramaScene run intact; layer scenes only clone existing meshes.
Unloading leaves0 roots/queued RAFs; dedicated browser/server close in finally.
Browser151.0.7922.34, exact Motion13.4.3/dom13.4.2/utils13.3.0, source/font/lock/
host hashes and actual original inputs live in mounted-entrance[-alpha]-v1.
Native independently uses actual core/full paragraph shaping, geometry and finite
history; full diagnostic replays EXACTLY equal its actual before/after stages.
All337 existing production/control PNG hashes remain unchanged.

The old failing y0 tiles contain active text, not neighbors (neighbors begin at
y416 or lower). At700ms original glow/soul ARE visible, unlike the3500ms prior
base-only failure. Direct additive Compose produces too much light: isolated
glow RGB absolute mass original7/9/41/29, native643/231/2846/1023. Each standalone
layer has a different union; their means cannot be added to explain a full frame.

Passive original WebGL2 reads establish alpha=true, premultipliedAlpha=true,
clearColor=[0,0,0,0], CSS background rgb(22,36,53). Raw framebuffer PNG alpha
peaks25/9 for glow and20/7/24/9 for soul. Independently compositing each raw RGBA
over CSS reproduces the browser screenshot within1 channel value, retaining all
differences. Three0.185.1 WebGLState nonpremultiplied AdditiveBlending accumulates
RGB with SRC_ALPHA/ONE and alpha with ONE/ONE; normal layers follow. Finally the
transparent canvas composites over CSS. Drawing Plus straight onto an opaque
Compose background omits that final attenuation of the background by accumulated
alpha. Source order17/18/19, fog, tone mapping and texture inputs are unchanged.

An ordinary8-bit saveLayer control was rejected: fixed48-frame sampled stage
mean1.917132→1.157481 improves, but16 mean/14 local regressions remain. A F16
temporary-layer diagnostic reduces these to11/10, mostly tiny normal-only
rounding differences; it is NOT yet a production fix. On the four entrance
active layers, channel peaks5–10 fall to1–2 with F16. Fixed48, SAME10-image union:

| Path | Mean before→F16 | Worst64 before→F16 | Mean/local regressions |
| --- | ---: | ---: | ---: |
|ordinary finite|6.258691→5.183102|17.852321→17.719937|2/5|
|sampled finite|1.917132→.913014|6.883721→3.410853|11/10|
|sampled media|16.297417→15.504730|67.744543 unchanged|2/3|

All failures/hashes/bounds/low-alpha pixels remain in comparison.json. This
union includes all original/direct/8-bit/F16 paths and differs from earlier
reports. No movement, foreground threshold, color fit, camera or time shift.
The fixed contact was inspected without boosting brightness for metrics.

Two JVM tests pass with pause, no repeated mip preparation, diagnostic round
trip, seek/remount and owner release. An expanded duplicate-control run exceeded
the default60s coroutine test timeout; exact failure is retained. Removing the
already rejected duplicate8-bit capture reduced the workload; assertions,
timeout and visual gates were not changed. This evidence-only step requires no
new Android APK; no device/window/PSS/CPU/formal visual claim follows.

Next: implement a bounded, explicitly opt-in JVM transparent text surface with
an independent RGBA blend control and lifecycle checks. Preserve the existing
ordinary fallback on Android; avoid unnecessary intermediate surfaces when no
additive layer exists, then re-evaluate the SAME48 matrix and all residuals.

```powershell
node verification/classic-browser/capture-diorama-mounted-entrance.mjs
node verification/classic-browser/capture-diorama-mounted-entrance.mjs --alpha
.\gradlew.bat :library:jvmTest --tests '*DioramaMountedStageTest'
python verification/compare-diorama-mounted-entrance.py
```

Source capture refuses versioned fixture overwrite. Comparisons require frozen
ignored old matrices and rejected8-bit outputs, not committed frame dumps. No
source texture is a runtime asset; complete source inputs stay diagnostics only.
