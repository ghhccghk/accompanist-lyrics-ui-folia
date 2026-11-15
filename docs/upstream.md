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

Historical scaffold milestone: no Folia rendering code had been ported then. Each later
adaptation must add its original file, target, change description and fixture here.

The historical static milestone was independently written from the data/layout
contract. Subsequent source adaptations and validation are recorded below; the
scaffold statement does not describe the current classic implementation.

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

Public stage audit: classic/Visualizer.tsx at the pinned commit → FoliaLyricsView.kt.
Viewport-derived font, 70% stage height, 32dp padding, 1152dp line cap, independent
bottom subtitle overlay and unbounded natural line height mirror the classic stage.
No shell background/player engine code is imported. PublicClassicParityTest verifies
public versus isolated core pixels on 800/380-wide surfaces; subtitle parity itself
remains incomplete and is explicitly recorded separately.

CSS whitespace audit: GlowWord.tsx whitespace-nowrap / block body → CssWordText.kt
and ClassicLine.kt source-boundary mapping. Canvas measurement retains whitespace
count while CSS display collapses ASCII spaces and removes block-edge spaces.
Glyph timing/count remains source based, including invisible whitespace. Browser
inspect-whitespace.mjs also observes 1/64 CSS pixel upward used-width rounding;
the original strict matrix tolerance was retained.

## Independent model input

Subtitle content audit: VisualizerSubtitleOverlay.tsx content resolver and runtime.ts
recent/next helpers → SubtitleContent.kt, ReadableText.{jvm,android}.kt and
FoliaLyricsView.kt. Preserve gap translations, readable Unicode L/N filtering,
trimmed alternate text, ordered deduplication and two-source-line preview bounds.
The existing three public visibility booleans choose rows without importing the
player settings contract. Browser calls original helpers; only the player-bottom
hook is replaced by the original default 112px for isolated rendering.

CSS baseline paint audit: original GlowWord.tsx block body/1.22 line-height →
CssTextMetrics.kt and ClassicLine.kt. Zero-size inline probes observe baselines
of eight fixed-font cases; separately rounded font metrics and floored half-leading
position the intact Compose paragraph and glyph masks together. Font metadata
compatibility and unchanged-outline proof are recorded in verification/assets/README.md.

Mounted-runtime audit: unchanged classic/Visualizer.tsx, GlowWord.tsx and word
variants at the pinned revision → ClassicSampler.kt, LineMotion.kt, GraphemeGlow.kt,
GraphemeTiming.kt, DisplayUnits.kt, ClassicLine.kt. capture-manual.mjs observes real
Motion values at controlled media times (590 word states). Preserve fractional
seconds alongside microseconds; round runtime elapsed milliseconds before glyph
delay subtraction. Same-target passed x/y springs continue, and running spring
duration uses Motion's 50ms duration probes. The old paused generator oracle now
plays before sampling, as independently confirmed by mounted original components.
No Motion runtime code is shipped in the library; mathematical sampling stays
stateless and seek-reconstructible. Browser screenshots still require paint review.

Resolved candidate: `com.mocharealm.accompanist:lyrics-core:0.5.0`.
Maven Central POM availability was checked directly. Actual public models and
parser entry points were read from its published sources JAR. The dependency is
`api` because the component's public signature exposes core types. Optional
`-PlyricsCoreRepository=<Maven URL>` can select another published repository;
there is no default local path or reference-project binding.

Subtitle motion/layout audit: pinned VisualizerSubtitleOverlay.tsx,
subtitleFontSizes.ts and utils/lyrics/subtitleTrackStyle.ts → SubtitleMotion.kt,
ClassicSubtitles.kt and CssTextMetrics.kt. Preserve overlay/default inner Motion
curves, responsive sizing, secondary rows, wrapping/ellipsis and preview blur.
FontMetrics.{jvm,android}.kt independently reads native metrics through public
Compose font resolution. No font engine implementation is copied. Browser captures
unchanged Overlay with background pad disabled; original text-shadow remains an
outstanding paint difference in Kotlin, not an accepted platform adjustment.

Halo kernel calibration: original wordGlow.ts text-shadow radii are unchanged.
GlowMask.kt is an independently written separable box integrator using CSS/SVG
Gaussian box-width mathematics, not copied Skia implementation. Research references:
Chromium core/style/shadow_data.h (BlurRadiusToStdDev = radius/2), Skia
src/core/SkMaskBlurFilter.cpp (three-box width/axis quantization), and CSS Filter
Effects Gaussian approximation. These are used to understand paint semantics;
no third-party implementation text is shipped. Browser positive .01px shadow
probe isolates glyph shadow coverage from normal foreground coverage. All fixtures
are original synthetic M and the pinned OFL font. New single-shadow PNGs and
original combined images provide independent pixel acceptance.

