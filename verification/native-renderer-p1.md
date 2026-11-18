# P1 platform / AAR verification — 2026-11-18

This milestone verifies a transparent native surface only, not a lyrics theme,
visual equivalence, physical presentation timing or performance improvement.

- Producer: Rust 1.91.1, locked wgpu 27.0.1, NDK 28.2.13676358, Android API 26.
- ARM64 and x86_64 release builds passed. AAR has both standard `jni/<abi>` entries,
  consumer JNI keep rules, notices and Kotlin/Rust source artifacts.
- `:native-renderer-android:assemble` and both library local Maven publication tasks passed.
- Rust tests cross-compiled for x86_64 Android and actually executed on a new,
  task-owned API 34 emulator: 2 passed (bounded dimensions, nonrecycled independent handles).
- Emulator: x86_64, API 34, SwiftShader Vulkan, 800×600 configured viewport,
  density 160, actual OS page size 4096. No physical-device app was touched.
- Independent consumer copied outside the checkout with a distinct Gradle home,
  private copy of Maven artifacts, controlled PATH containing no Cargo/rustc,
  no project/includeBuild references and no consumer native build tasks.
- Consumer `assembleRelease`, `assembleReleaseAndroidTest`, R8 and
  `verifyConsumerTaskBoundary` passed. The minified APK/test APK installed and ran:
  `PASS release/R8 packaged AAR ABI=1 and Vulkan TextureView submit/present`.
- Exact AAR → APK native binary SHA-256 checked after disabling redundant consumer
  stripping (producer already strips debug info):
  - ARM64: `0d4ae21f5efdc1c50c0729596e66dc183d3224eca89a32f62c68660e16982b3a`
  - x86_64: `753fdced5bc0c288fb4464865fb7f17f5dd41a5418b7989c6792f3d4522a6410`
- ARM64 ELF LOAD alignment is `0x4000`; NEEDED entries are libandroid.so,
  libdl.so, libm.so, libc.so. ARM64 execution and a 16KiB OS remain unverified.

Failures retained in ignored execution logs: cold consumer dependency resolution
took 10m47s; first verification used the wrong root-project APK basename; subsequent
comparison exposed consumer symbol stripping. Both verifier issues were fixed
without weakening binary provenance. First Vulkan initialization returned -14
because Android exposed INHERIT-only composite alpha. Nonopaque TextureView's
Android alpha contract is now accepted; the repeated release runtime probe passed.

Reproduce producer with `:native-renderer-android:publishAllPublicationsToVerificationRepository`
and base publication, then run `python scripts/verify-native-consumer.py --sdk <sdk>
--java-home <jdk25> --serial <task-owned-emulator>`. Without serial it reports build
verification only. No remote publication, push, user-player interruption, visual
or performance claim was made.
