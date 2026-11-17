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
