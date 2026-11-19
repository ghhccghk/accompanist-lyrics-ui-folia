# Rust renderer execution record

Updated: 2026-11-19. Starting commit: `fdcc1e5`, branch `codex/initial-implementation`.
This is the single current execution checklist; the design and implementation prompt remain the requirements.

## Completed

- Read both implementation documents and inspected the existing Partita preparation/draw path, sample playback, publication and consumer checks.
- Verified the read-only reference checkout is `d7ff03e083d28069c973cf204b4a8f0a9d5c8594`.
- Existing native code is CPU mask/colour only. No renderer was present at the starting commit.
- Preserved and excluded the pre-existing release signing hunk in `sample/android-app/build.gradle.kts` and untracked `gradle/gradle-daemon-jvm.properties`.

## Current: Android Diorama text preview and native acceptance

P1 platform milestone passed (`ad21f29`); evidence: `verification/native-renderer-p1.md`. P2 real core/prepared Partita + GPU passes, 6 Android Rust tests, existing Partita and bridge JVM tests, isolated release/R8 native consumer and paused frame stability passed; evidence: `verification/native-renderer-p2.md`. This is not complete visual/performance acceptance. Implement and validate the thin sample, persisted per-theme backends, failure/lifecycle and interaction behavior next.

P2 committed as `c0038b6`. Thin native sample and original Android debug APK both
build. Settings 11/11 pass, including per-theme backend persistence and old input.
Owned-emulator UI: song selection/LRC/settings/demo remain present; selecting
Partita + NativeRequired reports `NativeGpu Ready:PartitaMainAndGlow`. Force-stop
and cold restart retain the requested backend/theme (paused demo restarts at 0;
`Preparing` before a main scene is expected). Existing LocalSong, SongSelection,
EmbeddedLyrics and EmbeddedLyricsUi regression tests PASS. Initial sample build
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
locked crates and matching licenses, paired line-table symbols, ELF LOAD
alignment and 16 KiB APK alignment. The release/R8 lifecycle matrix passed on
Android 35 x86_64 with 16384-byte pages. See
[packaging evidence](../verification/native-renderer-p4.md). ARM64 runtime remains open.

The native sample passed Android OpenDocument selection, timed embedded tags,
real MediaPlayer playback in the `:playback` process, external LRC attachment,
and seek. Returning from Home preserved paused playback and lyrics selection.
See [the sample check](../verification/native-renderer-p3.md).

The first release-consumer visual matrix isolated a peak error of 97/255 at
Partita guides. Scene protocol v2 lets opaque, unfiltered groups replay directly
under their composed transform. Forced offscreen transitions, group alpha and
blur still use isolated layers. The independent release/R8 lifecycle and source,
symbol and 16 KiB package checks passed again. See
[visual results](../verification/native-renderer-visual.md) for the paired rerun.
The release consumer also captured three emoji and seven subtitle timing pairs.
The subtitle band matched through appearance and removal. Isolated main-stage
pixel peaks remain open.

At the user's direction, P5 implementation started while the first theme's
remaining acceptance work stayed open. The Android Diorama preview reuses the
Kotlin camera, shaped raster window, plane order, near/far clipping and source
objects. The optional AAR draws the projected lyric masks, fog, glow and soul
layers. The explicit backend picker selects it for Diorama. The standalone
Release/R8 consumer submitted visible Diorama frames on the API 34 x86_64
emulator at six media samples, including forward and backward seeks. A one-byte
texture budget exercised `NativePreferred` fallback. The full Partita lifecycle
probe passed with the same artifacts. The sample debug APK built successfully.

