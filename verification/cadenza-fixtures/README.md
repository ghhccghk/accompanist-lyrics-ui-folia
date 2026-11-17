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