Low-API layer adaptation: GlowWord body filter / classic line filter /
VisualizerSubtitleOverlay preview blur → ClassicLayer.kt and SoftwareBlur.kt.
Independently written finite Gaussian convolution over premultiplied colour;
no third-party engine code copied, no new native dependency. Both paths retain
whole-paragraph shaping, word-group alpha and media-time reconstruction. API30
uses software Canvas for every nested group, avoiding unsupported hardware-layer
replay on a software Canvas. This is a capability fallback; residual paint and
performance limitations remain documented, not a new acceptance standard.

Composited-body adaptation: original GlowWord.tsx body inside transformed word →
ParagraphRaster.kt / ClassicLine.kt. Preserve intact shaped paragraph coverage at
prepared layout size, tint and transform its raster with the shared glyph halo.
No browser rasterizer code copied. Direct/static font probes remain separate from
actual animated-lifecycle frame comparison; residuals are explicitly recorded.

CSS filter unit audit: original glowWordVariants.ts / glowWordTiming.ts use CSS
blur(sigma). Compose 1.12.0 SkiaBackedRenderEffect.skiko.kt and AOSP Android14
libs/hwui/jni/RenderEffect.cpp both convert platform radius to sigma. ClassicLayer.kt
independently inverts that documented arithmetic; native APIs cannot express
sigma <= .5, so nested software scopes preserve exact Gaussian capability there.
No Compose/AOSP implementation was copied. capture-blur.mjs is an original CSS
white-square diagnostic, separate from unchanged Folia runtime frame comparison.

Unicode sticky predicate: original cjkSemanticLayout.ts TRAILING_WORD_CHAR_REGEX →
SemanticLayout.kt trailingWordCharacter. Use the same Unicode Letter/Number
categories at the end of the whole string instead of a UTF-16 Char predicate.
Original-function fixtures include supplementary Adlam/Aegean, Roman numeral,
combining mark and emoji boundaries, retaining effective and source glyph timing.

Subtitle paint baseline: pinned VisualizerSubtitleOverlay.tsx CSS line-height →
ClassicSubtitles.kt / CssTextMetrics.kt / FontMetrics.{jvm,android}.kt. Skia
modules/skparagraph/src/TextLine.cpp documents rounded blob baselines despite
fractional paragraph metrics. Independently position each intact paragraph line's
coverage at CSS fractional baselines; no font engine code copied. Default browser
PNG ink bounds supplement the existing DOM geometry; LCD-disabled screenshots
are a separately labelled diagnostic, never a replacement reference.

Subtitle theme pad/shadow: pinned VisualizerSubtitleOverlay.tsx contentClassName,
subtitleGlowStyle and textShadow → SubtitleBackdrop.kt / ClassicSubtitles.kt;
FoliaColors.subtitleBackground and FoliaOptions.showSubtitleBackground expose the
user-approved optional theme color with transparent compatibility defaults.
Preserve 6/2px content padding, 40/24px outward insets, 115%/130% ellipse radii,
.96/.78/0 stops at 0/.62/1 and shadow 0/1/2px at .24 alpha. Pinned upstream
package-lock.json resolves Tailwind 4.3.3: its blur-2xl is 40px. The verification
stylesheet is generated by that exact compiler, retaining its MIT header; no
Tailwind runtime is added to the library. Custom semi-transparent host colors
multiply opacity, so the transparent default draws nothing.

Dense sequence: pinned classic/Visualizer.tsx and GlowWord.tsx actual mounted
Motion → verification/classic-browser/capture-sequence.mjs and
verification/classic-fixtures/browser-sequence-motion.tsv. Original synthetic
Amber/Silver lines, identical licensed font/viewport and media timestamps;
328 original states supplement, rather than replace, the sparse complex-script
fixtures. ClassicParityTest and BrowserClassicMotionTest compare direct Kotlin
reconstruction. No original lyrics, recordings or artwork are redistributed.

Fractional subtitle placement: pinned Overlay text-center behavior →
ClassicSubtitles.kt start-aligned intact paragraphs with per-line centering.
Android14 AOSP core/java/android/text/Layout.java:getLineStartPos/drawText/
getLineLeft/getLineRight read to diagnose integer center origins; independently
written Compose adjustment, no Android font/layout source copied. TextMotion
defaults follow the locked Compose source, retaining explicit host overrides.

