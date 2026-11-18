# Rust renderer execution record

Updated: 2026-11-19. Starting commit: `fdcc1e5`, branch `codex/initial-implementation`.
This is the single current execution checklist; the design and implementation prompt remain the requirements.

## Completed

- Read both implementation documents and inspected the existing Partita preparation/draw path, sample playback, publication and consumer checks.
- Verified the read-only reference checkout is `d7ff03e083d28069c973cf204b4a8f0a9d5c8594`.
- Existing native code is CPU mask/colour only. No renderer was present at the starting commit.
- Preserved and excluded the pre-existing release signing hunk in `sample/android-app/build.gradle.kts` and untracked `gradle/gradle-daemon-jvm.properties`.

## Current: visual, real playback and runtime stress validation

P1 platform milestone passed (`ad21f29`); evidence: `verification/native-renderer-p1.md`. P2 real core/prepared Partita + GPU passes, 6 Android Rust tests, existing Partita and bridge JVM tests, isolated release/R8 native consumer and paused frame stability passed; evidence: `verification/native-renderer-p2.md`. This is not complete visual/performance acceptance. Implement and validate the thin sample, persisted per-theme backends, failure/lifecycle and interaction behavior next.

P2 committed as `c0038b6`. Thin native sample and original Android debug APK both
build. Settings 11/11 pass, including per-theme backend persistence and old input.
Owned-emulator UI: song selection/LRC/settings/demo remain present; selecting
Partita + NativeRequired reports `NativeGpu Ready:PartitaMainAndGlow`. Force-stop
and cold restart retain the requested backend/theme (paused demo restarts at 0;
`Preparing` before a main scene is expected). Existing LocalSong, SongSelection,
EmbeddedLyrics and EmbeddedLyricsUi regression tests PASS. Actual Android
OpenDocument/audio-service/import flow is still unverified. Interaction/lifecycle
release-consumer checks are next. Initial sample build
failed on Activity `visible`/`setVisible` collision; renamed `hostVisible`.

Sample integration committed `44830ff`. Native input now hits before consuming
down; blank points pass to parent; movement beyond slop, second pointer, cancel
or parent consumption cancels click/long press. Focus/semantics/source mapping
continue through existing Partita. `:library:jvmTest --tests '*PartitaNativeInputTest*'
--tests '*RendererBridgeTest*'` PASS (3 tests); Android wrapper compiled. This is
gesture logic/offscreen validation, not yet an Android physical-touch result.
The isolated release/R8 lifecycle probe passed. It verifies sizing/font/reduced
motion, scene rejection and fallback, offset and seek, paused/inactive submission,
source semantics, blank touch, recreation and two instances. See
[lifecycle evidence](../verification/native-renderer-p3.md). The renderer defers
GPU initialization until a valid main scene and keeps the surface across
Partita tuning and size changes.

The standalone Kotlin coordinate consumer passed JVM render/callback and Android
compilation on November 19. The check covers both POM and Gradle module metadata
and rejects native libraries inside the base AAR/JAR, including its nested classes JAR.
The first run lacked an SDK path. The passing run used `ANDROID_HOME` and disabled
the optional CPU mask experiment during local publication.

The P4 local Maven archives passed static checks for both runtime ABIs, 63
locked crates and matching licenses, paired line-table symbols, and ELF LOAD
alignment. See [packaging evidence](../verification/native-renderer-p4.md).
The release APK also passed 16KiB zipalign. ARM64 and 16KiB OS runs remain open.

## Remaining

- P3: real Android sample playback and imports, platform touch cancellation/long press, foreground/background and multi-scene visual comparison.
- P4: repeated lifecycle, device error and resource-pressure evidence, ARM64/16KiB OS behavior, and paired hardware performance.
- Only after the first theme closes: trace-led second theme, then separately validated desktop host.

## P0 observations and fixed acceptance conditions

- Latest historical real-device capture identifies Partita software blur (25.01% inclusive samples); `docs/performance.md` records the later same-algorithm Kotlin optimization. These are targeting evidence, **not** a current GPU performance baseline.
- Android Partita currently uses CPU-prepared grapheme shadows, per-word blur and nested line/row/word layers. Preserve the existing layout/motion functions, full shaped word boundaries and the actual blur work.
- Minimal native dependency direction: JNI + Android native window + wgpu Vulkan + raw-window-handle + synchronous async initialization helper. No iced text system, player, network, QuickJS, keyring, fonts or reference application bridge is necessary for prepared textures.
- Start with pinned Rust 1.91.1, NDK 28.2.13676358, wgpu 27.0.1. The reference's declared Rust 1.99.1 is not assumed installed or required by this smaller graph.
- Fix comparison inputs before measurement: same core fixture/font/view size/density/fontScale/quality/palette/offset/motion/media time; no image registration, alpha removal or reduced effects.
- Performance gates remain P95 whole-frame >=20% improvement, reliable over-budget count >=30% reduction, P99 <=5% regression, three alternating paired rounds. Unknown present/GPU measurements remain unknown. Default stays Kotlin until all gates pass.
- Connected physical device is user-owned/unassigned: no install, reset, interruption or capture is authorized there. Create a separate task-owned emulator if available. Historical AVDs and captures do not prove new native runtime success.

## Verification log

- Git identity configured; no staged changes at start.
- Android SDK and pinned NDK present; project-local Rust 1.91.1 present.
- ARM64/x86_64 locked builds, AAR publication, Rust 2/2 tests on Android and isolated release/R8 packaged runtime probe: PASS. Exact hashes, initial failures and scope are in `verification/native-renderer-p1.md`.
- Partita scene adapter + real GPU submit in isolated minified consumer: PASS; 6/6 Rust Android tests; 2/2 bridge tests and existing Partita suite PASS. Paused diagnostics no longer resubmit unchanged frames. See P2 evidence for exact limits and open visual differences.
- ARM64 runtime, actual Partita visual/interaction/lifecycle/performance and 16KiB OS: pending.

## Next action / acceptance commands

Compile/test the thin native sample and original sample; persist backend per theme with old-settings compatibility. Then extend release consumer with lifecycle/failure/interaction matrix. Keep defaults Kotlin and all unverified hardware/visual/performance gates explicit.
