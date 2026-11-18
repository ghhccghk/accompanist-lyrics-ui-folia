# Pendolo actual follower rest semantics

Pinned Folia `cc6226e464050a456a2913e062374f23d3844cc5`, AGPL-3.0-only.
This is a production tail/large-jump correction with independent source evidence,
not completion of Pendolo's visual acceptance.

## Two distinct source expectations

The existing tick-response/tick-track fixtures invoke `animateMotionValue` /
JSAnimation and their scale-dependent rest defaults. They remain byte-for-byte
unchanged, as do their 1e-9/1e-8 assertions. A test-only LegacyDispatcherTrack
retains the old trajectory to keep those historical expectations meaningful.
They do not define the actual mounted original `useSpring` follower's limits.
The corrected production track differs from237 legacy expectation fields beyond
the old1e-8 gate (mixed value/velocity/angle fields, peak3.607703). Every failed
legacy-vs-production field is retained in comparison.json; this peak is NOT an
original-follower error or a pixel displacement. The old fixture was not updated
to conceal these differences; the new actual-follower evidence justifies the
versioned migration.

Original `VisualizerPendolo.tsx:334–350` calls useSpring, which in the EXACT
resolved Motion13.4.3/dom13.4.2/utils13.3.0 goes through `useFollowValue` /
`attachFollow`. That path explicitly supplies **restDelta0.001/restSpeed0.01**,
even for a jump greater than5 indices. Previous Kotlin used displacement0.005
(or0.5 for a large jump) and velocity0.01 (or2). This prematurely stopped the
wheel. The source FollowAnimation also tests the generator on actual RAF samples,
not the dispatcher's 50ms duration-resolution probes.

`generate-pendolo-follower.mjs` calls actual original attachFollow with the
verbatim extracted original physical options and unmodified original profiles.
Its custom driver only supplies observation time. Actual option values and
original generator results, source/host/lock hashes and inputs are retained in
`../pendolo-fixtures/follower-v1`. No source checkout or expected value was edited.
54 original incoming-state controls /972 raw queries /3501 semantic track queries:
raw peak3.552714e-14, first-rest time peak1.310063e-14s, track peak4.796163e-14.
Original first-rest isolation independently scans100/200 MICROseconds and bisects
48 times; both scans agree within3.11e-15s in this matrix. These are numerical
root controls, not a playback scheduler or a source RAF equality claim.

22 exact bisection-boundary raw queries lie on opposite adjacent floating-point
representations of the same inequality crossing (source snaps value, Kotlin may
still report raw). They are counted, not silently included as exact boolean/value
matches. Each must be within1e-12s of the independently recorded first root; both
stable sides±1us assert the original predicate, all velocities retain1e-9, and
the independently measured first times retain1e-9. Raw error above excludes these
22 snapped/raw value ambiguities. Existing original mounted/dispatcher assertions
and all old expected values are untouched.

## Media-time adaptation and actual schedule residual

Kotlin now uses the true follower limits and latches the **first continuous
physical entry** into both limits. An independent oscillator root solver splits
at displacement/velocity/acceleration zeros, so each absolute constraint is
monotone, then bisects the entry; this also covers critical/overdamping. Solved
once during semantic track preparation, never during stable display frames.
No UI-frame accumulation, fixed60Hz playback, millisecond rounding or phase delay.
Cappella and ordinary dispatcher paths keep their original algorithms/defaults.

This latching rule is explicitly a media-domain adaptation: original RAF can
miss a short rest interval or observe it later. It cannot be exactly equal to all
original frame schedules. Direct original driver controls at30/60/144Hz prove the
remaining mounted worst case is principally a sampling boundary, not evidence to
add a fixed delay. At6.8s the original line is still inclusively active. Its gap
target first arrives AFTER6.8s; source FollowAnimation also rounds elapsed to ms.
At7s the source samples167/183/194ms rather than semantic200ms. For chaotic:

