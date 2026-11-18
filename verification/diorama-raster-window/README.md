# Complete-shaped Diorama raster windows

Pinned Folia cc6226e464050a456a2913e062374f23d3844cc5; AGPL-3.0-only.
Internal text preparation only. Diorama remains unavailable in the sample.

`DioramaActiveText.prepare` previously allocated a full-line RGBA bitmap before
copying its integer-owned windows to every unit texture. The line width was
unbounded even when all individual words were small. It now draws the **same
complete shaped layout** directly into each owned window, with an integer origin
shift. It does not shape individual characters/words, change source units, alter
internal timing or change the source pivot. No full-line bitmap is allocated.
The previous whole-run coverage partition and layered drawing remain unchanged.

Independent untouched original `buildLineGraphemeTimeline` and
`splitDioramaLyricUnits` supply414 unit/grapheme records for the two new32-word
synthetic cases (`../diorama-fixtures/raster-window-v1`). Real published core
objects are used by the Kotlin test; every source unit/range/time and internal
glyph survives. Actual resolved Motion locks, revision, generator/source hashes
and complete inputs are recorded; this source oracle uses Node/Vite SSR, not
browser pixels. Existing original full-run and isolated-unit stage oracles remain
unchanged; none is replaced by native expected values.

| Case | Complete advance | Eliminated full-line RGBA allocation | Retained body+halo RGBA | Widest unit | Changed pixels across all1024px windows |
| --- | ---: | ---: | ---: | ---: | ---: |
| Latin | 9205.248px | 13,515,840 bytes | 40,222,080 bytes | 526px | 0 |
| Mixed bidi | 10136.063px | 14,856,480 bytes | 54,656,640 bytes | 747px | 0 |

These are calculated image extents, **not measured process memory or a performance
gain**. Halos/scratch and whole-song retention remain substantial. A single huge
unit or discontiguous bidi-owned unit can still require a wide texture; this
change does not claim a global4096px bound or make arbitrary songs production-ready.
Direct clipped drawing may trade preparation CPU for the eliminated temporary;
there is no device timing claim. Stable display frames do no new preparation.

Before the change, `.work/diorama-direct-raster-before.json` preserved native
hashes. After completion of all capture tasks,90 active-base,180 active-layer
stage frames and54 neighbor after-frames are exactly unchanged, as are24 emitted
unit body/halo images,54 neighbor before-controls and16 shaping controls (418
files total). All previous original-stage MAE/local residuals, including the
small regressions recorded by the earlier coverage fix, therefore remain. No
new source images were sampled or selected; no alignment, alpha threshold or
expected-value update. The existing contacts remain valid for these identical
images, so no redundant screenshot set is committed.

Checks passed: new long-line/source-clock coverage test; original complete-run
coverage and full active/neighbor matrices; core offset/resize/reduced-motion/
pause/seek/unmount tests; Android library and Desktop sample compilation.
Logs `.work/diorama-direct-raster-initial.log` and
`.work/diorama-raster-window-final.log`. An initial test compile failure used
Long instead of the published model's Int times; retained in
`.work/diorama-raster-window-test.log`, corrected without changing expectations.
No Android runtime, real-window, full-song memory, fog/depth or theme acceptance.

```
node verification/classic-browser/generate-diorama-raster-window.mjs
./gradlew :library:jvmTest --tests '*DioramaRasterWindowTest' --tests '*DioramaShapedMaskTest' --tests '*DioramaTextPlaybackTest' --tests '*DioramaActiveTextTest' --tests '*DioramaActiveLayersTest' --tests '*DioramaNeighborStageTest' :library:compileAndroidMain :sample:desktop-app:compileKotlin
python verification/compare-diorama-raster-window.py
```

Generator refuses overwrite. Comparator requires the preserved before manifest
and completed native outputs; it fails on any changed/missing hash. Use Windows
`gradlew.bat`. Next: allow cancellation **inside** long-line ownership/raster/halo
preparation, not just before/after each line; whole-song bounded residency and
single-unit tiling remain further prerequisites before public exposure.
