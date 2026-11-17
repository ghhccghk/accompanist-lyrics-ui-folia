# Cadenza fixed-source calibration

## Public Cadenza preview (2026-11-17)

With fixture server, run `capture-cadenza-public.mjs`,
`:library:jvmTest --tests '*Cadenza*'`, and
`python verification/compare-cadenza-motion.py --public`.
32 actual original RAF/public FoliaLyricsView pairs share integer-millisecond
media snapshots,exact OFL font bytes,DPR1 and viewport. No rate-specific phase
fit, target settling, image registration or source function replacement.
Source state snapshots and PNGs are retained in build/verification; reports and
contact sheet in verification/cadenza-public. Repeated source raster captures
have a small MAE fluctuation near6; do not treat rerunning source images as a
native improvement or acceptance threshold. Public preview/lifecycle tests,
data fallbacks,quality/memory scope and unresolved original cross-line shared-id
state are documented in docs/cadenza-preview.md. Preview is not final parity.

## Original line selection (2026-11-17)

Run `node verification/classic-browser/generate-cadenza-timeline.mjs` (Node24,
builtin stripTypeScriptTypes), then `:library:jvmTest --tests '*CadenzaTimelineTest'`.
The exact original findLatestActiveLineIndex declaration is extracted from the
clean pinned source and only TS annotations are erased. Its SHA256 is recorded;
no app settings, i18n or network module initialization is needed.12 scenarios/
1269 source observations cover absent/annotated hints, empty, gaps, zero/micro/
short lines, overlaps/resumption, same starts, unsorted arrays and inclusive ends.
Native active/recent/upcoming indices agree exactly in sequential, randomized
and reverse queries. Mount epoch on resumed older lines is reconstructed from
canonical media boundaries rather than prior screen history. No classic extra
exit retention. Logic verification only; host composition and actual multi-line
source/native rendering are separate work, not claimed by this fixture.

## Chorus circles and keyword colors (2026-11-17)

Run `node verification/classic-browser/capture-cadenza-accents.mjs`,
`:library:jvmTest --tests '*Cadenza*'`, and
`python verification/compare-cadenza-accents.py` with the fixture server.
Original Canvas.arc parameters and unrounded strokeStyle assignments are
observed while the original component remains unchanged.38 stage-space circles
agree in center/radius/Canvas-stored float stroke within1.706e-13,alpha1e-10.
The source writes1.2px but Canvas reads1.2000000476837158; native Stroke also
uses Float. Initial test compared Double1.2 with the Canvas getter and failed;
the actual Float drawing representation is now compared, not a looser epsilon.
17 keyword probes import the actual original resolver, including CJK containment,
Latin punctuation/case,FEFF trim,NEL nontrim,Arabic/accent and empty fallbacks.

Four same-font synthetic scenes,380/800x600,DPR1,chorus=true,energy0,glow1,
alternate primary/accent and4 keyword colors,8 settled times each:32 pairs.
Mean foreground RGB MAE6.991457 (Latin6.507372,wrap6.715800,Arabic4.373869,
CJK10.368786). Contact sheet inspected; no transform/time registration.
Pause/random native revisits are pixel identical. All128 earlier default
target PNGs stay byte identical. Not an acceptance threshold or completed theme;
internal chorus is explicit/defaultfalse, not inferred from core accompaniment.

## Continuous playback response (2026-11-17)

Run the local fixture server, then
`node verification/classic-browser/capture-cadenza-motion.mjs`,
`:library:jvmTest --tests '*Cadenza*'`, and
`python verification/compare-cadenza-motion.py`.
Original component formulas/drawing are unchanged. A test-only Map.set observer
copies the eight animated fields after original updates; the harness advances
RAF and media together, unlike settled target probes. Initial mount must have
exactly one original1/60 update per word. Four synthetic scenes at380/800x600,
same OFL fonts/DPR1,energy0/glow1;30/60/144Hz and irregular cadences produce
16 trajectories/9113 word observations and32 actual60Hz/native screenshot pairs.

Native piecewise cubic/sinusoidal forcing uses analytic exponential integration;
event nodes are independent of display cadence. Immediate mount matches1e-10,
constant channels1e-12, integral moments against independent Simpson quadrature
1e-10; finite long/reversed/zero/instant timing and randomized revisit/freeze pass.
Truncating preparation at each query gives identical response. No source-rate
phase correction or fixed-FPS frame replay is used. Original RAF endpoint sampling
remains cadence-dependent:60Hz maximum position0.14666px/rotation0.62183deg/
scale0.04071/alpha0.03784/blur0.37831px/mix0.02617/glow0.06084;
other rates and each scene are fully reported in comparison.json.

32 image pairs mean foreground-union RGB MAE6.024388 (Latin5.539713,
wrap5.406618,Arabic3.888200,CJK9.263023). Contact sheet inspected. Native
random revisits and pause images strictly identical. This is a distinct playback
matrix, NOT an improvement comparison against128 settled target images.
Cadence differences, CJK/raster/coverage differences, chorus ripple, configurable
colors and host line switching remain unresolved. Internal calibration only,
no public/sample entry or final theme acceptance; Android runtime unverified.

