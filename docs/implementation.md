# Implementation and verification log

Date: 2026-11-14. This file describes actual implementation; design documents
remain the roadmap and must not be treated as an existing API support list.

## 2026-11-16 — Rust CPU mask experiment

User authorized a native experiment. NativeMaskExperiment explicitly loads a locally
built Rust1.91.1 Windows GNU DLL; 30 random coverage cases and JNI invalid-buffer checks
pass with -Xcheck:jni. Three alternating-process measurements compare equivalent ARGB
kernel work and bitmap preparation, including same-algorithm Kotlin. See performance.md
and verification/native-mask for actual results. Rust loses to optimized Kotlin at
640×320; no native runtime or backend selector is wired into the sample/library.
Default Android/Desktop builds and mask/Motion tests pass without Cargo tasks.
Cargo unit-test executable link failed on this toolchain; DLL build/load passed.

ed80f1b preserves the independent internal Tilt stage and original mounted controls;
its 144-frame Arabic/layout residuals are explicitly not accepted and it is not public.

## 2026-11-16 — Partita stage (visual calibration in progress)

FoliaStyle.Partita now dispatches to an independent chunk stage, not renamed classic.
Original seeded rows, interrupted tween/springs, guides, shaped body/word/grapheme
glow and fit are used in the sample's actual theme controls. Existing settings remain
readable. Classic shared subtitles retain their default sizes and Partita uses its own.
Source/provenance and the complete behavior matrix are in themes-parity.md/upstream.md.

Actual original Motion chunk fixtures (702 states), 429 body-filter values, 30 scene
geometry/pause/reverse captures, public resize/click/offset checks and explicit shaping
regressions pass. A 0.53125px geometry failure was repaired by preserving original clamp
serialization; a short-word blur error was fixed using actual mounted source values.
360 full-frame foreground RGB MAE mean=4.620552, max=15.473299. Residuals are retained;
this is not final visual acceptance. Previous 42 classic and 84 subtitle PNG hashes
are unchanged. Android debug assembly, Desktop compilation and shared sample tests pass.
Only headless Chromium and offscreen JVM UI were run for this new theme; Android device
and visible Desktop window/recording acceptance remain unverified. No background/Rust/SPW
expansion. User desktop packaging and daemon-criteria edits remain outside these commits.

Tilt follow-up: independent sentence partition/layout/timing core is implemented
internally; 378 original partitions, 2430 layouts under explicit synthetic width
metrics, 120 pulse states and 108 word-range/grapheme timing cases pass. Source
declarations are executed verbatim; EGC-safe fallback is documented separately.
This is not a public Tilt theme or visual acceptance. Next: actual normal/italic
stage and same-font original browser comparison. No sample placeholder was added.

## Starting point

- Independent repository initialized on `codex/initial-implementation`.
- Four existing design documents saved unchanged in af528a8.
- No applicable AGENTS.md found in this directory or its ancestors.
- Accompanist is read-only. Its pre-existing .idea modifications/untracked files
  were observed before work; do not remove, stage or commit them.
- User selected AGPL-3.0-only after fixed-source declaration review.

## P0 — scaffold built

Independent library, shared sample and two platform entry points created.
No Folia renderer port, animation, import, performance claims or Rust backend yet.
Verified `:sample:desktop-app:classes :sample:android-app:assembleDebug` with the
independent wrapper: BUILD SUCCESSFUL. Both platform entry points compile;
actual window/device interaction is pending the first renderer milestone.

Initial host: Windows x64, Android SDK platform 37; an adb device is available.
First attempt with JBR 25.0.4 failed native allocation under host memory pressure.
Retried successfully with Temurin JDK 21.0.5, two workers, in-process Kotlin and
1536 MiB Gradle heap. No local runtime/SDK path
is committed. Distribution binaries are obtained through the independent wrapper.

## Outstanding acceptance

Classic shaping/layout/visual comparison, deterministic animation, playback,
import, lifecycle, publication consumption and performance remain to implement.
minSdk 21 is the library target; Android sample starts at 23. Actual manifest
merging and device checks determine the verified matrix. Low-API Unicode and
blur behavior have not yet been experimentally verified.

## P1a — core adaptation and interval queries

Implemented actual 0.5.0 karaoke, synced and unchecked models; source identity,
nested accompaniment, exact text and phonetic separators are retained. Unknown
types and reversed lines produce diagnostics. Line-only lyrics retain line-only
timing. Normalized intervals cover outlying fragments without rewriting sources.

Sweep deltas with 64-boundary checkpoints support unordered overlapping inputs,
half-open endpoints, gaps and random seek without a per-frame full-song scan.
Verified `:library:jvmTest :library:compileAndroidMain`: five tests passed,
including 2,000 seeded random queries against a naive overlap oracle.
Visual display/epochs are not yet implemented at this milestone.

