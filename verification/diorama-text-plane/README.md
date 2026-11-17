# Diorama independently shaped plain text under frozen perspective

Pinned Folia cc6226e464050a456a2913e062374f23d3844cc5, AGPL-3.0-only.
This replaces shared-original-PNG isolation with Kotlin's OWN complete shaped
paragraph for a neighbouring/plain lyric plane. It remains an internal bounded
capability, not a selectable Diorama stage or original-scene visual acceptance.

## Original oracle and conditions

Untouched original rasterDioramaLine, buildDioramaPath, getDioramaTextPlacement,
frameQuaternion and resolveFrameFitScale. Actual Three.js0.185.1 PlaneGeometry /
MeshBasicMaterial / PerspectiveCamera55°, WebGL SwiftShader, DPR1, background
#162435 and white text. Explicit frozen camera at two local viewpoints; no
CameraRig, active-unit material update, fog, depth intersection or background.
No original source changes. Additional Canvas measureText calls only observe
alphabetic/middle baseline differences, without modifying original rendering.

Latin, Arabic, CJK and repeated long synthetic line ×380/800/801×600 ×two views:
24 frames. Font files, text, viewport, geometry and measured baselines are in
../diorama-fixtures/text-planes.json/tsv. All font cmaps checked: no missing
synthetic characters. Identical OFL font bytes on both sides, no system fallback.
Full source capture remains unchanged when adding passive baseline diagnostics:
all24 original PNG hashes match the retained pre-typographic report.

## Implementation and results

DioramaPlainText prepares a full TextLayoutResult and plain ImageBitmap once
per content/font. No isolated UTF16 glyph shaping, no source raster import.
128px source texture em,700 default weight,1.4em vertical band,.2em pad,
4096 shrink trigger and24px minimum follow original formulas. The pad stays at
its pre-shrink size exactly as the source. Complete selection advance includes
trailing spaces that paragraph line-width hangs; this fixes the original long
fixture failure3967 expected vs3956 actual without altering the oracle.

JVM middle baseline now reads standard OpenType OS/2 typographic ascent/descent
through public Skia Typeface table APIs; the previous normalized native hhea
ascent is wrong when these tables differ. At128px, original observed middle
shifts are36.46875 Latin,19.265625 Arabic and48.640625 CJK. At43px, long Latin
is12.25. All24 baseline observations meet1e-6 and advance/font/canvas dimensions
are exactly equal. No pixel registration/translation or fitted font-name offsets.
CJK originally had5–7px screen baseline error; current all24 ink bounds differ
by at most2px. The complete paragraph retains Arabic joining and visual order.

comparison.json records all-nonbackground union INCLUDING low alpha, local
worst64px tile error, channel peaks, source/native ink bounds and PNG hashes.
Foreground MAE13.08175967 (Latin14.09087105, Arabic7.48672885,
CJK11.58899357, long19.16044522). contact.png was inspected.
Remaining texture filtering/antialiasing differences are not waived or accepted.
pre-typographic report/contact preserve the earlier failed CJK baseline; each
report uses its own source/native union, so their means are NOT a fixed-mask
before/after gain. Existing shared-texture nine-frame matrix is separate.

Camera changes do not rebuild text; frozen clock, content/camera restore and
unmount/recreation are pixel deterministic. No autonomous tasks or global raster
cache. Android APK/Desktop compile passed; JVM offscreen runs passed.
Android currently lacks resolved Typeface OpenType metadata through this adapter;
its explicit fallback uses Paint metrics and may retain the CJK middle-baseline
error. This is UNIMPLEMENTED Android parity, not an intentional final platform
difference. No Android runtime or true Desktop window/device performance claims.

## Reproduce / remaining work

```
node verification/classic-browser/capture-diorama-text-plane.mjs
./gradlew :library:jvmTest --tests '*DioramaPlainTextTest'
python verification/compare-diorama-text-plane.py
./gradlew :sample:android-app:assembleDebug :sample:desktop-app:compileKotlin
```
Use gradlew.bat/JDK21/configured Android SDK on Windows. Dedicated4174 browser
and server close in finally. Generated48 full PNGs stay ignored under build/.
Failures: .work/diorama-text-plane-first.log (trailing space), third.log (nullable
Skia Typeface compile error). Baseline failure images/report retained above.
Passing .work/diorama-text-plane-fourth.log includes both builds. No expected
source value, numeric gate or visual residual was changed to declare success.

Still required: shaped active-unit slices/internal-clock reveal, three-layer
light/soul/base composition, real depth/near clipping/fog, source CameraRig
trajectory and media reconstruction, public integration and device verification.
A plain projected paragraph alone is not the original theme.
