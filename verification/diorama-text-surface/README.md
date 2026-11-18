# Internal transparent text surface — 2026-11-18

Source: Folia cc6226e464050a456a2913e062374f23d3844cc5,
`DioramaScene.tsx` transparent WebGL canvas and `DioramaActiveUnitPlanes.tsx`
additive/normal material order. See previous `diorama-mounted-entrance` for
the passive original framebuffer isolation, rejected 8-bit candidate and failures.

Production opt-in uses a local F16 surface on JVM only when an additive text
layer is visible, then composes it over the host. Original time/geometry stays
unchanged. With no additive layers it draws directly, avoiding extra rounding.
The 108 independent actual Three solid-color/RGBA/CSS cases have peak channel
error 1 (pre-existing gate 2). No fitted alpha or per-scene constants.

Budget: 32MiB F16 pixel payload and 8192px edge, including actual device-space
host transform. This is an allocation estimate, not measured total memory.
Invalid/projective-horizon/oversize requests return false without drawing;
caller uses ordinary rendering. Android currently does exactly that. Canvas
save count, matrix and clip restore after success/exception; original draw
exception propagates. No surface survives a draw or has cross-frame ownership.

`compare-diorama-text-surface.py` uses the same 48 original frames and a fixed
five-image union (original, previous/new finite and previous/new media). All
337 frozen previous native/control hashes remain unchanged. Finite mean
1.964789→0.944027, worst64 6.883721→3.410853; no new mean/local regressions.
Media mean 17.365684→16.567190, worst64 69.117781 unchanged; two local
regressions at 800px/3499ms remain (60Hz 41.246106→41.252336;
144Hz 22.426667→22.433333). These are recorded, not hidden by averages.

Finite/material source-state isolation and media-clock playback are different
claims. This fixes a compositing error; camera, sampling and other visual
residuals remain. No final visual acceptance, Android surface, device or
performance claim. The prior save-count test failure and its boundary diagnosis
are retained in `validation.json`; outer scope restoration is checked as well.

Reproduce with the three targeted suites in `validation.json`,
`:library:compileAndroidMain :sample:desktop-app:compileKotlin`, then
`python verification/compare-diorama-text-surface.py`. Original oracle capture:
`verification/classic-browser/capture-diorama-text-surface.mjs`; conditions,
actual resolved versions and original framebuffer values are in
`verification/diorama-fixtures/text-surface-v1`.
