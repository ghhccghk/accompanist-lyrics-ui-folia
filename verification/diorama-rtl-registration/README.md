# Original right-edge RTL word registration — 2026-11-18

Baseline3bcc28d; fixed Folia cc6226e464050a456a2913e062374f23d3844cc5.
Actual original unit positions come from the existing active-base.json captures
and versioned source.tsv, not native outputs. Original source/frames/fonts/
times/viewport/comparison rules are preserved; no pixel registration or fitted
screen translation is used. Diorama is still internal and not formally accepted.

## Independent cause and minimal change

Pinned dioramaSceneLayout.ts:layoutDioramaActiveUnits measures prefix/whole/unit
advance, rounds the UNIT raster width up, and anchors RTL from the RIGHT:
`centerPx=totalPx/2-(prefixPx+ceil(unitAdvancePx)/2)`. Raster drawX is pad90
(dioramaTextRaster.ts:rasterDioramaUnit). Native canonical crops previously
computed a ceil-width pivot from the full-run range LEFT and retained the
complete paragraph's fractional raster phase.

Original captured Arabic pivots versus native BEFORE:

| Unit | Source pivot | Before pivot | Error | ceil(range.width)-range.width |
| --- | ---: | ---: | ---: | ---: |
|0|192.951965|193.608002|.656036|.656036|
|1|-165.672028|-164.951965|.720062|.720032|

This source-formula mismatch was identified before editing the candidate.
Source prefixes/unit advances now determine the pivot/raster registration for
ONLY single-direction RTL words whose ALL boundaries are whitespace/line edges.
The same complete shaped paragraph is painted into the original pad90 windows;
standalone layouts are measurements only, not glyph drawing. Pivots now match
both independently captured original values exactly; source canvas widths match.
Internal units/grapheme clocks and complete-run line advance remain unchanged.

Cross-unit ligatures/parser fragments and mixed bidi retain canonical ownership,
integer raster phase and the existing full-run coverage guarantee. LTR/CJK
paths are untouched. This guarded word-registration adaptation is distinct from
breaking shaping or translating compared PNGs to hide a mismatch. It does NOT
make arbitrary source per-unit shaping equivalent; mixed/fragmented registration
and CJK's separate ceil/phase differences remain unresolved.

## Rejected broader candidate and fixed matrices

The broader all-independent-words registration was implemented and tested first,
then rejected despite its better aggregate mean: it adds10 Latin380 local
regressions (e.g.5250ms4.686542→7.698413). Its complete patch, all90 hashes/
metrics and fixed contact are preserved. Rejected images remain ignored in
`.work/diorama-registration-rejected-all-words`; no re-picking of source frames.
The final change is bounded to the actual evidenced RTL defect; LTR geometry
already matched original pivots to about6.1e-5px before this change.

Same90 base frames, SEVEN-image union including original/old ordinary+sampler/
rejected ordinary+sampler/final ordinary+sampler:

| Path | Mean before→after | Worst64 before→after | Mean/local regressions |
| --- | ---: | ---: | ---: |
| Ordinary |6.578335→5.735074|26.238095→24.666667|0/0|
| Internal base sampler |3.422528→2.344873|33.035714→8.970537|0/0|

Same180 complete glow/soul/body frames, four-image union of original/old
ordinary/final ordinary/final sampler: ordinary mean4.438836→3.899779,
worst64 26.238095→24.666667, zero mean/local regressions. Final sampled stage
versus OLD ORDINARY is1.798270 mean/worst8.970537, but has12 CJK local regressions
(.8–1.2 increase around1100/1500/1900ms at800/801). That is NOT an RTL-patch
before/after sampler comparison: an old sampled180 baseline was not captured.
These failures are explicitly retained for the opt-in sampler and must be
isolated, not hidden behind the full-stage mean. Full source/old/final bounds,
peaks/union/hash and every failure remain in comparison.json.

The full mounted48 Latin matrix and unsafe coverage PNGs total353 remain
byte-for-byte identical to3bcc28d, including all700ms entrance failures. All
Latin/CJK base ordinary+sampler and Latin/CJK ordinary-layer frames are exact.
Fixed contact inspected for original before/broad/final at Arabic800/1500,
801/2300/3100 and rejected Latin380/5250. No new frame delay, threshold, LOD fit,
alpha deletion, source rewrite or acceptance-gate change.

## Validation and limits

12 targeted JVM tests pass: original geometry oracle, exact unchanged full-run
ownership (ligature/split Arabic/mixed bidi/CJK), base3/layers3/mounted2 and
owner/core-time2. Stable time does not reshape/rasterize, pause/seek/remount and
owner release remain checked. Original material1e-7 and camera1e-9 gates pass.
Android library/Desktop compile pass; no device, real-window, PSS/CPU or formal
theme acceptance claim. No public Diorama or new native dependency is introduced.

```powershell
.\gradlew.bat :library:jvmTest --tests '*DioramaActiveRegistrationTest' --tests '*DioramaShapedMaskTest' --tests '*DioramaActiveTextTest' --tests '*DioramaActiveLayersTest' --tests '*DioramaMountedStageTest' --tests '*DioramaActiveBaseRendererTest' :library:compileAndroidMain :sample:desktop-app:compileKotlin
python verification/compare-diorama-rtl-registration.py
```

Comparator needs the saved pre-change/rejected ignored matrices and fails on
modified originals/baselines/unrelated paths. Next isolate CJK active sublayers
at the exact original800/1100ms stage before modifying any glyph/camera/halo.
700ms entrance, media camera, Android fragment capability, full-song ownership
and final cross-platform visual acceptance remain separate unfinished work.
