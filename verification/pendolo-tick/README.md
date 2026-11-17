# Pendolo mechanical tick kernel

Pinned Folia cc6226e464050a456a2913e062374f23d3844cc5 / AGPL-3.0-only.
Original `VisualizerPendolo.tsx` spring options, wheel-angle and counter-rotation
expressions are extracted verbatim and hashed; the original motion profile and
fallback functions are imported unchanged. `tick-reference.json` records scope.
Actual locked Motion dispatcher/generator supplies 1134 responses over all
three intensities and .5/1/2 snappiness, including under/overdamping, velocity,
reversal and >5-index movement with different rest thresholds.

Native `PendoloTickTrack` reconstructs its immutable response at semantic lyric
start/render-end events, carrying analytic velocity. It includes inclusive
exact ends and separate right limits, rapid gaps, overlap resumption, tied
starts, empty input, pause, random seek, remount and reduced motion. No display
frame replay, epsilon-shifted production time or extra playback clock.

The source physical equations are sampled at arbitrary media elapsed time.
That is distinct from mounted browser `useSpring`, whose velocity estimate and
RAF quantization still need separate comparison. **No UI/theme/device acceptance
is claimed by this fixture.** Native origin is media0 by default; requests before
the explicit finite origin freeze at that origin. Remount reconstructs the
canonical media history rather than resetting a wall-clock animation.

Reproduce:

```
node verification/classic-browser/generate-pendolo-tick.mjs
./gradlew :library:jvmTest --tests '*PendoloTick*' --tests '*CappellaResponse*'
python verification/summarize-pendolo-tick.py
```

Residuals in `build/verification/pendolo-tick`; summary in `comparison.json`.
3402 response fields: peak4.246604e-14; 23040 track fields including random
revisits: peak3.197443e-14. Assertions remain1e-9/1e-8.

Initial oracle incorrectly reused a dispatcher after completion: subsequent
earlier-time queries incorrectly returned its final target. Preserved initial
TSVs `.work/pendolo-tick-stateful-fixtures` and failure logs
`.work/pendolo-tick-{initial,diagnostic}.log`; generator now creates a fresh
real dispatcher for every arbitrary query, and distinguishes its finished state
from raw generator `done`. No expected-value edit or tolerance relaxation.
The tiny strict signed-zero assertion was corrected to numeric zero comparison.

The existing Cappella oscillator was extracted without algorithm changes to
`internal/motion/DampedSpring.kt`, with a compatibility wrapper preserving all
Cappella configuration and result types. Both complete theme test suites, Android debug APK assembly and Desktop
compilation pass (`.work/pendolo-tick-regression.log`).650 Cappella and168
Pendolo existing native PNG hashes remain unchanged. No connected-device run.
