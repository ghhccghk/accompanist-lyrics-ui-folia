# Diorama registered halo / soul / body composition

Pinned Folia cc6226e464050a456a2913e062374f23d3844cc5, AGPL-3.0-only.
Actual original split/layout/raster/updateDioramaActiveUnits with real Three planes.
Source matches DioramaActiveUnitPlanes: all renderOrder17 additive halo at z-.01,
all18 additive crisp soul at z-.005, then all19 normal bodies. No depth test/write.
Original four pure-shadow passes at128px: blur20.48 alpha.95/.85,
blur40.96 alpha.55,blur59.392 alpha.3. Kotlin independently rasterizes the SAME
complete-shaped body slices, bakes four coverage shadows once, draws real perspective
planes and shares material tint across all three layers. Soul lift/swell are local
plane transforms around each unit center, not screen-Y offsets. No source PNG reuse.

Scope of time injection: analytic media-domain charges (attacks14/12,releases4.5/2.2)
are injected into original Float32 arrays with delta0, life/breath1,power0. Source
material logic/raster/planes are untouched. This is an isolated composition oracle,
NOT original RAF charge quantization, CameraRig, continuous complete scene or audio.

Matrix: same three fonts/text/380,800,801×600/DPR1,10 times as active-base;
current-soul on/off =>180 pairs. Font hashes, actual materials and all inputs in
../diorama-fixtures/active-layers.json/tsv and active-layers-materials.tsv.
Ten original material/visibility/transform fields per unit match within1e-7;
observed peak1.7707252886e-8 (Float32 source storage vs analytic doubles).
Pause/reverse seek/remount exact pixels;3 text/halo preparations for3 scenes,
no per-time reshaping. Android APK/Desktop compile and JVM offscreen pass.
No Android runtime/device or real Desktop window validation.

Current all-nonbackground union (including lowest alpha) foreground MAE4.427326;
full foreground bounds peak21px, notably low-alpha released halos. Source vs native
raw halo alpha full-texture MAE: Latin unit0 .785718,CJK unit6 .346221,Arabic unit1
.690148. Dimensions identical in these controls. Independent raw textures remain
ignored build outputs; hashes of all complete frames retained in comparison.json.
Contact inspected; not a final acceptance gate or overall theme equivalence claim.

Initial implementation blurred/rounded body first, then multiplied pass alpha.
Actual source shadowColor belongs to each shadow before blur. Corrected that order:
same raw texture alpha MAE before1.133433/.613032/.881372; after above. Initial
complete frames/metrics are preserved by pre-shadow-alpha-comparison.json/contact.png
and their hashes. All180 original image hashes are unchanged after passive texture
export. Original full foreground bounds peak29px now21px, but union masks changed;
DO NOT treat MAE4.399851→4.427326 as a fixed-mask improvement/regression claim.
Residual alpha support/Skia Canvas vs software box blur is unresolved, not waived.
No alpha threshold, image registration, phase delay or tolerance altered.

Other limits inherited from active-base: advance-bound slices may cut overhang,
mixed-bidi/ligature boundary fixtures pending; Android OS/2 midpoint unavailable.
No fog/depth/full CameraRig or public/sample Diorama. Next integrate real camera
shot targets with the same text pipeline, retain full-rig follower/transition gaps.

```
node verification/classic-browser/capture-diorama-active-layers.mjs
./gradlew :library:jvmTest --tests '*DioramaActiveLayersTest' --tests '*DioramaActiveTextTest' --tests '*DioramaUnitFrameTest'
python verification/compare-diorama-active-layers.py
./gradlew :sample:android-app:assembleDebug :sample:desktop-app:compileKotlin
```
Windows: gradlew.bat/JDK21/Android SDK. Dedicated4175 server/browser finally closed.
Logs .work/diorama-active-layers-final.log,shadow-alpha.log,rasters.log retain checks.
