# Neighbour foreground coverage correction — 2026-11-18

Before c3e464c. Original Folia cc6226e4, Windows JVM offscreen. This corrects one
input-raster difference in internal Diorama; no public/sample Diorama entry or
complete visual acceptance is implied.

## Cause, source and change

The real mounted `DioramaLineRasterCache` calls original
`dioramaTextRaster.ts:rasterDioramaLine`:128px white Canvas foreground, left/middle
baseline and26px pad for these fixed inputs. The passive452-callback cache
control in `../diorama-mounted-raster` establishes exact dimensions/advance/middle
baseline but lower native alpha coverage. It does not identify a particular
browser/Skia gamma/contrast implementation; no such cause is asserted here.

`DioramaPlainText.prepare` now applies the **existing** `classicBodyRasterStyle`
policy before its normal full-paragraph preparation. That policy already serves
the other browser-derived text themes: Windows SubpixelAntiAlias/Normal with
subpixel positioning and no forceAutoHint, any explicit host paragraph style
preserved. Other desktop OS and Android return the original style. No changes
to font bytes, shaping, advance, baseline, origin, alpha curve, image dilation,
blur, camera, material, clock or media contract. Active glyph/halo input remains
separate. The change runs at preparation, not per frame.

## Frozen before/after results

The actual source72 PNGs are unchanged: mounted48 and original plain24. Before
native144 mounted/24 plain/16 raw images and hashes were saved BEFORE modifying
production. No source re-capture, screenshot registration, time shift, threshold
change, low-alpha deletion or omitted failing frame. Each report uses one fixed
before/after union; do not compare its means with reports using other unions.

| Same matrix / mask | Before foreground MAE | After | What remains |
| --- | ---: | ---: | --- |
| 8 actual cache alpha inputs | 3.784223 | 1.606260 | Peak130–133→20; input still not identical |
| 24 plain projected paragraphs | 13.079068 | 11.776553 | All24 means improve;11 local64 regressions |
| 48 mounted finite camera / analytic material | 7.058182 | 7.001486 | 34 improve,2 regress,12 unchanged |
| 48 mounted finite camera / finite material | 7.003199 | 6.946489 | Same34/2/12; source192-state control passes |
| 48 mounted production media camera | 20.561934 | 20.529350 | 24 improve,12 regress,12 unchanged |

Finite local64 peak20.434397→20.364780, but2 local probes regress. Media local64
peak82.267296→82.462264,7 local probes regress. Plain local64 peak52→50.719298,
but11 individual tiles regress, largest+3.857143 at arabic-801--3.6-1.9.png.
Every mean/local regression and unfiltered per-frame bounds/hash is retained in
comparison.json. Improved raw coverage is a bounded correction, **not** a claim
that all projected samples pass or that media-camera residuals are resolved.

Same original font/viewport/colour/geometry/time and complete shaped paragraph
on each side; Latin/Arabic/CJK/long6 frames each. Raw geometry matches all8
original rows, source numeric camera/material gates stay1e-9.8 targeted JVM tests
pass, including pause/pixels, unordered seek, remount, core identity, size/offset
and bounded raster preparation. Fixed contact sheet inspected. See validation
record for additional public integration/build checks. Android/other OS defaults
are unchanged; no Android device, true Desktop window or performance claim.

```powershell
# After rendering c3e464c and before changing production, this refuses stale,
# incomplete or already-existing baseline data:
python verification/compare-diorama-plain-coverage.py --save-before
# Apply the coverage change, then:
.\gradlew.bat :library:jvmTest --tests '*DioramaMountedStageTest' --tests '*DioramaPlainTextTest' --tests '*DioramaRasterWindowTest' --tests '*DioramaBreathTest'
python verification/compare-diorama-plain-coverage.py
```

The separate raw report is the historical candidate investigation at c3e464c;
do not overwrite it with this new production output. Next bounded question:
using identical actual source neighbour textures as an explicitly labelled
diagnostic control, how much700ms projection/backend residual remains? That
control must not feed production, and replaying own textures must first match
the existing production stage exactly.
