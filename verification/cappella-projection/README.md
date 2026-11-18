# Dense projection snapshot probe — rejected

Latest bounded evidence: **Frame phases v2** below. Earlier “next probe”
instructions are historical; the rejected production candidate stays rejected.

Pinned original cc6226e4, exact Motion13.4.3/dom13.4.2/utils13.3.0. This is a
FAILED candidate, not a behavior repair. Production CappellaPlaybackPreparation
was restored byte-for-byte; hashes in comparison.json. Original240 images/999
rows and all assertions/thresholds remain untouched. Baseline bcf23e0/0844f4f.

Original evidence: CappellaMessageRow.tsx layout=position and retained local
MotionValue subscriptions. The passive didUpdate observer now includes message
identity and untransformed offset/computed height, uses its own4183 instance,
and records actual dependencies/browser/revision. Rapid/chaotic113 original
events are in original-commits-rapid-chaotic.json. No projection function or
delta was patched. At.95/.966667/.983333 row line-0.94-3's layout tops are
174.59375/166.59375/157.5; snapshots stay178.93751/178.93750/178.93750. At1.016667
the SAME source update makes two layout commits with heights61.2656 and122.266;
the snapshot top remains164.18220. At1.02 the row moves to flow top48 with
snapshot163.697826 and115.697826 compensation. Source actual sampling/commit
times differ from native semantic renderEnd1.007 and are not shifted to match.

Candidate added measuredAt to each episode. Only local commits during moving
preceding flow sampled the previous projection at measuredAt, rather than the
new event time; structural reentry/exit code remained unchanged. This tested the
hypothesis that repeated semantic glyph commits were erroneously decaying a
shared precommit snapshot. Exact candidate source/logs remain ignored as
.work/CappellaPlaybackPreparation-rejected-snapshot.kt and
.work/cappella-snapshot-candidate.log. Initial native episode diagnostics remain
.work/CappellaProjectionDiagnosticTest.kt and projections.txt in the native build
directory; the temporary reflective test is not a shipped test/API.

The same source/before/after union includes ALL nonbackground pixels including
low-alpha halos. Before6.370006762→after6.047082910 is a NEW paired metric, not
comparable to historical >8 MAE9.172424. Geometry22.1885925→32.1714781px, opacity
peak unchanged.0503857, presence/order exact. However overlap/calm1.3s foreground
MAE21.964217→35.801429, with further regressions at1.367/1.377s. The first worst
rapid/chaotic worst row improves only.537938px, while overlap/calm row
line-1.11-3 reaches32.1714781px at1.3s. The mean cannot justify those failures. Contact sheet
shows predefined worst rapid/overlap scenes; raw images/complete numeric geometry
and source hashes are preserved. All6 pressure tests, pause, random seek, remount
passed during the probe; those checks do not override visual rejection.

Correction after9aa439a: the initial report compared frames while the candidate
Gradle capture was still running. Its mean6.376147986→6.209810328 and claimed
peak21.6506547px are INVALID partial-capture results, retained here as a failure
record only. comparison.json was regenerated from the COMPLETE preserved frame
directory after capture finished and already contains the final values above.
Comparators must run only after the capture process has completed successfully.

The candidate is insufficient: source snapshot transport must distinguish
parent/local and repeated commits within ONE media update, not freeze every
local-event projection. Next falsifiable probe: seed the same original precommit
DOM layout/transform, observe both original commits around1.016667, and compare
the snapshot before/after the transient inline frame-height change. No fixed
frame delay, forced60Hz playback or intensity-specific parameter fitting.

Reproduce passive evidence:
```
node verification/classic-browser/probe-cappella-layout-commits.mjs rapid chaotic
```
Before the candidate, preserve native frames as.work/cappella-projection-before;
after rendering the candidate preserve them as.work/cappella-rejected-snapshot-frames.
`python verification/compare-cappella-projection.py` reads those two explicit
directories, never currently restored production frames as the candidate.
Full geometry output stays in build/verification; compact worst fields, every
image hash/MAE/tile/channel peak and the unchanged conditions are tracked.

No Android/real Desktop-window/device performance claim. Original user-owned
Gradle/signing/daemon changes are excluded. Continue independent Pendolo exact-lock
and runtime work rather than repeat this rejected freeze rule.

## Two-commit original boundary observation v1 (2026-11-18)

`commit-boundary-v1.json` retains the unchanged original rapid/chaotic schedule
prefix through1.1s, with a control and an instrumented run.17 records wrap original
`willUpdate`/`updateLayout` with unchanged this/arguments and observe `didUpdate`;
no Motion projection method, clock or DOM state is replaced. Source/lock/font
hashes, Chromium151 and exact Motion versions are recorded. Both runs' geometry
equals the existing original pressure baseline. A separate repeat in
`.work/cappella-commit-boundary-repeat.json` reproduces ALL states and trace records
exactly; its checked log is `.work/cappella-commit-boundary-repeat.log`.

