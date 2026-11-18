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
