# Tilt accessible scale: row preparation repair

Pinned Folia v0.7.26 / cc6226e464050a456a2913e062374f23d3844cc5,
AGPL-3.0-only. Original `tilt/VisualizerTilt.tsx:225-334,350-356,576-580`
uses its lyric scale in fit/resplit, CSS font clamps, line height and stagger.
The Compose drawing font previously received `LocalDensity.fontScale` implicitly
through sp, but fit, row height, centering and stagger did not. At scale 1.4/2,
large glyphs therefore occupied unscaled row boxes. The production repair applies
host × system scale once before all those computations, then cancels sp's automatic
second multiplication. It keeps intact shaping and internal grapheme clocks.
Shortest-decimal Float conversion preserves CSS half-round boundaries:
`3.125 * 1.4 * .5 = 2.1875 -> 2.188rem`. Measurement uses its requested size;
the drawing raster's hundredth-pixel truncation must not turn 126 into125.99px.

## Two separate original inputs, not a replaced expected result

`font-scale-v1` is the untouched mounted original at its natural default Canvas
font. 12 scenes,44 rows: wrap synthetic lyrics,380/800×600,DPR1,scales1/1.4/2,
normal/one-italic, fixed2400ms, licensed parity font, 10ms manual Motion/RAF.
Chromium151.0.7922.34, Motion13.4.3/dom13.4.2/utils13.3.0, pinned source/font/lock
hashes and complete raw row rectangles are in its conditions.json. Existing
shell/background/subtitle aliases remain; this is not a subtitle comparison.

The initial native/source assertions failed, and are preserved in
`.work/tilt-font-scale-before.log`,`.work/tilt-font-scale-first-fix.log`,
`.work/tilt-scale-raw-measure.log` and `.work/tilt-font-scale-before`.
Natural-default residuals are **still failing** the same .011px font gate:
8 fields, .016px peak. No original fixture or tolerance was changed to pass.
Source's invalid trailing-italic Canvas declaration retains its earlier cached
space; browser default10px sans has space2.958984375/Synthetic43.935546875px,
where this Windows Skia SansSerif has2.7392578125/40.224609375px. Independently
measuring installed Segoe UI in Chromium gives the exact latter metrics.

`font-scale-native-default-v1` is an ADDITIONAL diagnostic, not the natural oracle.
Its host explicitly initializes pretext's1×1 OffscreenCanvas to10px Segoe UI,
then executes the unchanged original source/caches/CSS/Motion. This controls the
initial metric input to match the measured Windows Skia default; no font is copied
or bundled, and original actual lyric font remains the same licensed parity font.
Its44 rows now match native effective font size exactly; width peak .20275px,
height .01565px, all original .011/.51/.03125 gates pass. The extra run confirms
scale arithmetic independently from platform-default fallback metrics; it does
not establish Android/Linux/macOS default-font equality or final visual parity.
The initial controlled run still failed at1.4 due Float half-rounding, preserved
in `.work/tilt-font-scale-controlled-native.log`; shortest-decimal repair fixes
the actual cause instead of changing the source numeric expectations.

## Same natural matrix before/after

`comparison.json` uses one original/before/after non-background union per frame,
including every low-alpha pixel. No registration, alpha cutoff or fitted shift.
The known unused right margin of the800px test root is cropped to the requested
380px viewport; content coordinates are unchanged. Raw frames are in build and
the original before run in.work. The labelled contact only scales thumbnails.

Natural matrix mean foreground MAE84.91247149 ->14.68370515;
font error peak30.672 ->.016px; row-height peak41.406225 ->.03125px.
Full per-frame/channel/local64px results and hashes are retained, including
unchanged scale1 controls. The natural metric failure and rendering/animation
residuals remain; a large aggregate improvement is NOT theme acceptance.
Do not compare the controlled oracle's mean against the natural oracle's mean.

`TiltFontScaleTest` checks controlled original geometry, real published core public
versus internal exact pixels, host/system scale geometry identity, pause, reverse
seek, offset once, mount and drawn-row source-identity clicks. Windows fixed-font
conditions apply; a different system SansSerif needs its own explicit metric
input evidence, not a relaxed assertion. Existing12 TiltParity/3 PublicTilt/2
TiltLayout regressions pass (18 tests including the new one). Android library
and Desktop sample compilation pass. No new device, real window, subtitle or
performance evidence is claimed in this commit.

```
node verification/classic-browser/capture-tilt-font-scale.mjs
node verification/classic-browser/capture-tilt-font-scale.mjs --native-default-metrics
./gradlew :library:jvmTest --tests '*TiltFontScaleTest' --tests '*PublicTiltTest' --tests '*TiltParityTest' --tests '*TiltLayoutTest' :library:compileAndroidMain :sample:desktop-app:compileKotlin
python verification/compare-tilt-font-scale.py
```

Capture refuses to overwrite each original oracle; preserve old data before any
intentional later capture. Before frames use0ed3d4c production TiltLine and the
same native capture inputs; the test records all frames before its final failed
assertion. The next source-conditioned check is the actual subtitle overlay at
large font sizes: this text-only repair does not claim all overlaps are fixed.
