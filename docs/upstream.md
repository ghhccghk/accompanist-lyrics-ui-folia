# Source ledger

## Tilt public calibration preview — 2026-11-16

Same fixed Folia cc6226e464050a456a2913e062374f23d3844cc5, AGPL-3.0-only.
VisualizerTilt.tsx:334–534 -> TiltLine/TiltTextFeatures (whole-run shaping,
optional kerning disabled to match independent source EGC boxes; required joining
and retained internal timings remain). Source:645 -> sampleTiltLineOpacity and
VisualTimeline's optional per-theme 450ms exit retention. Other themes unchanged.
Source TiltTuning/getColors -> FoliaStyle.Tilt/TiltTuning/TiltColorScheme and
independently written sample controls/settings. This is explicitly a calibration
preview; Arabic/transform/global cache differences remain unresolved.

Investigated pretext 0.0.9 src/measurement.ts getSegmentMetricCache/getSegmentMetrics:
the source invalid trailing-italic fontSpec retains earlier cached token/space
widths even after another valid Canvas font is assigned. TiltLine independently
reconstructs a per-line cache; no pretext runtime dependency is introduced.
MIT notice is retained in LICENSES/MIT-pretext-0.0.9.txt as conservative attribution.
This is not a port of the full pretext line breaker; dictionary/global-history
differences remain open. No font, lyric, cover or source demo assets copied.

capture-tilt-geometry.mjs reads original untransformed fractional CSS dimensions;
capture-tilt-exits.mjs records actual parent Motion values and screenshots.
Existing integer geometry and Motion-target fixtures remain unchanged; a repeated
capture had identical targets but differing computed CSS-transform history,
saved under .work rather than silently replacing the checked-in reference.
Numeric/visual evidence and limitations: verification/tilt-public and themes-parity.md.

## Tilt internal stage — 2026-11-16

Pinned Folia cc6226e464050a456a2913e062374f23d3844cc5, AGPL-3.0-only:
VisualizerTilt.tsx TiltLine -> TiltLine.kt (Compose intact-paragraph masks,
normal/italic rows, tracking, nonspace reveal, stagger, source-time pulse,
independent row/line compositing); row/char variants -> TiltMotion.kt.
Native renderer has no DOM/global Canvas history. Per-line compatibility models
the source's invalid trailing-italic Canvas font assignment; source global-cache
history and intact Arabic shaping versus per-EGC DOM remain unresolved differences.
No source checkout edits, unlicensed lyric/font/art assets, or source demo resources.
capture-tilt.mjs mounts actual original code and preserves CSS transform transition;
pretext 0.0.9/integrity matches upstream lock and is verification-only (MIT).
144 paired screenshots, actual Motion fixtures, numeric/paused/reverse controls
and unresolved large Arabic residuals are described in themes-parity.md.
This internal renderer is not yet a supported public theme or a visual acceptance.

## Tilt sentence/layout/timing core — 2026-11-16

Same approved Folia cc6226e464050a456a2913e062374f23d3844cc5, AGPL-3.0-only.
`src/utils/lyrics/sentenceLayout.ts` -> `internal/styles/tilt/TiltSentences.kt`:
punctuation/bracket/Western/CJK/special separator hierarchy, semantic midpoint
fallback, seeded selection and shortest-neighbor merge. Uses the existing platform
word segmenter. UTF-16 fallback cuts are moved to EGC boundaries; indivisible
clusters are retained rather than creating invalid strings. Source strings and
offsets survive; no other simplification of the partition algorithm is intended.

`src/components/visualizer/tilt/VisualizerTilt.tsx` private determineLineCount,
seededRandom/buildTiltLayout -> TiltLayout.kt; findSegmentWordRange/buildCharTimings
-> TiltTiming.kt. Preserves re-split/fit floors/short-final boost, inside-word ranges,
nonspace timing and pulse/tail. Syllable inputs reuse attributed grapheme timing.
Original source fragments are not replaced by display groups.
generate-tilt-sentences.mjs executes the original export; generate-tilt-layout.mjs
extracts named private declarations verbatim into an ignored Vite test wrapper.
Font lookup/width measurement use explicit synthetic seams for this numeric control,
not a browser-font or visual acceptance claim. Source checkout is not modified.
No song/preview/font/cover assets copied. The internal core alone is not a supported
public theme; a separate renderer and mounted-source screenshots are still required.

## Partita layout — 2026-11-16

