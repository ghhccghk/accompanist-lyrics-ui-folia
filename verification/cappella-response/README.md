# Original Cappella response kernel

Pinned Folia cc6226e4 actual `getCappellaIntensityConfig`, locked
framer-motion13.4.3 / motion-dom13.5.1 `animateMotionValue` dispatcher.
Generate: `node verification/classic-browser/generate-cappella-response.mjs`.
Verify: `gradlew.bat :library:jvmTest --tests '*CappellaResponseTest*'`.
Response inputs/original values live in cappella-fixtures/response.tsv;
independent Kotlin residuals and maximum errors are retained here.

1134 observations cover three original profiles: row enter/exit opacity,y,scale;
0.2s resize;0.18s timestamp;mass-bearing avatar scale/margin springs with zero
and nonzero initial velocities. Raw analytic spring position/velocity and rest
decisions also checked. Calm is slightly overdamped, not approximated by the
existing classic underdamped-only helper. Native immutable event tracks preserve
position/analytic velocity, retain unchanged in-flight targets and support
arbitrary reverse/shuffled seek without accumulating render frames.

Initial submillisecond tests exposed an incorrect assumption: ordinary Motion
RAF ticks round milliseconds, but `JSAnimation.sample` explicitly bypasses that
step. The new media response samples the continuous curve; no source query or
tolerance was changed. Native normal exit duration also corrected from the
initial mistaken0.25s to original0.24s. Failure diagnostics remain in report.
Motion uses a12-subdivision/1e-7 cubic inversion, unlike browser CSS's
higher-precision inversion in the existing glyph calibration.

This proves individual original configured responses, NOT full mounted
row/layout/resize interruption history or theme animation acceptance. Original
RAF tick quantization, finite-frame event sampling, layout projection and
inherited velocity history still need complete-stage comparison. No Motion JS
runtime/source is bundled into the library, no new native dependency.
