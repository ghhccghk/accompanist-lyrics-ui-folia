# Diorama desired camera trajectory with independently shaped text

Pinned Folia cc6226e464050a456a2913e062374f23d3844cc5, AGPL-3.0-only.
DioramaCameraTarget.kt ports cameraPath.ts resolveShotOffset (all13), hold settle,
read-head tanh saturation, seeded idle drift, and CameraRig target composition.
DioramaCameraLine caches full EGC timeline/direction and actual renderHints end once;
raw LAST word end retained, including unsorted/overlapping words. RTL reverses truck,
not the source word-progress used by the tracking shot's sine. No chorus/senders or
music data are invented. The current isolation fixtures explicitly select a shot;
weighted getDioramaShot selection is not implemented or claimed here.

Oracle: diorama-camera-source.mjs extracts exact private source ranges from original
CameraRig, with source line spans and SHA256 in camera-target-conditions.json.
Only wrappers provide input/ref equivalents and return outputs; Vite Oxc removes TS
types. Actual original timing/renderHints/cameraPath functions imported unchanged.
No original checkout edits. No SmoothDamp, orientation alignment/follower, scene
sequencer, transition or RAF loop mounted. These are desired camera targets,
NOT original final camera poses. Original transition sweep is explicitly0 in the
look target block and flight state is absent. No audio or background-engine work.

Numerical matrix:2808 controls,13 shots,6 source lines including Arabic/CJK,
micro/short/empty/unsorted-note inputs,12 times,3 aspects and zero/normal/1.35 motion.
All11 fields peak1.06581410364e-14 at1e-9 gate; random-order/reverse queries independent.
Scene matrix:132 pairs, three synthetic texts and fonts,380/800×600,DPR1,
all13 shots for Latin800, four shots (orbit,track,hold,glide) for remaining5 cases,
media.7/1.5/3.45/8.92. Real Three three-layer planes, original isolated target and
analytic charge injection from previous stage. Kotlin computes its OWN target
from its OWN complete-shaped width/fit and draws its OWN textures, not browser
camera coordinates or source PNGs. World pose peak3.553e-15 on this matrix.
Font hashes/colors/inputs/actual targets in camera-targets.json/tsv.

All-nonbackground including lowest-alpha foreground MAE5.773035
(Latin5.982687,Arabic5.371602,CJK5.728959); foreground bounds peak20px. Full hashes,
local64px worst/channel peaks retained in comparison.json; contact.png inspected.
Source also clips words in some spatial target poses: actual CameraRig alignment /
frame-keep / damped orientation is deliberately absent, so DO NOT expose this as a
usable complete theme or accept edge clipping by lowering the full-theme standard.
Halo support/low-alpha raster and complete-shaped slice/overhang differences remain.
No fitted registration, source formulas rewritten or frame timing delayed.
Pause/reverse seek/unmount/remount exact pixels;3 preparations per3 scenes, not per time.
JVM offscreen, Android APK and Desktop compile pass. No device/real window/runtime
performance or original continuous full-theme acceptance. Android OS/2 midpoint,
near-plane/depth/fog, original weighted picker and full follower still incomplete.

```
node verification/classic-browser/generate-diorama-camera-targets.mjs
node verification/classic-browser/capture-diorama-camera-targets.mjs
./gradlew :library:jvmTest --tests '*DioramaCameraTargetTest' --tests '*DioramaCameraStageTest'
python verification/compare-diorama-camera-targets.py
./gradlew :sample:android-app:assembleDebug :sample:desktop-app:compileKotlin
```
Windows uses gradlew.bat/JDK21/SDK. Dedicated4176 browser/server closes in finally.
Original private-target observation initially failed virtual TS parsing; fixed by
normal Oxc TS transpilation, not by changing extracted original calculations.
Numeric scanner now disables unrelated HTML dependency discovery.
Logs .work/diorama-camera-target-first.log,stage-first.log,build.log retain results.
