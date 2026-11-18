# Rejected bitmap-shader sampling control — 2026-11-18

Baseline62418a3, fixed Folia cc6226e4, the same actual mounted380×600/DPR1/60Hz
incoming700ms mesh7 clone and actual original cache7 as `diorama-mounted-input`.
No original logic, images, geometry, timing or acceptance thresholds changed.

The installed Compose1.12.0 `SkiaBackedCanvas.skiko.kt` creates an Image from a
Bitmap, converts filterQuality to Skia sampling and calls drawImageRect with
strict=true. This was a hypothesis to test, not proof of a root cause.
The rejected candidate instead creates a public Skia Bitmap shader with
FilterMipmap(LINEAR,NONE or LINEAR), drawing through Compose ShaderBrush/drawRect
to retain parent alpha. Its cache is test-local and closed after use. Production
would retain the default null hook. The full candidate, including its test-only
sampler, is preserved in `rejected-candidate.patch`; it has been REVERSED.

| Same-input control | Foreground MAE | Nonbackground pixels | Positive RGB mass |
| --- | ---: | ---: | ---: |
| Original actual mesh clone | — | 409 | 22421 |
| Restored production/default | 13.76202118989405 | 193 | 27678 |
| Shader, no mipmap | 13.76202118989405 | 193 | 27678 |
| Shader, trilinear | 13.76283618581907 | 193 | 27655 |

All-four-image nonbackground union409 pixels, including lowest contributions;
no alignment, threshold, alpha fitting, camera change, resampling of the oracle
or time shift. No-mip pixels and PNG hash exactly equal default. Trilinear changes
23 RGB components and slightly worsens MAE; bounds and worst64 remain unchanged.
The earlier probe's `peak46` was a SOURCE-DIFFERENCE metric, not brightness above
background; comparison.json labels its brightness peak57 explicitly as `peak`.

Reject this candidate. It establishes neither that strict suppresses mipmaps nor
that mipmap/anisotropy is irrelevant. Do not retry another filter hint without
new evidence. Next isolate actual original mesh projected corners/UVs/material
and independently computed native homography before adding a sampling adapter.

Candidate narrow JVM test passed; after exact reverse-apply, the same narrow test
passes and production/input-control PNG hashes match the frozen62418a3 report.
The original source PNG and fixture remain unchanged. No production changes,
Android/real-window/device/performance or formal visual acceptance claimed.

Reproduce only on a clean62418a3-or-compatible checkout, retain output before
reverting the patch; do not overwrite historical reports:

```powershell
git apply verification/diorama-sampling-shader/rejected-candidate.patch
.\gradlew.bat :library:jvmTest --tests '*DioramaMountedStageTest.narrow'
git apply --reverse verification/diorama-sampling-shader/rejected-candidate.patch
.\gradlew.bat :library:jvmTest --tests '*DioramaMountedStageTest.narrow'
```
