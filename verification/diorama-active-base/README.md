# Diorama active complete-shaped slices: base reveal and gradient

Pinned Folia cc6226e464050a456a2913e062374f23d3844cc5. Actual unmodified original
buildLineGraphemeTimeline, splitDioramaLyricUnits, layoutDioramaActiveUnits,
updateDioramaActiveUnits, original plain unit rasters and real Three perspective.
The source stage has glow/soul explicitly0, gradient1 and frozen view/life1;
light/soul arrays start0 and delta0, irrelevant to this gradient-driven base.
This isolates source base behavior, NOT original continuous CameraRig/RAF effects.
No source formula or raster is rewritten. No source PNG is used by Kotlin.

3 synthetic lines/fonts (Latin/Arabic/CJK),3 widths380/800/801×600,DPR1,
10 media times,90 original/Kotlin pairs. Exact text/timing/font SHA256/colors/
unit materials and placement in ../diorama-fixtures/active-base.json and.tsv.
Kotlin slices each unit from ONE complete shaped paragraph at128px em and keeps
all internal EGC clocks. Words/EGCs are never shaped as UTF16 units. Original
128/.7em active texture padding and360px height retained. Complete advance width
matches original within.001px; reverse seek, frozen time and remount exact pixels.
Time probes do not reshape/reallocate text. Linear material colors encode to
sRGB once, using original per-unit own-clock gradient/reveal formula.

The source positions an isolated unit using kerned prefix width and its rounded
isolated advance. Kotlin uses full shaped selection geometry, so contextual
joining survives. This is a necessary shaping adaptation; mixed bidi, kerning
and joining-boundary overhang still need additional fixtures. Slices currently
clip body to unit advance bounds; overhang across a unit boundary is an unresolved
limitation, not final acceptance. Source/Kotlin placement/raster differences
remain measured; no fitted frame/pixel registration or alpha deletion.

90-frame all-nonbackground union including low alpha foreground MAE6.60591874:
Latin7.51194221,Arabic6.39974292,CJK5.90607110. Ink-bound peak2px. All hashes,
local worst64px/channel peaks in comparison.json; contact.png inspected.
No source threshold/tolerance adjusted. These are independent fixture statistics,
not improvements over neighbour-plane metrics or final theme equivalence.
JVM offscreen tests and Android APK/Desktop compile pass; no device/real window.
Android typographic midpoint metadata remains an explicit unimplemented parity
item inherited from the neighbouring-text adapter. Diorama is NOT selectable.

```
node verification/classic-browser/capture-diorama-active-base.mjs
./gradlew :library:jvmTest --tests '*DioramaActiveTextTest'
python verification/compare-diorama-active-base.py
./gradlew :sample:android-app:assembleDebug :sample:desktop-app:compileKotlin
```
Windows uses gradlew.bat/JDK21/SDK. Dedicated4174 browser/server close in finally;
full PNGs ignored under build. .work/diorama-active-base-first.log / build.log pass.
Next: original registered halo and phase-correct ghost layers, then real camera/
fog/depth and public integration. This base-only verification does not cover them.