Source revision: Folia cc6226e464050a456a2913e062374f23d3844cc5 (v0.7.26),
AGPL-3.0-only per the existing approved license determination.
`src/components/visualizer/partita/partitaLayout.ts` (buildSequentialColumns,
buildPartitaWordConfigs) and `partitaOverlap.ts` (getPartitaOverlapSpacing)
adapted to `internal/styles/partita/PartitaLayout.kt` under this project's own
package. Kept seconds-based sine seeds, random consumption order, semantic/sticky
layout units, per-word source timings, rem/em separation and CSS serialization.
Uses existing Kotlin wordReach/pairGap rather than importing a browser runtime.
Shared SemanticLayout.kt exposes its existing display conversion for chunk reuse;
classic conversion behavior stays unchanged. A line-only lyrics-core input uses
one explicit line-timed run. No fonts, lyrics, cover art or preview assets copied.
generate-partita.mjs executes unmodified original modules for synthetic fixtures;
PartitaLayoutTest compares numeric results. That first layout-only commit did not expose a renderer.

Stage follow-up: PartitaChunk.tsx -> PartitaMotion.kt (initial tween, interrupted
200/20 spring, unchanged-target continuation) and PartitaGuides.kt (CSS sizes,
shadow, grouped opacity/scale). VisualizerPartita.tsx -> PartitaGeometry.kt,
PartitaLine.kt and FoliaStyle.Partita (intrinsic rows, fit, media-time float).
The stage reuses the attributed GlowWord sampling/full-paragraph raster/layers,
not ClassicLine's free-form layout. DisplayUnits.kt exposes the existing shaping
merge; invalid grapheme row breaks in core fragments are joined while keeping
both effective and original source clocks. ClassicSubtitles.kt keeps classic
defaults and adds Partita's original sizes. Sample controls are independently
written. capture-partita.mjs mounts actual original modules with the existing
text-only shell/background/subtitle isolation and CSS utility subset, records
actual Motion and DOM geometry, not hand-written JS targets. No example lyric,
SF Pro, cover, avatar/emoji pack, audio analyzer or SPW code copied. Final visual
acceptance is pending; see themes-parity.md for results and remaining differences.

## Own namespace migration — 2026-11-15

Own library/sample package prefix changed to top.ghhccghk under user instruction;
published Folia group is top.ghhccghk.accompanist. Existing source attributions,
original source revisions/paths/licenses and external lyrics-core coordinates
remain unchanged. Target Kotlin directories now use top/ghhccghk/accompanist.
Historical evidence and the rejected direct-paragraph patch retain their original
source identities; use their recorded commits when reproducing old captures.

## Local audio sample change — 2026-11-15

Read-only Accompanist source revision: 6b1ab72319bf2143225acf7797031efa7491897c.
SongSelectionDialog.kt, PlayerLayout.kt, PlayerPresentation.kt, Android PlayerScreen.kt/
PlaybackService.kt and Desktop README were inspected. Android uses Media3 service;
Desktop follows external players and cannot be treated as the same local transport.
New LocalSongApp.kt and platform LocalPlayback files are independently written:
system MediaPlayer / JDK Java Sound, no borrowed audio framework/service/assets.

sample/shared/.../player/SelectedLyrics.kt -> sample/shared/.../importing/SelectedLyrics.kt:
adapt timed-translation merge by exact start, preserving nested vocals; add bounded
strict UTF-8 input, actual published lyrics-core0.5.0 AutoParser checks and unmatched
translation error. Apache-2.0 source remains Apache-2.0; license copied unchanged
to LICENSES/Apache-2.0.txt and included with library/sample notices. No SF Pro,
cover, lyrics or other reference resource copied. Project AGPL-3.0-only decision
does not replace individual source notices.

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

Static paint origin: pinned GlowWord.tsx will-change-transform word layer ->
ClassicLine.kt separates CSS-pixel static layout rounding from continuous media
transforms. This behavior is measured on the unchanged original component at
DPR1/2 by capture-sampling.mjs (external left/top vs waiting-variant x/y); no
Chromium source is copied and no approximate CSS renderer replaces GlowWord.
Fractional measured geometry remains available for spacing/fit; only the painted
word center and matching hit transform use the static snapped origin. Paragraph
shaping, internal timings, animation curves and glow source stay intact.


