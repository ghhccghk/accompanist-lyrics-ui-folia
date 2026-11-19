# Partita lifecycle and independent release consumer

Verified November 19, 2026 (Asia/Taipei) against local Maven snapshot
`0.1.0-20261118.230350-10`. The consumer uses only the published coordinates,
with no Rust on PATH or native build task. Release and instrumentation APKs
were minified with R8.

The release probe passed the following scenarios in one process:

- Partita GPU submissions and ten stable paused observations.
- Seek 1500 to 850 to 1500 ms without another texture upload.
- At a 1600 ms host time and 100 ms offset, the sampled frame remains at 1500 ms.
- Unsupported Classic: Preferred displays Kotlin with an explicit reason;
  Required reports failure. Both remove the native view.
- A 1-byte prepared coverage-copy budget makes preparation fail for both
  backends without moving media time. Restoring budget recovers native rendering.
- The click semantics action returns the original lyrics-core object.
- A tap clicks the line; long press reports the long callback. Drag and
  ACTION_CANCEL suppress both callbacks. Touch in blank space reaches the parent.
- 640x480, font scale 1.5, reduced motion and guide changes submit correctly.
  Activity recreation restores the request and creates a new TextureView.
- Two instances render separately. Removing one leaves one view. An inactive
  view submits no frame; resume submits the updated sample.
- Three Kotlin/native transitions retire each old renderer worker before the
  replacement worker is counted.

The API 34 baseline run and API 35 / 16 KiB runtime run both passed. On API 35,
the system initially displayed the one-time immersive-mode confirmation over
the app, blocking the accessibility tree. The task-owned emulator was configured
to suppress that confirmation and the full probe passed.

The native sample was tested through Android DocumentsUI using the synthetic
12-second silent MP3 from `verification/generate-embedded-audio.py`. The sample
showed timed embedded lyrics and advanced to the second line at 0:04. Its separate
`:playback` process owned an active NuPlayer and AudioTrack. This verifies media
decoding and timing, not audible output.

In the same playback session, an external LRC was selected and applied. The
source label changed to external lyrics. Seeking to 0:08 showed the second line;
settings reported `NativeRequired`, `NativeGpu` and `Ready:PartitaMainAndGlow`.
Returning from Home preserved the selected file, external lyrics and paused
0:08 position.

The initial lifecycle version had one emulator-only `Create:-12` and encoder
hang during frequent view replacement. Deferring Vulkan initialization until a
nonempty scene was insufficient. Reusing the surface for tuning and size changes
allowed the entire matrix to pass. The earlier error was not isolated.
Submission diagnostics here do not measure physical presents or performance.

The scene v2 sample at `d93f038` also passed three Home round trips while
playing the same embedded-lyrics MP3 and one configuration rotation. The
sample intentionally pauses in `onStop`. Returning preserved the song and
embedded lyrics, and Play continued from the retained position. UI PID 8405
and playback PID 8831 stayed the same. Android changed the viewport from
800x600 to 600x800. The restored sample reported `NativeRequired`, `NativeGpu`
and `Ready:PartitaMainAndGlow`. Logcat showed no ANR or fatal crash for the run.

| Operation | Playing before (s) | Paused on return (s) | Playing after resume (s) | UI PSS (KiB) | Playback PSS (KiB) | Combined PSS (KiB) |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Before stress | | | | 95699 | 48858 | 144557 |
| Home 1 | 1 | 3 | 5 | 107754 | 48706 | 156460 |
| Home 2 | 4 | 5 | 7 | 119472 | 48793 | 168265 |
| Home 3 | 3 | 5 | 6 | 106896 | 48785 | 155681 |
| Rotation | 3 | 5 | 6 | 116226 | 48999 | 165225 |

Each PSS reading is a `dumpsys meminfo <pid>` snapshot. These short runs did
not isolate Vulkan driver memory or establish long-term memory stability.
The raw observations are in the ignored `.work/renderer-playback-stress.json`.
