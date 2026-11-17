> Historical pre-entry function-shell baseline at e532ff2. The actual React
> parent entry correction and exact Motion dependency audit are in ../fume-mounted.
> PublicFumeStageTest now writes fume-public-kotlin-entry; do not overwrite this
> baseline with those changed screenshots using the historical comparison command.

# Fume public actual-core article stage

Baseline: Folia v0.7.26 cc6226e464050a456a2913e062374f23d3844cc5.
No source implementation files were modified. Source shell imports the actual
article layout/measurement, camera60Hz step, static snapshots and live-text draw.
It is a function-pipeline integration, **not** a mounted VisualizerFume React
component: React debounce/layout cache, useFumeCanvasStage lifecycle, subtitles
and backgrounds are not covered by this oracle. Public acceptance remains open.

Four invented lines (one actual KaraokeLine, three SyncedLine), same OFL font
bytes, DPR1, 380/800/801×600, glow0/1, smooth/stepped,18 fixed times =216 pairs.
Print/retention/keyword/viewport/seed match. Original float clock is controlled
as media seconds with origin0 (old scene matrix retains9.25). The shell exposes
retarget/target observations but never rewrites original camera behavior.
No frame offset, registration, dropped low-alpha pixels or changed thresholds.

PublicFumeStageTest first independently mounts FumeArticleStage to generate
geometry and pixels, then mounts actual-core FoliaLyricsView: every public frame
is pixel-identical. Camera TSV is from that independently mounted, pixel-equal
stage; it is not directly scraped from the public composable. Random seek,
pause and remount hashes match. PublicFumeTest covers inverse-transformed input,
original object click/long press, blank passthrough, offset, resize/restore,
subtitle/paint readiness, theme change, reduced/Minimal, cancellation/replacement,
unmount and120-line system-fontScale preparation/restore. Cancellation checkpoint
and tail query order also tested; the older non-extended clamp contract remains.

Whole nonbackground RGB union including low alpha:
- MAE12.0167818067; byglow0=14.1783891669, glow1=9.8551744466.
- Camera screen-position peak15.9332727512px, scale0.0562212535.
- comparison.json records all216 foreground/full means, channel peaks, local
  worst64px tile errors and source/native PNG hashes. contact.png was inspected.
- This matrix differs from old float-origin9.25/>8-mask evidence, so means are
  **not a before/after improvement**. Short-flight, overview, raster differences
  remain actual unresolved differences, not intentional final adaptations.

Run from repository root (Gradle commands need the configured JDK21/Android SDK):
```
node verification/classic-browser/serve.mjs
node verification/classic-browser/capture-fume-public.mjs
./gradlew :library:jvmTest --tests '*PublicFumeStageTest' --tests '*PublicFumeTest' --tests '*FumeCameraTrackTest'
python verification/compare-fume-scene.py --native-dir build/verification/fume-public-kotlin --source-dir build/verification/fume-public-browser --camera-fixture verification/fume-fixtures/public-camera.tsv --all-foreground --out verification/fume-public
./gradlew :sample:shared:jvmTest --tests '*SettingsTest' --tests '*FumeSampleTest' :sample:android-app:assembleDebug :sample:desktop-app:compileKotlin
```
Use gradlew.bat on Windows. Dedicated headless source capture, one page at a
time, closed in finally; generated216+216 PNGs remain ignored underbuild/.
Only contact/report/fixture are retained, no user lyrics/audio.
Failed early compiler/input-test logs preserved under.work/fume-public-first.log
andsecond.log. Failing input assertion assumed first row always topmost;
actual article can have later visible rows above it. Test now computes a visible
point and checks the actual inverse draw-order source, not a changed hit contract.

Public focused/regression logs: .work/fume-public-third.log,
.work/fume-public-regression.log; sample/build: .work/fume-public-build.log.
No real Desktop window, Android device/runtime, continuous recording or device
performance acceptance. Existing source60Hz fixture sampling is not a claim of
continuous React playback equivalence.

Final .work/fume-public-final.log: public stage/lifecycle/whole-song plus Android APK/Desktop compile passed after hold-cache/disposal edits; all432 source/native PNG hashes unchanged.
