# Public Pendolo / actual lyrics-core comparison

Pinned cc6226e464050a456a2913e062374f23d3844cc5, AGPL-3.0-only.
Original full wheel measurement/JSX and mounted useSpring remain unchanged.
Host inputs are explicitly integer-ms equivalents of real published lyrics-core
models. Original annotateLyricLines generates renderHints; source selection is
latest inclusive active render end, with original fallback anchor. Controller
RAF/WAAPI/performance intervention is as in pendolo-chorus-stage; no audio,
smart-beat/gears/manual scrolling. No upstream checkout modifications.

45 fixed probes across five font/viewport/scale/density-of-lines configurations,
186 source rows. Each native frame is captured from PUBLIC FoliaLyricsView with
actual SyncedLyrics/MainKaraokeLine/KaraokeSyllable (translation and phonetic
included). Before capture, it must equal a separately mounted internal wheel
using the bridge outputs. Internal rows additionally check exact source row
presence/order, fonts, actual active DOM line texts. Rotation-isolation images
are internal controls and clearly separate from public captures.

1302 numeric observations: full box peak6.019072px; source-rotation control
.231502px. Every non-background pixel (including low-alpha halo) contributes to
foreground union metrics, with no registration, alpha cutoff, threshold change
or fitted delay. Public MAE6.137633/peak27.915080; isolation5.550928/10.778672.
Per-frame full MAE, channel peak and worst64px local foreground error recorded.
New sample set/statistic; do not compare this mean with old >8-threshold reports
as an improvement. Contact checked including worst frame; dense tick discrepancy
remains visible. Calibration preview, NOT final visual acceptance.

Exact public/internal images, pause/random seek/remount/scale+resize restore
checks; separate PublicPendoloTest covers no-word original-derived clocks,
transformed click/long-press/blank-parent input, exact original core identity,
offset once, song/theme replacement, reduced-motion/Minimal and disposal.
Sample tests select the real theme without starting demo/altering local session,
and render the demo while preserving its clock on theme switches. Settings
round-trip optional explicit chorus/keyword inputs and old/invalid fallback.

Reproduce:
```
node verification/classic-browser/generate-pendolo-tick-stage-reference.mjs
node verification/classic-browser/serve.mjs
node verification/classic-browser/capture-pendolo-public-stage.mjs
./gradlew :library:jvmTest --tests '*PendoloChorusWheelTest.public*' --tests '*PublicPendoloTest*'
./gradlew :sample:shared:jvmTest --tests '*PendoloSampleTest*' --tests '*SettingsTest*'
python -X utf8 verification/compare-pendolo-public-stage.py
```
Inputs/conditions/frames/rows: verification/pendolo-fixtures/public-stage-*.
Raw PNGs: ignored build/verification/pendolo-public-* (sample screenshots under
sample/build/verification/pendolo-public-sample). Android build/Desktop compile
passed; Compose offscreen only, no device/real window/performance acceptance.
Public contract/known limits: docs/pendolo-preview.md.
