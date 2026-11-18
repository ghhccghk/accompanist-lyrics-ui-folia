# Actual source-cache input / cloned mounted neighbour control — 2026-11-18

Baseline46f89e7, fixed Folia cc6226e4. Diagnostic only; no production use of
original pixels, new style entry, device/performance or final visual acceptance.

Source replay exactly380×600/DPR1/60Hz at incoming700ms, outgoing history0–6.8s,
same font/inputs/runtime locks and452 original callbacks. Actual source cached
neighbour7 is retained as a35,731-byte **synthetic test-only** texture, with its
independently captured c3e464c SHA-256. This bounded fixture is needed to make the
shared-input test reproducible without private build output; other7 raw rasters
and full screens remain ignored. No user lyric/cover/font or SF Pro assets.

The actual original mesh is cloned with its current world matrix, geometry,
texture, cloned material and mounted camera/fog/renderer. No original mesh/ref
is changed. This extra diagnostic render is explicitly not an original whole
scene capture. Restoring the real original scene without another RAF gives the
frozen original screenshot hash again; original component state unchanged;
teardown roots0/cache RAF0. `mounted-input-layers-v1` records original source,
host, font/browser/lock hashes, exact history and diagnostic-layer PNG hash.

Native own-texture diagnostic replay MUST match real production drawStage
pixels exactly before substitution. The test meets this strict check and retains
the independently checked source finite camera/material gates. It then replaces
only cache7's image, retaining native core, widths, world frames, placement,
camera/material values and full active shaping. Source expected poses are never
renderer inputs. Test-only per-plane counts: neighbour7=193 nonbackground pixels,
active=89, other7 neighbours=0 in this fixed native view.

Same all-nonbackground three-image union, no alpha threshold, registration,
phase shift, fitted camera or omitted pixel:

| Diagnostic | Own input MAE | Shared cache7 input MAE | Channel peak |
| --- | ---: | ---: | ---: |
| Whole mounted view / native finite camera+materials | 11.070573 | 11.114052 | 46 /46 |
| Original actual neighbour7 mesh clone only | 13.705786 | 13.762021 | 46 /46 |

Source isolated plane covers409 pixels; native covers193 with either image.
Bounds source[176,416,270,425], native[177,417,270,424]. Native has MORE integrated
positive RGB mass27,493/27,678 vs22,421, not a simple uniform faintness problem.
This rules out cache7 font coverage as a sufficient explanation. It does not
prove every material/projection/backend path equal, establish a specific mipmap
cause, or justify translating/dilating the image. Raw input coverage correction
and these graphics residuals are separate. Full metrics/contact preserved.

Next falsifiable investigation: inspect the exact installed Compose draw path,
particularly whether its strict source-rectangle constraint suppresses mipmap
sampling even when Medium is requested. The old all144-identical filter hint
candidate remains rejected; do not merely repeat that hint. Test any new lower
level adapter on this identical shared-input control first, then the frozen full
matrix, preserving failures and original source inputs.

```powershell
# Source --layers refuses existing frozen fixture, does not replace mounted48:
node verification/classic-browser/capture-diorama-mounted-raster.mjs --layers
.\gradlew.bat :library:jvmTest --tests '*DioramaMountedStageTest.narrow'
python verification/compare-diorama-mounted-input.py
```
