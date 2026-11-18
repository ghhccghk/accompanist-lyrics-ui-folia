# Text plane near/far and FrontSide clipping

Folia pinned cc6226e4 DioramaActiveUnitPlanes.tsx uses default Three FrontSide
materials, even when depthTest=false. Homogeneous near/far clipping still applies.
The old projective Canvas image path had neither plane-boundary clipping nor
backface rejection; it could draw huge glyph pieces in front of the near plane,
past the far plane, or mirrored text from the rear.

DioramaPlaneDrawing.kt independently clips a texture rectangle in DOUBLE local
plane coordinates against actual view depth before float homography division.
Front normal is -cross(x,y) because texture Y points down. Entirely visible
front planes retain the EXACT old image path; partial planes clip their local
polygon, hidden/back planes do not draw. This helper is wired into full-line
text and all active base/halo/soul layers, with actual layer lift/depth/scale.
It is NOT inter-plane depth testing,nearby mesh occlusion,fog or a 3D engine.

Original oracle:locked Three0.185.1 actual MeshBasicMaterial default FrontSide
and GPU homogeneous clipping; same existing original synthetic transparent raster
isolates geometry. No new font/lyric/texture copied.18 frozen frames,380/800×600,
DPR1,near=.5/far=2 for the explicit crossing stress (normal theme still.1/1000):
front,near-cross,behind-cross,before-near,beyond-far,backface,far-cross,eye-cross,
both-depth-cross. Source raw geometry/projected corners,exact dependency versions,
Chromium151,texture hash/conditions in ../diorama-fixtures/clipping-v1.
No source function modified,no mounted original CameraRig/full scene claim.

Native before is the exact old image/homography path IN TEST ONLY. Production has
no bypass/flag. Comparison uses ONE identical source/before/after union including
lowest-alpha pixels,worst64px/channel peaks,no registration or pixel filtering.
Mean foreground MAE99.915892→3.080586 on this18-frame stress matrix. All10 actual
original blank cases now EXACTLY blank,removing221550 spurious old foreground
pixels. Partial clip geometry still has GPU/Canvas edge/sampling differences;
this is not full-theme or readable-framing acceptance. Contact inspected:near
and both-depth cuts match source shape; old extra giant glyph/rear text removed.

216 previous own forward frames (192 finite/media follower,24 own complete-shaped
plain text) re-rendered and SHA256 IDENTICAL; forward-regression.json records all.
This proves no new forward raster drift,not original visual equivalence.8 targeted
JVM tests and Android library compile pass (.work/diorama-clipping-first.log):
numeric front/depth polygon,infinite-division avoidance,actual clipping Canvas,
reverse-query restore,old text/follower captures. No new device/window/performance.

```
node verification/classic-browser/capture-diorama-clipping.mjs
./gradlew :library:jvmTest --tests '*DioramaPlaneClipTest' --tests '*DioramaClipCanvasTest' --tests '*DioramaFollowStageTest' --tests '*DioramaPlainTextTest' :library:compileAndroidMain
python verification/compare-diorama-clipping.py
```
Before regression rerender,preserve hashes of the existing216 forward frames in
.work/diorama-before-plane-clipping-hashes.json; tracked forward-regression.json
retains the canonical list. Exporter refuses overwrite,source remains fixed.
Wait for both native completion markers/capture process success BEFORE comparator.
Dedicated4187 browser/server closes in finally;raw PNGs ignored under build.
Diorama remains INTERNAL. Weighted picker is02b8d8c,follower de7baa6; unresolved
media-camera/raster28px,Android midpoint,flight/depth/fog remain open.
