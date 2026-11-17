# Dense projection snapshot probe — rejected

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
