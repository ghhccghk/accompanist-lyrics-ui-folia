# Diorama source grouping, timing and published-core bridge

Pinned Folia cc6226e464050a456a2913e062374f23d3844cc5, AGPL-3.0-only.
DioramaText.kt ports splitDioramaLyricUnits (dioramaSceneLayout.ts:145–205),
CJK_GRAPHEME_RE (dioramaSceneTypes.ts), and shared graphemeTiming.ts rules.
This is an internal text preparation feature, NOT a selectable theme or visual pass.

Actual unmodified original functions loaded through Vite SSR, Intl.Segmenter;
67 invented inputs: the55 existing shared synthetic timing cases plus12 category
boundaries (CJK, kana, geometric bullets, supplementary Han, emoji/ZWJ,
combining marks, Arabic, Indic, whitespace and repeated/missing parser words).
Inputs/expected values/source SHA256: ../diorama-fixtures/text*.{tsv,json}.
641 graphemes and226 display units have identical text, UTF16 ranges and word
identity; all start/end values meet the original1e-12 gate. Local measured peak
is recorded in build/verification/diorama-text/numeric.txt (0.0 seconds).
No expected source observation or numerical gate was changed after capture.

Published-core bridge keeps original objects/SourceRef, nested accompaniment,
subtitles, declared source bounds and each raw fragment clock. No alignment
inference or invented word timing. With no words, the source evenly distributes
line time over EGCs; this is derived timing. Grouped Latin words retain their
internal grapheme clock list rather than reducing to one interval.
Unsafe parser splits (combining mark / ZWJ) reconcile only display EGC boundaries,
retaining every covering source interval and UTF16 range. This intentionally
repairs the original local-syllable index mismatch; it is documented shaping
adaptation, not a claim of bug-for-bug source output for malformed EGC fragments.
Grouping does not rasterise any UTF16 code unit or prescribe isolated glyph shaping.

First custom bridge assertions incorrectly used decimal literal.15 for the
original expression .1+(.2-.1)/2 and assumed ZWJ+rocket was one standalone EGC.
The implementation already retained all THREE source intervals of woman / ZWJ /
rocket. The assertions were corrected to original seconds arithmetic and all
actual source intervals; the independent67-case oracle remained unchanged and
passed before/after. Failures remain .work/diorama-text-first.log and final.log;
passing build is .work/diorama-text-verified.log, numeric report run evidence.log.

Run:
```
node verification/classic-browser/generate-diorama-text.mjs
./gradlew :library:jvmTest --tests '*DioramaTextTest' --tests '*DioramaProjectionTest'
./gradlew :sample:android-app:assembleDebug :sample:desktop-app:compileKotlin
```
Use gradlew.bat/JDK21/configured Android SDK on Windows. JVM logic, existing
path/projection and both builds pass; NO screenshots, real Desktop window,
Android runtime or performance acceptance supplied by these pure text tests.
Next is full-shaped text under frozen perspective, then actual camera/lighting
and composition; existing nine shared-texture images do not verify Kotlin shaping.
