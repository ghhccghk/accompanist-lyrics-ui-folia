# Real-song stutter investigation — 2026-11-16

User played their own song, asked to start sampling. No playback UI interaction,
media extraction, screenshots, player state changes or installation during capture.
Device: 2509FPN0BC, arm64-v8a, API 37, physical 1200×2608, density 480.
Installed debug APK: ebcaffd checkpoint, native-enabled artifact described in
`../native-mask/device-load-fix/android-apk.json`. Actual Rust switch and quality
were not recorded. Stack identifies Partita, not Classic, as the active stage.

`simpleperf record --app top.ghhccghk.accompanist.folia.sample -e cpu-clock
-f 250 -g --duration 15` recorded 3,390 samples, zero lost, reported duration
14.9219s. Inclusive report: SoftwareBlurKt.blurPremultiplied 25.01%, including
blurBoxCoverage 13.33%; ClassicLayer.record 34.48%; blurredMasks 3.81%.
Inclusive percentages overlap and must not be added. This is CPU sampling,
not elapsed-frame attribution. Most sampled work was on the main thread.

Raw traces/reports remain in ignored `.work/folia-song-stutter.data`,
`song-{cpu,inclusive,samples,gfxinfo,meminfo}.txt`; do not commit user sessions.
`analyze-sample.py` produces the sanitized `song-sample.json`. Text export has
3,367 stacks (not all recorded samples); its percentages use that denominator.
The retained frame ring overlaps only the latter part of capture: 76 records,
all with raw flags 32, P50 16.38ms, P95 1451.05ms, max 1563.46ms between
IntendedVsync and FrameCompleted. We retain flags without interpreting their
platform-specific meaning. These are neither complete-session FPS/jank metrics
nor evidence of UI improvement after the fix. Historical gfxinfo aggregate
percentiles are intentionally not used as a controlled baseline.

## First measured fix: avoid Bitmap dimension JNI in pixel loops

`SoftwareBlur.kt` previously read `source.width` in the transparent-padding scan
and for every cropped pixel. On Android those are Bitmap/JNI accesses. Cache
the two immutable dimensions once, pass explicit readPixels extents, and copy
cropped rows with copyInto. Keep the exact bounds, kernel, premultiplication,
channel order, rounding, output origin, and output pixels. No effect reduction,
software-fallback removal, native dependency or clock/timing change.

`SoftwareBlurDeviceProbe.java` is an app_process main in androidTest sources.
It runs in its own shell process with a supplied APK classpath: no install,
Activity or instrumentation attach to the playing app. It creates only synthetic
colour bitmaps, warms each case eight times, then measures 15 operations.
1200×500 is a padded image probe, not the user's font/lyric/viewport fixture.
One exploratory before/after pair, uncontrolled thermal/background load:

| Sigma | Before P50 ms | After P50 ms | Reduction |
| --- | ---: | ---: | ---: |
| .25 | 13.147 | 9.897 | 24.72% |
| .5 | 17.188 | 13.692 | 20.34% |
| 2 | 20.306 | 16.604 | 18.23% |
| 6 | 23.115 | 19.172 | 17.06% |

Eight cases have identical dimensions, packed origins and 32-bit ARGB array
hashes; this initial probe did not yet emit SHA-256. The current source additionally
emits SHA-256 and exits nonzero on failure. The after measurement loaded the
new SoftwareBlurKt DEX ahead of the unchanged baseline APK; this was the first
dimension/crop patch, before spelling out equivalent readPixels defaults.
Two overlay attempts missing the Compose graphics D8 classpath failed before
measuring; adding the exact graphics classes fixed the harness. No failed runs
enter the measurements. The device disconnected before repeated alternating
runs and a final-APK device check. Do not claim statistically established gains,
whole-renderer speedup, a resolved playback stall, or final-APK runtime verification.

`compare-probes.py` checks case sets, origins, sizes and every available digest,
then emits `software-blur-comparison.json`. The frozen ebcaffd implementation
in JVM tests supplies byte-exact coverage checks for 405 combinations of size,
padding, transparent RGB, clipping, Gaussian and box-filter sigma.

To rerun without installing a package: build `:sample:android-app:assembleDebug`
and `:sample:android-app:assembleDebugAndroidTest`, push the application and test
APKs to `/data/local/tmp/folia-blur-before.apk` and `folia-blur-probe.apk`, then:

```sh
adb shell 'CLASSPATH=/data/local/tmp/folia-blur-before.apk:/data/local/tmp/folia-blur-probe.apk app_process / top.ghhccghk.accompanist.folia.sample.SoftwareBlurDeviceProbe'
```

Repeat with a candidate APK and alternate variants. For a Rust user-test build,
retain the native opt-in flag from `docs/native-mask-sample.md`. A plain debug
build must not replace the user's native-enabled package.

Next: bound the tiny-sigma fallback's nested software recording, transparent
groups, synchronous line preparation and retained surfaces using the captured
hotspot. A CPU dimension fix cannot alone establish that the 1.56s stalls are fixed.