Classic intensity: pinned classic/Visualizer.tsx placementOf, lineConfig,
calculatedMargin and lyricContainerFloat; glowWordVariants.ts passed opacity ->
FoliaOptions.kt FoliaAnimationIntensity, ClassicSampler.kt, ClassicGeometry.kt,
LineMotion.kt and ClassicLine.kt. Restore Calm/Chaotic alongside the existing
Normal default, without a theme/background engine. Waiting coordinates are
computed from double source config before drawing Float conversion. Real original
custom configs, DOM geometry, Motion and two-period breathing traces come from
capture-intensity.mjs; fixtures and pixel residuals are recorded in classic-parity.md.

Fractional line surfaces: pinned classic/Visualizer.tsx:350-379 (70vh padded
stage, flex-centered independent line boxes), useOverlapFit.ts clientHeight and
glowWordTiming.ts:59-100 (blur(0px) -> transitionEnd filter:none) ->
ClassicStage.kt, FoliaLyricsView.kt, ClassicLine.kt and ClassicLayer.kt.
Keep 1/64 CSS layout offsets and integer clientHeight fit budget; snap static
word origins after accumulating offsets to the nearest composited surface.
Filtered lines retain their own raster surface even for zero blur, with continuous
Motion transforms and the same painted-space hit testing. Browser control probes
capture-line-centers.mjs, capture-line-sampling.mjs and capture-line-isolation.mjs
observe the real original; none/restored are pixel-identical while blur(0px)
changes pixels without changing DOM geometry. No browser source or media copied.

Short viewport fit: pinned useOverlapFit.ts:49-62 -> ClassicLine.kt preparation.
Use actual padded availableHeight, including values below 80px. Compare its
ratio to integer offsetHeight before another .98 shrink; keep MIN_FIT=.5 and
the .005 convergence test. capture-short-stage.mjs observes actual original
Classic at 800x100/150/180/200/240/300 in all three intensities, with the same
synthetic Amber/echo and OFL face. ClassicShortStageTest checks line heights,
public/core drawing identity, pause and height restoration. Debug-only Android
fixture accepts an optional height (default 600); production public API unchanged.

Natural-line baseline: original GlowWord.tsx intact body block with line-height1.22
and CSS font metrics -> ClassicLine.kt naturalFontMetrics/cssFontBaseline, plus
paintedNaturalBaseline in FontMetrics.jvm.kt/android.kt and CssTextMetrics.kt.
Actual original fitted-size baseline probes are capture-fit-baselines.mjs; the
zero-size marker leaves pixels/height unchanged and real Classic uses offsetTop
without touching its transform. Clearing/restoring a transform changed raster
history and was rejected. JVM native TextLine controls identify actual paragraph
paint baselines by exact alpha equality; paragraph reported baseline is not used
as a proxy for natural-height paint. Explicit-height subtitle path stays separate.
Only API behavior/measurements referenced; no Skia source copied.

Explicit subtitle paint baseline: pinned VisualizerSubtitleOverlay.tsx translation/
secondary/upcoming line-height1.5 -> FontMetrics.jvm.kt paintedLineBaseline.
Actual original Overlay CJK probes reuse only the registered OFL subset and new
synthetic subtitle-cjk-scenes.tsv; capture-subtitles.mjs --cjk guards probe pixel
identity. Skiko0.150.1 resolves Skia tag m150-1f14f1166a; TextLine.cpp
buildTextBlob/getMetrics explain the distinct painted and reported baselines.
Public Compose cursor rectangles recover the unrounded data; no Skia source is
copied. SubtitlePaintBaselineTest compares complete shaped paragraphs against
native TextLine alpha at explicit baselines and rejects uncovered probe glyphs.

Subtitle preview intrinsic overflow: pinned VisualizerSubtitleOverlay.tsx:181-267
inline-block, px-1.5 and getUpcomingLyricsClassName truncate/max-w-2xl ->
ClassicSubtitles.kt nowrap min-content width, 42rem cap, shared wrapper/light-pad
origin and truncated line edge. Preserve complete paragraph shaping; do not
prematurely ellipsize to the viewport. capture-subtitle-preview.mjs mounts actual
original Overlay for synthetic CJK/long/translation+next/light-pad cases.
FoliaParitySubtitle-Bold.otf is a separately renamed OFL subset of the same
pinned CJK source, adding covered U+2026 without changing original outlines.
Original body font/fixtures unchanged. Public-entry Desktop window and Android
debug probes reuse the same source/font/viewport/media grid. See classic-parity.md
for residuals, failed fallback-font candidate and platform limitations.

## Accompanist waiting dots (2026-11-15)

