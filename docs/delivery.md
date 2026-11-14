# Development delivery checklist — 2026-11-15

This checkout is a working development baseline, not completion of all P2/P4
acceptance requirements. Implementation history is in implementation.md.

Latest verification: clean tracked-source 555a26e passes 21 library + 15 shared
+ one independent consumer tests, zero failures, both sample builds and archive
notice presence checks. The later RTL sample update passes 16 shared tests and
both sample builds. Current library has API34 hit/whitespace/large-font/Arabic
smoke coverage; the later LTR progress-label change has Compose runtime coverage.

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
| c795fc7 | Follow-up visual/performance acceptance plan |
| fab4317 | Primary status lifetime across gaps and replacements |
| 81bfb98 | Synthetic media-time GIF/PNG/checksum sequences |
| 43f60ae | Measured paired-mask raster/scratch reuse |
| 555a26e | Retained visible glow pixel budget and gradual fallback |
| 1c56ffc | Clean tracked-source build/tests and independent consumption |
| 5d184a5 | RTL progress ordering and large-font sample coverage |

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

- Fixed licensed-font pinned-upstream browser comparison and real-window motion
  recordings (sampled synthetic GIF checkpoints now exist); exported upstream glow fixtures, semantic CJK grouping, full layout
  collision/rotation envelopes and dynamic body blur.
- Complete preparation/cache budgeting beyond the implemented 32MiB retained
  visible mask-pixel limit, asynchronous geometry preparation and measured
  disposal/memory behavior under dense exits, long lines and resizing.
- Full mask/layout preparation latency beyond the paired-mask CPU/JVM allocation
  probe, full-sample CPU/GPU/frame P50/P95/P99,
  allocations/GC/memory and representative Android-device performance.
- Physical/low-API devices, broader accessibility/font-scale and Android RTL-locale
  matrix beyond API34 font-scale 1.5/Arabic text smoke; alternate providers/revoked access/native cancel;
  Desktop native picker and OS persistence restart interaction.
- Import history and URI/file restoration across process death; safe reselect
  hint when the chosen document is unavailable. Current history is session-only.
- Real Desktop-window capture (Windows capture helper timed out earlier);
  native dialogs' Escape/focus restoration across platforms.
- Complete direct/transitive binary/source notice audit and CI automation
  (local tracked-source export validation now passes).
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
