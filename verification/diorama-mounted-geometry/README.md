# Actual mounted mesh7 projection control — 2026-11-18

Baseline2ccdc1a, untouched Folia cc6226e4. Same380×600/DPR1/60Hz/incoming700ms,
font, full452-callback outgoing/incoming history and actual source mesh7/cache7.
Read actual Float32 PlaneGeometry positions/UV/index, world/view/projection
matrices, projected corners, material and texture settings. Host-only getter
does not update any matrix, mutate a ref/material or render an extra frame.

Full original PNG before/after still228c7487..., state and EVERY callback delta
match the earlier frozen mounted-input-layers-v1. Motion/direct rendering
versions, browser, source/host/lock/font hashes recorded in fixture scene.json.
Teardown roots0/queued RAF0. No new baseline frames replace the original48.

Native uses independently prepared published-core paragraphs, frame/placement,
finite source-history camera operator and normal production homography; source
expected coordinates only assert results. Original actual buffer vertices are
Float32, native plane coordinates Double. Fixed first-run gates: world/depth1e-6,
projected pixel1e-5, internal homography consistency1e-9. No fitted translation,
camera, alpha threshold or fixed time shift.

Measured corner peak: world4.054829e-9, projected1.482564e-7px, depth1.617355e-9;
homography consistency5.684342e-14px. This cannot explain the existing193 versus
409 covered pixels. It only isolates this mesh/instant, not all geometry.
Actual texture is LinearMipmapLinear, anisotropy4, generated mipmaps, sRGB,
non-premultiplied alpha; WebGL multisample4. The analytic texture-to-screen
Jacobian and its singular values are in comparison.json, without hand-picked LOD.

Next diagnostic: on a separate CLONE of this exact source mesh and texture,
compare original sampling against no-mipmap linear and anisotropy1 trilinear.
Restore original screenshot/state without another RAF; do not call these
modified diagnostic renders original acceptance frames. That can test whether
source sampling itself explains the coverage spread before another native patch.

```powershell
# Refuses existing fixture; retain frozen data rather than rerun in place:
node verification/classic-browser/capture-diorama-mounted-raster.mjs --geometry
.\gradlew.bat :library:jvmTest --tests '*DioramaMountedStageTest.narrow'
python verification/compare-diorama-mounted-geometry.py
```

Narrow JVM passes, all production/input-control PNG hashes unchanged. No
production changes, public Diorama, device/window/performance or visual pass.
