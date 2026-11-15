# Source ledger

## License decision — 2026-11-14

Fixed reference: https://github.com/chthollyphile/folia-major/tree/cc6226e464050a456a2913e062374f23d3844cc5
(tag v0.7.26). Directly downloaded package.json, README.md and LICENSE from
this commit, not the moving main branch. package.json has `license: AGPL-3.0`;
README says the project is open source under `AGPL-3.0`; LICENSE contains the
GNU Affero General Public License version 3, 19 November 2007. The template
at its end does not itself grant later versions for Folia.

The user explicitly selected **AGPL-3.0-only** for this project. PLAN.md and
DESIGN.md now use that identifier. This records the project decision without
claiming repository boundaries automatically isolate license obligations.
SPW's declaration is not used as authorization for Folia code.

## File ledger

| Source file / revision | Target | Changes |
| --- | --- | --- |
| Folia LICENSE / fixed commit above | LICENSE | Unmodified AGPL v3 text; project-specific version grant is in NOTICE |
| Gradle v9.5.0 `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar` | Same paths | Unmodified official bootstrap files; distribution SHA-256 pinned |
| Accompanist PlayerLayout.kt, PlayerPresentation.kt (read-only local reference) | sample/shared/.../FoliaSampleApp.kt | Conceptual narrow/wide arrangement only; newly written shell, no code/assets copied |

No Folia rendering code has been ported at the scaffold milestone. Each later
adaptation must add its original file, target, change description and fixture here.

The static milestone is independently written from the documented data/layout
contract; no upstream rendering implementation has yet been copied.

## Classic numerical adaptation

All Folia rows below refer to cc6226e464050a456a2913e062374f23d3844cc5.
Upstream attribution: chthollyphile and Folia contributors, AGPL-3.0 as declared;
this project's adapted code is AGPL-3.0-only.

| Original file | Target under library/src/commonMain/.../folia/ | Modifications / verification |
| --- | --- | --- |
| src/utils/lyrics/renderHints.ts | internal/animation/ClassicSampler.kt | Integer microseconds, threshold and render-end formulas; ClassicSamplerTest endpoints |
| src/components/visualizer/glowWordTiming.ts | internal/animation/ClassicSampler.kt | Stateless lookahead/active-end profile, no React state |
| src/components/visualizer/glowWordVariants.ts | internal/animation/ClassicSampler.kt | Closed-form mass=1 spring, active endpoint sampled before passed interpolation, media-time transitions |
| src/components/visualizer/classic/Visualizer.tsx | internal/animation/ClassicSampler.kt | Seconds-based deterministic seed/normal-intensity pose; no backgrounds/audio/theme engine |
| src/components/visualizer/classic/Visualizer.tsx | internal/styles/classic/ClassicLine.kt | Compose measured safe-unit flow and media-time breathing; layout differences in visual-spec.md |
| src/components/visualizer/wordGlow.ts | internal/styles/classic/ClassicLine.kt; internal/rendering/GlowMask.kt | Referenced 20/40, 18/32, 14/24 radii; independently implemented bounded software masks, provisional opacity curve; paired layers now reuse one source raster and CPU scratch, with naive-convolution and unchanged-checkpoint verification |
| src/utils/lyrics/graphemeTiming.ts | internal/text/GraphemeTiming.kt | Integer microseconds, ICU complete grapheme ranges and retained original syllables; cross-fragment graphemes share the covering interval |
| src/utils/lyrics/cjkSemanticLayout.ts; wordSegmentation.ts | internal/text/SemanticLayout.kt; GraphemeTiming.kt | Exact CJK alignment and sticky contractions/punctuation, semantic CJK flattened for classic; ICU dictionary replaces Intl, core has no saved segmentation fields; shaping-only merges preserve source timing |

The isolated verification/classic-browser harness loads unmodified pinned sources
from an ignored checkout and generates verification/classic-fixtures. Font source,
license and SHA are in verification/assets/README.md (Noto Sans, SIL OFL 1.1).
No font or sample asset is copied from Folia/Accompanist. Browser shell/backdrop
and subtitles are isolated from the core text comparison; this limitation must
remain visible in every comparison report.

