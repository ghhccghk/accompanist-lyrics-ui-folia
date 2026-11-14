# Development delivery checklist — 2026-11-15

This checkout is a working development baseline, not completion of all P2/P4
acceptance requirements. Implementation history is in implementation.md.

Final local verification snapshot: 15 library + 15 shared + 1 independent
consumer tests, zero failures. Android sample assembleDebug and Desktop classes
pass. The latest independent-consumer script passed with newly rebuilt artifacts
and packaged notice checks. Android smoke coverage predates the final hit-test
change, as called out below.

## Saved implementation commits

| Commit | Independently saved milestone |
| --- | --- |
| af528a8 | Original design baseline |
| 6f00d52 | Independent multiplatform scaffold and sample shells |
| a0a0679 | Published-core adapter and overlapping timeline index |
| c2ca989 | Deterministic preview playback clock |
| e019839 | Static classic view and interactive sample loop |
| 05c1c43 | Seekable animation sampler and safe shaping units |
| dc7a75e | Canvas motion, cached glow and canonical exits |
| f56b8d8 | Native local LRC import and reload |
| b7dcaeb | Retained sessions, lifetime pause and shortcuts |
| 86750c4 | Measured Kotlin timeline optimization |
| 5817138 | Display preference persistence and actual render diagnostics |
| 9a73bb3 | JDK21 artifacts and independent Maven consumer |
| 97bc56a | Interrupted progress handling and centered gaps |
| 1dea5dc | Short-wide controls and API34 runtime evidence |
| 68086b0 | Transform-aware glyph hit testing |

The documentation closeout follows these implementation commits in local Git.

## Verification and remaining work

Completed evidence:

- Independent Git/Gradle root, published core dependency, both sample builds.
- Deterministic model, timeline, clock, animation and inverse-hit tests.
- Actual Compose/Skia rendering, pixel reconstruction after seek, callbacks,
  wide/narrow/short-wide layout, pointer progress, visibility and settings tests.
- API34 emulator smoke: native LRC import/cancel, resize retention, background
  pause, preferences after force-stop/relaunch. See android-smoke.md.
- Local Maven root/platform/source archives, version-3-only POMs and embedded
  notices, independent JVM runtime and Android compile consumer.
- Initial timeline CPU/allocation probe with reproducible conditions; Kotlin
  optimization and explicit decision to keep Rust out pending further evidence.

Outstanding requirements (do not mark these as supported/verified):

- Fixed licensed-font pinned-upstream browser comparison and continuous motion
  recordings; exported upstream glow fixtures, semantic CJK grouping, full layout
  collision/rotation envelopes and dynamic body blur.
- Complete preparation/cache budgeting, asynchronous geometry preparation and
  measured disposal/memory behavior under dense exits, long lines and resizing.
- Cold/warm mask/layout latency, full-sample CPU/GPU/frame P50/P95/P99,
  allocations/GC/memory and representative Android-device performance.
- Latest transform-aware hits on Android runtime; physical/low-API devices,
  accessibility large fonts, alternate providers/revoked access/native cancel;
  Desktop native picker and OS persistence restart interaction.
- Import history and URI/file restoration across process death; safe reselect
  hint when the chosen document is unavailable. Current history is session-only.
- Real Desktop-window capture (Windows capture helper timed out earlier);
  native dialogs' Escape/focus restoration across platforms.
- Complete direct/transitive binary/source notice audit and clean-checkout CI.
  Artifacts are local verification snapshots, not a public release.

Preserved local state: gradle/gradle-daemon-jvm.properties appeared during the
session and remains untracked/uncommitted. It selects a local daemon independently
of explicit JDK21 compiler/test toolchains. Do not overwrite it or claim the
working tree is completely clean. The reference repository's existing IDE
changes remain intact; this task made no source/config/license changes there.

## Follow-up source-export verification — 2026-11-15

scripts/verify-checkout.ps1 exports tracked HEAD to a unique ignored build folder,
builds/tests both platforms, then runs that export's independent Maven consumer.
It does not copy local.properties, untracked daemon settings or existing outputs.
SDK/JDK installations and the host dependency cache remain prerequisites; this
is not an offline or reproducible-binary claim. No CI service was configured.

Executed successfully for 555a26e: 21 library, 15 shared and one consumer tests,
zero failures; Desktop classes and Android APK build; independent Maven JVM
runtime/Android compilation and archive notice presence checks. The script keeps
the source export, reports and result.txt under build/clean-checkout-* for review.