Reference: Folia v0.7.26 / cc6226e464050a456a2913e062374f23d3844cc5.
All text is synthetic, no application lyrics or SF Pro resources.

Run `node verification/classic-browser/generate-cadenza-envelopes.mjs`, then
`:library:jvmTest --tests '*CadenzaEnvelopesTest'`. The generator imports the
untouched upstream functions, verifies clean pinned checkout, and records
30 absent/annotated/custom hint scenarios,2952 word-time observations and
20 CSS shadow probes. Numeric tolerance1e-10; status exact. Random revisits
use the same observations, not newly fitted expected curves.

Includes zero/reversed durations, exact lookahead and reveal boundaries,
word/line tails, custom render ends and zero/reversed explicit glyph times.
Absent hints intentionally stay normal: source Cadenza does not implicitly
call getLineRenderHints. BlurScale is unused by original buildDomTextShadow;
all three CSS layers remain40px.

This proves pure timing only, NOT actual rendered text, camera/placement,
Android runtime, sample integration or final theme equivalence.

## Seeded fragment placements

Run `node verification/classic-browser/generate-cadenza-placements.mjs`, then
`:library:jvmTest --tests '*CadenzaPlacementsTest'`.108 source scenarios /450
placements cover all intensities, positive/negative/fractional seeds, intact
hero, split-word exclusion, CJK/combining/ZWJ words, ECMAScript blank text,
dense collision fallback and interlude mode. Maximum field error3.638e-11.
Inputs and original placement order/id/geometry/drift are retained.

Fragment geometry is an EXPLICIT input seam; this does not establish actual
browser-vs-Compose wrapping, word-fragment construction or visual equivalence.
Source fallback may overlap when space is exhausted; the port must not
rewrite expectations as a blanket no-overlap invariant.

## Actual browser preparation / fragment bridge

With the local headless fixture server, run
`node verification/classic-browser/capture-cadenza-geometry.mjs`, then
`:library:jvmTest --tests '*CadenzaFragmentsTest'`.114 preparations of19
synthetic lyrics use180/380/800px and font scales1.12/.85, same bundled OFL
Latin/Arabic/CJK fonts as recorded in geometry-conditions.json. Source
buildPreparedState/pretext0.0.9 supply actual segments, widths, wrapping,
word ranges and final placements. Kotlin independently reconstructs ranges,
word-relative indices, split identities and placements FROM those explicit
source segment/wrap inputs.205 final placements (2 split fragments), maximum
numeric error1.137e-13. This is NOT independent native font layout evidence.

The lyrics-core adapter retains raw syllables and only joins parser fragments
that cut a single EGC; combining accent and astronaut ZWJ tests preserve
source times. Whole words still remain available for shaping. Default tuning
fontScale1.12/widthRatio.72 matches the source. Source ingestion annotations
are a separate explicit step; resolveLineRenderTiming absent-hint behavior
remains unchanged.

## Independent Compose font layout and actual mounted target images

Run `node verification/classic-browser/capture-cadenza-targets.mjs`,
`:library:jvmTest --tests '*CadenzaTargetStageTest'`, then
`python verification/compare-cadenza-targets.py`. Actual original Cadenza is
mounted in React, with its DOM/Canvas methods unchanged. Fixed media time
plus210 original RAF iterations isolates its converged targets/raster; it
does NOT validate native smoothing, pause transitions or host line switching.
The shell forwards its DOM ref for the original ResizeObserver; a host-only
settings selector supplies the original default resolution1/antialias auto
to the untouched quality resolver, avoiding unrelated settings/background
registries. No Cadenza frame/layout/fragment/glow function is replaced.

Four scenes x380/800x600 xglow0/1 x8times =128 original/native pairs, same
recorded OFL font bytes. Kotlin independently measures and wraps;8 geometry
sets preserve all fragment ids/order with maximum field delta9.156e-5px
(assertion1e-3px accounts for floating-point platform text measurement).
First candidate mistakenly hung trailing space via getLineRight; corrected
selection width preserves it.128 source hashes unchanged, mean foreground
MAE26.428939→9.971613. By scene:Latin7.070232,wrap7.404520,Arabic5.754925,
CJK19.656774. First/current reports/contact sheets retained. Contact sheets
inspected; pixel residuals are NOT a completed-theme acceptance. CJK paint
baseline/coverage and glow raster calibration remain.

Native targets support deterministic random revisits and media-time freeze,
whole-fragment body shaping and per-EGC clipped shadow masks, source line
opacity/blur/scale and ordered placement targets. Masks are bounded per
prepared fragment, lazy and not time-keyed. This stage is INTERNAL only;
Cadenza RAF smoothing, chorus ripple, colors/settings/accessibility/sample
integration and runtime Android validation remain.
