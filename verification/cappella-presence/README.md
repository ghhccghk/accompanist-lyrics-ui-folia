# Cappella interrupted presence and overlap

Uses the same pinned original JSX, covered Latin font, 380×480 viewport and
controlled browser clocks as ../cappella-playback/README.md. Two synthetic
timelines (rapid 30 ms lines, overlapping short/long lines), all three intensities,
exact start/render-end/post-end probes: **240 paired images / 999 original rows**.
Inputs are recorded separately from original output geometry.

Reproduce: run the browser fixture server, then
`node verification/classic-browser/capture-cappella-presence.mjs`,
`gradlew :library:jvmTest --tests "*CappellaPresence*"`, and
`python verification/compare-cappella-playback.py --presence`.

The first native capture failed all six configurations: a message returning
before its exit unmounted created duplicate title/lyric bubbles. The failing
log is .work/cappella-presence-before.log; all baseline images/geometry are
preserved in build/verification/cappella-presence-kotlin-before-reentry.
No assertion was removed or relaxed.

Implemented keyed episode reuse, continuous cancellation of entry/exit transforms,
shared completion of the pending exit batch, frozen popLayout outer boxes,
continued internal reveal/resize subscriptions for exiting active messages,
and reentry position compensation. The original PopChild reads offsetTop and
computed row height; resizing its inner frame must not move its frozen bottom.
Source references are in docs/upstream.md. No React/Motion runtime is bundled.

All 240 source presence/order observations now match exactly, including zero-alpha
DOM rows retained by the exit batch. Every distinct media probe is revisited in
shuffled order; fixed-media pause and unmount/remount are pixel identical.
Pure response tests cover continuity through multiple exit cancellations.

**This is not full rapid-transition visual acceptance.** The current foreground
RGB MAE is **11.890487659876555**, geometry peak **40.10946682468773 px** and
group×row opacity peak **0.05038571441375628**. Interrupted layout projection,
React effect/RAF sequencing and CSS flow remain materially different, especially
rapid changes while a prior frame size is still animating. No new tolerances
were introduced to declare those differences acceptable. Contact.png, residual.tsv,
comparison.json and raw source/native PNGs preserve the discrepancy.

The separate regular matrix remains present/order exact, geometry peak3.637201px;
mean MAE3.393810 after respecting integer popLayout offsetTop. These two matrices
must not be averaged together or compared as if they were the same scenario.
No Android device is connected; only Android build and Desktop offscreen execution
are verified. No public Cappella entry has been added yet.

Semantic-layout follow-up: prepare layout commits on actual parent selection or
visible-line changes and local reveal/target/timestamp updates. Like original
Motion, compare rounded top/bottom boxes, including animated height for
`layout="position"`; a changed measurement can interrupt a running compensation.
Pressure MAE decreased from 13.291425656391402 to 11.890487659876555. The peak
geometry and opacity errors did not improve. All regular-matrix PNG hashes are
unchanged. The contact sheet was inspected; rapid stacking remains visibly
different. All Cappella JVM tests, Android debug assembly and Desktop compilation
passed (.work/cappella-semantic-layout-final.log); no device runtime claim.

## Continuous-flow snapshot correction (2026-11-18)

Passive `didUpdate` observation of original Motion projection nodes is reproducible
with `node verification/classic-browser/probe-cappella-layout-commits.mjs`. It
uses the exact pressure probes/60Hz observation sequence and original JSX; no
layout function or delta is patched. JSON stays in build/verification. Original
source: CappellaMessageRow.tsx outer layout=position, local visible/target count
subscriptions; Motion projection node notifyLayoutUpdate/didUpdate snapshot.

For overlap/chaotic row7, source layout top at2.016667 is167.203125 while
snapshot top170.468760 gives3.265635px compensation; at2.09 it is136.03125
against170.468740, giving34.437490px. Previously native restarted only its
existing offset and discarded displacement while preceding heights changed.
Now retain that measured-flow displacement during ongoing size/margin response.
Settled flow uses its current box: using a stale commit there produced a false
26.466px displacement at3.0364; rejected trial log .work/cappella-flow-snapshot.log
and preserved initial captures build/verification/cappella-presence-before-flow-snapshot.

Same240 pairs/999 rows: MAE11.890488 ->9.173245, geometry peak40.109467
->22.188593px; opacity peak remains.050386, presence/order exact. Prior worst
row7 avatar at2.1 improves40.109467 ->14.422399px. All210 regular-matrix source
and native PNG hashes remain identical. Random seek/pause/remount checks pass.
Contact sheet inspected: rapid stacking still visibly differs; semantic commit
reconstruction does not reproduce all original React/RAF batching. This is a
partial correction, not final visual acceptance or a new acceptance threshold.
