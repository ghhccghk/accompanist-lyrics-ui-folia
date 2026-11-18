# Rust renderer execution record

Updated: 2026-11-18. Starting commit: `fdcc1e5`, branch `codex/initial-implementation`.
This is the single current execution checklist; the design and implementation prompt remain the requirements.

## Completed

- Read both implementation documents and inspected the existing Partita preparation/draw path, sample playback, publication and consumer checks.
- Verified the read-only reference checkout is `d7ff03e083d28069c973cf204b4a8f0a9d5c8594`.
- Existing native code is CPU mask/colour only. No renderer was present at the starting commit.
- Preserved and excluded the pre-existing release signing hunk in `sample/android-app/build.gradle.kts` and untracked `gradle/gradle-daemon-jvm.properties`.

## Current: P2 prepared Partita scene and GPU passes

P1 platform milestone passed: separate minSdk 26 AAR, dedicated JNI library, versioned multi-instance handles, ordered TextureView lifetime, coordinate-only release/R8 consumption and actual Vulkan submit/present on a new owned x86_64 emulator. Evidence: `verification/native-renderer-p1.md`. This is not theme completion. Implement the narrow prepared-scene adapter and GPU text/blur/composition next.

## Remaining

- P2: narrow experimental prepared-scene bridge; real core objects stay in Kotlin, whole-run shaping and cache invalidation, prepare-only texture transfer.
- P3: complete Partita GPU blur/glow, transforms, guides, overlays, hit/source mapping, backend selection and explicit fallback/required failures; thin native sample.
- P4: independent release/R8 consumer, packaged sources/notices, lifecycle/error/resource tests, visual and paired performance evidence.
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
- Partita scene adapter initial JVM compilation: PASS, not yet submitted to native GPU.
- ARM64 runtime, actual Partita visual/interaction/lifecycle/performance and 16KiB OS: pending.

## Next action / acceptance commands

Complete the prepared Partita bridge and bounded native command parser/GPU passes, run scene/FFI tests and paired synthetic runtime captures, then commit the feature separately. Keep P1 and future GPU/theme evidence distinct.