Read-only source commit: 6b1ab72319bf2143225acf7797031efa7491897c, Apache-2.0.
The following paths are relative to src/src in that repository:

| Source | Target | Changes |
| --- | --- | --- |
| commonMain/kotlin/com/mocharealm/accompanist/lyrics/ui/internal/effects/PreparedBreathingDots.kt | library/.../internal/waiting/PreparedBreathingDots.kt | Own package/defaults name, remove unused imports; phase math unchanged |
| commonMain/kotlin/com/mocharealm/accompanist/lyrics/ui/composable/lyrics/KaraokeBreathingDots.kt | library/.../internal/waiting/WaitingBreathingDots.kt | Own internal defaults/component; centred fixed stage, remove list collapse/alignment/trailing slot; reduced-motion spatial freeze |
| jvmTest/.../composable/lyrics/BreathingDotsTest.kt | library/src/commonTest/.../WaitingDotsTest.kt | Own package/class/defaults imports; original assertions unchanged |
| internal/effects/PreparedBreathingDots.kt | library/src/jvmTest/.../SourcePreparedBreathingDots.kt | Test oracle; only package/class/defaults names changed |

Original source files contain no per-file author header; attribution remains with Accompanist
contributors and LICENSES/Apache-2.0.txt. The original PreparedBreathingDots comment,
"Static phase boundaries ported from feature-text-engine renderer/draw.rs.", is retained.
Read-only searches of the available source, all local refs and draw.rs history did not locate
that older renderer. The earliest available introduction (2ff60821eda63fa2cbfad97c322a7d1e73b94246)
already has this comment. This does not establish that renderer's origin/version; no Rust file
was copied, and no claim of an independently verified renderer attribution is made.
The migrated files are covered by the source repository's Apache declaration; retain this
provenance limitation rather than erasing the comment or substituting the Folia/SPW license.
BreathingDotsLayoutTest was read as reference: its collapsing list row is intentionally
not migrated into Folia's fixed stage. Core numerical/visual checks are documented in local-playback.md.
Accompanist internal/playback/LyricsPlaybackTimeline.kt:89-106,185 establishes
the strict >5000ms intro/interlude threshold. New internal/waiting/WaitingTimeline.kt
uses that rule but independently unions every normalized vocal interval, rather than
adjacent row bounds, to meet the overlapping-voice requirement. FoliaLyricsView waits
for existing classic visual tails/exits before showing the dots at the original gap phase.

Hardware coverage control: verification/classic-browser/capture-foreground-coverage.mjs
mounts the same pinned GlowWord via raster.tsx. Its only measurement changes are hiding
the independent halo, moving the actual baseline to191 and choosing an opaque black/dark
background. ForegroundCoverageActivity is independently written debug code comparing a
whole Compose paragraph on a checked hardware canvas to an identical software raster;
no font outlines, Rust, native renderer code or production compensation were copied.

Color-dependent body coverage: pinned GlowWord.tsx:122-128 paints the body with
the animated color from glowWordVariants.ts. BodyParagraphRaster.kt and its
platform policy retain one intact shaped paragraph at the existing raster scale,
rasterize its actual ink on Windows, and retain only the latest immutable image.
Android/other desktop backends keep white-mask tint pending evidence. No font
outline, gamma compensation or browser raster implementation is copied. The
existing body/word/line layers and timing remain unchanged. ClassicColorParityTest,
capture-classic-colors.mjs and body-colors.tsv compare actual pinned mounted
Classic colors plus full/body-isolated screenshots. Remaining full-frame
regressions are recorded in classic-parity.md, not waived as platform differences.

Translucent shadow composition: pinned wordGlow.ts:textShadowAt/glowKeyframes and
GlowWord.tsx:86-119 apply activeColor (including alpha) to EACH 20/40, 18/32 or
14/24 shadow, then animate the grapheme span opacity around the pair. GlowMask.kt
now applies ink alpha before source-over; GlowMaskPaint.kt keeps translucent
span opacity in its own saveLayer after tint, avoiding software paint rounding
that made low-coverage tails too bright. Opaque ink keeps the exact prior draw
path. ClassicLine.kt invalidates prepared masks on active-color alpha changes.
capture-translucent-halo.mjs executes actual original GlowWord/wordGlow targets;
75 synthetic PNGs include 45 paired/group-opacity and 30 isolated single-shadow
controls. No source modification, font outline or upstream example asset copied.
The same registered OFL font/source baseline is reused. Full-image residuals and
Android runtime limits remain documented in classic-parity.md.


