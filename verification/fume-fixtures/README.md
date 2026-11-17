# Fume timing oracle

Source: Folia v0.7.26, `cc6226e464050a456a2913e062374f23d3844cc5`,
AGPL-3.0-only. No upstream songs, artwork or fonts copied.

Regenerate with `node verification/classic-browser/generate-fume-reveal.mjs`.
The script verifies a clean pinned `.work/folia-major` and directly imports
`fumeReveal.ts`, `fumeTextStyle.ts`, `fumeMath.ts` and `renderHints.ts` through
the existing Vite reference loader. It does not rewrite the expected functions.

- `inputs.tsv`: 18 synthetic lines, base64 UTF-8 text, source words/syllables.
- `ranges.tsv`: 30 exact word ranges and internal grapheme clocks.
- `samples.tsv`: 1,584 boundary/random samples: integer printing, continuous
  focus progress, next-line cutoff and render-end cutoff.
- `metrics.tsv`: 494 cutoff/glow/retained-text cases, including non-finite
  guards and the source's last-by-start (not maximum-end) hold duration.

`FumeRevealTest` evaluates every sample forward and backward. Complex grapheme
fragments split by lyrics-core are tested separately: native adaptation joins
only unsafe boundaries and retains all original fragments inside the word.
This is not a screenshot fixture and proves no article/camera/render parity.

The source's global duration memo rounds keys to three decimal places; the
generator clears it before each duration query. The port computes the result
without that history-dependent memo. Printing still respects all source
rounding/minimum durations. Infinity in `resolvePassedDimAmount` returns 1
in the original; a later renderer must preserve the caller's hold-style logic
rather than interpreting this helper alone as the complete hold behavior.

## Article geometry

`node verification/classic-browser/generate-fume-article.mjs` executes the original
`fumeArticleLayout.ts` generator and the original `chooseSingleLineFontPx`
declaration. Only its text measurement import is replaced with a labelled seam:
each EGC advances `fontPx * .57`, hard newlines are honored, and each candidate
is measured directly. Unused render-detail imports return no render lines;
the source's placement loop and final block construction remain intact.
`article-inputs.tsv` and `article-layout.tsv` cover 96 layouts / 1,560 blocks,
including empty blocks, fallback heroes, chorus heroes, Unicode and long lines.
These are numerical algorithm fixtures, not a substitute for actual fonts.

With the existing browser server running, use
`node verification/classic-browser/capture-fume-article.mjs` for actual unmodified
`buildArticleLayout` and `createStaticBlockSnapshot`, the registered OFL Latin
font, DPR1, 380/800/1440 by600. Six articles / 150 real-font blocks are recorded
in `article-browser.tsv` and `build/verification/fume-article-browser`.
`FumeArticleFontTest` applies the same font and viewport in Compose, uses the
library's FumeTextMeasure, and writes candidate pictures/CSV into
`build/verification/fume-article-kotlin`. Article/block geometry and font sizes
are checked at 1e-8, without widening the goal after initial failures.

`python verification/compare-fume-article.py` creates the committed contact and
raw-position comparison in `verification/fume-article`. The source pictures
use actual original static snapshots; native pictures are a geometry diagnostic
using drawText, with no final camera, active glow, baseline calibration or
hero text inset. Both include diagnostic block outlines. Current mean RGB
foreground-union MAE70.5127 is **not accepted visual parity**. The source's
middle-baseline/static snapshot renderer remains the next rendering task.

The failed initial integer intrinsic-width candidate is retained in
`.work/fume-article-font-first.log`; the failed hanging-space-only and
fractional-width candidates are also retained. The passing candidate measures
fractional shaped line advance, hangs trailing ASCII spaces for fit only, and
uses hundredth-pixel font quantization. Source text/timings are never trimmed.
Browser font memo heuristics were not copied; more fonts/scripts and Android
runtime still require their own comparison.

## Static snapshot follow-up — 2026-11-17

