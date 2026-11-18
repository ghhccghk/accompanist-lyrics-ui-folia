# Bounded original aniso4 fragment control — 2026-11-18

Baseline4147b4a, pinned Folia cc6226e4, unchanged actual mounted mesh7 at700ms,
380×600/DPR1, source cache7 base image, fonts,452 callbacks and original render.
Native geometry/camera/material continue to be prepared independently from
published core. This test shares only the original white base raster; original
GPU mip/pose/material results never drive the native image.

Same five-image nonbackground union520, including every nonzero contribution:

| Image | Pixels | Positive RGB mass | Original MAE | Peak | Worst64 |
| --- | ---: | ---: | ---: | ---: | ---: |
| Original trilinear/aniso4 |409|22421|0|0|0|
| Original isotropic diagnostic |512|20614|2.432051|13|2.499533|
| Production shared-cache input |193|27678|10.824359|46|11.037348|
| Previous fragment isotropic |451|20570|2.548718|14|2.629318|
| New fragment aniso4 |390|22385|.296795|2|.308123|

The original409 and native390 counts are not identical: rounding/low-contribution
residuals remain, and no pixels were removed from the comparison to hide them.
The existing2-byte local channel gate is met; this is NOT a formal visual gate.
No time/pixel shift, fitted LOD, opacity adjustment or new original expected
image. Previous isotropic PNG hash is exact; all168 production images are exact.
The contact is a fixed nearest-neighbour crop for inspection, not a metric input.

Algorithm: invert each fragment's projected Jacobian; footprint ratio is the
longest screen derivative squared divided by derivative parallelogram area,
capped4; round to1..4 taps along that derivative; choose trilinear LOD from
longest derivative divided by ratio. Preserve separate linear RGB/alpha through
sampling, then original tint→ACES→explicit sRGB→fog→blend. The old isotropic
path remains independently selected. No UI-frame accumulation or new clock.

Driver math reference: google/swiftshader commit
708e9e77773860bbb2ad9bb2d61b451de4b84e31, src/Pipeline/SamplerCore.cpp.
This revision is NOT proven to match Chromium151's bundled driver. Only the
math informed this independently written test; no driver implementation was
copied. Agreement here does not establish all-GPU anisotropy equivalence.

Validation: narrow JVM passes; pause-fixed pixels and isotropic↔aniso4 round
trip exact. Initial comparer failed because the old manifest also contains an
`input` diagnostic group; corrected its folder bookkeeping to select the168
documented mounted/plain baseline images, not the expected pixels. Resources
remain test scoped; no new dependency, Android or production shader introduced.

```powershell
.\gradlew.bat :library:jvmTest --tests '*DioramaMountedStageTest.narrow'
python verification/compare-diorama-anisotropic.py
```

Requires frozen original sampler captures and saved c0e2804 production hashes.
Next reuse the same fragment sampling on OWN complete-shaped Latin/Arabic/CJK/
long rasters and the unchanged24 original plane matrix, including near views,
fractional width and resize restoration. White-only/mip0..5/front-plane/fixed
fog constraints still preclude production integration. Diorama remains internal;
no Android/window/device/performance or full-theme visual claim.
