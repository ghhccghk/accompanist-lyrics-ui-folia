# Cappella active EGC opacity over complete shaped ink

Pinned original cc6226e464050a456a2913e062374f23d3844cc5 / v0.7.26.
AGPL-3.0-only. This follows930d104's passed/reduced-text repair. It does not
resolve normal layout projection or certify the complete Cappella theme.

## Behavior and source

Original `CappellaBubbleParts.tsx:133–159` renders `ActiveCappellaText` using
the original grapheme plan and one CSS fade per visible EGC.
`cappellaReveal.ts` owns character counts/timings/fade durations. Those clocks
remain unchanged in Kotlin; no UTF-16 character is independently shaped.

The previous native active path drew the complete shaped row repeatedly through
individual caret paths. That discarded overhang/ligature/bidi ink even when all
EGCs had identical alpha. `CappellaInk.kt` assigns disjoint visual columns to
existing EGC clock indices, including outer overhang; a logical EGC may own
multiple runs. `CappellaBubble.kt` applies their alpha as a step-gradient brush
on ONE complete-row draw. Fully equal alpha uses the ordinary solid-color path;
adjacent equal-alpha stops are coalesced. No new mask bitmap, independently
shaped character, frame timer, native dependency or source geometry is added.
Ownership is prepared with the shown row layout, not each fade tick.

New independent original30 records cover five fixed fonts/text cases, staggered
.1/.35/.65/1.2s and uniform.5/2s. Actual unchanged `ActiveCappellaText`, reveal and
CSS animation run in Chromium151.0.7922.34 at800x300/DPR1/64px/700/lineHeight1.45.
Fixed host frame780x160,16/20px padding,1px transparent border. Source CSS fades
are paused/reanchored to their ORIGINAL reveal times for deterministic media
comparison; this injection is not original wall-clock mount-history equivalence.
No glyph positioning, shaping, opacity formula or source algorithm is replaced.
Original/host/font/lock hashes and exact Motion13.4.3/dom13.4.2/utils13.3.0 are in
`../cappella-fixtures/active-ink-v1/conditions.json`. Original timing/count/CSS
opacities in `frames.tsv` are independent expected data, not Kotlin output.

## Failures retained and verification

Before repair,15 full/uniform-alpha native controls lose226/85/77/66/0 pixels by
script, at BOTH full and half-opacity. All15 now equal a single complete-row draw
exactly. Original EGC count and CSS alpha pass their existing1e-6 serialization
precision gate at all30 times. Random seek and pause reconstruct exact pixels.
Actual production painter also matches the whole row exactly at.75/1/1.3 scale
and23.25/17.5px translation for all five fonts. Per-column ownership has exactly
one owner throughout the tested visual range. Combining/ZWJ-split parser timing
regressions remain unchanged/passing in CappellaRevealTest.

The first candidate used disjoint VECTOR clips and repeated text draws. At.75
scale those clipped antialiased edges were not complementary. It was rejected;
candidate and strict pixel failure are retained in `.work/CappellaBubble-active-clips-rejected.kt`,
`.work/CappellaActiveInkTest-vector-clips-failed.kt`,
`.work/cappella-active-vector-clips-failure.xml`. No tolerance was relaxed.
A subsequent constant-gradient primitive differed in8 Arabic pixels; product
keeps all equal-alpha text on the original solid path. That failure/primitive
test also remain under `.work/*uniform-brush*`. The final transformed assertion
exercises the ACTUAL production painter against the SAME complete-row pixels.
Initial source host incorrectly read revealTimes from getCharacterRevealPlan;
the original separate buildCharacterRevealTimes API fixed the host, not source.
First empty capture and failure log are preserved, no source frames selected away.

Original/before/after30-frame comparison retains ALL nonzero-alpha pixels in a
single union, premultiplied RGBA, no registration/phase fit/omitted cases. Contact
inspected. Mean12.375545→11.677862; worst64px223.259843 unchanged. Early italic
stagger100ms slightly regresses142.509852→143.168309: original CSS's shared fi
ligature/opacity ownership still differs from native. This remains required work,
not an accepted platform exception. Raster/font/early shared-ligature residuals
are not hidden by the mean. All per-frame/partial failures are in comparison.json.

Original dense240 source was NOT recaptured. Complete pre-active native matrix
is preserved in `.work/cappella-active-pressure-before`. All six geometry/order/
opacity tables remain byte-identical;22.188593px projection residual unchanged.
128 PNGs change; source/before/after low-alpha union mean6.387741→6.387782,
worst local109.25 unchanged. This tiny aggregate regression is retained, not
called a stage improvement. Full hashes and metrics:pressure-regression.json.

## Reproduction

```
node verification/classic-browser/capture-cappella-active-ink.mjs
./gradlew :library:jvmTest --tests '*CappellaActiveInkTest' --tests '*CappellaCompleteTextTest' --tests '*CappellaPresenceTest' --tests '*PublicCappellaTest' --tests '*CappellaRevealTest' --tests '*CappellaCoreAdapterTest' :library:compileAndroidMain :sample:desktop-app:compileKotlin
python verification/compare-cappella-active-ink.py
python verification/compare-cappella-ink-pressure.py --baseline .work/cappella-active-pressure-before --output verification/cappella-active-ink/pressure-regression.json
```

Source generator refuses overwrite and closes only its ephemeral-port instance.
Preserve old original directories before an intentional audit. Before/after
comparators require the preserved native baseline and completed Gradle captures.
Sixteen targeted JVM tests, Android library and Desktop compile pass; logs
`.work/cappella-active-final.log`. No new physical-device, real-window, continuous
recording or performance claim. Native screen/consumer regression is separate.
