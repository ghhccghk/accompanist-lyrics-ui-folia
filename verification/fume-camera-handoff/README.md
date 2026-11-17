# Fume handoff investigation: rejected single-factor candidate

Pinned cc6226e464050a456a2913e062374f23d3844cc5. Production FumeCameraTrack is
UNCHANGED. Candidate was tested, failed and restored byte-for-byte. No thresholds,
fixtures or assertions weakened; existing camera-continuous table preserved.
FumeMountedPublicTest now permanently exports all9 pose fields and retarget metadata
so a future fix can reproduce the full handoff without an ignored test-file copy.

Source findings (all under src/components/visualizer/fume):
- fumeCameraStep.ts:157 starts retarget from previous camera;278 branches on CURRENT
  phase;312–324 bridge updates focus to B(now), independent of pursuit position.
- Current FumeCameraTrack anticipated():88–92 uses t+h/2 and t+h to choose the
  whole operator; a3s/.13s bridge can exit near3.113333 while its true end is3.13.
  Original controlled RAF last bridge tick3.116667,first spring3.133333.
- Current rk():146 stores anticipated future focus;380/smooth3.15 original
  focusScale1.139023293324062 equals original B(3.15). B(3.1583333333) is
  1.159019809295168,close to native1.158383379492508. A real semantics difference.
- At3s800/smooth the retarget kind/start/duration match, but fromX differs
  1622.960641 vs1625.128787; velocity124.084633 vs111.442458. Initial state is
  not identical, so the whole3.15 residual cannot be attributed to bridge exit.
- Original overview semantic threshold9.690000000000001 is first observed at9.7
  by this60Hz capture. Native uses semantic time; no fixed delay is proposed.

Single-factor probe: leave derivative/anticipation/forcing unchanged; after each
accepted RK result set bridge fx/fy/fs from intent(result,time+h).bridgeTarget
instead of anticipated(result,time+h).bridgeTarget. Exact candidate and logs remain
ignored .work/FumeCameraTrack-current-focus-probe.kt and fume-current-focus-probe.log;
pre/probe screenshots remain .work/fume-focus-before-public and
.work/fume-current-focus-probe-frames. Source92 frames/hashes/inputs unchanged.

Using the SAME original/before/probe all-nonbackground union including low alpha:
MAE8.175929497→8.664703766; camera screen peak15.933272752→15.981225955px;
scale peak.056221253508→.056220979468. All92 hashes/metrics in
rejected-current-focus-probe.json. These are NOT comparable to the earlier parent
entry union means without these exact masks. The candidate worsens aggregate
image/position evidence; NOT merged, NOT a fix or visual acceptance.
Old adapted-trajectory check failed at scenario0/t=.0333333333333/field4:
expected1072.8023287711198,actual1107.6956868260638,tolerance1e-5.
That table is an adapted-model regression, not raw upstream equivalence; changing
it alone cannot prove an improvement. It remains unchanged and must pass again
after restore. Mounted seek/pause/remount checks passed during the rejected probe.

Next: derive the full9-state handoff with branch/forcing/algebraic focus separated,
then compare identical incoming source state BEFORE a whole mounted matrix.
A current-focus-only edit is not sufficient; do not retry it or add a fixed delay.
No Android/device/performance claim from this investigation.

## Second isolated candidate: midpoint branch only (rejected)

Removed only anticipated()'s end-intent override; midpoint forcing and the old
focus write remained. Thus the switch occurs near end-h/2 rather than end-h,
without an arbitrary fitted offset. This is NOT the full current-phase repair.
Same92 original frames and same union mask within this before/probe experiment:
MAE8.020819356→8.806774170,screen peak15.933272752→18.297365327px;
scale peak.056221253508→.056220994902.800/smooth3.15 screen15.344397498→18.297365327,
scale.010091546848→.012086519400. Not merged. This union differs from the first
probe's union; compare each candidate with its OWN before, not means across probes.
All9 source/before/probe states/differences retained in
rejected-midpoint-branch-probe.json. Candidate/frames/log remain ignored under
.work/FumeCameraTrack-middle-branch-probe.kt,fume-middle-branch-probe-frames and
fume-middle-branch-probe.log. Existing adapted-source gate failed at
scenario0,t=.6333333333333,field0,expected62.4531242989148 vs62.37370895330261,
tolerance1e-5; no expected data changed. Product source restored byte-for-byte.
Both experiments show local changes do not jointly repair pursuit/focus/velocity
handoff. Do not repeat either edit as a proposed complete fix.
Next concrete experiment: seed BOTH sides with the identical original incoming
9-state snapshot and retarget; record last-bridge/first-spring steps around3.13
and the5.4s short-bridge control. Separate forcing,stored focus and velocity clamp
before changing whole-song reconstruction. Existing original fixtures remain intact.

Restored-production verification: original FumeCameraTrack and mounted public
checks plus all modified Diorama camera/active-stage tests passed unchanged
(.work/theme-final-restored-check.log,41s). No rejected candidate remains in
production. Android/Desktop build evidence remains the unchanged c5ceac9 code;
no new runtime/device claim from these rejected experiments.

