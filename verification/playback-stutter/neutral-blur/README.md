# Exact reuse of neutral colour channels — 2026-11-16

The optimized-APK capture still contains ~20% inclusive software colour blur and
~40% layer recording. This change eliminates duplicate convolution inputs:
actual white pixels need only alpha filtering; actual neutral pixels need alpha
and one RGB filtering. A coloured pixel, even the last pixel, rejects that
classification. Zero-alpha RGB is ignored as in premultiplied filtering. The
general colour kernel, all sigmas, rounding, sizes/origins and shaping remain
unchanged. No palette approximation or render-resolution reduction is involved.

`FullChannelSoftwareBlur.kt` freezes the production function at 4962f62; it uses
the unchanged shared box kernel and production output type. 360 targeted image
cases include 0/1/2/127/128/254/255 alpha, white, variable grey, black, red,
arbitrary colour, transparent colour and last-pixel counterexamples. All output
dimensions, origins and pixels match. The existing 135 colour comparisons,
120 nested-layer cases and CSS blur spread checks pass. 78 freshly captured
Partita / classic software frames match the previously committed hashes exactly,
with their existing upstream geometry, pause and reverse-seek assertions active.

## Device kernel measurement

Physical 2509FPN0BC / arm64 / API37, supplied baseline/candidate APKs loaded by
separate `app_process` invocations. No installation, Activity, song access or
player operation. The user-configured playback continued, so CPU/thermal/load
conditions were uncontrolled. This is a synthetic CPU function measurement,
not an application frame/PSS benchmark. 97x43 and padded 1200x500 inputs,
sigma .25/.5/2/6, white/neutral/colour, 8 warmups then 15 calls per case;
two pairs reversing baseline/candidate order. All **48 paired output SHA-256,
ARGB hashes, sizes and packed origins match**.

For the large padded white input, sigma .25 P50 is 12.91→4.02ms and
13.17→4.01ms in the two pairs. Neutral input is 13.07→5.67ms and
13.10→5.58ms. Other sigmas and colour controls are retained in `device.csv`;
not all small inputs improve, and host/JIT/GC noise is visible. Managed array
allocation is essentially unchanged; this patch does not claim a PSS reduction.

The initial candidate classified during the full bounds scan and branched inside
the packing loop. Device colour controls regressed roughly 4–11%; it was rejected.
`initial-device.csv` retains those measurements. Final classification exits on the
first non-neutral visible pixel and packing dispatch occurs outside pixel loops.
Final large colour controls are near baseline or faster in these two pairs
(one .5 case is ~1% slower); no timing assertion or excluded unfavourable sample.

`jvm.csv`: Windows x64 / i7-12650H / JDK21, full synthetic images,
5 warmup pairs and 15 alternating measured pairs; includes bitmap conversion.
ThreadMXBean reports JVM managed bytes only. Do not mix these full images with
the padded Android fixture or treat timing as playback speedup.

## Reproduction and remaining work

```sh
./gradlew :library:jvmTest --tests '*NeutralBlurTest*' --tests '*SoftwareBlurTest*' --tests '*TransparentLayerTest*' --tests '*PartitaParityTest.narrowNormalWrap' --tests '*PartitaParityTest.narrowNormalArabic' --tests '*PartitaParityTest.narrowNormalGroup' --tests '*ClassicParityTest.softwareBlurCoreCheckpoints' --tests '*NativeBlurParityTest*'
./gradlew :sample:android-app:assembleDebug :sample:shared:compileKotlinJvm
```

`checks.json` records source/APK hashes and limits; raw synthetic device logs,
failed initial candidate, probe DEX and APKs are under ignored `.work/neutral-blur`.
JVM/build logs: `.work/neutral-blur-final.log` and
`.work/neutral-blur-final-tests.log`. The first direct javac command used the
nonexistent android-37 directory; it failed before measuring. Actual installed
SDK directory android-37.0 was used successfully. The extended existing
SoftwareBlurDeviceProbe accepts `white`, `neutral` or `colour` (default).
See the parent README for the separate-process classpath command.

Visible nested software recording and transformed image sampling remain;
the new APK has not replaced the user's playing application. No claim that the
observed 1-second stalls are fixed. User next requested explicit reduced render
resolution and bounded next-line prewarming; neither is part of this change.