Additional adaptation: wordGlow.ts → internal/animation/GraphemeGlow.kt and
ClassicLine.kt per-grapheme masks. Original variants plus the pinned Framer
keyframes generator produce glow.tsv (108 active/passed samples); Kotlin matches
within 0.00002 opacity. Easing uses the runtime's 12-step Bézier resolution.
Masks slice a complete shaped run before blur; body is never redrawn as characters.
Blur kernels and selection-boundary coverage still need pixel calibration.
Full original-browser visual equivalence is pending.

glowWordOverlap.ts → internal/styles/classic/ClassicGeometry.kt: same rotated
active and nine sampled passed reaches, RTL pair gaps, overflow. 160 original
function fixtures, max tolerance 0.0001. classic/Visualizer.tsx → ClassicGeometry.kt
and ClassicLine.kt: width-based one-decimal spacing, flex wrapping without word
splits, seed justification, overlap-only fit (min .5, slack .98, .005 convergence).
Canvas measurement includes trailing space; CSS word boxes collapse it. Compose
uses fractional caret advances rather than rounded maxIntrinsicWidth for both.
Core fixed-font two-line browser layout fixtures match all margins at 0.0001;
DOM offset dimensions/positions match within integer-pixel rounding (0.51px).

glowWordTiming.ts/glowWordVariants.ts/classic/Visualizer.tsx → LineMotion.kt,
ClassicSampler.kt, ClassicLine.kt: per-property default/explicit easing, physical
spring rest thresholds, CSS negative leading, full body/line blur layers with
explicit padded recordings, normal breathing keyframes. generate-motion.mjs calls original variants
AND actual Motion animateMotionValue dispatcher (not merely keyframes defaults).
42 word states and 120 line enter/exit combinations match within 0.0001.
Motion runtime algorithm behavior was inspected from locked motion-dom 13.5.1 /
motion-utils 13.5.0 (MIT); only mathematical response is independently implemented,
no runtime code bundled into the component. Android RenderEffect needs API31+;
older-platform continuous blur remains unimplemented, not an accepted final difference.

Second grouping audit: cjkSemanticLayout.ts → SemanticLayout.kt preserves semantic/
sticky flags until display conversion, rather than attaching after semantic flattening.
Shaping-only joins carry both existing effective envelopes and source envelopes.
textDirection.ts → ICU first-letter direction in Graphemes.{jvm,android}.kt; complete
Arabic words now animate independently, while fragments within a word stay shaped.
Additional original-function fixtures cover mixed CJK/Latin, leading whitespace,
semantic contractions and consecutive punctuation. Noto Arabic font provenance is
recorded alongside the Latin font. Browser matrix uses six synthetic scenes and
actual original Classic at pinned fonts/sizes (including 380px wrap + overlap fit).
ClassicLine/ClassicGeometry also reproduce CSS three-decimal font clamp serialization,
Chromium hundredth-pixel glyph sizes, 1/64px line heights, and integer offsetHeight fit.
These rounding rules were observed with inspect-layout.mjs and exact DOM fixtures;
margin tolerance was not relaxed to conceal rounding discrepancies.

Halo compositing audit: GlowWord.tsx / wordGlow.ts at the same pinned commit →
ClassicLine.kt word/glyph grouping and GlowMask.kt compositeGlowMasks. The original
span opacity applies after both shadows have been painted; word opacity applies
after body plus glows. Browser isolation source is actual GlowWord with identity
layout and hidden body, opacity forced only for paint measurements. Generated PNGs
contain original synthetic "M", using the pinned OFL font, no upstream lyric assets.
No third-party blur implementation was copied. Existing software blur remains an
approximation with measured residuals, recorded in classic-parity.md.

## Independent model input

Resolved candidate: `com.mocharealm.accompanist:lyrics-core:0.5.0`.
Maven Central POM availability was checked directly. Actual public models and
parser entry points were read from its published sources JAR. The dependency is
`api` because the component's public signature exposes core types. Optional
`-PlyricsCoreRepository=<Maven URL>` can select another published repository;
there is no default local path or reference-project binding.
