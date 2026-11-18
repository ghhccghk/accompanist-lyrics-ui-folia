# Bounded internal plain-text sampler — 2026-11-18

Baseline9eaa844; fixed Folia cc6226e4. Internal opt-in library path, not a public
Diorama theme. Original frames, source locks/font/input/timing and previous
failed experiments remain unchanged. No old expected fixture is regenerated.

`DioramaPlainRenderer` owns only requested visible immutable plain rasters:
explicit `preparePlainRenderer(renderer,time)` OUTSIDE Canvas, then
`drawStage(...,plainRenderer=renderer)` on the owning drawing thread. Default
stage calls keep their old path; no automatic whole-song shader allocation.
Changes in visible set close stale entries. Limits:8 entries,32MiB estimated
additional image payload (F16 straight RGB base + F16 RGB/ARGB alpha lower mips).
Estimate excludes shared original raster, transient preparation buffers, shader/
driver/GPU overhead, and is NOT PSS or a device memory improvement claim.
Over-budget or unavailable samplers use the real old plane path; unsupported
choices are remembered for the unchanged visible set. Preparation errors
propagate after native cleanup; no broad catch-and-ignore.

JVM backend preserves full straight RGB/coverage independently, including colored
glyph input, through full mip chains and per-fragment capped4 anisotropic taps.
It applies linear tint BEFORE original ACES/sRGB/encoded fog, then blends. Same
FrontSide/double near/far polygon clip and Compose Canvas transform/parent state
are shared with existing planes. No per-frame glyph/mip preparation, original
GPU mip input, JNI dependency, source output pose injection or new media clock.
Closing/eviction disposes builder/effect/children/owned mip bitmaps and releases
references; original paragraph images remain shared and usable.

Android factory explicitly returns unavailable: ordinary Kotlin/Compose drawing
continues. No Android RuntimeShader equivalence is claimed or silently enabled.
The new path is not a cross-platform final renderer; Android remains a separate
capability/measurement task. Existing public seven themes are unchanged.

Same original48 mounted frames, all-five-image per-frame foreground union:

| Comparison | Before MAE | Sampler MAE | Before worst64 | Sampler worst64 |
| --- | ---: | ---: | ---: | ---: |
| Independent finite camera/material |6.7396726454|4.2943885774|20.3647798742|20.3647798742|
| Independent media camera/material |19.9224291502|18.6844694220|72.2582236842|69.1177808095|

Both matrices have zero mean/local regressions; complete peaks, bounds, edge
contacts and hashes are retained. All24 own-shaped isolated planes EXACTLY
match the independently validated fragment control (mean1.446805, worst3.363636
in its original24 comparison). All168 ordinary baseline PNG hashes remain exact.
No alignment, fitted time/LOD, dropped low-alpha pixels or cherry-picked rerun.
Fixed contact was inspected, including known3.2s/near/text-edge cases. The
unchanged finite worst20.365 and media geometry/material residuals remain real
failures to resolve, not permission to open Diorama or lower final acceptance.

11 targeted JVM tests pass: both mounted widths, all three own-plane widths,
owner budget/unavailable/error propagation/disposal, parent alpha/clip/fallback,
RGB byte order, partial near/far crossing, shared original-image lifetime, plus
existing clipping tests. Pause/size restoration/unordered view/remount and no
stable-frame preparation remain checked. Android library and Desktop compile
pass. Initial compile failed on expect/actual nullable return mismatch; fixed
the declaration, retained `.work/diorama-plain-sampler-integration.log`.
No device/real-window/performance result is inferred from those checks.

```powershell
.\gradlew.bat :library:jvmTest --tests '*DioramaPlainRendererTest' --tests '*DioramaPlainFragmentMatrixTest' --tests '*DioramaMountedStageTest' --tests '*DioramaPlaneClipTest' --tests '*DioramaClipCanvasTest' :library:compileAndroidMain :sample:desktop-app:compileKotlin
python verification/compare-diorama-plain-sampler.py
python verification/compare-diorama-anisotropic.py
```

The source driver math reference/limits remain in the anisotropic-control
README. It is not proof that all GPU drivers use identical tap choices.
Next locate active-plane/halo contributions to the fixed finite worst region,
using the existing source layer/input controls before adding another renderer.
Do not revisit old center-LOD/isotropic blurred candidates or change cameras
to compensate for text sampling. Android fragment capability, magnification/
more fonts and full stage resource/performance validation are still incomplete.
