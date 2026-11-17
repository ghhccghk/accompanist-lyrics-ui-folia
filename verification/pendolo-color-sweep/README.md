# Pendolo range-colored timed sweep

Pinned Folia cc6226e464050a456a2913e062374f23d3844cc5 / AGPL-3.0-only.
`wordColoring.ts` range entry and `pendoloColorRuns.ts` → `PendoloColoring.kt`.
Source is not Cadenza's individual-token matcher: preserve UTF-16-length
priority, stable overlap selection, original ASCII-word matching, exact CJK
phrases, whole-EGC coverage and adjacent equal-color coalescing. Original10
controlled cases/281 fixture rows include overlap, normalization, source order,
combining marks, ZWJ emoji, Arabic no-match and ECMAScript whitespace edges.
All ranges/token colors/global run identities match unchanged source functions.
Native callers already supply typed `Color`; this is not a CSS color parser or
validation of malformed JavaScript option objects.

`PendoloSweepTarget` prepares color clip paths against the original **complete
shaped row**, then masks the combined color layer with the original continuous
timed gradient. No per-character reshaping/drawing; timing/row ranges stay intact.
Paths are prepared when text/layout/palette change. Default no-match coloring
keeps the unchanged sweep path. Internal wheel entry is wired to the same
prepared ink, but colored full-wheel/chorus acceptance is still pending.

Real original `PendoloActiveLyricSweep` remains unchanged. Four synthetic scenes,
same licensed covered font bytes/700/28px,150/300px row constraints,380×480,DPR1,
11 times produce88 original/native pairs. Arabic's keyword intentionally does
not match the source ASCII matcher; no fabricated Arabic keyword support.
176 complete-row/prefix observations remain exact. Random seek/resize, pause,
remount, disabling/restoring colors and actual visible-ink change checks pass;
Arabic no-match preserves identical ink.312 prior native default-sweep/static/
mounted-wheel image hashes are unchanged.

Mean foreground MAE5.799414, peak9.237356, no registration/color/phase fitting.
`contact.png` inspected; residual gradient/AA/color-boundary coverage remains.
Not full-theme visual or Android-device acceptance. Full Pendolo JVM suite,
Android debug APK assembly and Desktop compile pass
(`.work/pendolo-color-final.log`); no device/window launch.

Reproduce (restart the fixture server after editing an already-served TSX file):

```
node verification/classic-browser/generate-pendolo-coloring.mjs
node verification/classic-browser/serve.mjs
node verification/classic-browser/capture-pendolo-color-sweep.mjs
./gradlew :library:jvmTest --tests '*PendoloColor*' --tests '*PendoloSweep*'
python verification/compare-pendolo-color-sweep.py
```

Raw PNGs/residuals: `build/verification/pendolo-color-sweep-{browser,kotlin}`.
Conditions/palette inputs and PNG hashes are tracked. First source capture had
stale no-HMR transformed host code (no wordColors input); it was rejected before
comparison/commit. Restarted only the verified project fixture server and added
a required nonempty-palette guard. The tick-stage capture now also rejects a
stale width-observer host. A fresh recapture preserved all tick/geometry/width
TSVs, but2 exact-start source PNGs varied; latest report uses current same-source
baselines. This variation remains a source-capture issue, not native parity.

Next: original explicit chorus fill/glow/halo/scale and full-wheel palette
comparison, then public/core calibration entry. No theme selector is exposed yet.