## Sample clock

Anchor-based monotonic clock supports play/pause, explicit seek, speed changes,
terminal stop, looping and duration replacement. No accumulated frame deltas or
fixed-rate timer. Owner-driven refresh means disposal owns no hidden job.
Verified `:sample:shared:jvmTest :sample:shared:compileAndroidMain`: four tests
cover sparse/frequent refresh equivalence, frozen pause, continuous speed change,
seek state and multiple loop wraps. UI/lifecycle wiring follows separately.

## Static component and sample loop

`FoliaLyricsView` now shapes complete text runs with Compose, supports responsive
size, wrapping, original-line click/long press, translation/phonetic toggles,
next-line preview, all overlapping secondary voices and explicit gaps/empty state.
Ten original scenarios cover mixed language, short lines, long lines, gaps,
overlap, Unicode, line timing, 300 units, empty and invalid data.
Sample has narrow/wide layouts, scene selection, clock playback/pause, speed,
loop, release-to-seek progress and disabled-by-default checkpoint diagnostics.

Verified both sample builds plus all nine existing logic tests. Desktop `run`
started an application window without a runtime exception. Windows capture
failed repeatedly (`FrameArrived timed out`) including after helper reset;
visual acceptance is still pending. Android device model 2509FPN0BC / API 37
rejected installation with INSTALL_FAILED_USER_RESTRICTED, so no device UI
verification is claimed. No device security setting was changed.
Word transforms/glow and canonical line exits are the next separate feature.

## Classic numerical sampling and shaping boundaries

Implemented media-time render hints, 100/180ms thresholds, seeded normal poses,
mass=1 spring, continuous passed endpoint, fixed lookahead and safe grouping.
Both JVM/Android use ICU4J 78.3 for consistent grapheme boundaries (including ZWJ).
Complex scripts retain a whole run; adjacent Latin fragments inside a word merge.
This deliberately sacrifices independent transforms where shaping safety is unknown.

Verified nine library tests and Android compilation, including 2,000 random
stateless animation seeks, zero-duration words, short-word continuity and
combining/ZWJ/Arabic/Latin boundaries. These are numerical checks, not visual
equivalence or frame-time measurements. ICU adds binary size; low-API device
behavior and complete transitive packaging remain unverified.

## Classic Canvas and canonical exits

Safe shaped units now render with seeded position/scale/rotation, timed color,
cached inner/outer halos, media-time breathing and canonical line epochs/exits.
Unit measurement/mask generation occurs during preparation, never in draw.
Quality and reduce-motion controls are functional; mask budget fallback is
observable through FoliaStatus. All overlapping source voices remain available.

Both samples build. Twelve library tests include the actual Compose/Skia render:
seek 1500→2500→1500ms gives identical pixels and click returns the original line.
Two epoch tests cover overlap resumption, random access and bounded exits.
Opaque-background screenshots are produced in library/build/verification.
Initial apparent halo edges were traced to tiny-alpha transparent screenshot
preview compositing; opaque diagnostic snapshots avoid that ambiguity. Mask
generation now has an explicit fixed radius and a per-line memory ceiling.
See visual-spec.md for all deviations; this is not final Folia visual parity.

An untracked gradle/gradle-daemon-jvm.properties appeared during the session
(selects JDK 25). It was not authored or staged by this task and is left intact.
Later local verification may therefore select that runtime despite JAVA_HOME.

## Local LRC import

Android OpenDocument and Desktop AWT FileDialog now select local documents;
UTF-8 reads are bounded to 1 MiB and parsing runs off the UI owner. The published
EnhancedLrcParser accepts tested standard/enhanced LRC. No other format is claimed.
Cancellation retains the scene; explicit errors retain the scene and are shown.
Generation guards reject stale results; selecting a fixture cancels pending work.
Reload uses the last session-only document/file capability, without storage permission
or persistence claims. The unbounded final standard-LRC source time is preserved;
the no-audio clock ends five seconds after the final start (or later finite end).

Verified both sample builds and eight shared tests, including real parser fixtures,
invalid UTF-8/unsupported/oversized input and stale request invalidation.
Native picker interaction and Android provider permissions have not been exercised
on a device; Compose surface tests do not establish native-dialog validation.

## Session lifetime and keyboard

SampleSession owns lyrics, clock and display options. Android retains it in a
ViewModel across configuration recreation and pauses onStop; process-death
restoration is not claimed. Desktop pauses when unfocused/minimized and on close.
Composition removal cancels its frame/import jobs and pauses the clock. Returning
to foreground requires an explicit play action. Shortcut events bubble after
focused controls: Space, arrows ±5s, Ctrl+O and Ctrl+R; dialogs keep their own input.

