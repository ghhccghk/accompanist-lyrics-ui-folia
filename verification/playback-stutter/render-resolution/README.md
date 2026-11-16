# Sample rendering resolution experiment — 2026-11-16

User explicitly requested lower rendering resolution as a separate quality option.
Implementation and usage: `docs/render-resolution.md`. The library API and default
quality are unchanged. Sample settings expose 100% / 75% / 50% in both local-song
and demo modes, persisted with a full-resolution fallback for absent/invalid data.

The child stage has smaller physical constraints and proportionally smaller
density, preserving logical dimensions apart from integer pixel rounding. An
explicit offscreen layer is upscaled once. Host font conversion is forwarded;
player controls and metadata are outside this layer. This reduces raster work,
but no measured device playback speedup or PSS reduction is claimed.

## Evidence

Windows x64 / JDK21 / offscreen Compose/Skia, viewport800x540, stage380x540,
Latin body font `verification/assets/FoliaParityLatin-Bold.ttf` at48sp, synthetic
`mixed` scene at8000/8150/8600/10400/13000ms, paused. Chinese subtitle fallback
is platform-dependent. 30 distinct PNGs (two themes × three resolutions × five
timestamps), paused duplicate capture and reverse-revisit equality, and width
380→800→380 restoration pass. Direct original stage versus100% wrapper pixels
are exactly equal. These are regression/determinism checks, not new upstream
browser parity claims. Reduced-resolution frames are deliberately different.

The independent pointer test uses400x300 physical pixels, host density2, RTL,
and a custom host font converter. Observed/asserted child dimensions are400x300,
300x225 and200x150. Logical width stays200dp;20sp stays10dp according to that
host converter. A physical(320,210) tap maps to the expected scaled child point.
The first candidate placed the low-resolution stage at the RTL start edge,
offsetting both image and pointer; it failed the unchanged coordinate assertion.
The fix anchors the stage to physical top-left. No expected values or tolerances
were relaxed. A separate initial compile failed for an incorrect desktop Font
import, corrected to the existing parity harness's platform.Font import.

`resolution-comparison.png` contains synthetic examples at8600ms, composited over
the sample background #162435. `image-differences.csv` reports entire380x540-stage
RGB MAE/changed pixels after the same composition; this is a descriptive quality
comparison with no pass threshold. Blank areas dilute the mean. Raw PNG hashes
are in `image-digests.json`; raw30 captures remain in ignored build/verification.
Run `report-images.py` after the UI test to regenerate the report (Pillow/NumPy).

Sample settings/old-preference compatibility, both entry points, paused position,
layout, Rust-switch and mock-player/error recovery tests pass. Android/JVM shared
compilation and default native-enabled Android debug assembly pass. checks.json
records APK SHA, native payload and signature check. Device disconnected during
validation; no app replacement, Android UI test, same-song three-mode frame
measurement, or Linux/macOS run occurred. Existing local user formatting in
LocalSongApp.kt is retained in the worktree and excluded from this commit.

Commands/logs:

```sh
./gradlew :sample:shared:jvmTest --tests '*RenderResolutionTest*' --tests '*SettingsTest*' --tests '*SampleLayoutTest*' --tests '*ExperimentalMaskControlsTest*' --tests '*LocalSongTest*'
./gradlew :sample:android-app:assembleDebug :sample:shared:compileKotlinJvm :sample:shared:compileAndroidMain
python verification/playback-stutter/render-resolution/report-images.py
```

Logs `.work/render-resolution-tests.log` (import failure),
`.work/render-resolution-tests-fixed.log` (RTL failure),
`.work/render-resolution-final.log` (all selected checks/build passed),
`.work/render-resolution-final-tests.log` (additional local-song setting checks),
`.work/render-resolution-apksigner.log`.

Next: same-device/song/interval/theme/backend comparison with screenshots and
frame/CPU/PSS capture at each resolution. The earlier second-long nested software
stalls are not declared fixed. Bounded next-line preparation remains a distinct
task; no hidden whole-song composition or shader precompilation was added.
