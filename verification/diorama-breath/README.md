# Diorama media-domain glow breath — 2026-11-18

Pinned Folia `cc6226e464050a456a2913e062374f23d3844cc5`,
`DioramaScene.tsx:546–555`, supplies
`0.9 + 0.1 * sin(frameState.clock.elapsedTime * 1.9)` to the active
unit material operator. Production previously always supplied `breath=1`.
`DioramaUnitFrame.kt` now retains that exact modulation; `DioramaTextPlayback`
supplies a reconstructible lyric/media phase, including the same offset as
text/camera. Explicit cut/loop anchors carry only the accumulated phase value;
seeking does not create an anchor, clock task or frame-history dependency.
Existing full shaping, internal unit clocks, camera integration and sources
remain unchanged. This is still an **internal** text stage, not a new sample theme.

## Necessary time-domain adaptation

The original wall clock continues while media is paused. The library contract
requires paused media-driven animation to freeze and arbitrary seeks to
reconstruct. This implementation intentionally uses media phase, not wall RAF.
At the frozen mounted cut, both phases are `6.8 + incoming lyric time`, so their
breath factors coincide without a fitted offset or source-state injection.
Normal playback uses that phase; reduced motion uses a constant factor1 and
continues suppressing the soul movement. No claim of general wall/RAF history
equivalence is made. The source Float32 envelope/history remains a separate
finite control, not the production media clock.

## Fixed full mounted comparison

Before is b85e4ce, preserved at `.work/diorama-breath-before/native` before
modification. All48 original mounted PNG hashes,4686 camera intervals and192
independent material controls retain their previous gates. Same source/font/
viewport/density/configuration/times and five-image all-nonbackground union per
frame (original, finite before/after, media before/after); lowest alpha retained,
no registration, delay, filtering, frame omission or selective recapture.

| Path | Before foreground MAE | After foreground MAE | Before/after worst64 |
| --- | ---: | ---: | ---: |
| Own finite camera + analytic material | 6.954943 | 6.887058 | 20.434397 / 20.434397 |
| Own media camera + analytic material | 20.290248 | 20.258454 | 83.400000 / 83.400000 |

Finite:28 improve,0 regress,20 unchanged. Media:22 improve,6 regress,20
unchanged;7 frames have a worse local64 block. All six mean regressions near
3199/3200/3201ms, and all local regressions, stay in `comparison.json`.
Fixed six-probe `contact.png` inspected: camera position/size, clipping and
font raster differences remain plainly visible. A small mean improvement is
**not** final visual acceptance. Do not compare means across other reports
that use different unions.

## Contracts and reproduction

`DioramaBreathTest` checks the original phase formula/range plus an actual
core/full-shaped text stage: modulation changes pixels, advancing only the UI
clock does not; reverse seek, positive/negative offset, cut/loop phase,
380→800 resize restoration, remount and reduced motion restore deterministic
pixels. Its frontal view deliberately isolates modulation; it is not an
upstream screenshot. Full mounted replay still checks independent source
material states at1e-9, Float32 envelopes exactly, and original grouping/timing.
Existing direct-Three tests explicitly keep their historical breath1 condition;
their original oracles are not rewritten to bless the new production policy.
All21 targeted JVM checks pass; Android library and Desktop compile. The610
old direct-Three PNGs and48 finite-material controls remain byte-identical.
Counts and test scopes are in `validation.json`.

```powershell
.\gradlew.bat :library:jvmTest --tests '*DioramaBreathTest' --tests '*DioramaMountedStageTest'
python verification/compare-diorama-tonemapping.py --breath
```

No new Android runtime, real-window/audio, memory or device-performance claim.
Media-camera equivalence, analytic-vs-finite envelopes, glyph raster/coverage,
complete sequencer/depth/residency and devices remain unresolved.
