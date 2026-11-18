# Cappella passed-text shaping coverage

Pinned Folia v0.7.26 / cc6226e464050a456a2913e062374f23d3844cc5.
This fixes complete native ink coverage for passed messages and the no-fade
reduced-motion path. It does not repair the dense projection/React-commit problem.

## Source and falsifiable regression

`CappellaBubbleParts.tsx:46–63` returns one complete `fullText` node from
`CappellaText`; `CappellaMessageRow.tsx` selects it for inactive lyric messages.
`CappellaBubble.kt` instead clipped each EGC's caret-selection path, even with
all character alpha=1. A caret path is not an ink mask: it cuts italic overhang
and can paint overlapping antialiased ligature/bidi fragments more than once.

New `CappellaCompleteTextTest` compares production drawing with an independently
drawn, single complete shaped row at the same baseline. Before the patch, passed
AND reduced rows differed by226/85/77/66/0 pixels for italic/Arabic/mixed-bidi/
Latin/CJK. The italic tail lost9px of ink bounds. All ten comparisons now have
**zero pixel difference**. No internal character timing, target sizes, row
geometry, font layout, click mapping or active-fade code changed. Pause remains
exact. Initial failed log/images/table remain at
`.work/cappella-complete-text-before.log` and `.work/cappella-complete-text-before`.

## Independent original component

Actual unchanged original `CappellaText` is mounted in a transparent fixed
host frame:800x300/DPR1,64px/700,1.45 line height,16/20px padding,1px transparent
border, rounded overflow-hidden frame. The italic case explicitly requests the
same synthetic italic in both hosts. Existing licensed parity fonts and hashes,
browser151.0.7922.34, exact Motion lock, source/host/lock hashes and all inputs
are in `comparison.json`. This isolates the original passed-text node, **not**
a full row/message stage. No source algorithm edits, opacity injection,
clock/phase changes, registration, dropped cases or low-alpha cuts. Transparent
RGB is compared after premultiplication with retained alpha, on a single
source/before/after union.

Five original pairs:mean foreground premultiplied RGBA MAE7.962819→7.081652.
Italic local64px worst109.412121→10.674576; CJK control hash unchanged.
Contact inspected:cropped italic tail is restored; source/native edge raster
differences remain. Not an acceptance threshold or a full-theme visual pass.

Existing exact-lock mounted240 dense frames were rerun after preserving the
complete native baseline under `.work/cappella-complete-pressure-before`.
All six geometry/opacity/order tables are byte-identical, retaining the
**22.188593px** projection residual.240 PNGs have glyph-edge changes, including
the title. Same all-nonbackground source/before/after union:
mean6.387616→6.387741, worst local109.25 unchanged. The small aggregate regression
is preserved, not hidden as an improvement; restoring complete shaped ink is
independently proven above. Projection and active-opacity coverage remain work.
Full hashes/metrics are in `pressure-regression.json`.

## Validation

```
node verification/classic-browser/capture-cappella-complete-text.mjs
./gradlew :library:jvmTest --tests '*CappellaCompleteTextTest' --tests '*PublicCappellaTest' --tests '*CappellaCoreAdapterTest' --tests '*CappellaPresenceTrackTest' :library:compileAndroidMain :sample:desktop-app:compileKotlin
python verification/compare-cappella-complete-text.py
./gradlew :library:jvmTest --tests '*CappellaPresenceTest' --tests '*CappellaCompleteTextTest' --tests '*PublicCappellaTest' --tests '*CappellaCoreAdapterTest' --tests '*CappellaPresenceTrackTest'
```

One-shot source capture refuses overwrite, checks pinned clean source and exact
locks, and closes its dedicated ephemeral-port server/browser in `finally`.
For intentional repeat, first preserve the previous source directory. Run
comparison only after native capture finishes. Generated raw PNGs remain ignored;
contact and numeric/hash evidence are tracked. JVM coverage/public lifecycle,
dense seek/pause/remount, Android library and Desktop compile pass
(`.work/cappella-complete-text-after.log`, `.work/cappella-complete-pressure.log`).
No new native-window, Android runtime, device or performance conclusion.