The first preview used the ordinary Android plane baseline and RGBA8 blending.
The first projective protocol was v3. Partita continues to emit scene v2.
The JNI ABI remains 2. Local snapshots: core Android `0.1.0-20261119.040652-28`,
native Android `0.1.0-20261119.040652-16`.
The next increment uses an F16 attachment for the projective text pass and
checks blend and filter support on the Vulkan adapter. Unsupported devices
follow the requested backend's failure policy. Attachment bytes count toward
the existing GPU budget. Local native snapshot `0.1.0-20261119.041541-17`
passed the API 34 Release/R8 Diorama smoke and Partita lifecycle checks.

Scene v4 adds the calibrated mip material for neighbor and body textures.
Vulkan builds straight linear RGB and alpha mip levels on demand within the
current scene's GPU budget. Projection controls up to four trilinear taps.
The renderer tone maps the sampled material before fog, then composites in
F16. Glow and soul use their prepared coverage at level zero. Replacing a
raster window retires its mips along with the source textures. Debug APK and
coordinate-only Release/R8 builds passed. The API 34 consumer rendered the
baseline at 1500 ms, then passed seeks to 3300, 3550, 900, 3700 and 1500 ms.
The same native binaries also passed the API 34 Partita lifecycle probe.
The final local native snapshot `0.1.0-20261119.042845-20` passed static
checks for both ABIs, symbol IDs, 16 KiB ELF and APK alignment, and shader
source and license packaging. The matched visual and performance acceptance
matrix remains open.

## November 19 review follow-up

- R1: isolated native session failures and made destroy status visible. The
  same-process Vulkan regression reproduced A's `-99` poisoning B with `-98`
  under the baseline lock. With per-session locking, A and B drew, A failed
  at the test-only upload injection, B drew again, A retired twice, and C
  created and drew. A separate concurrency test kept B usable while destroy
  waited for A's admitted render. Run with `scripts/test-renderer-android.ps1`
  on the API 34 x86_64 AVD using `-gpu host` (NVIDIA). SwiftShader and Lavapipe
  behind gfxstream instead reported `Failed to unbox VkDevice` after A's
  destruction and B returned a new GPU error rather than `-98`; do not claim
  this driver-level failure is fixed. The first OOM from the review remains
  unclassified. This tests unwind, not abort or a native crash.
- R1 host behavior (`45eff40`): API 35 x86_64 / 16 KiB, Release/R8,
  `--availability session-fault` passed. After a real frame, the test-only
  consumer passes an invalid texture ID through JNI. Upload returns `-42`
  and the regular worker reports `Failed:Render:-4` on its next request.
  Preferred falls back to Kotlin and returns the original lyric line on click
  at 1516 ms. Required exposes the failure. Both workers retire and repeated
  destroy succeeds.
- R2: guarded the test consumer's API 30 window calls without changing
  minSdk 26. The independent Release/R8 APK without the renderer `.so`
  passed `availability=absent` on separate API 26, 28, and 29 x86_64 images:
  Kotlin source action at 1500 ms, Preferred fallback with the same source
  and position, seek to the second lyric at 3400 ms, and explicit Required
  failure. The with-runtime API 29 attempt loaded the library but had no
  compatible Vulkan surface adapter (`Create:-11`); no native frame is
  claimed on API 26¨C29. `scripts/verify-native-consumer.py --availability
  absent` builds the control; `--availability present` requires a real submit.
- R3: API 34 x86_64 (`-gpu host`, NVIDIA), fresh process: the wall-clock
  playback window, 98 baseline and 76 wrap timestamps, theme changes,
  seeks, font and viewport changes completed. Estimated renderer texture
  bytes reached 162,685,248 in wrap, plateaued from 1900 ms to the end
  and fell on theme replacement. PSS checkpoints ranged from 40,905 to
  77,762 KiB. This does not reproduce the original SwiftShader OOM or
  prove its cause. A separate offscreen Vulkan process measured 137,088
  bytes of source, 365,312 bytes of Diorama mips, 29,582,400 bytes of
  Partita scratch (upper bound before the fence, idle after), and 137,088
  bytes of pending upload payload before submission. These are texture and
  payload estimates, not actual GPU allocation sizes. Retirement returned
  to the target-only baseline. No production budget or policy changed.
  Reproduce using `scripts/probe-native-pressure.py` and
  `scripts/test-renderer-android.ps1 -Pressure` on a task-owned emulator.
