# Implementation and verification log

Date: 2026-11-14. This file describes actual implementation; design documents
remain the roadmap and must not be treated as an existing API support list.

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
com.mocharealm.accompanist:lyrics-ui-folia:0.1.0-SNAPSHOT (+ platform variants).
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