At1.0166666666666666s the original first commit measures height61.265625,
then the local inline frame returns from49px to110px and the second commit
measures122.265625. The snapshot tops are164.18219912323443 and164.18220135160144
(2.23e-6px round-trip difference), BOTH projection progress0 at the two didUpdates.
The second commit changes the bottom, not a second elapsed-time decay of the top.
This falsifies the simple double-decay explanation; it does NOT validate the
rejected measuredAt/freezing candidate or remove the22.188593px residual.

The first snapshot is taken BEFORE the current RAF projection advances progress
from.1030175197 to.1403623690. At1.02s the precommit DOM has also moved with
preceding frame height while projection progress is still0; updateLayout sees
.0133801252 before didUpdate resets it. Thus a further valid comparison must
separate pre-render DOM transform, continuous flow and post-RAF projection for
the SAME original prior commit, rather than evaluating every value at one t.
That schedule-dependent transport is not yet a media-reconstructible repair.
Production is unchanged. Do not add a fixed display-frame delay.

The first log reported false because the baseline lookup incorrectly relied on
numeric-property enumeration order. The retained oracleLookupNote describes
that reader correction. The independent repeat uses explicit time matching and
fails closed if either baseline or observer geometry differs; no source data,
threshold or failed geometry was changed. Node syntax check and repeat passed.
No new product build is claimed from this observation-only change.

```
node verification/classic-browser/probe-cappella-commit-boundary.mjs .work/cappella-commit-boundary-repeat-2.json
```
The default output refuses overwrite; use a fresh path for an audit. No new full
pressure matrix was run. Continue the independent, already authorised Diorama
text follower rather than retry a disproved Cappella compensation rule.

## Frame phases v2 (2026-11-18)

`probe-cappella-frame-phases.mjs` observes the same original rapid/chaotic prefix,
through1.1s, on its dedicated4191 server/browser. It performs pure reads, including
DOM rectangles (which can force layout); no setters, wrappers, prototype changes
or snapshot writes. Original control and instrumented geometry both equal the
preserved pressure baseline. A separate fresh-path run
`.work/cappella-frame-phases-repeat-final.json` reproduces ALL states/trace records
byte-for-value. Both exact Motion locks, Chromium151, source/host/font hashes and
380×480/DPR1 conditions are embedded in the records. v1 retains24 observations;
v2 adds6 **after-browser-task** observations (30 total). Keep both: the narrower
v1 phase must not be described as the final React commit.

For `line-0.94-3` at1.0166666666666666s, before-media has frame110px,
flowTop97, stored layout157.5, projection19.2290628493. After media commit the
frame becomes49px; after RAF flowTop157/rowHeight61 and progress.1403623690.
Only AFTER the browser task returns does frame110px/rowHeight122 reappear,
with layout204.53125, progress0 and delta-40.3490486484. At1.02s the new original
snapshot163.6978263516 transports to target48 and delta115.6978263516. A sibling
also reenters normal flow in this transaction. These are distinct flow, own
transform, snapshot and target measurements, not a second simple time decay.

`analyze-cappella-frame-phases.py` independently removes own translation/scale
from the original **incoming DOM rectangle**, following the semantics of actual
motion-dom13.4.2 `projection/node/create-projection-node.mjs:636–737` and
`projection/geometry/delta-remove.mjs:removeAxisDelta`. It does not use native
values or the snapshot as an input. All11 observed snapshot tops reconstruct
with peak0 (1e-9 gate). For the key row the resulting164.18219912323443 exactly
equals the captured original snapshot; rounded offsetTop + panelTop + delta is
164.2290628493, and stale measured layout is157.5. Neither is the same input.

This validates the source inverse-transform identity only. A usable native fix
still needs media-reconstructible precommit/live flow and post-transaction target
semantics; the current heuristic mixes a prior measured top with a current event.
Do not replace that unresolved scheduling problem with a fixed RAF delay, freeze
all local commits or lower the22.188593px pressure standard. Production, original
fixtures, the240 source frames and previous rejected comparisons are unchanged.
No new visual/device/performance pass is claimed by this diagnostic.

```
node verification/classic-browser/probe-cappella-frame-phases.mjs .work/cappella-frame-phases-repeat-next.json
python verification/analyze-cappella-frame-phases.py
```
The probe refuses overwrite and closes its own browser/server. Python analysis,
Node syntax check, original/control identity and independent repeat passed.
