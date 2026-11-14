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
