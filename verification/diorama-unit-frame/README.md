# Diorama active-unit material / media envelope kernel

Pinned Folia cc6226e464050a456a2913e062374f23d3844cc5, AGPL-3.0-only.
Actual original updateDioramaActiveUnits writes real Three.js0.185.1 material
and mesh instances. Nothing in the original function/formulas was rewritten.
832 explicit cases: regular/instant/reversed/5ms units, start/end/hold/trail
boundaries, four linear-color palettes including white/black/near-neutral,
optional keyword, enabled/disabled active ghost, strengths0/1/1.5/2,
initial Float32 charges,0/144Hz/60Hz/50ms delta, life/breath controls and power0.
Input/output in ../diorama-fixtures/unit-frame*.tsv/json.

DioramaUnitFrame.kt preserves unified linear-color fill, neutral tint separation,
per-unit bounded gradient wake, dim→sung opacity, source Float32 envelope write,
registered glow, disjoint current/finished ghost, handoff/lift/swell and visibility
thresholds. All13 fields in832 rows match at unchanged1e-10, measured peak0.0.
This verifies the FINITE MATERIAL OPERATOR, not real graphics or continuous RAF.
No playback loop calls that operator once per rendered frame.

The separate media envelope is the analytic exponential with zero charge before
a unit, attack14/12 while current and release4.5/2.2 afterward. It uses only its
own start/end and queried time; random/reverse queries, long tail and pause need
no history. It matches source exponential steps partitioned at true media
boundaries1e-12. Original per-RAF right-end target sampling / Float32 truncation
remains a future continuous-frame comparison, not silently called equivalent.
No audio power, breath modulation or beat is invented by the media adapter.

Tests check hidden keyword before start, one-way decay/no line-level retint,
active-ghost switch no flight leak, unchanged base fill across ghost toggle.
First custom color assertion assumed lerp(1) copies its endpoint exactly;
actual THREE.Color.lerp retains floating subtraction/addition. The assertion now
uses the original expression. The independent832-case oracle passed unchanged
before/after; failures retained .work/diorama-unit-frame-first.log.
Passing build/test: .work/diorama-unit-frame-final.log. Android APK/Desktop
compile pass; no Android runtime/true window, frame rate or visual acceptance.

```
node verification/classic-browser/generate-diorama-unit-frame.mjs
./gradlew :library:jvmTest --tests '*DioramaUnitFrameTest'
./gradlew :sample:android-app:assembleDebug :sample:desktop-app:compileKotlin
```
Use gradlew.bat/JDK21/SDK on Windows. Exact source revision and Motion transitive
lock are checked. Generator uses one SSR server and disposes materials/closes it.

Next: full-shaped active slices with internal clocks, actual three-layer
perspective/raster comparison, original CameraRig/depth/fog and public bridge.
The existing plain-text frozen plane is NOT an active-theme visual substitute.
