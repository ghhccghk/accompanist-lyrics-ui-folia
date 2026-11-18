# Frozen original spring / saturation mapping v1

This is a bounded diagnostic, **not a production repair or visual acceptance**.
Original pinned Folia `cc6226e464050a456a2913e062374f23d3844cc5` remains unchanged.
Fixtures: `verification/fume-fixtures/frozen-clamp-v1`. Existing handoff, mounted
92-frame captures, camera-continuous expectations and acceptance gates are intact.

`generate-fume-frozen-clamp.mjs` extracts the verbatim original spring block from
`fumeCameraStep.ts:329–371`, transpiles types only and imports original clamp/mix
and scale constants. Source/fragment/Motion/input hashes are in conditions.json.
Inputs come from the first actual spring in the preserved original handoff trace:
six source cases, including 800px at 3.1333333333333333s and both widths at
5.533333333333333s. The 380px/3s cases have no spring inside the preserved 250ms
window; they are explicitly skipped rather than extrapolated or manufactured.
Each original full-nine result is reproduced exactly before any sweep is made.
This is a Node/SSR numeric experiment, not a new browser or mounted capture.

Freeze the original target, duration, phase=1, coefficients and distance; sweep
incoming X/Y/scale velocity around the algebraic clamp threshold, both signs,
three durations (original dt, 1/144 and 1/30). These durations probe the **source
operator**, not a playback scheduler. 756 source maps are retained. Many algebraic
thresholds require normally inadmissible incoming velocities: only 46 rows have
incoming velocity inside that axis's bound. Do not generalize all 756 to normal
playback. 108 pairs of distinct incoming nine-states map to identical COMPLETE
outgoing nine-states, verifying loss of incoming velocity at saturation.

The independent Kotlin finite formula reproduces all source maps with peak 0
(gate 1e-9). The existing unsaturated log response matches 96 calibration-duration
controls to 3.865352482534945e-12 (same gate). Thus this experiment does not blame
the original focus filter or unclamped matrix logarithm.

For the 8 **admissible saturated calibration-duration** rows, integrating the
current frozen projected-log derivative and accepted velocity/scale projection
still differs in position by up to **0.6846002664931348 world units**. Refining
512 to 1024 quadrature steps changes position by at most 0.003026018396440122.
Those steps are diagnostic numerical refinement, never display frames. This is
not a certified exact solution/error bound. The source moves by the final capped
velocity across the WHOLE interval; the continuous projection reaches the bound
partway through. Source focus filtering precedes velocity, then position.

Across all admissible durations the largest position difference is 3.2029096457,
but that case is UNSATURATED at 1/144s. It includes the known change away from the
1/60s log calibration, so **it is not evidence of a clamp-only error**.

Original one/two/four/eight/sixteen partitions retain all nine outputs. One vs two
equal partitions changes X by up to 4.805516718215586 world units, despite the same
frozen target/coefficients/total duration. The family of original finite maps is
not a duration semigroup. No autonomous continuous nine-state operator can equal
ALL those maps simultaneously. This does not rule out an explicitly justified
media-time adaptation, extended state, or a different future reconstruction.
It rules out claiming exact source equivalence merely by reducing RK tolerance.

The immediate production patch remains blocked on a justified saturation/handoff
adaptation with original mounted improvement and schedule independence. Do not
retry focus-only/midpoint-only edits, introduce fixed-frame playback, or replace
old expected values. A falsifiable next sub-experiment is a branch-boundary-local
reconstruction that accounts for capped interval displacement explicitly; compare
both 3.13s and 5.53s controls from these identical source states BEFORE any whole
song candidate. Its chosen reconstruction policy must be explicit and cannot
claim agreement with every original RAF rate.

Validation: FumeFrozenClampTest, FumeCameraHandoffTest, FumeSpringResponseTest and
FumeCameraTrackTest pass (`.work/fume-frozen-clamp-verified.log`). The first prior
test log and failed initial SSR source extraction are retained under `.work`.
No production source changed, no new screenshot/device/performance conclusion.

Reproduce without overwriting the independent oracle:

```
node --check verification/classic-browser/generate-fume-frozen-clamp.mjs
./gradlew :library:jvmTest --tests '*FumeFrozenClampTest' --tests '*FumeCameraHandoffTest' --tests '*FumeSpringResponseTest' --tests '*FumeCameraTrackTest'
```

The generator refuses to replace existing source fixtures. Preserve them outside
the target directory before an intentional independent source audit. Native
per-case output and aggregate metrics are generated under
`build/verification/fume-frozen-clamp`.