The paragraph above describes the initial article-only commit3224bb6. The
current native candidate uses FumeStaticText with complete shaped row bitmaps,
Canvas middle baseline and the source hero inset. The matrix now includes a
long synthetic Latin paragraph and explicit hard newline: 120 synthetic-seam
layouts / 1,632 blocks; nine actual-font articles / 159 blocks / 289 render rows.
`article-baselines.tsv` holds159 actual Canvas middle shifts and
`article-rows.tsv` the source row text/geometry. Native checks use1e-8 for
article/block/font/baseline values and1/64px for row advances/insets. Actual
maximum row width error is0.0007893px; maximum inset error0.00003052px.

Row text, row count and breaks match exactly, including hanging spaces. The
source em ascent and total em must be rounded separately to1/64 before the
midpoint; the resulting shift can be on1/128. The initial whole-offset rounding
failed and remains recorded in `.work/fume-static-baselines.log`. A subsequent
row test caught native trailing-space trimming and extra wrapping; both were
fixed without changing the assertions (`fume-static-rows*.log`).

The original six frames now average3.1370 foreground RGB MAE (previous70.5127).
All nine average6.0209, worst13.7049; no registration or rescaling is applied in
the numerical comparison. Contact sheet thumbnails are only for inspection.
Static snapshots remain short of final visual parity, especially multiline
raster/compositing and fonts with differing hhea/OS/2 typo metrics. Live effects,
camera, other scripts and Android runtime remain pending. The comparison JSON
includes browser version, font hash and hashes for every full-size PNG.

## Live draw recording — 2026-11-17

Run `node verification/classic-browser/generate-fume-frame.mjs` to regenerate
`frames.tsv` / `glyphs.tsv`. The original `drawFumeLiveBlock`, block timing,
static-layer resolver, text styles and color helpers execute unchanged. The
Canvas records paint/rect operations; each synthetic segment is one EGC with
explicit advance `8.5 + index % 3`, so every glyph paint can be inspected.
This is a controlled animation/paint seam, **not** a shaping or raster comparison.
1,104 frames, 8,970 glyph paints, 317 stamps. Source checkout must be clean and
fixed at the SHA in every fixture header. Native timing, RGB/alpha, shadow radius,
stamp geometry, crossfade and overview restoration compare at 1e-8; reverse
sampling and disabling stamps must preserve the remaining values.

The complete Fume renderer, camera and public integration remain unfinished.

## Live text raster comparison

Start `node verification/classic-browser/serve.mjs`, run
`node verification/classic-browser/capture-fume-live.mjs`, then
`:library:jvmTest --tests '*FumeLiveTextTest'` and
`python verification/compare-fume-live.py`. Uses actual original article blocks
and untouched original live/static Canvas draw code. `live-geometry.tsv` is an
explicit shared geometry input so this matrix isolates text rendering from
article selection and camera. Three synthetic Latin scenes (parser words,
uneven syllables, long wrapped paragraph + hard newline), two viewport widths,
two glow settings and fourteen media times: 168 pairs. Licensed font/hash,
browser version, all original/native PNG hashes and unregistered RGB errors
are in `verification/fume-live/comparison.json`; images in `build/verification`.

Mean foreground MAE 2.9653, maximum 9.8170. Not final visual acceptance.
Native mask compaction changed zero PNGs; random revisits and pause checks
are pixel identical. No Android device is attached, so Android runtime,
other scripts, moving CTM and the complete camera remain unverified here.

## Camera target geometry

`generate-fume-camera.mjs` executes unmodified `fumeCamera.ts` and block
visibility code. `camera-blocks.tsv` / `camera-rows.tsv` give the explicit
synthetic geometry seam. `camera-targets.tsv` contains 1,253 original samples:
entry, stepped/smooth focus, cross-row window, scale, retarget durations,
overview fit, source focus selection, flight bridges and overscan. Kotlin
checks every value at 1e-8 and repeats in randomized order. Not evidence of
the subsequent spring trajectory or full-theme raster equivalence.

The original smooth focus returns from the next row to the previous one
across the exact row boundary. Source samples now include boundary ±1e-8;
this target discontinuity must not be silently replaced by an invented curve.

## Original spring/bridge operator

