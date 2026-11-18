# Partita lifecycle and independent release consumer

November 19, 2026 (Asia/Taipei). API 34 x86_64 task-owned emulator,
SwiftShader Vulkan, 800×600 at 160 dpi, 4096-byte OS pages.
Published `0.1.0-20261118.230350-10` to the local Maven repository.
`verify-native-consumer.py` built release and release instrumentation APKs
with R8 in a project and Gradle home outside the checkout, using the coordinate
without Rust on PATH or a native build task. Both `.so` entries match the AAR.

`NativeLifecycleProbe` passed the following in one process:

- Real Partita GPU submission and ten stable paused observations.
- Seek 1500 → 850 → 1500 with no extra upload. The shared offset path samples
  1500 ms for a 1600 ms host snapshot and 100 ms offset.
- Classic Preferred returns Kotlin with `UnsupportedTheme:Classic`.
  Required reports failure. Both remove the native view.
- A 1-byte coverage-copy budget makes actual preparation fail for Preferred
  and Required without moving the host clock. Native recovers with the normal budget.
- Accessibility click returns the original lyrics-core object. Touch in blank
  space reaches the parent.
- 640×480, fontScale 1.5 and reduced motion submit the requested sample.
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

The native sample was also tested through Android DocumentsUI. The sample APK
SHA-256 was `5422e47fae217fc5c27eb87070711d88d44f92e750438c728dc1319dd8b7eb20`.
Open the 12-second synthetic `uslt-v23.mp3` from
`verification/generate-embedded-audio.py` as the audio file. The UI showed
`内嵌计时歌词`, advanced to 0:04 with `Echo 回声`, and MediaPlayer reported an
active NuPlayer/AudioTrack for the sample's separate `:playback` PID. The input
is silent PCM encoded as MP3, so this checks decode, transport and lyric timing.

Next use **补选歌词／翻译** to select a UTF-8 LRC with `Amber` at 6 s and `Echo`
at 8 s, then **应用歌词**. The UI changed to `外部歌词`. On seeking to 0:08 it
showed `Echo`, and settings reported `NativeRequired` / `NativeGpu` /
`Ready:PartitaMainAndGlow`. MediaPlayer stayed in the same playback process.
Returning from Home preserved the selected file, external lyrics and paused 0:08
position. This run covered one foreground/background transition while paused.

Remaining runtime checks include platform line long press and drag/cancel,
repeated churn with memory samples, and ARM64 and 16KiB OS runs. Submission
stability here does not measure physical presents or performance.
