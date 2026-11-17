# Cadenza fixed-source calibration

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
