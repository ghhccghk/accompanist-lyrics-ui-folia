# Diorama text-plane fragment fog

2026-11-18. INTERNAL only; Diorama remains absent from the public/sample theme
list. This implements the normal text-plane fog, not a background engine, depth
buffer, cross-song flight or complete original CameraRig.

## Source and implementation

Pinned Folia `cc6226e464050a456a2913e062374f23d3844cc5`:
`dioramaSceneConstants.ts` FOG_NEAR=12 / FOG_FAR=30 and `DioramaScene.tsx`
scene-owned Fog / damped background colour. Actual installed Three0.185.1
`meshbasic.glsl.js` orders output encoding BEFORE fog; `fog_vertex` supplies
`-mvPosition.z`, and `fog_fragment` mixes RGB with smoothstep without changing
alpha. `UniformsUtils.getUnlitUniformColorSpace` supplies the renderer's output
colour space for a direct-screen render. Linear material colours, Euclidean
distance-life and screen-output fog are distinct operations. Three MIT notice
is retained in LICENSES; Folia attribution/AGPL-3.0-only is unchanged.

`DioramaFog.kt` implements that algebra and an affine local-plane depth gradient,
transformed by the existing perspective homography. The alpha mask is retained
with SrcIn; layer restore uses the original normal/additive blend. This is shared
by normal neighbouring lines and all active base/soul/halo layers, with their
actual transformed planes. Internal fog=null preserves the exact prior path.
The host supplies a current fog colour; damped palette/song-transition ownership
is NOT implemented by this helper. No frame clock, per-pixel CPU loop, glyph
re-raster, full-song mask, dependency or public API was added.

32 gradient intervals approximate smoothstep. The independent second-derivative
bound is 6/(8*32²)=.000732421875, at most .186768 of an 8-bit channel before
storage. Half-float extended-sRGB stops add at most 1/4096 in [0,1]. Input palette
and output framebuffer quantization are separately acknowledged. This is bounded
shader approximation, not a claim of exact GPU fragment equivalence.

## Numeric failure and correction

The initial scalar test failed its ORIGINAL .5/255+1e-6 gate:
expected .4803968568233363, actual .47843137383461. Both input and mixed output
were being packed into 8-bit Color; tiny original encoded-endpoint differences
plus output rounding exceeded a gate that had budgeted only output rounding.
`initial-failure.txt` and the complete ignored .work XML/source/log preserve it.
Rather than widen that gate, stops now use extended sRGB, which keeps the same
transfer function with half-float storage. Source fixtures and both original
packed/interpolation gates are unchanged. Added exact-double shader-channel
checks use original Three endpoints, independent of Compose packing.

1221 original samples: factor peak=0, exact-algebra peak=0,
packed RGB peak=.0002437951663, interpolated-stop peak=.0006524898174.
Actual shader uniforms and every depth/end-boundary sample are in fog-v1/fog.tsv.

## Actual original and native pictures

Two complete, fixed-condition captures; no frame replacement/registration,
pixel threshold, alpha removal or changed foreground mask:

* **54 real-core text stages**, Latin/Arabic/CJK, original fonts and integer-ms
  times, 380/800×600 DPR1, same frozen original views, original window/life/unit
  material functions, scene Fog attached at original 12/30. Full source + own
  no-fog + own fog nonbackground union. Mean foreground MAE
  **12.221155139 → 3.742277715**, worst frame **28.497013142 → 10.485979671**,
  worst64px local **46.276676110 → 21.879310345**.
* **56 actual Three plane stress frames**, shared original synthetic raster and
  solid calibration mask; fixed before/near-cross/middle/full-band/off-axis/
  far-cross/beyond poses, normal/additive, .63 alpha, both widths. Independent
  of font shaping and distance-life hiding far fragments. Same three-way union:
  mean **39.521592001 → 1.824068809**, worst frame **120.748171166 → 7.307819144**,
  worst64px local **122.333333333 → 15.982093664**.

Supplementary solid-plane controls project PRESET uv quartiles and analytically
intersect the corresponding pixel-centre rays. They do not replace/filter any
full-image comparison. Original/scalar peak=.518749741 byte; own/scalar=1.685;
paired=2, within declared original<=1 / own-and-paired<=2-byte rounding and
interpolation gates. Additive fully-fogged ink still adds background-coloured
RGB; it is not incorrectly treated as transparent/removed text. Plane depth is
view-axis, not camera distance (off-axis control).

Both preselected contacts inspected: receding glyphs and oblique per-fragment
gradient match the source structure; narrow CJK clipping still exists on BOTH
sides and is not excused by the mean. Coverage/halo/backend sampling residuals
remain; neither matrix constitutes full-scene or final visual acceptance.

Source fixture conditions include revision, actual exact Motion lock, original
and capture lock hashes, browser151.0.7922.34, Three version, source/host/shader/
raster/font hashes. Original source files remain unchanged. The frozen source
camera/analytic unit envelopes isolate composition; this is NOT a mounted React
CameraRig or source RAF-equivalence capture. Capture instances close in finally.

## Validation and reproduction

10 targeted JVM tests pass: exact scalar controls, 56 shared-raster frames, 54
real-core fog frames, reverse queries, pause, offset, settings/subject restoration,
source identity and remount; no reprepare on stable time/view changes. The old
**108 no-fog native PNG SHA256 values remain identical**, retained in
no-fog-regression.json. Existing original no-fog pictures/fixtures were not
regenerated. Android library and Desktop sample compilation pass.

```powershell
node verification/classic-browser/capture-diorama-neighbors.mjs --fog
node verification/classic-browser/capture-diorama-fog-plane.mjs
.\gradlew.bat :library:jvmTest --tests '*DioramaFogPlaneTest' --tests '*DioramaFogTest' --tests '*DioramaNeighborStageTest' --tests '*DioramaOffsetContractTest' --tests '*DioramaTextPlaybackTest' :library:compileAndroidMain :sample:desktop-app:compileKotlin
python verification/compare-diorama-fog.py
```

Exporters refuse overwrite: preserve existing fixture/raw pictures before an
intentional rerun. Native completion markers and successful task must precede
comparison. Raw110 source /220 native PNGs remain ignored in build/verification.
Logs: .work/diorama-fog-{source,precision,plane-source,plane-native,compare}.log.
No Android device, new true-window/audio or performance conclusion is claimed.
Remaining: continuous camera residuals, palette/flight, depth interaction,
whole-song bounded residency, Android baseline metrics and device runtime.
