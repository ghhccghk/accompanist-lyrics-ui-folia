# Original weighted shot selection, connected to internal core text playback

Pinned Folia cc6226e464050a456a2913e062374f23d3844cc5, AGPL-3.0-only.
cameraPath.ts:374–548 -> DioramaShotPicker.kt:actual positive note profile,
chorus/part/block and word-count weights,calm/normal/chaotic adjustments,.05 floor,
seeded ordered pick and previous-two BASE pick dedupe. Lookback is not recursive
previous final output. Original motion presets also drive placement/move/drift/
smooth in DioramaTextPlayback; no additional audio or formation work.

Core supplies no chorus/songPart/blockIndex. The internal optional metadata
defaults to absent/false; do not infer it from alignment, nested accompaniment or
lyrics content. Words are actual bridge fragments with their source intervals,
not invented semantic tokens. Explicit shots remain an internal calibration
override; default text playback now uses actual source weighted selection.
No public Diorama style/sample entry or new public contract.

generate-diorama-shots.mjs imports actual original getDioramaShot and motion
resolver; exact private weights block is exported read-only with source ranges/
SHA256 and its extracted picker checked against the real exported one. Source
checkout unchanged. ../diorama-fixtures/shots-v1 retains inputs and1404 expected
controls:36 lines,plain/explicit metadata,3 modes,6 string/numeric/default seeds,
including UTF16 emoji,invalid indices,0/3/4/7/8/12 words,nonpositive notes,
1.3s long-note and.34s staccato boundaries,section changes and anti-repeat.
All13 archetypes covered. Selection exactly equal; all13 weights peak0 at1e-12.
Motion presets exactly equal. These are ORIGINAL generated expected values,
not Kotlin output or modified prior fixtures.

Actual core two-line text integration now uses the default picker and checks
both sides/exact1.9s line switch,original refs,offset,pause,reverse seek,logical
resize,reduce-motion restoration and unmount/remount. No new source-raster or
continuous visual acceptance claim. Source finite/media camera and28px low-alpha
residuals from ../diorama-follow remain; this does not repair those errors.
4 targeted JVM tests and Android library compile pass
(.work/diorama-shots-first.log). No new device/window/performance claim.

```
node verification/classic-browser/generate-diorama-shots.mjs
./gradlew :library:jvmTest --tests '*DioramaShotPickerTest' --tests '*DioramaTextPlaybackTest' --tests '*DioramaCameraTrackTest' :library:compileAndroidMain
```
Exporter refuses existing fixture overwrite; preserve previous originals for an
intentional audit. No browser or external service is needed for numeric export.
