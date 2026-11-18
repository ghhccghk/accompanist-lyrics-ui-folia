# Frozen CJK texture / placement cross

Baseline08aaa09, fixed original cc6226e4 CJK800/1100ms BASE layer. No source
recapture or production edit. Existing original shader/texture/placement values
and source hashes are retained in the prior source-input and layer fixtures.
Here `texture-placement` names input-only crosses; source data NEVER drives
the actual library stage. Own-own replays the actual library base sampler
exactly. Every combination freezes and round-trips with no text reprepare.

All six images share ONE foreground union (including all low-alpha pixels):

| Inputs | Mean | Peak | Existing failing tile |
| --- | ---: | ---: | ---: |
|own-own|2.628243|75|5.092308|
|source-own|2.499104|37|3.988604|
|own-source|3.570627|75|5.843875|
|source-source|.639597|6|.698006|
|foreground-own|1.387852|21|2.130484|

The last diagnostic changes ONLY glyph coverage to the existing Windows
classicBodyRasterStyle; complete shaping/geometry/pivots/clocks stay identical.
Same-origin raw alpha for ALL7 CJK units improves MAE and peaks (default
peak93–135 versus foreground19–20). Unequal extents are only transparent
zero-extended, never shifted. Complete values/mass/extent/hash remain recorded.
Original source texture is shared from the existing passive export, not used as
a new target/asset. Cross geometry table retains canonical fractional offsets
and the known .5px CJK ceil pivot. Merely swapping source placement worsens the
stage; no guessed shift or all-word registration is adopted from this result.

One JVM exact-replay/input-round-trip/pause test passes. Contact inspected.
This is evidence for a body-only coverage hypothesis, NOT production/final
visual/device/performance acceptance. Next test that hypothesis on fixed90/
180 matrices while preserving grayscale halo input and complete shaping; reject
it if the same matrices reveal regressions. Do not retune halo/curve/threshold.

```powershell
.\gradlew.bat :library:jvmTest --tests '*DioramaCjkInputIsolationTest'
python verification/compare-diorama-cjk-input.py
```
