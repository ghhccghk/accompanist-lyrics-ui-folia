# Partita Android visual comparison

November 19, 2026 (Asia/Taipei). The API 34 x86_64 SwiftShader emulator ran the
release consumer of local Maven snapshot `0.1.0-20261118.230350-10`.
Each pair used the same paused timestamp, core fixture, packaged font, viewport,
density, font scale, guide flag and reduced motion setting. The surface was
800x600 at 160 dpi. The background was RGB (22, 36, 53). Cold start order
alternated Kotlin first and NativeRequired first. Valid native main submissions
matched the requested media time. Each capture had two consecutive matching
image regions. The waiting fixture was sampled before the first line, while the
original Kotlin overlay was visible and the native session was still preparing.

Exclude the fixed top 40 pixels used by the diagnostic label. The large-font
case also excludes the area outside its 640x480 viewport. All other cases use
the full width and remaining height. `compare-renderer-captures.py` reports
absolute RGB error over every pixel differing from the background in either
capture, including one-code-value differences. It also reports the worst
32x32 tile by mean error among its foreground pixels. There is no geometric
registration. Values below are in 8-bit channel units.

| Case | Sample (ms) | Foreground pixels | Mean | P99 | Peak | Worst tile mean |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| baseline-850 | 850 | 48186 | 0.397 | 2 | 3 | 1.000 |
| baseline-1500 | 1500 | 84063 | 0.751 | 10 | 86 | 6.313 |
| baseline-3300 | 3300 | 22690 | 0.548 | 2 | 4 | 1.000 |
| wrap | 1500 | 58878 | 1.223 | 14 | 58 | 4.493 |
| arabic | 1500 | 107183 | 0.593 | 9 | 25 | 1.881 |
| cjk | 1500 | 74378 | 0.863 | 11 | 97 | 6.040 |
| bidi-rtl | 1500 | 88152 | 0.664 | 9 | 25 | 5.833 |
| bidi-ltr | 1500 | 113254 | 0.543 | 7 | 52 | 2.401 |
| cluster | 1500 | 63236 | 0.784 | 12 | 54 | 8.874 |
| large-reduced | 1700 | 28634 | 0.478 | 5 | 12 | 1.503 |
| waiting | 9000 | 518 | 0.000 | 0 | 0 | 0.000 |
| subtitle-1500 | 1500 | 74943 | 0.763 | 12 | 87 | 6.303 |
| subtitle-3300 | 3300 | 64331 | 0.609 | 2 | 4 | 1.159 |
| diagnostic-guides-off | 1500 | 83949 | 0.658 | 9 | 35 | 3.793 |

The subtitle overlay region matched exactly in both subtitle captures. This
shows the Kotlin-owned subtitle layer remains aligned while the Partita main
stage is supplied by the native surface. The vertical guide in the 1500 ms
Latin fixture shows an 86-channel-unit peak with the same visible line and input. CJK has a
97-channel-unit peak. Local error remains a visual acceptance failure. The
large-font row used guide-off and reduced-motion settings on both backends.
It is a separate appearance case. These measurements contain no performance
or upstream-parity assessment. Emoji and timed caption transitions remain open.

To reproduce a paused scene with the same consumer, start its exported activity
with `--es backend Kotlin` or `--es backend NativeRequired`, and pass the same
`--es scene`, `--el mediaMs`, `--ei width`, `--ei height`, `--ef fontScale`,
`--ez reduced`, and `--ez guides` arguments for both runs. Start each run in a
fresh consumer process. Check the `FoliaNativeConsumer` status and save PNG
bytes from `adb -s <task-owned-emulator> exec-out screencap -p`. For an 800x600
case run:

```text
python scripts/compare-renderer-captures.py kotlin.png native.png --region 0 40 800 600
```

## Scene protocol v2 rerun

The same 13 fixture pairs were captured from local Maven snapshot
`0.1.0-20261119.015134-12` on the same emulator. The guide-off diagnostic
is omitted. Kotlin keeps the same drawing behavior. The native renderer
replays eligible layers under the composed transform, while keeping
translucent, filtered and forced-offscreen groups isolated.

