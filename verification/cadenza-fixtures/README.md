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
