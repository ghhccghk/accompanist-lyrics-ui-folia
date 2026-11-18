# Diorama complete-shaped mask ownership

Pinned Folia `cc6226e464050a456a2913e062374f23d3844cc5`, AGPL-3.0-only.
This repairs the internal text adapter's lost/duplicated ink; it does **not**
establish complete original per-unit theme equivalence or open a public theme.

## Independent reference and scope

The dedicated browser host calls unchanged original `rasterDioramaUnit` with a
COMPLETE run, plus `splitDioramaLyricUnits` and `buildLineGraphemeTimeline` with
the actual source fragments/times. This supplies an independent complete-shaping
reference and unit boundaries. It is explicitly not the original isolated-unit
Three stage: Folia normally rasterizes each split unit separately. That stage's
existing 90/180/54 images remain the visual oracle, not replaced by this probe.
`../diorama-fixtures/shaped-mask-v1/conditions.json` records original/source host,
lock/font hashes, Chromium151.0.7922.34, Motion13.4.3/dom13.4.2/utils13.3.0,
128px white text, middle baseline, transparent background and 90px padding.
The supplied italic face is synthetic italic of the recorded Latin font.

Real published lyrics-core lines produce the four test scenes: a ligature split
between f/i, Arabic split across parser fragments, mixed Latin/Arabic/digits,
and CJK. Original unit text/ranges/times are asserted independently. Original
object identity and every internal clock are retained; no UTF-16 glyph drawing.

## Failure and repair

Clipping complete text to each unit's advance bounding rectangle loses italic
overhang. A mixed-bidi logical range can span unrelated visual glyphs and paint
them twice. A rejected transparent colour-span mask preserves layout size but
breaks part of the cross-unit fi ligature: a colour boundary can split shaping.
Both controls are retained in the test/output, not substituted for the product.

`DioramaShapedSlices.kt` assigns every integer column of the ONCE shaped/rasterized
paragraph to its nearest visual grapheme-selection interval. Disjoint ownership
avoids clipping away or double-painting coverage, including padding. A logical
unit can own disjoint visual intervals. Integer crops keep the canonical raster
phase; `imageOffsetPx` retains the old unit pivot under actual soul scaling.
This is an explicit coverage-ownership adaptation for complete shaping. It does
not prove that a cross-boundary ligature's individual animated colour ownership
equals the original separately shaped units. That residual requires stage-level
comparison, not an assertion that keeping total coverage solves every case.

| Scene | Advance-clip changed pixels / lost alpha / extra alpha | Rejected spans | Product vs own complete paragraph |
| --- | --- | --- | --- |
| italic | 756 / 154131 / 0 | 327 / 79122 / 0 | 0 / 0 / 0 |
| Arabic | 32 / 1178 / 1 | 0 / 0 / 0 | 0 / 0 / 0 |
| mixed bidi | 1504 / 1004 / 79624 | 0 / 0 / 0 | 0 / 0 / 0 |
| CJK | 0 / 0 / 0 | 0 / 0 / 0 | 0 / 0 / 0 |

Alpha sums are 8-bit coverage sums, not pixel counts. Test also checks exactly
one owner per texel column, integer reconstruction origins, unchanged unit pivot
and pivot invariance at scales0.8/1/1.3. The full own alpha gate is EXACT, not a
new tolerance for original visuals. Complete original Canvas foreground MAE,
same original/control/product union: italic9.200091→3.435579,
Arabic2.992106→2.958575,mixed3.863731→3.040748,CJK4.250570 unchanged.
Contact visually checked: clipped f/i/j and duplicate mixed-run ink are removed.

## Existing stage regression — retained, not waived

Before rendering the candidate, complete old native matrices from2ce67b1 were
saved in `.work/diorama-shaping-before/{active-base,active-layers,neighbors}`.
All original images/fixtures, camera inputs, times, colours and fonts stayed
unchanged. Same original/before/after foreground union includes every low-alpha
pixel; no registration, threshold, delay, frame selection or expected update.
Every frame's hashes, errors and regressed-frame list are in `comparison.json`.

| Matrix | Frames | Foreground MAE before→after | Worst frame | Worst64px tile |
| --- | --- | --- | --- | --- |
| active-base | 90 | 6.604446→6.642979 | 18.145734→18.194899 | 26.873016→26.238095 |
| active-layers | 180 | 4.425606→4.451274 | 18.145734→18.194899 | 26.873016→26.238095 |
| neighbors | 54 | 6.614592→6.618104 | 12.806258→12.791817 | 22.985632→22.706897 |

These mean regressions are real. CJK800/5.25s increases6.409256→6.550508 and
Arabic800/5.25s8.840409→8.980776; source/before/after inspected at the same
viewport. Original unit pivots, full-run advance and scale mapping are preserved,
but canonical raster phase/crop coverage and halo preparation now differ from
the previous fractional per-unit raster. Their separate contributions to these
stage residuals are NOT yet isolated. Do not call the whole stage visually
improved, classify the increase as proven backend-only, or waive its repair.
The reason to keep this correction is independently demonstrated full-shaping
coverage conservation, not an aggregate image score. Diorama remains internal.

## Reproduction and limits

```
node verification/classic-browser/capture-diorama-shaped-mask.mjs
./gradlew :library:jvmTest --tests '*DioramaShapedMaskTest' --tests '*DioramaNeighborStageTest' --tests '*DioramaActiveTextTest' --tests '*DioramaActiveLayersTest' --tests '*DioramaTextPlaybackTest' --tests '*DioramaTextTest'
python verification/compare-diorama-shaped-mask.py
./gradlew :library:compileAndroidMain :sample:desktop-app:compileKotlin
```

Use gradlew.bat on Windows. Source generator refuses to overwrite the independent
fixture: preserve it first for a deliberate re-audit. Stage comparator additionally
requires the preserved2ce67b1 native outputs and existing original stage captures;
it fails if any matrix/frame or exact-coverage gate is absent. These large PNGs
are reproducible ignored build/work files; hashes and compact contact retained.
JVM offscreen, Android library compilation and Desktop compilation passed in
`.work/diorama-shaped-mask-matrix.log`. No new device, real-window, audio,
performance, long-song memory, fog/depth or complete CameraRig acceptance.
Preparation remains outside the display loop. Full-song texture ownership and
very long active-line bounds remain unresolved before a public theme.

The neighbor fixture's empty-window list now serializes as `-` rather than a
trailing TSV tab. Source numbers and images are unchanged; this corrects a
whitespace defect in the earlier evidence file, not its expected values.
