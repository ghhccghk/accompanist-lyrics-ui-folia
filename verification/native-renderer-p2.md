# P2: prepared Partita text and actual GPU passes

Date: 2026-11-18. Parent platform milestone: `ad21f29`.

The optional Android entry point now consumes real lyrics-core objects through a
version-1 experimental bridge. Compose performs the existing whole-run shaping,
layout and motion sampling. Rust uploads prepared coverage once per scene, runs
the real shadow/blur/composition passes and submits to the Vulkan TextureView.
This is **CPU-prepared text + Rust GPU drawing**, not Rust shaping. Source objects
remain in Kotlin. Subtitles, secondary lines and finite waiting dots use the base
Kotlin composables. There is no player or new media clock in native code.

## Checks actually run

- `:library:jvmTest --tests '*Partita*'`: PASS (existing Partita regression suite).
- `:library:jvmTest --tests '*RendererBridgeTest*'`: PASS, 2 tests. Checks core
  identity, semantic callback, both shadow radii, forbidden CPU mask invocation,
  unchanged scene across media updates, reverse seek reconstruction and matrices.
- `scripts/test-renderer-android.ps1 -Sdk <sdk> -Serial emulator-5580`: PASS,
  6 tests on API34 x86_64 SwiftShader Vulkan, 4KiB OS pages. Includes bounded
  protocol/handle tests and actual GPU readback compared with independent f64
  reference convolution. Fixed maximum differences: 2/255 per mask, 4/255 for
  two-layer ink-alpha composition. Covers sigma 1, 2.25, 10, 20, 40, 128 and
  small Gaussian 0.25, 0.7, 1.6; transparent borders and 1/255 coverage included.
  Readback/COPY_SRC is test-only and absent from production textures.
- Local Maven publication of both modules: PASS, including final target-key fix.
- `scripts/verify-native-consumer.py --sdk <sdk> --java-home <jdk> --serial
  emulator-5580`: PASS, independent release + R8 coordinate consumer with no
  cargo/rustc on PATH, real core Partita submission and paused frame stability
  sampled ten times over one second after handoff settles. Runtime JNI ABI is 2;
  scene version is separately 1. AAR/APK runtime bytes match exactly.

The initial GPU numerical test failed because default device limits requested a
64KiB uniform binding against the emulator's 16KiB limit. It now uses the same
downlevel/resolution limits as production; numerical assertions were unchanged.
An earlier diagnostics->invalidate->submit loop was fixed with frame structural
deduplication and worker packet-ID deduplication.

Packaged runtime SHA256 at this milestone:

| ABI | SHA256 |
| --- | --- |
| arm64-v8a | 5fa41b547d4e5a3cdec7e4f9857443d11f763a4d5c155d6109b4e392f6e9bd7c |
| x86_64 | 3b7ae1965a7b42d2ce8b2089526045ad82940c73b0797b231ed547048eaf1e65 |

## Visual observation, not acceptance

Task-owned emulator captures in `.work/renderer-visual/baseline-{native,kotlin}.png`
use the same licensed Latin fixture font, 800x600 density 1, media 1500ms, guides
and full quality. Background samples match RGB (22,36,53). Excluding the fixed
40px diagnostics area gives mean RGB absolute error 0.1409955, P99=2, max=86.
These whole-image values contain much background and do **not** prove foreground
or worst-local equality. Cross-backend quality tolerance and broader local visual
matrix remain pending. No quality setting or convolution assertion was relaxed.

## Boundaries still requiring verification

- Submission ACK drives hit geometry; no reliable physical-present timestamp or
  displayed frame-ID measurement is available. Neither submit count nor host
  callback rate is presented as physical FPS.
- Dedicated native sample, per-theme persisted backend, interaction pass-through,
  lifecycle/failure/multi-instance matrix and full source/license bundle follow.
- Texture accounting is bounded (192MiB); it excludes CPU copies, staging,
  driver/swapchain and total process memory. It is not a total-memory claim.
- ARM64 and 16KiB OS runtime, hardware performance, sustained playback and paired
  performance gates are unverified. SwiftShader is functional evidence only.
- Default remains Kotlin and the renderer remains experimental. No remote
  publication, push, original Apache project edit or user-device install occurred.
