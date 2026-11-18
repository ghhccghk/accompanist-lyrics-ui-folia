# Actual mounted neighbour-cache texture inputs — 2026-11-18

Baseline e69f81d, fixed Folia cc6226e4. This completes the bounded700ms input
question; it is not a production renderer fix or final theme acceptance.

`DioramaScene` and `DioramaLineRasterCache` remain untouched. A passive Fiber
getter reads the real cached CanvasTexture images and already-set2D font/metrics.
No new call to rasterDioramaLine, context write, source-ref mutation, frame or
pixel injection. The dedicated380×600/DPR1/60Hz replay retains the entire original
outgoing0–6.8s and incoming0–.7s452 callbacks. Before AND after reading8 real
cache rasters, the full screenshot is byte-identical to the independently frozen
mounted-380-60-700.png (228c7487...). Original component state is unchanged;
unmount leaves zero R3F roots and queued cache RAFs. Actual original-lock Motion,
all direct R3F runtime versions, browser151.0.7922.34, font/source/host hashes and
intervals are in `../diorama-fixtures/mounted-raster-v1/scene.json`.

Kotlin independently prepares the existing production full-shaped paragraphs
from published-core objects. Original font/advance/width/height values are only
assertions, not drawing inputs. All8 exact geometry rows pass; every middle
baseline is36.46875px, font128px, pad26px. Y ink bounds match55–183; X bounds differ
by at most1px. Alpha sums are1.149–1.347% lower, with all-positive-alpha union
foreground MAE3.784223 and peak130–133. This reveals a real pre-projection input
difference; it does not establish that it explains the mounted residual or
identify a particular font-engine cause.

One falsifiable, non-fitted candidate reuses the project's existing Windows
foreground raster settings (SubpixelAntiAlias/Normal/subpixel=true/autoHint=false),
through the public Compose paragraph style, preserving any explicit host style.
All8 raw rows improve: alpha MAE1.606260, peak20, while advance/middle baseline
stay equal. The candidate is test-only at this commit. Complete mounted48 and
the existing Latin/Arabic/CJK/long-line matrix must be compared using the SAME
before/after union before adopting it. No pixel curve, dilation, registration,
time shift, low-alpha deletion or fixture expectation was fitted/relaxed.

`comparison.json` retains every raw row including bounds, alpha mass/nonzero,
local64 worst, peak and source/native/candidate PNG hashes. `contact.png` shows
fixed unscaled raw crops and unamplified signed differences; raw full rasters
stay ignored under build/verification. Windows offscreen results only, no new
Android runtime, real Desktop window, audio, performance or visual acceptance.

```powershell
# Initial capture refuses an existing frozen fixture. Preserve it; do not rerun
# the whole mounted48 just to inspect a cache image.
node verification/classic-browser/capture-diorama-mounted-raster.mjs
.\gradlew.bat :library:jvmTest --tests '*DioramaMountedStageTest.narrow'
python verification/compare-diorama-mounted-raster.py
```

Sources: original dioramaTextRaster.ts:rasterDioramaLine; existing native
DioramaPlainText.prepare and text/FontMetrics.jvm.kt:classicBodyRasterStyle.
No new upstream assets/runtime enter the library. Diorama remains internal.
