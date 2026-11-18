# Diorama actual R3F ACES — 2026-11-18

Pinned original Folia cc6226e464050a456a2913e062374f23d3844cc5 uses R3F
ACESFilmic/exposure1 for its ordinary text MeshBasicMaterials. The original
particle materials that opt out are outside this text-only task. Previous direct
Three hosts used NoToneMapping; their oracle is not changed or relabelled.

## Independent shader control and implementation

`classic-browser/diorama-tonemapping.ts` renders the actual original-lock
Three0.185.1 shader into its default WebGL framebuffer and reads the centre pixel.
There is **no JS ACES expected formula**. 16 linear colours include black, low
values, HDR, saturated RGB and the three original scene palette colours; each
uses alpha1/.63/.2, normal/additive and fog-depth6/12/21/30/36, for None and ACES:
960 frozen controls. Conditions, source/host/shader/lock hashes, exact Motion
versions, Chromium151.0.7922.34 and original revision live in
`../diorama-fixtures/tonemapping-v1`. Source checkout is unchanged. Dedicated
headless server4189/browser and WebGL context close after capture.

`DioramaToneMapping.kt` adapts Three's column-major ACES matrices and fit in
Double, with source exposure1. It keeps the upstream MIT copyright/notice,
already included in the project's packaged notices. Full text playback maps
neighbor, glow, soul and body linear RGB before output sRGB, then encoded fog,
then alpha blending. Alpha/life/coverage and complete shaping are unaffected.
Low-level direct-Three test paths explicitly retain None. No new runtime or
native dependency, frame sampling, glyph preparation or background engine.

Initial missing-ACES control failed with opaque/full peak147 byte values.
The first fix gave opaque peak0 but full peak106: NoToneMapping HDR had been
clamped to bytes BEFORE fog. Extended encoded RGB now survives until fog,
matching shader order. Full original control/failed native tables and XML remain
in `.work/diorama-tonemapping-{before,first-fix}`; `shader-comparison.tsv` retains
all960 before/first-fix/final rows. Final **opaque peak0 <=1 byte; full blend/fog
peak2 <=2 bytes**, using the gates declared in the capture before implementation.
No HDR cases were omitted or gate relaxed.

## Same mounted48 images before / after

Baseline175dabe raw native96 frames and reports are preserved in
`.work/diorama-aces-before`. Frozen original48 hashes all match. Same original
input, default picker, core, fonts,380/800×600,DPR1,60/144 schedules and12 media
probes as `../diorama-mounted`. **One shared five-image all-nonbackground union**
for original / before-finite / after-finite / before-media / after-media, down
to lowest alpha, no registration, phase shift, filtering or excluded frames.
Camera4686 controls and residuals remain byte-identical; finite pose peak remains
6.04e-14/1e-9. This is a colour correction, not a camera fix.

| Method | Mean foreground MAE before → after | Worst frame | Worst64 local |
| --- | ---: | ---: | ---: |
| Own finite camera, analytic media material | 7.251753 → 6.954840 | 20.492229 → 17.308414 | 24.617612 → 20.434397 |
| Own reconstructible media camera/material | 20.348141 → 20.289881 | 50.141831 → 46.444817 | 87.853968 → 83.400000 |

Not every frame improves: finite22 improve/**26 regress**, media21 improve/
**27 regress**; local errors regress in28/29 frames respectively. Every case,
including narrow144Hz2.099s regression8.200420→10.470451 MAE (see exact
before/after in comparison.json), is retained. ACES algebra is independently
confirmed; that does not resolve the remaining source Float32 envelopes/wall
breath, text/backend sampling, pose and cropping differences or prove the whole
picture equivalent. Fixed six-probe contact inspected: corresponding text and
clipping remain, several brightness differences persist. Diorama stays internal
and **not visually accepted**, with no public/sample option enabled.

25 targeted JVM tests pass, including all above controls, media schedule
independence, paused pixels, reverse seek/remount, core identity/offset and
public Pendolo/Cappella/Fume regression. Android library/Desktop compilation pass.
All610 pre-existing direct-Three-policy native PNG hashes remain identical.
No new Android runtime, true Desktop window/audio or performance conclusion.

```powershell
# Exporter refuses overwrite; preserve frozen controls.
node verification/classic-browser/capture-diorama-tonemapping.mjs
.\gradlew.bat :library:jvmTest --tests '*DioramaToneMappingTest' --tests '*DioramaMountedStageTest' :library:compileAndroidMain :sample:desktop-app:compileKotlin
python verification/compare-diorama-tonemapping.py
```

The comparison requires preserved175dabe native outputs from the same frozen
mounted matrix; regenerate them only from that revision in a separate checkout,
never patched code pretending to be the before condition. Existing
`diorama-mounted/comparison.json` and all old original oracles stay untouched.
Next bounded comparison: reproduce original per-callback active envelopes and
wall breath with independently computed core state in this full mounted matrix,
separating material-history residual from text raster/pose. Do not inject source
material values or a fixed UI-frame lag into production to win the comparison.
