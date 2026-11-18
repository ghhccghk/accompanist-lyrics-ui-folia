# Own-shaped anisotropic plain-text matrix — 2026-11-18

Baseline438176e. Unchanged original24 plane frames/inputs/fonts, fixed Folia
cc6226e4 and Chromium151.0.7922.34. Four scenes Latin/Arabic/CJK/long, dimensions
380/800/801×600/DPR1, both original camera offsets. Native derives camera from
the same input seed/offsets; no original rendered pose/texture drives drawing.
This original isolated Three stage has NO tone mapping/fog, unlike mounted R3F;
the test explicitly selects those original policies, not a fitted material.

Native independently prepares each complete white paragraph once; full shaping
and raster dimensions remain unchanged. Full linear RGB occupancy/separate
alpha mip chains, per-fragment capped4 anisotropic taps/LOD as the already
validated shared-cache control. No source mip asset, frame accumulation,
registration, alpha deletion, manual LOD or rewritten upstream formula.

| Same24 per-frame three-image unions | Mean foreground MAE | Worst64 |
| --- | ---: | ---: |
| Existing production |11.7426769523|50.7192982456|
| Own-shaped fragment control |1.4468047603|3.3636363636|

All24 means and local worst64 improve; no mean/local regression. All original
hashes verified against the independently preserved pre-candidate manifest,
all production hashes unchanged. Every row/peak/bounds/edge contact is retained
in comparison.json. Fixed contact includes previous isotropic candidate's
Latin/Arabic near-view failures, plus CJK, long and fractional-size cases. It
was inspected: the previous near-view blur is not reintroduced. Thumbnails
are never metric inputs. Remaining edge/font/backend differences still count;
this is not full-theme or all-platform visual acceptance.

Four targeted JVM tests pass (three sizes plus old mounted narrow). Checks:
camera queries reuse text/mips; unordered view/content restoration exact;
actual Canvas half-width→full-width projection changes then restores exact
pixels without preparation; pause no changes; unmount closes child shaders/
builder/effect and empties owner; remount reconstructs identical image. Old
isotropic/default-aniso controls and all168 production hashes remain exact.
No Android/real-window/device/performance claim or production adoption yet.

```powershell
.\gradlew.bat :library:jvmTest --tests '*DioramaPlainFragmentMatrixTest' --tests '*DioramaMountedStageTest.narrow'
python verification/compare-diorama-anisotropic.py
python verification/compare-diorama-plain-fragment.py
```

Next move the established sampler into a bounded, explicitly owned internal
text-stage path: outside-frame preparation, real front/near/far clipping and
parent alpha, original linear tint before tone mapping, eviction/disposal.
Recheck fixed mounted48/plane24 before adoption; don't eagerly allocate a
whole-song mip/shader set. Android capability remains a separate validation.
Diorama stays internal; seven public previews remain unchanged.
