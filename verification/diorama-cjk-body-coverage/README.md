# Separate foreground coverage from grayscale effect input

Baseline5a6bcb0, fixed Folia cc6226e4, 2026-11-18. This repairs an independently
isolated Windows CJK body-input difference; it is not full Diorama acceptance.
The original fixed source90 base/180 layer frames, fonts, widths380/800/801,
times, colors and controls stay unchanged. No source recapture or fitted image
translation/threshold/gamma/LOD/time shift is used.

The previous layer/input cross proved that the failing unsung tile contains
no glow/soul contribution. Merely swapping source placement worsened it; the
existing host-preserving Windows foreground raster settings improved ALL7 raw
CJK alpha means and peaks with geometry held fixed. See the preserved source
inputs/failures in diorama-cjk-input-isolation, not native-as-expected data.

`DioramaActiveSlice.body` now holds a separate foreground image only when the
original independent CJK/kana/symbol display-unit class applies to the whole
line AND `classicBodyRasterStyle` actually changes host settings. Full paragraph
shaping and existing column ownership determine both images; original grayscale
`image` still feeds registered blur and additive soul. Only base order19 uses
`body`, including the optional current-line sampler owner. Geometry, pivots,
imageOffset, materials, internal glyph clocks and camera are unchanged. Other
platforms/explicit host paragraph settings preserve their actual defaults and
share the original image; no Android shader or public theme is introduced.

This keeps one additional body image per applicable prepared unit on Windows.
It is an accuracy allocation tradeoff, NOT a memory/performance improvement.
Mip-owner budget still counts only its own additional payload. Existing
whole-song prepared image residency is not resolved by this change.

All following figures use the SAME FIVE images' union per frame (original,
old ordinary/sampler,new ordinary/sampler), including every low-alpha pixel:

| Matrix/path | Foreground mean before→after | Worst64 before→after |
| --- | ---: | ---: |
|90 base ordinary|5.750058→5.496042|24.666667 unchanged|
|90 base sampler|2.351242→1.841756|8.832765→7.849341|
|180 full layers ordinary|3.898053→3.732787|24.666667 unchanged|
|180 full layers sampler|1.797512→1.466458|8.832765→7.849341|

Zero mean/local regressions in all four comparisons. Previously retained12
CJK sampled-vs-old-ordinary local regressions are absent in this fixed matrix;
this does not establish arbitrary-font/platform/stage equivalence. The prior
exact800/1100 frame now has sampler worst64 5.109777→2.270341 on this NEW
five-image union (not the previous three-image mask). Fixed contact inspected,
including fading soul and halo; no blur input was changed to obtain this gain.

All14 original native grayscale/soul and halo input hashes remain EXACT. Latin/
Arabic base and complete-layer ordinary/sampler paths remain byte-identical.
Unsafe ligature/split Arabic/mixed-bidi/CJK original grayscale masks remain exact.
The body additionally reconstructs its independently drawn whole paragraph with
EXACT alpha for all4 mask scenes, verifying no lost/duplicated body ownership.

12 targeted JVM tests pass: body separation, raw/geometry controls, exact full
stage replay, base3/layers3, complete-mask conservation and bounded owner/core
time; Android library/Desktop compile pass. Additional body-conservation
assertions were run after the initial matrix and pass; this did not rerun or
replace source images. Pause/seek/remount/no stable-frame mip preparation and
resource release remain checked. No device/window/PSS/CPU/formal visual claim.

```powershell
.\gradlew.bat :library:jvmTest --tests '*DioramaCjkBodyCoverageTest' --tests '*DioramaCjkLayerIsolationTest' --tests '*DioramaCjkInputIsolationTest' --tests '*DioramaActiveTextTest' --tests '*DioramaActiveLayersTest' --tests '*DioramaShapedMaskTest' --tests '*DioramaActiveBaseRendererTest' :library:compileAndroidMain :sample:desktop-app:compileKotlin
python verification/compare-diorama-cjk-body-coverage.py
```

Comparator requires preserved old ignored matrices and rejects changed originals,
grayscale/effect input or unrelated-script output. Full errors/hashes/peaks/
bounds and failures remain in comparison.json. Next refresh Android debug and
independent consumer; then isolate the still-existing700ms mounted entrance
residual before another renderer change. Media-camera, Android sampling,
long-song preparation/residency and full theme validation remain incomplete.