## Same-original-incoming experiment v1 (2026-11-18)

This supersedes the **next experiment** above, not its failures or original data.
Production camera is still unchanged. New fixtures live in
`../fume-fixtures/handoff-v1`; `same-incoming-v1.json` records residuals/hash inputs.

Eight cases: widths380/800, smooth/stepped, original mounted states AFTER the3s
and5.4s frames. All nine pose fields and the entire retarget are copied from the
PRESERVED ORIGINAL mounted-observations, never Kotlin output. A fresh original
mounted component supplies its actual full article/pretext geometry; every block
position/size must exactly match the old mounted source. The target is still
computed by the original function, not a target inverted from Kotlin.

An isolated SSR module injects passive reads immediately before original branch,
bridge write, XY clamp and scale clamp. Every injection anchor, pinned source hash,
font/lock/input hashes, actual Chromium151.0.7922.34 and resolved Motion
13.4.3/13.4.2/13.3.0 are recorded. The source checkout is unchanged. 520 original
steps cover mounted60,144 and irregular schedules over250ms. Only mounted60
matches the original mounted schedule; the others test the ORIGINAL operator's
own rate dependence. Source dt includes useFumeCanvasStage's1/240..0.05 clamp.
No added probe tick disturbs those trajectories.

FumeCameraHandoffTest reconstructs both sides from IDENTICAL incoming state,
retarget, article, font-derived rows, source time and float origin. The port's
finite one-step operator peak is2.2737367544323206e-13; target peak is
1.1368683772161603e-13 (unchanged1e-9 gate). This isolates the problem to the
continuous adaptation, rather than the source target/step port or earlier song.
Reflection probes private integration/anticipation; no test seam or playback API
was added. Native diagnostic all-nine fields/derivatives/forcing/cap are written
to build/verification/fume-handoff/continuous.tsv. Original spring records retain
unclamped/clamped values and focus order.

Concrete800/smooth3s result (all times are ACTUAL source evaluation times):
- At3.1, position differs1.476566world px; focus differs-25.603195world px.
- At3.1166666666666667, source phase0.8974358974 is BRIDGE; current reconstruction
  anticipated branch is SPRING. Same-incoming dx14.207132, scale-0.012085444.
- The next source interval is[3.1166666666666667,3.1333333333333333],dt1/60.
  Source current phase1 selects spring, filters focus FIRST, computes velocityX
  -1515.92647065, clamps it to-1320, THEN moves x by-22 in the same step.
  Continuous saturation instead evolves velocity before reaching the bound;
  its log-of-unclamped-operator flow plus a velocity clamp is not the source's
  saturated position map. At3.133333 dx11.475936 persists while both vx=-1320.
- At3.15, same-seed dx remains11.475936; this excludes ALL pre3s accumulated error.
  Scale is lower0.010023 and scale-velocity higher0.060486.
- A380/smooth3.15 bridge has dx0.314537 but focus dx-20.039323. Algebraic focus
  future-write is independently real even when the branch has not ended.
- 5.4s is NOT a uniformly negligible control: all widths/modes have an early
  anticipated spring at5.5166666667 while source remains bridge. Same-incoming
  screen peak8.164317..8.313238px (whole-song errors previously happened to cancel).

This falsifies attributing the handoff solely to earlier camera drift, solely to
future focus, or solely to midpoint branch choice. A justified next camera
candidate must treat branch boundary, algebraic focus and NON-INVERTIBLE saturated
finite-step position update together. Next falsifiable sub-experiment: freeze the
original first-spring target/coefficients and vary incoming velocity across the
source clamp threshold; compare its complete (x,v,focus) map with the current
unclamped-log-plus-clamp operator BEFORE deriving a piecewise media operator.
Do not add a fixed frame delay or integrate the player at hardcoded60Hz.

No new production candidate is merged and no camera-continuous expected value or
original threshold changed. Old fixture remains the old adapted-model regression;
legitimate future migration needs independent original evidence and versioned
regression, not a claim that its1e-5 gate proves raw equivalence. The mounted92
matrix was not re-run because there is no accepted production repair to evaluate.
Fume remains an explicitly imperfect public calibration preview. Continue the
independent Cappella dense exact-lock/readability task rather than guessing again.

Reproduce:
```
node verification/classic-browser/capture-fume-handoff.mjs
./gradlew :library:jvmTest --tests '*FumeCameraHandoffTest' --tests '*FumeCameraTrackTest'
python verification/summarize-fume-handoff.py
```
The source exporter refuses accidental overwrite; an intentional audit must first
preserve the previous directory and explicitly set FOLIA_RECAPTURE_HANDOFF.
Initial Kotlin fixture-column reader error is retained in.work/fume-handoff-baseline.log;
corrected reader passes in.work/fume-handoff-second.log. No tolerance was changed.
No new visual/device/performance acceptance follows from these numeric diagnostics.