| Original schedule | Value at7s | Wheel degrees |
| --- | --- | --- |
| 30Hz | 1.596001534879 | -1.200019185985 |
| 60Hz | 1.574939789567 | -0.936747369591 |
| 144Hz | 1.558217720957 | -0.727721511961 |

The60Hz value/rotation at7s/7.3s exactly reproduce the unchanged original mounted
fixture within1e-12. Driver velocity is the physical derivative, NOT the mounted
MotionValue's finite-frame getVelocity; those must not be conflated.

Same original/old native/new native144-frame matrix: every native PNG hash
unchanged. Rotation peak0.325106073°/content rect1.364706009px remain. New explicit
all-nonbackground union mean11.864272/worst20.898476/worst64tile89.370090 unchanged;
do NOT compare this statistic with the old >8 foreground metric as an improvement.
These probes missed the newly corrected tail windows; unchanged pictures alone
would not establish that the correction is user-visible.

## New complete original wheel vs public real-core stage

`capture-pendolo-follower-stage.mjs` mounts the unchanged extracted original wheel
JSX/useSpring in a dedicated4190 browser. The input host adds only an optional
`timing=tied` scenario (eight lines start together, initial jump-1→7); its default
regular input is unchanged. Source logic, spring/clock/geometry are not patched.
27 actual original frames: regular chaotic plus tied normal/chaotic, same Latin
font700,380×600/DPR1,colour/line/word times, source60Hz with exact integer-ms probes.
Complete conditions/states/hashes live in `../pendolo-fixtures/follower-stage-v1`.

After images come from PUBLIC FoliaLyricsView + published actual core KaraokeLine/
KaraokeSyllable. They must equal the independently mounted bridged wheel exactly;
original object refs survive. A separately labelled legacy-rotation CONTROL uses
the previous dispatcher response with the SAME core text/layout (not a historical
public binary or a new source oracle). Every numeric source follower value and
rotation in the27 frames matches within1e-9; observed rotation peak1.11e-14° vs
legacy control0.832573082°. Same source/control/public full foreground union:

| Field | Legacy rotation control | Public follower |
| --- | --- | --- |
| Mean foreground MAE | 14.925368 | 13.753192 |
| Worst frame MAE | 41.930028 | 16.299364 |
| Worst64px foreground tile | 82.583333 | 82.583333 |

Every frame/hash/metric is retained in comparison.json; no alignment, low-alpha
cutoff, threshold update or frame selection. Contact inspected: large jump no
longer snaps prematurely; native raster/font/layer residuals remain visible.
Worst local tile is NOT improved. No full-theme acceptance/performance claim.

## Checks and reproduction

```
node verification/classic-browser/generate-pendolo-follower.mjs
node verification/classic-browser/capture-pendolo-follower-stage.mjs
./gradlew :library:jvmTest --tests '*PendoloFollower*' --tests '*PendoloTickTest' --tests '*PendoloPlaybackWheelTest' --tests '*PublicPendoloTest' --tests '*CappellaResponseTest' :library:compileAndroidMain :sample:desktop-app:compileKotlin
python verification/compare-pendolo-follower.py
```

Windows: gradlew.bat. Source generators refuse overwrite. Preserve source fixtures
before a deliberate re-audit. Old144-frame comparator requires the saved native
outputs fromc4ef479 in `.work/pendolo-follower-before/native` and existing original
captures; it fails if missing. Large raw captures are ignored; compact contacts,
all hashes, numeric source oracles and regressions are retained. Dedicated source
browser/server close in finally. No user browser/player/device was operated.

JVM original response and legacy regression, new continuous follower, actual
public27-frame capture, old144-frame capture, pause/random seek/remount/reduced
motion, PublicPendolo offset/resize/theme/click/disposal and Cappella response
cross-checks pass. Android library/Desktop sample compilation pass. Logs:
`.work/pendolo-follower-{first,matrix,stage-source,stage-native,comparison}.log`.
Final combined validation: `.work/pendolo-follower-final.log`.
No new Android runtime/real Desktop window/audio/process/performance validation.
