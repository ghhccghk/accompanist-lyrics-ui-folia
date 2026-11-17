# Pendolo target text wheel — pinned original / native

Pinned Folia v0.7.26 / cc6226e464050a456a2913e062374f23d3844cc5,
AGPL-3.0-only. `stage-reference.json` records the unchanged source extracts.
The browser mounts the complete original lyric-wheel JSX subtree, actual
rotating-line, active-sweep and subtitle helpers. It omits audio, gears, store,
manual scroll and chorus. Rotation is explicitly zero: this verifies target
layout, **not mounted escapement motion or full theme acceptance**.

Four synthetic Latin/wrapped/Arabic/CJK scenes, licensed covered fonts from
`verification/assets`, 380/800 × 600, DPR1, subtitle none/both and five media
times/anchors produce 80 pairs. Font weight700 is deliberately fixed on both
sides; this is not a fallback-weight or user-font acceptance test.

Reproduce from repository root (fixture server in a separate terminal):

```
node verification/classic-browser/generate-pendolo-stage-reference.mjs
node verification/classic-browser/serve.mjs
node verification/classic-browser/capture-pendolo-stage.mjs
./gradlew :library:jvmTest --tests '*Pendolo*'
python verification/compare-pendolo-stage.py
```

Raw source/native PNGs, states and residual TSVs are in
`build/verification/pendolo-stage-{browser,kotlin}`; exact hashes/conditions in
`comparison.json`, inspected overview in `contact.png`. No registration,
phase fitting, crop alignment or matching source geometry is fed to native.
Foreground MAE uses the union of pixels differing by >8 from RGB22/36/53.

5808 fields: circle geometry/alpha/reserved heights maximum error0;
overall maximum0.0005px (CSS decimal serialization). Geometry tolerance1e-8,
actual content/box tolerance1/64px retained. Every random seek/resize revisit,
pause and remount is pixel-identical to itself. Existing88 isolated-sweep
native PNGs remain identical to the committed hashes.

First all four tests failed: wrapped width was incorrectly substituted for
max-content, fractional constraints lost a wrap, and SkParagraph discarded
Arabic space tracking. Failed PNGs retained in
`build/verification/pendolo-stage-kotlin-before-intrinsics`; initial log
`.work/pendolo-stage-initial.log`, passing final log `.work/pendolo-stage-final.log`.
Fixes preserve original full paragraph shaping: independently measure
max-content, apply CSS1/64px intrinsic/max rounding separately, and move
whitespace-separated already-shaped Arabic runs to restore missing space
tracking. `stage-arabic-space-tracking.json` is a real browser control.
That adapter covers the demonstrated Arabic/ASCII-space case only; mixed
scripts/non-ASCII whitespace still require investigation.

Mean foreground MAE10.650323, peak21.807819. Narrow fractional anchors can
overlap in the original as well; the port retains that behavior. Glyph AA,
tracking edge clipping and rotated/gradient coverage remain visually different,
especially small idle rows. No claim that these are accepted platform
differences. Actual tick motion, chorus/keywords, public/core integration,
Android device images and full-theme acceptance remain open. Android debug APK
assembly and Desktop compilation pass; no new device run is claimed.
