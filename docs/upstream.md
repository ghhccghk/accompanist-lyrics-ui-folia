# Source ledger

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