Verified both builds and nine shared tests. The added Compose test checks keyboard
seek, background pause, retained session reattachment and disposal pause. Actual
Android configuration recreation and OS minimize/focus interaction remain untested.

## Kotlin boundary lookup optimization

Measured then removed per-query VisualSelection/list allocations and quadratic
latest-primary scanning. Prepared immutable boundary selections make random seeks
binary searches; equal-start source priority is maintained through gap tails.
See performance.md for conditions, numbers, memory tradeoff and measurement limits.
Both sample builds and all library/shared tests pass; random source queries and
pixel reconstruction remain verified. This is only the initial CPU/allocation
baseline. No frame-rate claim and no Rust backend were added.

## Display preferences and actual status

Display options, classic tuning, font size, quality and offset now persist through
an app-owned versioned store (Android private SharedPreferences, Desktop Java
Preferences node LyricsUiFoliaSample). Reads/writes run off the UI thread;
failures are displayed. Invalid finite/range values fall back to safe defaults.
Import capabilities/history remain session-only; no URI permission is persisted.
The disabled-by-default debug panel shows actual quality, safe-unit count and
model/budget diagnostics, with Kotlin identified as the actual implementation.

Verified both sample builds; preference codec and injected-store Compose tests
cover restore/update without writing developer preferences during tests.
OS store persistence across a real application restart remains untested.

## Local Maven and independent consumer

Root metadata, Android AAR and JVM JAR publish only to build/local-maven under
top.ghhccghk.accompanist:lyrics-ui-folia:0.1.0-SNAPSHOT (+ platform variants).
POMs declare AGPL-3.0-only. Binary/source archives contain LICENSE, NOTICE,
THIRD_PARTY_NOTICES.md and upstream.md; no remote destination was added.

The first independent JVM run caught Java 25 bytecode produced by the local
daemon default. Library/shared/Desktop and independent consumer now explicitly
select JDK 21. Rebuilt artifacts pass independent JVM Compose render/callback
execution and Android compilation. The consumer is a separate Gradle root with
only Maven dependencies. scripts/verify-consumer.ps1 repeats the checks and
inspects current snapshot archives. Android runtime and final transitive notice
audit remain outstanding; this local validation is not a public release.

## Interrupted progress and gap alignment

Progress drag Stop commits exactly once; Cancel discards pending preview.
Losing foreground disables progress input and clears an interrupted preview;
tap/keyboard completion remains supported. Gap/empty labels are now centered.
Renderer snapshots are taken before click feedback to isolate media animation.

Both samples build; Compose pointer tests verify release-to-seek once while paused
and foreground-loss cancellation without seeking. Desktop synthetic touch Cancel
did not emit the native interaction in the test engine, so direct native cancel
gesture behavior remains a device/window acceptance item.

## Short-wide sample and Android runtime smoke

Short wide windows now use compact cover/metadata so transport remains visible;
an additional Compose test exercises 900×380. Both sample builds pass.
The existing read-only API34 emulator successfully installed and ran the sample.
Native LRC import/cancel, configuration-change retention, background pause and
preference restart restoration were checked. See android-smoke.md for device,
conditions, evidence and limits; historical physical-device rejection still stands.

## Transform-aware lyric interaction

Primary pointer hit testing now uses the last drawn media timestamp and inverses
line scale/breathing plus unit translate/rotation/scale. Invisible units and space
outside shaped-run rectangles do not trigger seek. Semantics and Enter/Space
retain accessible source-line activation. Secondary text keeps standard controls.
Cached color filters also avoid one repeated object per glowing unit per draw.

Both samples build. 2,000 seeded forward/inverse pose checks and an actual rendered
glyph touch test pass, along with a whitespace miss and original source identity.
Latest touch changes have Compose Desktop runtime coverage; the earlier Android
smoke predates this final hit-test change, so Android runtime recheck is pending.

## Verification closeout — 2026-11-15

Latest root results: 15 library tests and 15 shared tests, all pass. Both sample
builds pass. Rebuilt local Maven snapshots pass the independent JVM rendering test,
Android compilation and all current binary/source archive notice checks (one
additional consumer test). README/design status now distinguish implementation
from planned acceptance. delivery.md lists the saved milestones and remaining
work; no claim of full classic parity, complete performance baseline or final
release is made. The untracked daemon configuration and reference IDE edits
remain outside this task's commits.

## Primary status lifetime — 2026-11-15

FoliaStatus preparation state is now scoped to document, primary epoch and
requested quality. Gaps and empty replacements report zero primary units and
Minimal actual quality without a false GlowBudgetExceeded diagnostic. Exiting
and secondary lines are explicitly outside these primary-line counters.

Verified :library:jvmTest (16 tests) and :library:compileAndroidMain. Added
Compose coverage for active → gap → next line → empty document. This change has
in-process Desktop coverage; Android runtime recheck remains pending.

