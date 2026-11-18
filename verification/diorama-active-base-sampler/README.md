# Bounded internal active BASE sampler — 2026-11-18

Baseline baeef5a; original Folia cc6226e464050a456a2913e062374f23d3844cc5.
This is an explicit internal JVM option, NOT a public Diorama or formal visual
acceptance. All seven public preview themes and ordinary default draws stay
unchanged. Original fixed fixtures/frames/conditions and earlier failures remain.

## Source and behavior

Original dioramaTextRaster.ts:58–62 applies CanvasTexture anisotropy4/sRGB to
active base as well as plain neighbors. DioramaActiveUnitPlanes.tsx:63–77 uses
normal transparent base after all additive17 glow/18 soul meshes. The independent
worst-frame layer control proved that3500ms has no visible original glow/soul.
`DioramaActiveBaseRenderer` reuses the already controlled JVM RGB/alpha-pyramid
fragment sampler ONLY for base. Actual core/shaping/pivots/imageOffset/timing,
camera, material states, source-color order and existing additive layers stay.

Call `playback.prepareActiveBaseRenderer(owner,lyricTime)` OUTSIDE Canvas, then
`drawStage(...,activeBaseRenderer=owner)`; use one owning drawing thread. Owner
keeps only the current line's immutable base images, max32 entries/32MiB estimated
ADDITIONAL image payload. Estimate excludes shared original images, transient
buffers, driver/shader/GPU overhead; it is not PSS or a performance claim. Same
line does not rebuild mips; switching releases stale samplers. Unsupported or
over-budget images use the actual old plane; misses are remembered for that
line. Preparation exceptions propagate; explicit retry or empty/unmount after
partial success remains possible. Close is idempotent and retains original
paragraph images. Android explicitly reports unavailable and draws ordinary
planes; no Android shader, new native dependency or runtime enablement is claimed.

## Fixed comparisons and retained regressions

The actual3500ms active-only frame uses original/base-before/base-after shared
union813 (the earlier two-image union was808). MAE19.670767→1.308733,
channel peak74→22, worst64 20.624204→1.411890; pixels original808/before418/
after762. One bottom-row support difference remains. Fixed crop visibly removes
the previous minified jagged/undercovered text. No shift, threshold or input fit.

Same original48 mounted frames, ALL SEVEN images' per-frame foreground union:

| Matrix | Mean before→after | Worst64 before→after | Mean/local regressions |
| --- | ---: | ---: | ---: |
| Independent finite camera/material |4.273842→1.961853|19.047059→6.883721|4/4|
| Independent media camera/material |18.583225→17.276814|69.117781→69.117781|0/0|

Before already includes the unchanged plain-neighbor sampler. These new unions
differ from the previous five-image report; do not compare their aggregate means
as a gain. Four finite700ms entrance frames regress: means .575298→.627640,
.450188→.497038,.647074→.666791,.563495→.583650. Their worst tile is y0;
all complete results, bounds/edge contacts/hashes stay in comparison.json.

Own complete-shaped Latin/Arabic/CJK90 frames at380/800/801×600 use the unchanged
source None/no-fog isolated stage, same three-image foreground union. Means
6.580503→3.423427, zero mean regressions, but16 Arabic local regressions and
worst64 26.238095→33.035714. This is NOT whole-matrix visual acceptance. The
fixed full/cropped contacts were inspected, including Arabic800/1500 and801/
2300/3100. Preserve these failures; do not waive them as backend noise or alter
complete shaping to imitate separately rasterized original units.

An additional source-input isolation uses the90 EXISTING original observations,
original passively exported base textures AND original placements/materials.
Deterministic source.tsv extraction records exact provenance/hashes in
active-base-sampler-input-v1/conditions.json. No recapture or native-as-expected
fixture; these inputs NEVER drive the actual library path. Its foreground mean
.445855, peak8, worst64 1.8125 versus the original indicates own raster/placement
differences remain important; it does not isolate every input contribution or
prove sampler/GPU equality. Peak8 and complete raw differences are retained.
The original browser/font/locked conditions remain in original active-base.json
and mounted-cut-v1; the isolated stage does not depend on Motion envelopes.

## Verification and next

16 targeted JVM tests pass across owner/fallback/error/disposal, core media/
offset/reduced motion/actual resize/seek/remount, both mounted widths, three own
base widths, three original-input widths, original active-layer and clip tests.
Ordinary/control567 PNG hashes remain EXACT. Source callbacks, materials and
numeric gates are unchanged; no stable-frame glyph/mip preparation. Android
library and Desktop compilation pass. No new device/real-window/performance
result is inferred; full media-camera/halo/depth/long-song resource work remains.

Next resolve the input/placement contribution to the Arabic local regressions
with source-preserving controls, and entrance y0 coverage separately. The
independently demonstrated minification repair justifies keeping this bounded
opt-in capability, not enabling it as the final cross-platform renderer or
opening Diorama. Existing plain-owner partial-failure cleanup also needs the
same explicit failed-request state identified by this owner regression.

```powershell
.\gradlew.bat :library:jvmTest --tests '*DioramaActiveBaseRendererTest' --tests '*DioramaMountedStageTest' --tests '*DioramaActiveTextTest' --tests '*DioramaActiveLayersTest' --tests '*DioramaPlaneClipTest' --tests '*DioramaClipCanvasTest' :library:compileAndroidMain :sample:desktop-app:compileKotlin
.\gradlew.bat :library:jvmTest --tests '*DioramaActiveBaseSharedInputTest'
python verification/compare-diorama-active-base-sampler.py
```

The comparator requires preserved ignored pre-change hashes and generated
original/native PNGs. It fails on altered original or ordinary/control images.
Raw source inputs and large regenerable matrices are not production assets or
committed frame dumps; hashes, necessary compact contact and failures are kept.
