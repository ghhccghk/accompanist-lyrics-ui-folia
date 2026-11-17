# Pendolo mounted chorus wheel: original subtree comparison

Pinned upstream cc6226e464050a456a2913e062374f23d3844cc5 / AGPL-3.0-only.
`VisualizerPendolo.tsx:590–653` → `PendoloChorusTrack.kt`, `PendoloHalo.kt`
and `PendoloWheelTarget.kt`. The existing reference generator retains the
original text-wheel JSX and `useSpring` declarations unchanged. New host adds
explicit alternating chorus flags and keyword colors, without original gears,
audio-analysis or background engines. Fixtures retain covered licensed fonts.

Port preserves parent initial=false, retained easeOut scale retargeting, new
halo opacity0/scale.96, immediate removal on inactive (no AnimatePresence),
root16px-em insets, original radial stops/opacity product, CSS blur and layer
pivots. Main/subtitle reservations are not enlarged by decorative halo scale.
Semantic media-boundary history rebuilds state under random seek/remount;
transport pause never removes chorus. Reduced motion samples stable targets.

168 full-wheel pairs plus168 source-rotation isolation pairs cover three
intensities,Latin/wrapped/Arabic/CJK, gaps/ends and160ms overlapping switches.
804 rows/5628 numeric fields. Maximum parent-scale difference.001169,
halo-opacity.0157202 and halo-scale.00179547. Actual source inspection records
1.02s semantic start observed at1.033333s by60Hz RAF; at1.1s its original halo
WAAPI has66.6667ms currentTime, vs80ms semantic time. Evidence is
`chorus-source-events.json`; no fixed-frame delay was added to native.
This remains a measured discrepancy, not an accepted final standard.

Full comparison: mean foreground MAE11.918332, peak45.121911; content-box
peak6.019072px. Using actual source rotation only as an isolation control:
mean11.457311, peak20.502864, box peak.231502px. Dense tick/velocity sampling,
idle wrap-height and raster differences remain; do not declare full parity.

Halo-only control retains all layout and source wheel rotation, with content
group opacity0. All nonbackground pixels count (no >8 threshold hiding faint
halo).168 pairs:mean union MAE.313817, peak1.389192, peak channel error2/255.
48 forced-software controls:mean.294727, peak.996372, peak channel error2.
`halo-contact.png` uses clearly labelled x8 background-relative amplification;
raw PNGs and metrics are not amplified. Native low-alpha blur/coverage rings
still differ. `contact.png` and `halo-contact.png` inspected.

Failed isolation with inherited visibility:hidden exposed CSS transition-all
keeping idle text visible during its visibility transition. It was rejected;
raw failure images remain in `build/verification/pendolo-chorus-halo-visibility-invalid`.
Corrected opacity control hides the group without inherited discrete transitions.

First integration failed with StackOverflowError: DrawScope.record reused caller
context while using that same scope as Density. `ClassicLayer.kt` now snapshots
Density and calls the independent member recorder. New density/fontScale and
caller-coordinate pixel controls cover1/1.5/2 density, native and software.
Failure log/XML remain `.work/pendolo-chorus-wheel-stack-failure.*`.

All Pendolo tests, density control and two-end builds pass
(`.work/pendolo-halo-isolation-final.log`); shared transparent/native-blur,
Cadenza target/motion and Cappella playback regression passes separately
(`.work/pendolo-halo-final.log`).1034 old native PNG hashes are unchanged.
Random seeks, pause and remount preserve exact native pixels. No connected
Android device at verification; no visible window/emulator launched. Software
JVM is not Android-device acceptance. New effect performance is unmeasured.

Reproduce after the source generators have prepared the pinned JSX:

```
node verification/classic-browser/generate-pendolo-tick-stage-reference.mjs
node verification/classic-browser/serve.mjs
node verification/classic-browser/capture-pendolo-chorus-stage.mjs
node verification/classic-browser/inspect-pendolo-chorus-events.mjs
./gradlew :library:jvmTest --tests '*Pendolo*' --tests '*ClassicLayerDensityTest*' --tests '*CadenzaTargetStageTest*' --tests '*CadenzaMotionStageTest*' --tests '*CappellaPlaybackTest*'
python verification/compare-pendolo-chorus-stage.py
python verification/compare-pendolo-chorus-halo.py
```

Raw full/isolated/halo images live in `build/verification/pendolo-chorus-*`.
Next: resolve dense wheel sampling and remaining whole-text/raster differences,
then wire a clearly labelled public/core calibration preview and interaction.
No Pendolo selector or final-theme claim is introduced by this commit.
