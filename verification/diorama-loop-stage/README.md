# Original full text-only loop stage — 2026-11-18

This extends the32 fixed-width camera controls in `../diorama-loop` to ACTUAL
font geometry, published-core content and complete neighbour+active text. It
does not mount React/R3F CameraRig/DioramaScene or accept the full theme.

Folia v0.7.26/cc6226e464050a456a2913e062374f23d3844cc5,AGPL-3.0-only;
Motion13.4.3/dom13.4.2/utils13.3.0 checked at capture,Three0.185.1,Chromium151.
Original source extraction/range hashes, text/layout/material/sequencer hashes,
host/lock/font hashes and ORIGINAL incoming/output refs recorded in
`../diorama-fixtures/loop-stage-v1/scene.json`. Source checkout stays unchanged.

## Fixed inputs and clock

Three synthetic `SyncedLine`s, integer starts700/2100/3500ms and ends1500/2900/
4300ms, text `Synthetic loop line 0/1/2`, no genuine word annotation. Original
`words:[]` and core fallback derive internal timing; not described as real word
times. Same existing OFL Latin Bold font,128px source em,700 weight,DPR1,
380/800×600,seed Incoming mirror,orbit override, identical colours/config.

Source actual sequencer creates a corridor at original offset(epoch2), then
the verbatim original flight/follower runs outgoing0→4.2s. Its actual base refs
seed loop epoch3 in THE SAME corridor. Both60/144 schedules plus exact probes,
ten captures each:40 source frames. Source clock resets to local loop elapsed;
window closes at controlled media3.2s. These are explicit injection boundaries,
not the original global wall-clock timeout/React scheduler. Camera/ref formulas
are untouched. All integration deltas and sample times are reproducible.

Complete original text-only draw uses original window/visible-line/plain raster/
active layout/neighbour material functions and Three mesh three-layer composition.
Light/soul arrays receive the existing analytic media charge with delta0,
breath1/audio0. No fog, background, keyword/audio response or theme damping.
This is a full TEXT LAYER probe, not raw mounted-component/RAF-envelope evidence.

Native prepares real external core objects, its OWN full-shaped geometry/targets,
own outgoing finite flight and own loop finite flight. Source rendered poses,
rasters and images never drive the native renderer. All source-conditioned own
advances pass.40 captured eye/quaternion gates1e-9 pass, peak `1.4210854715202004e-14`.
Current source identities, window/outgoing membership, media scheduling
independence, reverse seek, pause and remount pixel reconstruction pass.

## Comparison and diagnosis

One identical source/own-finite/own-media ALL-nonbackground union per frame,
including lowest alpha; no registration, delay, alpha filtering or removed frame.

| Method | Mean foreground MAE | Worst image | Worst64px foreground |
| --- | ---: | ---: | ---: |
| Own complete text + source finite schedule | 4.765380 | 14.720234 | 36.963333 |
| Own complete text + adaptive media track | 14.574012 | 54.623715 | 80.101010 |

No blank frames; source/finite/media edge-touch24/23/22. All bounds/hashes and
every case in `comparison.json`. Native adaptive position residual up to
`.06682156623862667` world units,quaternion1-|dot|`.000011915725604771943`;
this includes the outgoing-history reconstruction, not a local-only handoff.
These are two methods on ONE matrix, not a before/after improvement or a pass.

The original ALSO exhibits strong enlargement/cropping/overlap for these inputs.
For example800/60/.7s source bounds[106,271,800,532],finite[107,272,800,532],
media[107,273,800,533]; at1.6s both source and native bounds touch left/right/bottom.
The fixed contact was visually inspected. This supplies original evidence that
these specific large/cropped views are not solely an invented native loop path.
It does NOT waive readability or prove all crop differences intentional: edge
sets differ, source/native raster differs, and media residual remains. No fitted
camera/extra frame/dynamic font shrink was added to hide it. Mixed-bidi/Android,
original mounted scene, full continuous recording and final readability pending.

Two targeted JVM capture tests pass (`.work/diorama-loop-stage-native-first.log`).
Production is unchanged from4a69b2a: its15 targeted JVM tests and Android library/
Desktop compile remain recorded separately, not recast as device evidence.
`python scripts/verify-consumer.py` additionally passes for4a69b2a: independent
Maven JVM draw/source callback, Android compilation, dependency/notice/archive
boundary checks. Local build/local-maven only; artifact hashes in
`../consumer/4a69b2a-validation.json`. No public/remote release, device/window/
audio or performance claim added. Source wrappers close owned browser/server.

```
node verification/classic-browser/capture-diorama-loop-stage.mjs
./gradlew :library:jvmTest --tests '*DioramaLoopStageTest'
python verification/compare-diorama-flight.py --loop
```

Exporter refuses frozen source overwrite; preserve before intentional audit.
Raw PNGs remain ignored under build/verification, only compact evidence/contact
tracked. Seven public preview themes unchanged; Diorama remains internal.
