# Diorama normal text window and distance lifecycle

Pinned Folia `cc6226e464050a456a2913e062374f23d3844cc5`, AGPL-3.0-only.
This integrates actual neighbouring text into the existing INTERNAL core stage.
Diorama is not a public theme or a sample selection, and remains unaccepted.

Original sources, under `src/components/visualizer/diorama`:

- `dioramaSceneLayout.ts:26–78`: normal mounted window and placement. This change
  uses only the single-song window, previous 2 / next 3. Backward seek does not
  invent an outgoing song/flight or carry old media history.
- `dioramaSceneConstants.ts:8–10,31–39`: original neighbour offset opacities.
- `dioramaSceneFrame.ts:103–131`: neighbour fit at FIXED 5.2 hero distance,
  Euclidean-distance life, past primary / future secondary colour. This does not
  replace life with view depth or continuously resize neighbours by live distance.
- `DioramaScene.tsx:612–651`: transparent, depthWrite=false order0 neighbours;
  `DioramaActiveUnitPlanes.tsx`: active layers order17/18/19. With no opaque scene
  geometry/depth writers, centre-depth far-to-near text ordering matches that
  transparent ordering. It is NOT general per-fragment depth testing.
- `DioramaScene.tsx:523`: active-group distance life, now passed to all three
  already ported unit-material layers instead of silently retaining life=1.

`DioramaNeighbors.kt` and `DioramaTextPlayback.drawStage` consume real published
core lines, own plain/full-shaped active rasters, original path/placement and
current media subject. All plain rasters are prepared with cancellation checkpoints;
identical text shares an immutable raster within ONE preparation/font context.
There is no global cache or frame-driven preparation. The existing internal
playback object still prepares a whole song: long-song total active/plain memory
has NOT been accepted, and a bounded texture ownership solution is required
before opening this theme. This is not a performance result.

## Independent original evidence

`capture-diorama-neighbors.mjs` runs the unmodified original window/visible-line,
plain/unit raster, layout, fit/life and material functions in a dedicated
Three0.185.1/Chromium151 WebGL instance. One synthetic 8-line single segment,
Latin/Arabic/CJK, same three font files/weight700, DPR1, 380/800×600, source seed
DioramaProbe, subjects0/3/7, eye distances5.2/14/36, time=subject start+450ms.
Both sides receive identical integer-ms word times. Source/host/font/lock hashes,
actual Motion13.4.3/dom13.4.2/utils13.3.0 are retained in neighbors-v1/scene.json.

The host constructs original meshes/materials and freezes a source camera to
isolate text composition, injects the already documented analytic unit charges
at delta0, breath1/audio0. It does not mount original React CameraRig/DioramaScene,
copy Kotlin geometry into the oracle, add fog or background, or claim raw RAF
equivalence. Those remaining capabilities are explicit, not waived.

37 original normal-window controls pass exactly. 180 original neighbour rows
match own advance/font size exactly and fit/opacity/linear colour/world centre/
view depth to peak **1.0811962436463318e-11**, unchanged 1e-9 numeric gate.
The core stage keeps the original core object at all three subject indices.

54 source/former-active-only/complete-own stage comparisons use ONE identical
three-way nonbackground union per frame, including lowest-alpha halos. Foreground
MAE **15.8950838429 → 6.6165123192**, worst frame **34.6599081932 → 12.8062575211**,
worst64px tile **49.5639571519 → 22.9856321839**. Every image hash, extent and peak
is retained. No registration, delay, foreground cutoff or rejection of blank
frames. The contact uses predetermined ordinary/narrow/final/distant conditions.
Inspection confirms formerly omitted past/upcoming lines now exist and recede.
The narrow CJK view is cut in BOTH source and own camera framing; geometry/raster/
halo and readability residuals remain, and the average does not certify them.

## Validation / limits

Targeted JVM tests cover original windows/material geometry, full core stage,
pause, arbitrary-order seek, offset exactly once, settings/unmount restoration,
unchanged preparation count while changing media/view/subject, and duplicate-text
raster ownership. The existing actual-media core playback test also covers
logical resize, input identity, theme settings and release. Android library and
Desktop sample compilation pass; no new Android device or real-window/audio
runtime or performance claim.

An initial source-only capture used fractional-ms synthetic word times, before
recognising actual core timestamps are integers. Its full inputs/images remain
under `.work/diorama-neighbors-initial-noninteger-times`; it is NOT a before frame
or a selected failed case in this comparison. Only input construction changed,
not original functions. The final complete 54-frame oracle was generated before
the JVM capture; the comparator ran after successful native completion.

```
node verification/classic-browser/capture-diorama-neighbors.mjs
./gradlew :library:jvmTest --tests '*DioramaNeighborStageTest' --tests '*DioramaTextPlaybackTest' --tests '*DioramaCameraTrackTest' --tests '*DioramaActiveTextTest' :library:compileAndroidMain :sample:desktop-app:compileKotlin
python verification/compare-diorama-neighbors.py
```

The source exporter refuses overwrite; preserve the old source fixture/images
first for an intentional audit. Dedicated4188 server/browser close in finally.
Raw source/native PNGs remain ignored under build/verification. Logs:
`.work/diorama-neighbors-source-ms.log`, `.work/diorama-neighbors-final.log`.
Unresolved: full follower residuals, fragment fog/depth, overhang/mixed bidi,
Android middle metrics, cross-song flight, long-song memory and device evidence.