## 2026-11-16 Fume printing / retained text

Fixed source: chthollyphile/folia-major v0.7.26,
`cc6226e464050a456a2913e062374f23d3844cc5`, AGPL-3.0-only.
Original files under `src/` → target:

- `components/visualizer/fume/fumeReveal.ts` →
  `library/src/commonMain/kotlin/top/ghhccghk/accompanist/lyrics/folia/internal/styles/fume/FumeReveal.kt`:
  word ranges, following-space ownership, discrete syllable printing, independent
  continuous focus progress, raw-line-end completion and next-line visual cutoff.
- `components/visualizer/fume/fumeTextStyle.ts`, `fumeMath.ts` → same target:
  retained style, cubic dimming, hold duration and delayed glow envelope.
- `utils/lyrics/graphemeTiming.ts`, `renderHints.ts` → same target:
  source-second clocks and render-end calculations. Native EGC ranges use the
  existing segmenter; source pieces remain available if a parser splits an EGC.

Changes: immutable Kotlin sampling with no event/frame integration or global
rounded-key memo; lyrics-core normalized source-fragment adapter; exact JS
whitespace set. No classic semantic/sticky re-timing. Test fixtures execute the
original modules through reference.mjs; source remains clean. The generator
clears the original hold-duration memo before each independent query. Inputs
are synthetic and contain no upstream lyrics/assets. `verification/fume-fixtures/README.md`
records evidence and limitations. No article, camera or renderer port claimed.

## 2026-11-16 Fume article / font fitting

Same fixed source and AGPL-3.0-only as above. Original
`src/components/visualizer/fume/fumeArticleLayout.ts` → internal/styles/fume/FumeArticle.kt:
stable UTF-16 FNV seeding, fallback/natural heroes, shortest-column and spanning
placement, font binary search and column/density scoring. Original
`fumeTextMeasure.ts:chooseSingleLineFontPx` → FumeArticle.kt; its measurement
contract → FumeTextMeasure.kt (whole Compose paragraphs, fractional advance,
trailing ASCII whitespace hangs for fit only, calibrated font quantization).
No global measurement-history heuristic or DOM/MessageChannel scheduler copied.
Callers must arrange preparation/invalidation; this is not yet a public renderer.

The article generator executes original source with a disclosed synthetic font
measurement import seam and the original font-search declaration. Browser
capture separately imports the untouched original article builder and
`fumeCanvasText.ts:createStaticBlockSnapshot`; only the fixed diagnostic framing
and block outlines are ours. Native diagnostic drawText is not claimed to match
source baseline, hero inset or snapshot composition. Source remains clean.
Font reuse/attribution is unchanged (`verification/assets/README.md`, OFL notices).
No upstream example assets, Accompanist code or background engine copied.
Exact procedures and current nonaccepted visual residual are documented in
verification/fume-fixtures/README.md and docs/themes-parity.md.

## 2026-11-17 Fume live frame parameters

### Camera target geometry

#### Rejected media-reconstruction experiment

Independent adaptive driver around the same original operator:
`FumeCameraTrackExperiment.kt` and its checks are **test-only**, not commonMain,
not published runtime and not selected by sample. Exact source-event splits,
adaptive step-doubling and immutable prepared nodes make pause/reverse seek
independent of display history. Source Float phase is anchored to media time
instead of the wall clock. `generate-fume-camera-track.mjs` runs the untouched
source operator under this explicitly adapted driver and also records original
60Hz finite-step output. Kotlin matches the adapted driver (360 samples,
maximum view-value error1.4e-10), and random queries do not mutate the track.

**Rejected for runtime**: against the actual original60Hz response, the maximum
distance is107.45 world px and scale difference0.06265; preparing8s requires
5,162–5,202 nodes. A converged/adaptive version of the frame integrator is not
automatically visual equivalence. These results are retained in
`verification/fume-camera-track/comparison.json` / `trajectories.png`, not
renamed an intentional final difference or hidden by the passing logic test.
Next derive a more efficient media-time response and calibrate actual source
finite-step behavior; no public Fume entry until the text/camera loop is usable.

`src/components/visualizer/fume/fumeCameraStep.ts:stepFumeCamera` → `FumeCameraStep.kt`:
original retarget/entry bias, direct and overview bridges, focus catch-up,
position/scale springs, velocity limits, idle float and overview text restoration.
Background/audio parallax is excluded. The Kotlin operator returns immutable
state and never changes its argument. Explicit input seconds replace the source
wall-clock-milliseconds argument without changing its phase. The source's
phase-before-flight-duration-extension ordering is intentionally preserved.

