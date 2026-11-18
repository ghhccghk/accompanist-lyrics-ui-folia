# Bounded mounted text-filter investigation — 2026-11-18

This is a **rejected candidate**, not a delivered sampling feature or visual
acceptance. Baseline325c2bd, fixed Folia cc6226e4, unchanged mounted48 scene/font/
view/time/renderer conditions from `../diorama-fixtures/mounted-cut-v1`.

Concrete source observation: `dioramaTextRaster.ts:makeTexture` creates a real
CanvasTexture with anisotropy4; original-lock Three0.185.1 Texture defaults to
LinearMipmapLinearFilter. Native was using the simple `DrawScope.drawImage`
overload. The falsifiable candidate was to request `FilterQuality.Medium` via
the source/destination rectangle overload for every plain/active/fog image draw,
without changing glyph rasters, geometry, alpha or camera/material states.

Both full mounted tests passed their independent1e-9 source controls. All144
native finite/material/media PNGs nevertheless remained **byte-identical** to
the baseline. No mean or local64 improvement at700ms or any other probe. The
original48 hashes remain unchanged. `attempt.json` retains all48 before/after
rows using one five-image original/material-before/material-after/media-before/
media-after all-ink union per frame, including lowest alpha and all regressions.
The material-control mean7.013977 and media mean20.603258 are unchanged; these
unions differ from other reports and cannot be compared across them as gains.

The entire candidate is retained as `rejected-candidate.patch` and **reverted
from production**. This run neither proves general Skia/Three sampling
equivalence nor diagnoses why the requested quality has no observable effect in
this offscreen backend. Do not repeatedly retry a filter hint, promote it as an
optimization, or infer Android/real-GPU behaviour from these identical PNGs.
Private before files/log/full diff also remain in `.work/diorama-sampling-*`.

Next bounded question: compare the actual mounted source neighbour-cache
canvas alpha/baseline and the independently shaped native texture for the same
700ms visible line, before projection, using the frozen font/size. This
separates input coverage from projection/backend contributions without fitting
source pixels or changing camera history. No new effect prototype is required.

```powershell
# After saving325c2bd build/verification/diorama-mounted-kotlin as the BEFORE:
git apply --unidiff-zero --ignore-space-change verification/diorama-filtering/rejected-candidate.patch
.\gradlew.bat :library:jvmTest --tests '*DioramaMountedStageTest'
# Revert only this candidate after inspection; preserve user changes.
git apply -R --unidiff-zero --ignore-space-change verification/diorama-filtering/rejected-candidate.patch
```

No native window/device capture, publication, runtime dependency, performance
claim or source-oracle change. The latest sample APK uses the restored
production source, never this candidate.
