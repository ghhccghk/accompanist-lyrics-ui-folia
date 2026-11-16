# Tilt controls

Fixed Folia cc6226e464050a456a2913e062374f23d3844cc5; source ledger in
docs/upstream.md. Synthetic lyrics and previously registered OFL fonts only.

- `generate-tilt-sentences.mjs`: executes original SentenceLayout.
- `generate-tilt-layout.mjs`: original private declarations, synthetic widths.
- `capture-tilt.mjs`: actual original mounted component; normal/tilt/single,
  Latin/Arabic, 380/800 × 600, DPR1, 12 media timestamps, 144 frames.
- `TiltMotionTest`: 258 mounted row and 3882 character Motion targets. Feeds
  original stagger amplitude to isolate animation from font/layout calibration.
  It does not assert equivalence of computed CSS transforms (60ms transition).
- `TiltParityTest`: internal Compose stage, text partitions, paused and reverse
  seek pixels, geometry report. Geometry residuals are not waived.

Start `node verification/classic-browser/serve.mjs`, then run
`node verification/classic-browser/capture-tilt.mjs` in another terminal.
Run `./gradlew :library:jvmTest --tests '*Tilt*'` and
`python verification/compare-tilt.py`. Keep both original and candidate evidence.
These controls do not establish Android device or real Desktop-window parity.

2026-11-16 follow-up: public/sample Tilt calibration preview and original 450ms
exit retention are implemented. `capture-tilt-geometry.mjs` adds fractional CSS
content widths without changing integer reference fixtures; `capture-tilt-exits.mjs`
records the actual parent exit. Both require the same server. PublicTiltTest checks
adapter/core pixels, pause, reverse seek, offset once, resize, hit/long press and
the 450ms retained exit. Latin row widths compare against mounted-geometry.tsv
at 0.51 CSS px; Arabic is reported, not silently accepted as equivalent.
Current evidence/residuals: verification/tilt-public and docs/themes-parity.md.
