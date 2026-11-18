# Internal incoming-text flight — 2026-11-18

Historical scope at486b8c1. Same-corridor return/window was added subsequently;
see `../diorama-loop`. Original flight fixtures/results below remain unchanged.

Pinned Folia v0.7.26 / cc6226e464050a456a2913e062374f23d3844cc5,
AGPL-3.0-only. This closes ONE internal incoming-text flight path, not the
complete dual-corridor transition, a public Diorama theme or final acceptance.

## Source and production behavior

`dioramaTransition.ts:16–89`, `CameraRig.tsx:57–139,289–440`, and
`VisualizerDiorama.tsx:365–390` are the references. Source block ranges/hashes
are in `../diorama-fixtures/flight-v1/conditions.json`.

- New `DioramaCameraFlight.kt`: seeded 46-unit incoming origin, captured
  horizontal perpendicular/length/side, 3.2s smootherstep smooth-time,
  sin² arc, upward lift, aim sweep and bank.
- `DioramaCameraFollow.kt`: suppress seek snap while flying, follow the LIVE
  target, apply aim sweep before look damping and arc before frame-keep.
  Visual arc/bank never feed back into position/orientation refs.
- `DioramaCameraTrack.kt`: independent adaptive media reconstruction, event at
  exactly 3.2s; perspective adds only the query-time visual offsets. No fixed
  frame delay, hardcoded display rate, per-frame preparation or UI accumulation.
- `DioramaTextPlayback.kt`: value-only outgoing ref/frame anchor, real core
  input and original identities, incoming corridor translation, actual complete
  shaped text/life draw. No outgoing playback/textures/jobs retained by anchor.

Original flight progression uses `state.clock.elapsedTime`. This internal path
uses incoming lyric media elapsed time (media minus offset), starting at zero,
so pause/seek/remount reconstruct the same flight. Finite original operators
and this clock adaptation are verified separately. Arbitrary late-start cuts,
interrupted-flight orchestration, same-corridor loop restart, outgoing corridor
window/pruning and late-lyrics scene gate are NOT implemented by this change.
Reduced motion uses the existing fixed-per-subject camera, no visual flight or
moving soul; media highlighting still follows the supplied time.

## Independent original numbers

`diorama-flight-source.mjs` exports original CameraRig blocks verbatim; only the
refs/target/frame/clock inputs are wrapped. Actual original transition helpers,
Three0.185.1 and pinned Motion13.4.3/dom13.4.2/utils13.3.0 are resolved. No source
checkout modification or implementation-derived expected pose.

480 finite equal-incoming-state controls: two seeds (including surrogate pair),
two widths, three epochs, ten times and four deltas. Incoming23 refs come from
the ORIGINAL outgoing normal follower; seeded origin and initial flight capture
are ORIGINAL results. State/rendered/target peak `1.4210854715202004e-14`, angle
diagnostic peak `3.0253577421035516e-15`; unchanged gates 1e-9/1e-7. 336 flying
and 144 normal far-snap controls. Endpoint/reverse-query/fallback checks included.

## Real core drawing and chronological observation

96 original frames: Latin/Arabic/CJK, existing OFL font hashes,380/800×600,DPR1,
orbit, eight times including3.199/3.2/3.201, BOTH60/144 finite schedules. Actual
original text raster/layout/material functions and distance life, plus extracted
original camera blocks. Shared explicit default incoming refs and seed; one
incoming line, no React mount/background/depth/fog/outgoing corridor. Analytic
light/soul charges are injected with delta0, as in the prior layer probe;
this is not original RAF envelope equivalence.

Kotlin prepares published `KaraokeLine` objects with actual spaces/timings and
its OWN full-shaped advances/targets/flight/finite steps. No original output
pose, texture or image drives Kotlin. Pose peak380px `3.55e-15`,800px `2.13e-14`
at1e-9 gate. Adaptive-media position residual up to `.03503820105378219` world
units, quaternion1-|dot| `.00006026669555891928`; not waived as visual equality.

Same three-way ALL-nonbackground union, including lowest alpha, no alignment,
time shift, discarded frame or foreground threshold:

| 96-frame method | Mean foreground MAE | Worst image | Worst64px foreground |
| --- | ---: | ---: | ---: |
| Own finite source schedule | 3.955025 | 14.142898 | 41.222222 |
| Own adaptive media | 12.366458 | 50.078315 | 116.332652 |

These are distinct methods on one matrix, not a before/after improvement.
Source/finite/media blank counts21/21/26, edge-touch47/47/42; all filenames,
bounds and hashes retained in `comparison.json`. Source and own clipping,
overhang/raster and media/RAF residuals remain. Contact visually inspected;
no full-theme/readability acceptance.

`capture-diorama-flight-sequence.mjs` additionally observes Latin800/60 in time
order at15Hz plus original probes,67 frames. All EIGHT old source keyframe
steps and PNG hashes are identical: observation does not alter the source
camera. Native same-schedule poses pass1e-9; independent media remains schedule
independent. Foreground mean finite4.043517/media19.084417; local20.333333/
101.207197. Ten blank frames in each path,29/29/28 edge-touch frames, not dropped.
`sequence-comparison.json` and fixed contact retained. Reproducible side-by-side
GIF is generated at `build/verification/diorama-flight-kotlin/flight-sequence.gif`
(raw images/movie ignored); no claim of a manual real-window recording.

## Lifecycle, failures and verification level

Actual two-line core cut checks outgoing media-offset anchor, same incoming refs
without far snap, live target switch at1.9s, original source/click inverse,
24/60/144 display-query independence, arbitrary reverse seek, exact duration
neighbors, pause pixel freeze, reduced motion, width restore, unmount/remount
pixel reconstruction and no time/settings-triggered reprepare.

First endpoint check failed boxed +0 versus source -0; now checks EXACT numeric
zero without a tolerance. First core-anchor check wrongly required bit-identical
dt0 position although original goal+(current-goal) has1.8e-15 cancellation
roundoff; numerical1e-12 coordinates retain the source formula. Source fixtures
and original gates unchanged. Logs `.work/diorama-flight-first.log` and
`diorama-flight-validation-first.log` retained; corrected second/final logs pass.

Twelve targeted JVM tests pass, including existing original1482 normal finite
controls and previous core/follower stage. Android library and Desktop sample
compile pass. Old `diorama-follow/comparison.json` is fromde7baa6, predating later
shaped masks/life changes: comparing rerendered images to it matched only120/288
hashes (`.work/diorama-flight-normal-oldreport.json`). It is NOT a current
pre-flight image baseline and no unchanged-image claim is made. Original
finite normal numerical regression passes; old report/images expected values
are untouched. No device, native window, audio or performance result added.

```
node verification/classic-browser/generate-diorama-flight.mjs
node verification/classic-browser/capture-diorama-flight.mjs
node verification/classic-browser/capture-diorama-flight-sequence.mjs
./gradlew :library:jvmTest --tests '*DioramaCameraFlightTest' --tests '*DioramaFlightStageTest' --tests '*DioramaCameraFollowTest' --tests '*DioramaCameraTrackTest' --tests '*DioramaTextPlaybackTest' --tests '*DioramaFollowStageTest' :library:compileAndroidMain :sample:desktop-app:compileKotlin
python verification/compare-diorama-flight.py
python verification/compare-diorama-flight.py --sequence
```

Exporters refuse to overwrite frozen source data: preserve it before intentional
audits. One owned4186 browser/server closes in finally; user browser untouched.
No native, player, public API or sample selector change. Source mapping in
`docs/upstream.md`. Diorama remains internal pending the rest of text-camera
composition and readability; seven existing public previews remain available.
