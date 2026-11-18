# Bounded straight-RGB fragment control — 2026-11-18

Baselinee5d8e2d, pinned Folia cc6226e4. TEST-ONLY JVM/Skia RuntimeEffect with the
same real source cache7 base image, own independently prepared published core,
paragraph geometry, finite history camera/material at380×600/DPR1/incoming700ms.
The original frozen layer and labelled trilinear/aniso1 diagnostic hashes are
checked before comparison. No source mip/pose/material is fed to native drawing.
Normal production drawStage and all168 pre-candidate stage PNG hashes unchanged.

Prepare opaque linear-F16 color occupancy and separate mask alpha ONCE, mip0..5,
before any rendering. Every fragment derives isotropic LOD from the projective
Jacobian, samples the independent straight-color and alpha channels, then applies
linear material tint→existing Three ACES→explicit sRGB OETF→encoded fog→blend.
Full original shaping remains the input. Render through Compose ShaderBrush,
retaining parent state; no bypass of its alpha multiplier. Runtime shader,
builder and child shaders are scoped to this test and closed. No new dependency,
JNI, frame accumulator, background engine, public theme or Android shader added.

The helper is deliberately BOUNDED: white source-cache7, mip0..5, full-front plane
without near/far crossing, fixed original fog parameters. It is not general
production code or proof of a real-time/low-memory solution. Actual source
default anisotropy4 is not implemented. Shared-input isotropic control is a
diagnostic dependency, NOT a replacement for the original acceptance target.

The independent CPU control samples actual GPU mip4/5 on the same native
homography and applies source material algebra; these source mip inputs are
only in the scalar oracle, never native drawing. Its result agrees with the
original isotropic diagnostic within2 bytes. Native independently prepared
fragment also agrees within2, foreground MAE~.326. No alignment, phase/LOD fit,
threshold removing low contributions or resampled source is used. All six-image
union measurements, original-default residuals, local64 peaks and hashes are in
comparison.json; union changes mean older report means cannot imply a benefit.

Preserved failure: the initial context-dependent fromLinearSrgb call gave the
same numeric output as linear ACES at fixed diagnostic points, e.g.70/74/79
instead of explicit encoded143/147/151 at[200,420]. Raw sampled RGB/alpha and
ACES numeric channels matched independently, locating the error after ACES.
Native mass11802 vs source isotropic20614; fixed explicit source OETF yields
20570. Initial comparison/fixed channel observations and unmodified failed PNG
are preserved. No camera/opacity/gamma fitting or expected-value replacement.
The context observation is NOT a universal claim about Skia color spaces.

Earlier compile failures are retained in ignored logs: ambiguous Compose/Skia
Shader imports (qualified the type); this SkSL profile lacks min(int,int)
(used the equivalent conditional). Narrow JVM passes, pause-fixed pixels and
channel round trip are exact. Not Android/real-window/performance validation.

```powershell
.\gradlew.bat :library:jvmTest --tests '*DioramaMountedStageTest.narrow'
python verification/compare-diorama-fragment.py
```

Requires existing frozen source sampler captures; no original recapture needed.
Next measure/implement the default aniso4 footprint on this SAME original layer,
retain the isotropic control, then revisit near-view Latin/Arabic/resize before
considering production adoption. A single-frame gate within2 bytes is not formal
theme visual acceptance. Diorama stays internal and seven public previews stay.
