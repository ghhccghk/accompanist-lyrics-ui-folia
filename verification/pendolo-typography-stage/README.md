# Pendolo independent typography scales

Pinned Folia cc6226e464050a456a2913e062374f23d3844cc5 / AGPL-3.0-only.
Original measurement declarations and complete wheel JSX are unchanged. The
existing extraction host now accepts optional caller lyricsFontScale and
subtitleFontScale (both previously fixed1). Dedicated headless host uses same
synthetic lyrics, OFL fonts, palette, 600px height, DPR1, normal intensity:
Latin380/800 lyric1.5/subtitle1; CJK380 lyric1/subtitle1.75; Arabic800 lyric.75/subtitle1.5.
Four fixed media times each; controlled original RAF/WAAPI method inherited
from pendolo-palette-stage, without changing upstream logic. No gears/audio.

16 original/native pairs,58 rows. Main/subtitle font sizes and actual active
DOM line texts exactly match.406 numeric fields: content-box peak .010182px;
foreground MAE6.604243, peak10.092899. Native/source-rotation isolation recorded
separately. Not final visual acceptance: existing rotation/event/raster residuals
remain. Full-screen MAE alone is not an acceptance threshold; contact inspected.

The initial large-font narrow case was64.125px short: platform breaker packed
part of the next oversized word, while original pretext tests the whole next
word. The independent ASCII active segment walker now preserves that rule and
preferred hyphens, still shaping complete rows. Same16-frame source matrix:
MAE16.078982→6.604243; no alignment, sample replacement or tolerance changes.
Initial PNGs remain build/verification/pendolo-typography-before-wrap; initial
report .work/pendolo-typography-before-wrap.json. First hyphen regression failure
is in .work/pendolo-typography-wrap.log; source expected rows retained and fixed.
Non-ASCII uses platform wrapping, not a claim of general pretext equivalence.

Random seek/pause/remount and live width+scale change/restore are pixel identical.
Existing88 sweep controls pass; all24 re-rendered default-scale palette PNGs have
unchanged hashes. Two-end builds pass (.work/pendolo-typography-wrap-final.log).
Android device/real Desktop window and performance not measured.

Reproduce:
```
node verification/classic-browser/generate-pendolo-tick-stage-reference.mjs
node verification/classic-browser/serve.mjs
node verification/classic-browser/capture-pendolo-typography-stage.mjs
./gradlew :library:jvmTest --tests '*PendoloChorusWheelTest.typography*' --tests '*PendoloChorusWheelTest.palette*' --tests '*PendoloSweepTest*'
python -X utf8 verification/compare-pendolo-typography-stage.py
```
Raw PNGs are ignored build/verification/pendolo-typography-*; conditions/input/
row evidence in verification/pendolo-fixtures/typography-stage-*.
Public Pendolo entry and transformed interaction remain the next work.