## Continuous media checkpoints — 2026-11-15

Added synthetic 480×360 classic/reduced-motion sampled GIFs, full-color PNGs,
conditions and pixel hash manifest. A seeded random seek pass reconstructs all
51 normal checkpoints; reduced motion must visibly change the sequence. Library
JVM test passes and the 1500ms PNG was visually inspected for readable text,
halo and captions. This is a platform-font diagnostic, not upstream parity or
an FPS measurement; no remote video/font/lyrics assets are redistributed.

## Shared halo preparation — 2026-11-15

Measured paired masks before optimizing: duplicated source reads and temporary
arrays were visible in JVM allocations. Multiple radii now share source read and
scratch buffers; ClassicLine rasterizes glyphs once for both layers. Mask tint
and breathing changes no longer invalidate prepared geometry/masks. Output
bitmaps remain independent. See performance.md for conditions and limitations.

Verified library/shared JVM suites, both sample builds, all 102 before/after
checkpoint hashes, plus an independent naive-convolution mask oracle. No new
Android runtime or native/GPU memory claims; no Rust dependency was introduced.

## Visible halo pixel budget — 2026-11-15

The primary retains at most 16MiB of RGBA glow masks. Originally, four exits each
received4MiB; original AnimatePresence comparison later proved this drops visible
text. All unfinished exits now share16MiB, divided by the document's peak count
with a minimum of four shares. Quality degrades Standard → Reduced →
Minimal according to available capacity. Total retained visible mask pixels are
bounded to 32MiB. This excludes source rasters, CPU scratch, layouts, GPU/native
overhead and replaced objects awaiting GC; async preparation/lifetime profiling
is still pending. Primary status intentionally excludes exit-line diagnostics.

Verified both sample builds and library suite, including overflow-safe budget
boundary checks and real Compose mask fallback/recovery without content changes.

## Clean tracked-source verification — 2026-11-15

Added scripts/verify-checkout.ps1. A Git archive of 555a26e built and passed 21
library + 15 shared + one independent-consumer tests without copying developer
configuration/build output. Both sample builds and packaged notice presence
checks pass. SDK/JDK and dependency caches still come from the host; no offline,
bit-identical binary or CI claim is made. Evidence is in ignored build output.

## Latest Android hit/large-font and RTL sample — 2026-11-15

Read-only API34 emulator verified current transformed Chinese glyph hit seeks
0:03 → 0:02; whitespace leaves the time unchanged. At font scale 1.5 controls
remain visible and Arabic stays connected in the Unicode scene. No AndroidRuntime
error. Font scale restored and our emulator stopped; details in android-smoke.md.

Added Compose 360×640 RTL/layout-direction + 1.5 font-scale test for visible
controls, Arabic source activation and diagnostic PNG. Inspection exposed reversed
duration/time order in RTL; the numeric progress label now explicitly uses LTR
paragraph direction. This is not exhaustive Android RTL-locale acceptance.
## Namespace migration — 2026-11-15

User-requested own prefix migration: `com.mocharealm` -> `top.ghhccghk`.
Maven group is now `top.ghhccghk.accompanist`; artifact names and version remain
`lyrics-ui-folia` / `0.1.0-SNAPSHOT` with JVM/Android variants. Library packages
start at `top.ghhccghk.accompanist.lyrics.folia`; samples use
`top.ghhccghk.accompanist.folia.sample`. Android namespace/applicationId and JVM
entry points, verification scripts and independent artifact consumer follow these names.
Consumers must update the Folia coordinate and imports; no old-package aliases are supplied.

Published external `com.mocharealm.accompanist:lyrics-core:0.5.0` and its public
model/parser imports remain unchanged, as do original copyright/source attribution.
All 118 moved Kotlin files were checked against the previous commit, permitting
only own namespace/reference/path replacements (Git line endings normalized).
The archived rejected direct-paragraph experiment patch is deliberately unchanged;
it records its historical source paths, not an active old-package dependency.

Validation: selected core/body/color regressions, sample shared JVM tests,
Android debug APK, Desktop classes and native-window verification classes passed.
All 108 color-control JVM PNG hashes equal the pre-migration capture.
`scripts/verify-consumer.ps1` published only to the local file repository, then ran
the separate consumer's JVM rendering/callback test and Android compilation.
New POM groups/dependencies, JVM entry class and packaged license/source notices
passed inspection. `aapt dump badging` confirms the new Android applicationId
and MainActivity. Logs: `.work/namespace-migration-{build,consumer}.log`.
This namespace change has no new Android device run or Desktop OS-window run;
the earlier runtime evidence remains tied to its recorded commits/APKs.
No remote repository, remote configuration, signing, public release or upstream project changed.
