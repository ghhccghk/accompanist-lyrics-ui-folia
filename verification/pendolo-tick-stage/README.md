# Pendolo mounted escapement / text wheel

Pinned original cc6226e464050a456a2913e062374f23d3844cc5, AGPL-3.0-only.
The original useSpring/options/update effect, rotation and counter-rotation
declarations are inserted unchanged into the already extracted original complete
wheel JSX. Hashes are in `tick-stage-reference.json`. Original `PendoloRotatingLine`
and active sweep run unchanged. The host only replaces stores/audio/gears and
sets smart-beat to zero. No chorus/keywords/manual-scroll claim.

Six conditions: Latin380×600 calm/normal/chaotic, wrapped380 normal, Arabic800
normal, CJK800 normal. Same synthetic8-line scenes, font bytes/weight700/colors,
DPR1 as the target test.144 source frames /694 rows, source kept mounted from
media0 through9s. Controlled performance/RAF60Hz plus exact probes, one browser
task per frame to release Motion's cached time. Original WAAPI/CSS effects are
anchored to their source start/creation times. Tailwind4's default transition
easing is explicitly retained from its locked `theme.css`.

Native `PendoloPlaybackWheel` reads a single media-time supplier. The prepared
tick track only changes layout targets at semantic boundaries; draw-time wheel
rotation and -.65 counter-rotation preserve each complete shaped row. Pause,
144 random seeks and unmount/remount checks are pixel-identical to themselves.
`renderEnd` stays separate from word/sweep end. No additional animation clock.

Reproduce:

```
node verification/classic-browser/generate-pendolo-tick-stage-reference.mjs
node verification/classic-browser/serve.mjs
node verification/classic-browser/capture-pendolo-tick-stage.mjs
./gradlew :library:jvmTest --tests '*Pendolo*'
python verification/compare-pendolo-tick-stage.py
```

Raw images/states/residuals are in `build/verification/pendolo-tick-stage-*`;
hashes/conditions in `comparison.json`, inspected overview in `contact.png`.
Primary native results **do not read source geometry or source spring values**.
An additional144-image isolation set explicitly supplies only source wheel
rotation to distinguish text/layout errors from response errors. It is not
counted as independent full-wheel parity. Images are not registered, phase-fit
or color-fit. Foreground MAE uses union distance>8 from RGB22/36/53.

Baseline at35ef276 (idle width subsequently fixed; latest below):
value0.026009, velocity1.825387 indices/s, wheel0.325107°,
counter-rotation0.211319° peak. Primary mean foreground MAE13.235468, peak23.317442;
source-rotation isolation mean13.087258/peak20.502865. Geometry box peak17.652888px
persists with the same rotation, alpha4.90e-7 (serialized CSS opacity).
These are **unresolved**, not new acceptance thresholds. Tests enforce source
target/visible index identities and determinism; the report records remaining
visual/numeric deltas instead of disguising them as passed parity assertions.

Historical next task at35ef276 (resolved below): source review identifies idle main text's `transition-all duration-200` max-width
transition, while native currently uses the target immediately. This is the next
specific repair. Mounted browser velocity/RAF quantization also differs from the
canonical continuous derivative. Before native-clock/easing corrections evidence
is retained under `build/verification/pendolo-tick-stage-browser-before-native-clock`
and `.work/pendolo-tick-stage-before-css-easing.json`. Initial Kotlin test syntax
failure log is preserved; final full Pendolo tests/Android APK/Desktop compilation
pass in `.work/pendolo-tick-stage-final.log`. No connected-device or public theme
acceptance is claimed.

## Retained idle-width repair

Latest `comparison.json` uses the same current source for before/after native.
Box peak17.652888→1.364707px, same-rotation box17.652888→0.105115px;
untransformed width peak0.03125px. Primary MAE13.235477→13.137682 and same-rotation
MAE13.087267→12.989472; pixel peaks remain. Original88 isolated/80 static targets
remain unchanged. Actual CSS interruption/reversal control and source-observer
hash changes are documented in `verification/pendolo-width/README.md`.
Historical integration status before b0ed98e: idle width is no longer the next task; continue chorus/keywords and remaining
raster/clock discrepancies before public preview. Full Pendolo tests/Android APK/
Desktop compile pass, no device acceptance.

2026-11-18: exact Motion-lock144-frame audit and targeted JVM recapture completed;
see `verification/motion-lock-audit/README.md` and `pendolo-dense.json`. Public
Pendolo already exists; remaining geometry/rotation/raster differences remain open.

Actual follower rest limits subsequently corrected; all144 native PNG hashes
remain unchanged. A separate source driver explains the7s schedule residual,
and27 new original/public frames cover corrected tails and large tied jumps.
See ../pendolo-follower/README.md. This does not resolve the old.325106°/1.364706px
or full-theme acceptance; no source fixture or expectation here was overwritten.