| Case | Mean | P99 | Peak | Worst tile mean |
| --- | ---: | ---: | ---: | ---: |
| baseline-850 | 0.371 | 2 | 3 | 0.609 |
| baseline-1500 | 0.420 | 2 | 11 | 0.887 |
| baseline-3300 | 0.565 | 2 | 4 | 1.000 |
| wrap | 0.476 | 4 | 9 | 1.030 |
| arabic | 0.393 | 2 | 8 | 0.758 |
| cjk | 0.401 | 2 | 32 | 0.902 |
| bidi-rtl | 0.415 | 2 | 8 | 0.819 |
| bidi-ltr | 0.383 | 2 | 9 | 0.833 |
| cluster | 0.380 | 2 | 26 | 1.120 |
| large-reduced | 0.426 | 2 | 4 | 1.000 |
| waiting | 0.000 | 0 | 0 | 0.000 |
| subtitle-1500 | 0.327 | 2 | 11 | 0.736 |
| subtitle-3300 | 0.653 | 2 | 4 | 1.151 |

The vertical guide now aligns in the Latin and CJK comparisons. The CJK
maximum of 32 is a single pixel on the horizontal guide near (607, 319),
where the solid Kotlin rectangle and the sampled GPU mask use different
edge coverage. The most affected 32x32 CJK tile averages 0.902. Preserve
this remaining local difference for further comparison.

Android Rust tests passed (7/7). The API 34 independent release/R8 consumer
passed the full lifecycle probe. Both packaged ABIs, matching symbol build
IDs, 63 crate sources and licenses, and 16 KiB APK alignment were checked.
JNI ABI stays at 2. Rust accepts v1 packets with the original isolation
behavior and requires scene v2 for direct groups.

## Emoji and subtitle timing

The same release/R8 consumer captured a profession ZWJ sequence with skin
tone and a regional-indicator flag, alongside Latin words. A second scene
sampled subtitle entrance, the retained translation and its removal when
the untranslated next line became primary. The AAR loaded the same native
binaries as the scene v2 rerun.

| Case | Mean | P99 | Peak | Worst tile mean |
| --- | ---: | ---: | ---: | ---: |
| emoji-850 | 0.358 | 2 | 3 | 0.691 |
| emoji-1500 | 0.284 | 1 | 8 | 1.000 |
| emoji-2280 | 0.440 | 2 | 44 | 1.000 |
| subtitle-750 | 0.310 | 1 | 2 | 0.462 |
| subtitle-850 | 0.362 | 2 | 3 | 0.609 |
| subtitle-1000 | 0.207 | 2 | 52 | 0.667 |
| subtitle-1500 | 0.327 | 2 | 11 | 0.736 |
| subtitle-3199 | 0.358 | 2 | 8 | 2.000 |
| subtitle-3300 | 0.653 | 2 | 4 | 1.151 |
| subtitle-3500 | 0.265 | 2 | 29 | 1.111 |

The subtitle band (full width, rows 440 through 509) matched pixel for
pixel at all seven times. It had 1,131 foreground pixels at 750 ms, 1,199
at 850 ms, 1,030 at 1000, 1500 and 3199 ms, and none at 3300 and 3500 ms.
The largest remaining main-stage errors in these scenes are isolated pixels:
44 at (588, 357) in emoji-2280 and 52 at (433, 288) in subtitle-1000.
The scene comparison keeps those pixels and all low-alpha coverage in the results.

With the release consumer installed on the task-owned 800x600, 160 dpi
emulator, capture the same matrix with:

```powershell
python scripts/capture-renderer-pairs.py --adb <adb.exe> --serial emulator-5580 --output .work/renderer-pairs
```

## November 19 review rerun against Android Kotlin

API 34 x86_64 with the host NVIDIA Vulkan backend. Release/R8 coordinate
consumer using the renderer rebuilt after `98d9b85`. Fixtures, font, density,
viewport, timestamps and quality settings matched within each pair. Captures
exclude the diagnostic label at y < 40. The figures are absolute RGB channel
errors over foreground pixels in either capture.