`generate-fume-camera-step.mjs` executes the original operator for 16 sequential
trajectories at 30/60/144 Hz and irregular explicit steps, smooth/stepped focus,
static/float, all three intensities, speed extremes, near/distant switches,
return transitions and overview. All 8,696 pose/retarget/restore states compare
at 1e-8 (restore1e-10). This proves the **operator**, not a media clock or seek
implementation. It is internal and must not be called once per display frame
as the public media model. Deterministic media-time reconstruction remains next.

`src/components/visualizer/fume/fumeCamera.ts` → `FumeCameraTargets.kt`:
stepped and smooth print focus, 0.7-EGC row blend, entry/scale targets,
micro/short/normal retarget durations, padded article overview and long-distance
overview flight bridge. `fumeBlockFrame.ts:isFumeBlockOnScreen` → the same file.
Native receives untransformed measured advances separately from text times/ink;
it does not infer camera geometry by rasterizing individual UTF-16 characters.
No wall clock, integration step, camera spring or public entry in this commit.

1,253 target samples are generated by running the untouched source modules:
explicit synthetic advances isolate target geometry, including no render rows,
small/zero viewports, out-of-order line times, exact row boundary ±1e-8,
clamps, near/far bridge activation and absent overview. Numeric tolerance1e-8;
random traversal produces the same outputs. Original cross-row focus has a
return jump at the row boundary (the later spring absorbs it): an initial test
incorrectly assumed continuity and failed; original boundary samples confirm
the jump, which is preserved rather than changing source fixtures or native
motion. Failure log `.work/fume-camera-targets.log` is retained locally.

`fumeCanvasText.ts:drawFumeLiveBlock/drawRenderTextRun` → `FumeLiveText.kt`:
whole-shaped-row clipping, ordered print-stamp commands, per-EGC runs,
separate line glow, static retained-text layers. Native sampling uses the
existing independently implemented premultiplied Gaussian blur and isolated
opaque text-shadow coverage. Canvas shadow source alpha is applied once,
before run clipping. Masks allocate lazily; only the latest blurred radius is
held per prepared segment and blur support is added around compact source
coverage, instead of padding every article word by the largest possible halo.
An initial oversized-mask implementation and compact one produce all 168
identical native PNGs. No new native/Rust dependency or timer is used.

Differences still under calibration: source pretext measured prefix advances
versus native whole-row selection geometry and ICU word segmentation; source
per-segment line-glow overhang versus a clipped shaped-row coverage partition;
multiline rasterization and non-Latin baseline metrics. Mask tint versus native
colored coverage can also differ. These are not final acceptance exemptions.
`verification/fume-live/comparison.json` records 168 real original Canvas /
Compose pairs at the same font, geometry, media time and 1:1 viewport, with no
camera. Mean foreground-union RGB MAE 2.9653 (glow off 2.4973, on 3.4332),
worst 9.8170. All 168 random revisits and pause checks are pixel identical.
The source geometry is an explicit input here to isolate rendering; it is
not evidence of an integrated article/camera/public Fume theme.

Same pinned Folia `cc6226e464050a456a2913e062374f23d3844cc5` (AGPL-3.0-only).
`src/components/visualizer/fume/fumeBlockFrame.ts` → `internal/styles/fume/FumeBlockFrame.kt`:
waiting/live/passed classification, raw-end completion versus render-end trail,
next-line cutoff, retained-text crossfade and overview restoration.
`fumeCanvasText.ts:drawFumeLiveBlock` → the same Kotlin file: per-EGC color,
integer live shadow radius, line glow and transient print-stamp geometry/alpha.
`wordColoring.ts:resolveWordColor` and `colorMix.ts` → typed sRGB ink interpolation
and Fume's first-match keyword rule; retain distinct CJK predicates of both source files.
Changes: pure media-time results instead of Canvas side effects; geometry relative to
the shaped EGC advance origin; caller still paints the complete shaped segment.
No public API or alternative theme selector is added by this numerical kernel.

`verification/classic-browser/generate-fume-frame.mjs` calls the untouched source
drawing function with a recording Canvas and explicit synthetic glyph advances.
It records 1,104 frames / 8,970 glyph paints / 317 stamps, including source
syllables, whitespace, emoji, CJK, Arabic, no words, next-line cutoff, hold,
three intensities and overview restoration. This isolates animation math and
colors; it is deliberately **not** screenshot, shaping or completed-theme evidence.
`FumeBlockFrameTest` compares those values at 1e-8, including reverse traversal;
there is no integration clock, accumulated animation state or fixed-FPS loop.

