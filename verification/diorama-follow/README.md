# Diorama internal text camera follower

Pinned Folia v0.7.26 / cc6226e464050a456a2913e062374f23d3844cc5, AGPL-3.0-only.
Desired targets now drive position/look SmoothDamp, reading-alignment yaw/roll,
safe-cone target and single quaternion orientation follower. DioramaPerspective
accepts the actual quaternion; its world-up lookAt default remains unchanged.
DioramaTextPlayback connects published core/source refs, complete-shaped text,
offset and real three-layer perspective drawing. This is INTERNAL, not a public
theme. Explicit shots, no weighted picker/flight/neighbours/near/depth/fog yet.

## Independent original finite operator

diorama-follow-source.mjs extracts the original CameraRig constants and normal
position/look/alignment/frame-keep/orientation blocks verbatim. Wrapper inputs
provide refs/line/frame/placement/target/delta; flight=false,arc/sweep/bank=0.
Source ranges/hashes, revision, exact Motion lock and Three0.185.1 are recorded
in ../diorama-fixtures/follow-v1/conditions.json. The source checkout is untouched.
This is not a mounted React CameraRig or an original background-engine capture.

1482 original steps:13 shots,380/800×600,60/144/irregular schedules, initial refs,
changed plane basis, true far snaps. finite.tsv retains ORIGINAL incoming23,
target6, resulting23 and branch diagnostics; no Kotlin-derived expected values.
State peak2.220446049250313e-16,150 snaps,62 safe-cone branches. State/target
quaternion channels pass1e-9. acos is ill-conditioned near zero: angle diagnostic
peak2.1073424255e-8rad at its stated1e-7 gate; resulting orientations still1e-9.

Initial test failed source row2 field17 by3.09e-9. Cause: older Three slerp assumed
instead of actual0.185.1 normalized-lerp at dot>=.9995. Implementation corrected
to pinned source; expected values/tolerances unchanged. First/second logs remain
.work/diorama-follow-first.log and second.log. Three scalar algebra has explicit
MIT authors/license attribution; no Three binary/runtime dependency added.

## Text and the distinct media adaptation

96 original WebGL frames: Latin/Arabic/CJK fixed font hashes,380/800×600,DPR1,
orbit,8 times,BOTH60/144 schedules with exact probe inserts. Original active-unit
raster/material stage now uses the actual extracted original normal follower.
Analytic unit charge injection remains explicit, not original RAF envelope/full
React scene equivalence. scene.json records complete original poses/conditions.
Own finite drawing calculates OWN targets from OWN complete-shaped widths/core
lines, then its OWN follower; no original pose or PNG drives Kotlin. All captured
pose/look/quaternion channels pass1e-9.

Independent media track uses immutable adaptive step-doubling checkpoints at
lyric/grapheme/read-need/settle boundaries, approximating the source operator's
continuous limit. No UI-frame accumulation, fitted60Hz playback or fixed delay.
Queries start from prepared nodes; no ticker/job/global cache or per-frame text
preparation. Preparation is cancellable, bounded song tail is held. Reduced
camera is fixed within one subject and moving soul is disabled.

4s two-line convergence control:5307 nodes vs10131 at quarter local tolerance;
position peak.000171996world units,quaternion channel peak.0000967565. This is
integration convergence, NOT original visual acceptance. Media vs source finite
sampling still reaches.0392603world units and1-|quaternion dot|.000127118.
Keep residuals; do not claim raw RAF equivalence or fix with a frame delay.

Same source/own-finite/own-media union includes ALL nonbackground pixels/lowest
alpha and worst64px tiles. Mean foreground MAE finite4.675817,media13.058764;
worst image17.441081 vs57.104488. Distinct methods on ONE matrix, not claimed
before/after improvement. Bounds peak28px atArabic800/60/3.1s; some original and
own narrow/follow views clip text. Original/own raster, overhang and media-camera
differences remain. Contact visually inspected. Twelve exact.7s first-start
frames blank in all three paths are explicitly listed, not dropped. No final
readability/theme acceptance, no Diorama sample entry.

Initial scene failed because real KaraokeLine concatenated tokens without
spaces. Fixed only input fixture to retain actual spaces at original zero-time
gap boundaries, with original advance asserted; source expected data unchanged.
Failure stays .work/diorama-follow-stage-first.log, corrected second.log passes.
Initial comparator did not handle all-three-blank frames; now records emptiness
and exact0 error without dropping cases or choosing new source captures.

## Verification/reproduction

13 targeted JVM tests pass: original finite, previous projection/plane/target,
media24/60/144 schedule independence,reverse/random seek,pause,cancellation,
actual core/source/offset,logical resize/settings/unmount/remount pixel restore.
No reprepare on media or settings. Android library compile and Desktop sample
compile pass (.work/diorama-follow-validation.log). No new Android APK/device,
real window/audio, continuous recording, heap or performance claim.

```
node verification/classic-browser/generate-diorama-follow.mjs
node verification/classic-browser/capture-diorama-follow.mjs
./gradlew :library:jvmTest --tests '*DioramaCameraFollowTest' --tests '*DioramaCameraTrackTest' --tests '*DioramaFollowStageTest' --tests '*DioramaTextPlaybackTest'
python verification/compare-diorama-follow.py
./gradlew :library:compileAndroidMain :sample:desktop-app:compileKotlin
```
Exporters refuse overwrite: preserve original fixture before intentional audit.
Dedicated4186 browser/server close in finally. Wait for completed native capture
BEFORE comparison. Raw PNGs ignored under build/verification; only compact
report/contact/numeric fixtures tracked. User signing/daemon files excluded.
