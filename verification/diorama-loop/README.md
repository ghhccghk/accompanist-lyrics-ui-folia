# Same-corridor text return — 2026-11-18

Pinned Folia v0.7.26 / cc6226e464050a456a2913e062374f23d3844cc5,
AGPL-3.0-only. INTERNAL real-core camera/text integration, no public Diorama
entry. Original finite numbers, native pictures and visual acceptance are
different verification levels.

## Source -> production

- `VisualizerDiorama.tsx:364–428`: loop keeps the existing corridor/frames/key/
  global indices; epoch increments and the outgoing index is captured. Only a
  new song appends a46-unit-offset segment. `DioramaCameraTrack.restarted` and
  `DioramaTextPlayback.loopAt` now preserve frames/subjects, reuse the SAME
  complete-shaped text, neighbor rasters, timings, picker output and core refs.
  Only the incoming media camera checkpoints are newly prepared. Cancellation
  does not mutate the previous playback. No new renderer/native dependency.
- `CameraRig.tsx` finite flight/follow: existing source-derived live-target
  return from outgoing base refs; no fixed return spline/seek snap/frame delay.
- `dioramaSceneLayout.ts:26–78`, `sceneConstants.ts:8–16,31–46`, and
  `sceneFrame.ts:103–131`: live-2/+3 and outgoing-2/+1 sorted union; a line is
  departing when distance to outgoing index <= distance to current (ties count).
  Departing opacity .7 for former active, .45 for distance<=2, otherwise .25,
  multiplied by actual Euclidean life, always primary tone. The new helpers
  in `DioramaNeighbors.kt` reach the ACTUAL production text draw stage.
- Source outgoing window clears by a wall-clock timeout. Here window/flight
  duration uses the same lyric-media clock, ending EXACTLY3.2s, so pause freezes
  and arbitrary seek restores both planes and camera; no second timer exists.

`loopAt` is an EXPLICIT caller-owned repeat edge. Ordinary media queries cannot
alter epoch, create loops or append a corridor. Original React detects loops
from backward indices to0/1; that heuristic is not presented as a general
player-discontinuity policy. Public host event wiring is not added while the
theme is internal. This avoids guessing loop versus user scrub from query order.

## Independent original controls

`generate-diorama-loop.mjs` extracts the original orchestrator branch verbatim,
with refs/sequencer inputs provided; its console is muted only. Original
sequencer/helpers and the already hashed original camera blocks run unchanged.
No original function is replaced with native output. Original normal outgoing
60Hz references lead to the original same-corridor flight. Two seeds, two
widths, zero/nonzero origins, three lines and eight observations per continuous
60Hz trajectory:32 frames, state/rendered peak `8.739675649849232e-13` at1e-9.
All original scenarios retain one segment/same frames/origin/globalStart and
increment epoch2→3. This finite fixture uses a declared12-world-unit track width,
not claimed actual font geometry. Adaptive media residual `.07429877479374625`
world units remains; no raw-RAF equality claim or gate relaxation.

`generate-diorama-loop-window.mjs` calls actual ORIGINAL mounted/visible/
neighbor-material functions with real Three objects.26 null/overlap/tie/end
windows,436 material rows, two viewports and two cameras. All source index/
departing flags exact; fit/opacity/color/center/view-depth peak
`1.4210854715202004e-14` at1e-9. Synthetic declared advance values isolate the
layout/material operator; they are not screenshots of a full mounted source.
Original records and source/harness/lock hashes live in separate `loop-v1` and
`loop-window-v1` directories. Previous flight/follower fixtures are unchanged.

## Real core / lifecycle and remaining differences

Actual published three-line core drawing checks text/raster/subject identity,
nonzero world origin retained, same outgoing refs, live line changes, source
callback/click inverse, cancellation, 24/60/144 query scheduling independence,
pause pixel freeze, arbitrary reverse seek, reduced motion, offset once,
resize restoration, unmount/remount and no reprepare on media/settings changes.
Window is present before3.2 and absent at/after3.2 exactly. Preparation is
caller-owned; no new job/ticker/global cache or reference to old playback added.

`native-contact.png` and `validation.json` retain ten actual core pictures/hash
records, with outgoing text present at time0. Contact visually inspected: strong
cropping/enlargement/overlap occurs during return. A same-font/geometry COMPLETE
original loop raster comparison has NOT yet isolated source behavior from the
adaptive media/raster difference. These pictures establish integrated drawing,
not visual equivalence or final readability. Do not hide those failures by
moving the text or changing the semantic3.2s boundary.

15 targeted JVM tests pass, existing offset/core/camera and normal neighbor
stage included. Android library/Desktop sample compilation pass. No new real
window, audio, Android device or performance result. First source probe lacked
required `words:[]` (`.work/diorama-loop-source-first.log`); corrected source
model input only before fixture creation. First real-core loop exposed exact
offset boundary selection; fixed separately in f7c2ecc, source data unchanged.
Initial material test field-name typo/compile failure retained in
`.work/diorama-loop-window-validation.log`; second log passes, no test disabled.

```
node verification/classic-browser/generate-diorama-loop.mjs
node verification/classic-browser/generate-diorama-loop-window.mjs
./gradlew :library:jvmTest --tests '*DioramaLoopTest' --tests '*DioramaCameraFlightTest' --tests '*DioramaCameraTrackTest' --tests '*DioramaOffsetBoundaryTest' --tests '*DioramaTextPlaybackTest' --tests '*DioramaNeighborStageTest' :library:compileAndroidMain :sample:desktop-app:compileKotlin
python verification/render-diorama-loop-contact.py
```

Exporters refuse frozen fixture overwrite; preserve old data before auditing.
No browser/device is needed by numeric export. Full cross-song departing text,
late-ready/reprocessed-lyrics scene updates, interruption orchestration, depth,
whole-song memory and device/readability remain pending. Seven public previews
remain unchanged. Fume/Cappella existing blockers have not been repaired by this.