## 2026-11-17 Fume static text snapshots

Fixed Folia revision/license unchanged. Original
`src/components/visualizer/fume/fumeCanvasText.ts:createStaticBlockSnapshot`
→ `internal/styles/fume/FumeStaticText.kt`: transparent padded bitmap, complete
line shaping, source middle-baseline placement, 8% hero inset, .5/.52 baseline
offsets and full trailing-space paint width. Row text and original UTF-16 ranges
are retained; draw/measurement owns no independent clock. Glow and live stamps
are not ported in this step.

Independent native baseline-grid calculation was calibrated against actual
Chromium Canvas metrics. Read-only implementation research also checked
chromium/chromium `f7bd09bb3a838b72872f9d0f5732a58aaa5c60c5`:
`third_party/blink/renderer/core/html/canvas/text_metrics.cc` (middle baseline)
and `third_party/blink/renderer/platform/fonts/simple_font_data.cc` (normalized
typographic metrics). No Chromium implementation text is copied into the project;
the Kotlin calculation is independently expressed and tested from Canvas values.
Current native hhea metrics are only calibrated for the registered Latin face;
fonts whose OS/2 typographic ratio differs remain explicitly unverified.

Verification expands synthetic article inputs and directly executes untouched
Folia static snapshot drawing. No additional artwork, font or song is imported.
All new functions remain internal until camera/live-text integration and visual
acceptance. Procedure and honest raster residual are in themes-parity.md and
verification/fume-fixtures/README.md.

## 2026-11-16 示例 CPU 遮罩开关

`ExperimentalFoliaMaskProcessor.kt` 为本项目独立编写的 scoped 可选边界；
`sample/shared/.../ExperimentalMaskControls.kt`、`NativeMaskBridge.kt`、
`native/experimental-mask/build-android.ps1` 独立编写，没有搬入新的 Folia/Accompanist 代码。
Rust 复用本仓库既有 `native/experimental-mask/src/lib.rs` 的独立 CPU 遮罩内核，
只增加 sample JNI 名称和 ABI 自检导出，保留旧基准导出；jni-sys 现有授权声明保留。
固定 Folia 来源提交与既有视觉/时间映射不变。具体运行边界与证据见native-mask-sample.md。

## 2026-11-17 Fume continuous response foundation

The pinned `src/components/visualizer/fume/fumeCameraStep.ts` target/retarget
selection is now exposed as the pure `fumeCameraIntent` inside `FumeCameraStep.kt`.
All 8,696 original steps retain their previous 1e-8 comparison.
`FumeSpringResponse.kt` independently expresses the real logarithm of the
original frozen-coefficient position/velocity/focus update, with an analytic
fractional-duration response. The default 1/60s is a coefficient-calibration
interval identifying one source response, NOT a fixed-FPS playback integration
loop. Original browser trajectories are rate-dependent. Dynamic coefficients,
clamps, entry/focus changes and flight bridges still require explicit media
reconstruction and full-scene validation; this foundation is not that result.

`generate-fume-spring-response.mjs` directly runs untouched original code with
fixed targets/coefficients: 25 coefficient sets, 125 samples, 375 axes, maximum
absolute error 1.1674e-12. Fractional semigroup, derivative, coincident
eigenvalue and hour-long seek checks pass. No source expectation was edited.
The initial derivative probe incorrectly compared a forward secant against
the start derivative; it now checks the midpoint derivative at the original
tolerance. Failure log remains `.work/fume-response.log`.
Android assembleDebug and Desktop compileKotlin pass; no device runtime or
new Fume visual acceptance is claimed. No public theme/config contract changes.

## 2026-11-17 Event-prepared Fume continuous camera (internal calibration)

`FumeCameraTrack.kt` independently adapts the pinned `fumeCameraStep.ts` intent
into continuous media-time response using `FumeSpringResponse`, RK4 step
doubling, source printing/focus knots and exact retarget boundaries.
The source target observation in `generate-fume-camera-continuous.mjs` uses
the untouched operator to recover its focus target; this dt=1 observation seam
is verification-only, never native runtime logic. Original 30/60/144 Hz and
irregular trajectories are recorded separately; none is substituted for the
native media clock. Bridge endpoint-forcing correction and source velocity
limits are explicit adaptations, not claimed exact upstream equivalence.