Windows body coverage: actual pinned GlowWord foreground/shadow screenshots →
ClassicLine.kt separate intact body layout; FontMetrics.jvm.kt selects LCD
smoothing/Normal hinting only for an unspecified host paragraph style on Windows.
Read locked Compose FontRasterizationSettings.skiko.kt/ParagraphBuilder.skiko.kt;
no font engine source copied. BodyRasterTest/compare-raster-settings.py preserve
AA, no-hint and LCD probes rather than replacing the original expected image.

Shadow-source coverage: pinned GlowWord.tsx separate text-shadow spans and retained
halo/M64 source PNGs → ParagraphRaster.kt:drawParagraphShadowSource and
ClassicLine.kt glyph masks. Independently written opaque off-raster foreground /
opposite shadow offset preserves full shaping and works around Android's shadow
alpha coupling. Body foreground paint explicitly restores the host shadow or
Shadow.None after cached paragraph reuse. Read locked Compose AndroidTextPaint:
setShadow(null) leaves state intact. No font engine source copied. BodyRasterTest
and compare-shadow-source.py retain the original source/kernel expectations.

Dense exits: pinned classic/Visualizer.tsx AnimatePresence mode=popLayout and
glowWordTiming.ts exit durations → VisualTimeline.kt retains every unfinished
epoch, replacing the previous arbitrary four-line cutoff. GlowBudget.kt shares
the existing total exit-mask budget by peak concurrency. capture-dense-exits.mjs
mounts unchanged original Classic for30 synthetic D00..D29 lines; dense-exits.tsv
keeps actual positive-opacity line IDs/values (zero-alpha mounted nodes are
documented separately). DenseExitParityTest verifies the public component,
direct/forward/reverse selection and paused PNGs. No original song media copied.

Interrupted entries: pinned classic/Visualizer.tsx AnimatePresence and
getGlowWordLineContainerMotion in glowWordTiming.ts -> capture-interrupted.mjs,
interrupted-lines.tsv and InterruptedLineParityTest. Two original synthetic
6-line/60ms sequences cover normal/fast entry interrupted before completion.
Actual Motion samples include event left limits at1us, explicitly documenting the
continuous media model. Public FoliaLyricsView captures forward/reverse/paused
frames; compare-interrupted.py retains descriptive RGB errors and original crops.
No upstream code/media copied or production curve changed for this validation.

Six-dot interlude: pinned classic/Visualizer.tsx isInterlude branch (fullText exactly
"......") -> ClassicSampler.kt:wordPose, ClassicGeometry.kt:classicGeometry and
ClassicLine.kt preparation. Preserve the original x=0, seeded y spread15,
scale1.5, zero configured rotations, inline-end3rem and centered justification.
Avoid-overlap's original early return contributes no extra reach. DisplayUnits.kt
keeps independently timed parser dots separate; an intact six-dot word remains
one shaped run. glowWordVariants.ts keeps a zero-target rotation spring running
when active/passed share that target; Pose now records whether rotation was
actually disabled. Synthetic scene/layout/Motion fixtures and public-entry PNGs
come from unchanged pinned Classic via capture-interlude.mjs. No media copied.

Inherited body direction: pinned classic/Visualizer.tsx dir attribute and
utils/lyrics/textDirection.ts whole-line first-letter rule -> ParagraphDirection.kt
and ClassicLine.kt intact paragraph styles. Explicit host textDirection is kept;
default words inherit the resolved entire-line direction. Physical left alignment
inside the integer Compose paragraph avoids a spurious RTL origin while the
separate flex geometry still flows RTL. No character reordering or per-character
reshaping is introduced. capture-bidi.mjs records the unchanged original body
Range boxes separately from untouched full animation frames; BidiClassicParityTest
checks those boxes and public paused/reverse-seek frames. New mixed-direction
verification face merges/subsets the already pinned OFL inputs with renamed
metadata (prepare-bidi.py), so no unverified system fallback is an oracle.

First-letter predicate: pinned utils/lyrics/textDirection.ts explicitly lists ten
RTL scripts; it does not test Unicode bidi classes. DirectionScripts.kt keeps
that exact list; Graphemes.jvm.kt/android.kt query the same ICU Script property
for the first Letter. New original-function direction.tsv probes include every
listed script, unlisted strong RTL scripts, Script=Common tatweel, combining
marks and Letter_Number. Original Kharoshthi/Old Hungarian/Meroitic/tatweel
results are LTR, unlike the previous broad bidi-class approximation. No font or
character rendering is inferred from this logical property test.