| Theme / case | Mean | P99 | Peak |
| --- | ---: | ---: | ---: |
| Partita / baseline-1500 | 0.274 | 2 | 10 |
| Partita / cjk | 0.185 | 1 | 35 |
| Partita / bidi-rtl | 0.265 | 1 | 8 |
| Partita / cluster | 0.135 | 1 | 24 |
| Partita / waiting | 0.000 | 0 | 0 |
| Partita / emoji-1500 | 0.190 | 1 | 8 |
| Partita / subtitle-1500 | 0.170 | 1 | 10 |
| Partita / subtitle-3300 | 0.543 | 2 | 4 |
| Diorama / baseline-850 | 7.477 | 44 | 58 |
| Diorama / baseline-1500 | 6.789 | 42 | 107 |
| Diorama / baseline-3300 | 3.781 | 35 | 72 |
| Diorama / baseline-3550 | 1.467 | 30 | 68 |
| Diorama / baseline-3700 | 6.798 | 41 | 88 |
| Diorama / cjk-1500 | 5.932 | 39 | 92 |
| Diorama / bidi-rtl-1500 | 6.834 | 39 | 59 |
| Diorama / wrap-1500 | 10.499 | 64 | 129 |

The Partita waiting sample and both subtitle regions matched exactly. The
CJK peak was localized and remains open, as do cluster coverage differences.
Diorama submitted each sampled frame, including the 3300–3700 ms line
transition. Its error spans larger regions. Keep the same F16, mip and
projection implementation as the starting point for isolating the discrepancy.
The Partita Release/R8 lifecycle check also passed seeking, source identity,
click and long press, cancelled gestures, multiple views and retirement.
Diorama seek, font and viewport changes passed the pressure probe. On API 35
with the 16 KiB image and Maven `0.1.0-rc.2`, both backends returned the
original source line on tap and long press at `(433,356)` with media paused
at 1500 ms. Both suppressed actions on drag and pointer cancellation and
routed a corner tap to the blank handler. A long press at `(364,317)` during
the 3550 ms transition reached the blank handler in both backends. Use
`verify-native-consumer.py --availability diorama-input` on the 800×600
fixture. This covers two paused timestamps. Hit geometry while playing,
during a song replacement and across more viewport and font settings remains
open.

This table compares the two Android implementations. ARM64 runtime and
frame-time gates remain pending.

Reproduce with `scripts/capture-renderer-pairs.py --theme Diorama` or
`--theme Partita`, a task-owned 800×600 emulator and the Release consumer.

## Baseline against pinned upstream Folia

The browser ran the text stage at `cc6226e464050a456a2913e062374f23d3844cc5`
using Chromium `151.0.7922.34`. Both sides use “Amber echo” and “Silver light”
with the same word times, Latin font file, font scale 1, 800×600 viewport,
DPR 1 and uniform palette. Partita uses the original mounted visualizer with
its default motion and guides. Diorama mounts the original CameraRig and
DioramaScene with seed `Diorama` and no audio or particles. Existing mounted
cut fixtures remain separate. The browser advances through the song with a
controlled clock. Android loads independent paused timestamps. Browser motion,
text rasterization and the mounting history can contribute to the measured
pixel differences. The browser clocks supply media positions for visual
comparison.

| Upstream case | Android Kotlin mean / P99 / peak | Android native mean / P99 / peak |
| --- | ---: | ---: |
| Partita 1500 ms | 3.770 / 56 / 146 | 3.728 / 56 / 145 |
| Diorama 850 ms | 22.570 / 112 / 163 | 19.833 / 103 / 143 |
| Diorama 1500 ms | 18.865 / 150 / 178 | 15.782 / 147 / 178 |
| Diorama 3300 ms | 22.876 / 91 / 126 | 21.855 / 84 / 126 |
| Diorama 3550 ms | 35.913 / 58 / 125 | 35.725 / 58 / 126 |
| Diorama 3700 ms | 9.908 / 107 / 153 | 10.189 / 97 / 149 |

The region, foreground mask and units match the Android pair table. Both
Android renderers show larger differences against upstream than against one
another in these samples. Theme parity remains open. Android Kotlin's
Diorama sampler factories return null, so the Android Kotlin path uses Compose
planes while native uses the projective shader with explicit mips. The capture
matrix measures the combined difference. Isolating the contribution of camera
timing, font edges, mip sampling, clipping, fog and glow requires focused probes.

```powershell
node verification/classic-browser/capture-renderer-partita.mjs
node verification/classic-browser/capture-diorama-mounted.mjs --android-baseline
python scripts/compare-renderer-captures.py .work/review-upstream-diorama/baseline-1500.png .work/review-visual-diorama/baseline-1500-NativeRequired.png --region 0 40 800 600
```