The first loose 0.002px local-error candidate failed the independent 1e-5
numeric seam near discontinuities (e.g. x69.380504669 vs69.380991617 at4.45833s).
The native/source integrators now split anticipated printing/flight knots and
converge with1e-7px local trial error; the original assertion/expectations were
not relaxed. 9,124 raw/adapted source rows across four smooth/stepped x
static/float scenarios match native with maximum4.182e-6 across all nine fields.
All1,940 distinct query times per scenario reproduce exactly in random/reverse
order and on repeat; sampling cannot mutate prepared nodes. Inputs outside the
prepared finite media window freeze to its boundary; invalid time is rejected.

This is NOT full-theme visual acceptance. Against the actual60Hz operator,
worst screen-position residual6.079px and scale0.007003 remain. Original30/144
and irregular residuals are much larger and are not suppressed in reports.
8s prepares2,606–2,791 nodes at this verification precision; whole-song cost
is unmeasured. Not wired into a selector/sample; contributes no playback work.
Evidence: `verification/fume-camera-continuous`, reproducible with the generator,
`FumeCameraTrackTest`, then `compare-fume-camera-continuous.py`. Next: actual
whole-article/camera/text captures and dynamic CTM shadow semantics.

## 2026-11-17 Integrated Fume text stage and CTM corrections

Pinned `useFumeCanvasStage.ts` text-only camera transform, block overscan and
chronological next-line cutoff → `FumeArticleDrawing.kt`. Prepared complete
rows now expose their native focus geometry; paragraph shaping and source EGC
times remain intact. No background/audio engine, subtitle or public selector
is added. `FumeLiveText.kt` now composites static paints into at most two
per-block snapshots before camera transformation; offscreen blocks release
those snapshot references. Native live shadow radius is compensated for the
camera CTM (line glow, glyph paint and print stamps); static shadows remain
baked world-space images, matching the two source draw paths.

The new browser shell directly executes untouched original article, camera,
block timing, passed hold-duration and Canvas drawing functions. Fixed OFL
font,380/800x600,DPR1,18 times,glow0/1,smooth/stepped give144 actual scenes.
Native independently measures the article/focus geometry. A second144-frame
series explicitly shares the original camera to isolate text rasterization;
that series is NOT evidence of native camera agreement.

The original144 PNG hashes are unchanged between raster comparisons. Shared
camera foreground-union RGB MAE4.822642→3.363080; whole scene15.754087→14.704448.
All288 random revisits and media pause comparisons reproduce exactly. Contact
sheets have been visually inspected. The old standalone168-frame text matrix
also remains deterministic and improves2.965264→2.880905 mean MAE.

NOT final theme acceptance: actual article exposes camera residual14.8672px
screen/scale0.055319, worse than the synthetic-geometry trajectory; this is
reported rather than hidden by the shared-camera seam. Static fractional
bitmap destination-size resampling, complex-script baselines/shaping advances,
continuous camera/retarget boundary calibration and full-song preparation cost
remain. No attached Android device; no device or visible-window claim.
Evidence/reproduction: `verification/fume-scene`, `fume-scene-source-camera`,
`capture-fume-scene.mjs`, `FumeSceneTest`, `compare-fume-scene.py`.

## 2026-11-17 Fractional Fume snapshot destination

Pinned `useFumeCanvasStage.ts` passes explicit block.width/height + padding to
Canvas drawImage; allocation rounds up but destination does not.
`FumeLiveText.kt` now retains both bitmap pixel extent and fractional target
extent; `FumeArticleDrawing.kt` opts into the stage path. Raw standalone
text probes intentionally retain intrinsic bitmap sizing, matching their source.
The actual-scene matrix adds801x600 (fractional article widths), making216
original/native scenes and216 explicit source-camera raster scenes. Old144
original hashes are unchanged. Expanded full/raster MAE15.811750/3.427406;
width801 alone18.026288/3.556037. These are a different matrix, NOT an
improvement claim against prior144-frame means. Old380/800 full mean
14.704481 vs14.704448; raster3.363090 vs3.363080, essentially unchanged.
Fractional contact sheet inspected; camera residual14.867149px/scale0.055318
still blocks acceptance. Tests cover432 random revisits and pause frames;
FumeSceneTest/FumeLiveTextTest, Android assembleDebug and Desktop compile
passed. No Android runtime or visible application window tested.