`generate-fume-camera-step.mjs` runs the untouched original `stepFumeCamera`
at explicitly recorded 30/60/144 Hz and irregular time steps; 16 scenarios,
8,696 steps, pose/velocity/focus/retarget/bridge/restore values. All compare
at 1e-8, and the port must not mutate its input. Both static and idle-float,
smooth and stepped modes, speed extremes and long flights are exercised.
`camera-step.tsv` is sequential source evidence, **not** the native playback
time model or proof that original frame-dependent motion is seek-rebuildable.

## Rejected adaptive media trajectory

`generate-fume-camera-track.mjs` uses the actual original operator with an
explicitly adapted event-split media driver and separately records original
60Hz finite-step trajectories. Run `:library:jvmTest --tests
'*FumeCameraTrackExperimentTest'` then `python
verification/compare-fume-camera-track.py`. The driver is test-only. The
360 adapted-source samples reproduce within1.4e-10 and arbitrary query order
does not change results, but actual original60Hz residual reaches107.45 world
px / scale0.06265, with >5,000 prepared nodes for8s. **Rejected for runtime**;
not a completed camera, not a new acceptance criterion, not included in sample.

## Continuous frozen-coefficient response

`node verification/classic-browser/generate-fume-spring-response.mjs` records
untouched source steps with fixed target/phase and 25 source coefficient sets
into `spring-response.tsv` (125 samples, 375 axes).
`:library:jvmTest --tests '*FumeSpringResponseTest' --tests '*FumeCameraStepTest'`
checks the analytic response (maximum absolute error 1.1674e-12), partition
independence, zero-duration freeze, derivative, coincident eigenvalue and
hour-long duration. No display clock is used. This isolates constant-coefficient
response, not a dynamic camera or integrated-theme screenshot comparison.

## Continuous dynamic media-camera calibration

`node verification/classic-browser/generate-fume-camera-continuous.mjs` writes
`camera-continuous.tsv`: actual source30/60/144Hz/irregular poses alongside an
explicit continuous adaptation using original target observations. Run
`:library:jvmTest --tests '*FumeCameraTrackTest'`, then
`python verification/compare-fume-camera-continuous.py`. Conditions, both SHA256,
source-rate residuals and trajectory plot live in `verification/fume-camera-continuous`.
No screenshot acceptance gate is replaced by this numerical seam.

Native queries use immutable prepared nodes; no fixed-FPS playback or display
history. The 1/60 calibration interval identifies one source response. The
current1e-7px local integration tolerance was tightened after the first coarse
candidate failed the unchanged1e-5 numeric comparison near discontinuities.
Exact source expectations remain unmodified. Sample/public theme integration
and whole-song preparation/runtime measurements are still pending.

## Actual article + camera + text scenes

With the local headless fixture server running,
`node verification/classic-browser/capture-fume-scene.mjs` captures144 original
scenes and writes `scene-camera.tsv`. Run `:library:jvmTest --tests
'*FumeSceneTest'`, then `python verification/compare-fume-scene.py` and
`python verification/compare-fume-scene.py --native-dir
build/verification/fume-scene-kotlin-source-camera --out
verification/fume-scene-source-camera`. The latter deliberately shares source
poses and isolates raster only; it does not verify the native camera. Both
series use independently measured native article/focus geometry.

Conditions/source and native hashes, unregistered pixel errors, camera residuals
and contact sheets are recorded. `raster-correction.json` retains initial and
corrected statistics with the identical144 original PNG hashes. First native
PNGs remain locally in `build/verification/fume-scene-kotlin-first` and
`fume-scene-kotlin-source-camera-first`. No time/position registration, fitted
colors, user lyrics or unlicensed font resources. This is calibration evidence,
not Android runtime or completed-theme acceptance.

The fractional-destination follow-up adds801x600:216 scenes per camera path,
432 random revisits and pause checks. Previous144-frame source PNG hashes
remain unchanged. `fractional-contact.png` shows801px source/shared/native
columns. Full/raster means15.811750/3.427406 belong to this expanded matrix;
they must not be presented as improvements over the older144-frame matrix.
