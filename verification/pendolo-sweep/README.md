# Pendolo active-text sweep calibration

Pinned v0.7.26 / cc6226e464050a456a2913e062374f23d3844cc5.
The harness mounts unchanged `PendoloActiveLyricSweep.tsx`, with its real
`pendoloTextLayout.ts`, `graphemeTiming.ts`, `monetLyricsModel.ts` and color-run
helpers. No geometry/curve substitutes. Original React/Motion render real DOM;
only media time is supplied. Two ordinary RAF tasks flush each fixed-media
value; the component has no geometry transition in this isolated non-chorus case.

Four fully covered synthetic scenes (Latin, hyphenated wrapping, Arabic, CJK),
150/300px text widths, 380x480 viewport, DPR1, identical 28px/700 font bytes,
11 media times: 88 source/native PNG pairs. Font hashes and browser version are
in ../pendolo-fixtures/sweep-conditions.json. No SF Pro/media assets.

Native platform wrapping, shaped-segment row widths, grapheme ranges and
whole-prefix advances independently match all 176 measured width/prefix fields
exactly in this matrix. Source Arabic prefix widths are non-monotone; native
preserves them. Native draws complete shaped rows twice (base + shader sweep),
never independently shaped UTF-16 characters. The original pre-wrap row retains
spaces while normal-whitespace prefix measurement hangs trailing spaces.
Native source-syllable EGC reconciliation retains the covering time interval;
this is separately tested and is not claimed identical to source's unsafe split.

Actual source DOM mask stops/opacity are compared to the native sweep (1e-3px
because CSS serializes stops to six significant digits). Original timing compares
at 1e-12; independent row metrics retain 1/64px checks, with actual error zero.
Each of the 88 frames is revisited in shuffled order; pause, resize and remount
are pixel-identical. No renderer-owned clock or running work after unmount.

Initial grayscale native foreground MAE was 8.461170. Reusing the established
Windows browser foreground raster adapter reduces mean to **5.681258961792994**,
peak **9.120120738149138**. Before images/report retained under build/verification/
pendolo-sweep-kotlin-before-lcd and .work/pendolo-sweep-before-lcd.json. Contact
sheet inspected. LCD/gradient compositing and Arabic/CJK raster residuals remain;
**this is not final visual acceptance**, nor complete rotating-wheel/tick/
subtitle/chorus/keyword/device acceptance. No public theme entry yet.

Reproduce from repository root:

```
node verification/classic-browser/serve.mjs
node verification/classic-browser/capture-pendolo-sweep.mjs
gradlew :library:jvmTest --tests "*PendoloSweep*"
python verification/compare-pendolo-sweep.py
```

The original Monet module also imports pretext/rich-inline. The fixture server
now resolves that actual installed export separately (the former broad root
alias incorrectly appended /rich-inline to the root JS file). No dependencies
were upgraded or source modules stubbed. Initial timeout is preserved in
.work/pendolo-sweep-source.log. Android APK/Desktop compilation pass; actual
native UI execution here is Desktop offscreen, no connected Android device.
