# Cappella reduced-motion flow: remove residual spatial interpolation

Source context: pinned cc6226e4 CappellaMessageRow.tsx targetSize/scaleOverflow and
CappellaBubbleParts.tsx AnimatedBubbleFrame. Normal source animates height/width,
margin and projection. Our existing reducedMotion policy removes spatial
scale/projection/presence and chooses instantaneous bubble targets; it is a host
accessibility adaptation, not a reduced-motion feature claimed from upstream.

Bug at2d472d4: boxes used direct target dimensions, but their flow origins and
rowHeight still inherited ordinary animated preceding heights and spring margins.
This left spacing moving after reduction, and could separate current boxes from
their intended rows by66.507928px. The fix uses target heights and no spring margin
throughout REDUCED flow and row-height calculation. Normal path is unchanged.
No glyph timing, shaping, source phase, animation formula, fixture or threshold
was replaced. Reveals still use the shared media clock.

Six tests use rapid/overlap x3 intensities, the same240 pressure probe times and
synthetic word inputs, converted through published lyrics-core millisecond
syllables and the actual core adapter. This new reduced matrix is NOT the original
double-time normal240 matrix; any floating boundary conversion is explicit.
All current expected target rows/heights are identical before/after. Numeric peaks:

| Condition | Before target-flow error(px) | After |
| --- | ---: | ---: |
| overlap/calm |42.110647|0|
| overlap/normal |53.269706|0|
| overlap/chaotic |61.470228|0|
| rapid/calm |40.460443|0|
| rapid/normal |54.116683|0|
| rapid/chaotic |66.507928|0|

Public-entry pixels equal the internal reduced renderer with identical palette;
pause, reverse seek and remount pass. Before fixing production, the six tests fail
their unchanged1e-9 target-flow gate; after fixing all pass. Normal original240
native PNGs and6 geometry tables are BYTE-identical to their saved baseline.
Normal projection22.188593px is still unresolved, not improved by this repair.
Contact shows all six predefined1.02s before/after cases, no selected recaptures,
alignment or alpha suppression. Raw native PNGs/TSVs are in build/verification;
before files stay `.work/cappella-reduced-flow-aligned-before` and normal baseline
`.work/cappella-reduced-normal-before`. Compact worst fields/hashes tracked.

14 targeted reduced/presence/public checks and Android library compilation pass:
`.work/cappella-reduced-after.log`. This is JVM offscreen drawing, NOT a real
Android/desktop-window run or performance evidence. The prior seven-theme real
window evidence2d472d4 predates this reduced-path repair and remains versioned.
Initial test accidentally used different public/internal accent palettes; its
pixel failures were a harness error, retained in `.work/cappella-reduced-before.*`.
Palette-aligned pre-fix failures are `.work/cappella-reduced-before-aligned.*`;
only that complete aligned run is used for the paired comparison.

```
./gradlew :library:jvmTest --tests '*CappellaReducedFlowTest' --tests '*CappellaPresenceTest' --tests '*PublicCappellaTest' :library:compileAndroidMain
python verification/compare-cappella-reduced-flow.py
```

Comparator requires the saved pre-fix directories and completes only after Gradle
capture has exited. It does not rerecord browser/source frames. The normal pressure
images already have exact Motion-lock proof; no redundant upstream recapture here.
