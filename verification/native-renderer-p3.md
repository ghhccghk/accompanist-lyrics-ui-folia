# Partita lifecycle and independent release consumer

November 19, 2026 (Asia/Taipei). API 34 x86_64 task-owned emulator,
SwiftShader Vulkan, 800¡Á600 at 160 dpi, 4096-byte OS pages.
Published `0.1.0-20261118.230350-10` to the local Maven repository.
`verify-native-consumer.py` built release and release instrumentation APKs
with R8 in a project and Gradle home outside the checkout, using the coordinate
without Rust on PATH or a native build task. Both `.so` entries match the AAR.

`NativeLifecycleProbe` passed the following in one process:

- Real Partita GPU submission and ten stable paused observations.
- Seek 1500 ¡ú 850 ¡ú 1500 with no extra upload. The shared offset path samples
  1500 ms for a 1600 ms host snapshot and 100 ms offset.
- Classic Preferred returns Kotlin with `UnsupportedTheme:Classic`.
  Required reports failure. Both remove the native view.
- A 1-byte coverage-copy budget makes actual preparation fail for Preferred
  and Required without moving the host clock. Native recovers with the normal budget.
- Accessibility click returns the original lyrics-core object. Touch in blank
  space reaches the parent.
- 640¡Á480, fontScale 1.5 and reduced motion submit the requested sample.
  Activity recreation restores the request and creates a new TextureView.
- Two instances submit independently. Removing one leaves one TextureView.
  Inactive updates do not submit. Reactivation submits the updated sample.

Earlier versions exposed a Vulkan device creation failure and a host encoder
hang during frequent view replacement. Defer the Vulkan session until a
nonempty scene is available, and reuse the session when only Partita appearance
settings or viewport change. With deferred creation alone the resize case still
failed with `-99`. After the reuse change the entire matrix passed. This does
not isolate the initial driver error. The worker now preserves the first
failure stage and code for future runs.

Remaining runtime checks include actual foreground/background transitions,
physical line long press and drag/cancel, repeated churn with memory samples,
real sample audio and imports, and ARM64 and 16KiB OS runs. Submission stability
here is not a physical present or performance measurement.
