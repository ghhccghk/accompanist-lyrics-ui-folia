# Pendolo retained idle max-width transitions

Pinned cc6226e464050a456a2913e062374f23d3844cc5 / AGPL-3.0-only.
Original `VisualizerPendolo.tsx` inactive main DIV uses `transition-all duration-200`
while subtitle rows use `transition-opacity`. Native now preserves this distinction:
only retained inactive main nodes interpolate maximum width. Active/inactive
element replacement and rows remounting start at their actual target. Subtitles
immediately use their own constraint, even while the parent main width moves.
Intrinsic parent width is the maximum of those separate contributions.

`PendoloWidthTrack.kt` prepares media-boundary responses and reconstructs current
width on seek. Original .2s and locked Tailwind4 cubic-bezier(.4,0,.2,1) are
preserved, including CSS reversing-shortening rules (CSS Transitions1 §3,
https://www.w3.org/TR/css-transitions-1/#starting).
No browser/fixtures or display frames are input to production. Focal reserved
heights are measured once per text/font/viewport configuration. Paragraphs retain
complete shaping and internal timing. Quantized width changes can require new
layout during200ms; whole-song preparation/layout cost is **not yet measured**.

Complementary actual Blink CSS control covers settled, interrupted, repeated
reverse and unchanged targets.248 max-width fields including shuffled revisits
peak0.000510644px;138 active duration fields peak2.637982e-8s. Assertions .001px
(computed CSS decimal serialization) /1e-5s. This control supplements, not replaces,
the original144-frame mounted JSX matrix. Overlap/pause/seek/rebuild/reduced-motion
logic also passes. Evidence: `comparison.json`, `width-response*.{tsv,json}`.

Reproduce (fixture server already running):

```
node verification/classic-browser/capture-pendolo-width-response.mjs
node verification/classic-browser/capture-pendolo-tick-stage.mjs
./gradlew :library:jvmTest --tests '*Pendolo*'
python verification/compare-pendolo-tick-stage.py
```

Current mounted144-image results are in `verification/pendolo-tick-stage`.
Same-source-rotation box peak17.652888→0.105115px; independent tick+box peak
17.652888→1.364707px. Untransformed box peak0.03125px: gap transitions can begin
on the browser's next observed frame, while native uses exact semantic media
events. Do not call this exact mounted-geometry parity. Wheel0.325107° remains.
Mean foreground MAE13.235477→13.137682 (primary) and13.087267→12.989472
(source-rotation isolation); unchanged peaks23.317442/20.502865 retain the
dominant raster/gradient discrepancy. Inspected updated contact sheet.

Comparison uses the **same current source PNG for both old and new native**.
Adding passive computed-width observation changed4/144 original PNG hashes at
Arabic/CJK exact starts; tick values and every geometric row TSV were unchanged.
We do not claim all source hashes stayed identical. The report recomputes both
baselines against the current source. Original old native PNGs/residuals are in
`build/verification/*-before-width-transition`; previous report retained at
`.work/pendolo-width-original-baseline.json`. The script includes these baseline
folders only when available; a clean checkout can regenerate current evidence.

All existing88 sweep/80 static stage native PNG hashes remain unchanged.
Full Pendolo tests, Android debug APK and Desktop compile pass
(`.work/pendolo-width-final.log`). No connected-device claim or public selector.
Chorus/keywords, residual text coverage, mounted clock sampling and public/core
integration remain open; not full theme acceptance.

The preceding integration/chorus status is historical before b0ed98e. Pendolo is
now public/sample calibration preview. 2026-11-18 exact-lock audit retained all
old evidence and reproduced byte-identical CSS-width fixtures; see
`verification/motion-lock-audit/pendolo-dense.json`. Width probes do not certify
the mounted Motion spring or resolve its remaining rotation discrepancy.