- R4 (`b989b0b`, `8e38cfd`): captured eight Partita and eight Diorama pairs
  against Android Kotlin on API 34. Matched baseline browser captures also
  ran for both themes against the pinned Folia text stages. On API 35,
  both Diorama backends passed the Release/R8 paired tap, long press, source
  identity, drag and pointer cancellation, blank touch and paused transition
  input checks (`--availability diorama-input`).
  Partita lifecycle passed. Partita edge coverage, broader Diorama visual
  differences, the wider interaction matrix and ARM64 performance remain
  open. See [Android and upstream results](../verification/native-renderer-visual.md).
- R5 (`71593c2`, `ae5c1c9`): locally published matching Kotlin and native
  candidates. RC.2 ran all 76 producer tasks and passed both Release/R8 app
  probes on the API 35 x86_64 16 KiB emulator. Core packages, runtime, symbols,
  16 KiB alignment and dependency archives checked out. The offline Rust
  source rebuild outside the checkout needed nine additional locked Windows
  host crates. See [candidate evidence](../verification/native-renderer-candidate.md).

## Remaining

- P3: residual main-stage edge coverage. The real playback sample passed three
  Home round trips and a rotation while playing, with pause on stop and resume
  at the retained position. See the P3 evidence for both process PSS readings.
- P4: ARM64 runtime and paired hardware performance. Resize and scene-retirement
  Vulkan tests now account for reclaimed scratch texture bytes; see P4 evidence.
- P5: matched Diorama visual, interaction, device capability and performance
  validation. The Vulkan mip and F16 implementation is in the experiment.
- P6: separately validated desktop host and independent platform consumer.

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
- November 19 follow-up: 40 targeted Partita/bridge JVM tests passed; 8 Rust Vulkan tests passed on the API 34 emulator. The release/R8 lifecycle probe passed on both API 34 / 4 KiB and API 35 / 16 KiB x86_64 emulators using snapshot `0.1.0-20261119.020648-14`; the isolated consumer built with no Rust or NDK tasks. The sample debug APK rebuilt.
- Matched release-consumer captures: 13 original Latin/wrap/Arabic/CJK/bidi/cluster/large/waiting/subtitle pairs, 3 emoji timestamps and 7 subtitle timestamps. Full aggregate metrics and residual pixels are in `verification/native-renderer-visual.md`; the subtitle region matched exactly during enter/hold/exit.
- Real playback: three Home transitions and one 800x600 to 600x800 rotation retained the embedded song and paused media time; explicit resume advanced from the retained position. UI and playback PIDs remained stable. Per-process PSS is recorded in `verification/native-renderer-p3.md`.
- Scene resize and independent retirement checks verify resident GPU bytes return to baseline after four viewport sizes, over-budget allocation is rejected without counter growth, and two scenes release independently.
- Fixed `Render:-40` during large Partita transitions by sizing isolated GPU
  layers to their drawn content and blur footprint. The independent Release/R8
  consumer passed the new 2160x2400 transition regression on both x86_64
  page sizes. See [budget regression evidence](../verification/native-renderer-p4.md#large-partita-viewport-budget-regression).
- ARM64 runtime and paired physical-device performance remain unverified. Local SwiftShader provides Vulkan correctness, not hardware performance. Partita visual edge differences remain documented.

## Next action / acceptance gate

Keep this theme on the experimental, explicitly selected path. The first-theme
exit gate remains: resolve the measured local visual differences without
changing the Kotlin scene or reducing effects, and collect a paired physical
performance trace. Continue the explicitly selected Diorama preview in parallel
under the same acceptance criteria. ARM64 runtime needs a suitable host or
device. The connected physical device remains out of scope.
