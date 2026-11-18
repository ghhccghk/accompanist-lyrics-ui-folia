# Bounded two-corridor text cut — 2026-11-18

Internal Diorama only; seven public calibration previews are unchanged. This
implements the actual departing plain-text window, not a new isolated target or
a public theme. Full sequencer, depth/occlusion and visual acceptance remain open.

## Source and fixed comparison

Folia v0.7.26 / cc6226e464050a456a2913e062374f23d3844cc5,
AGPL-3.0-only. Original VisualizerDiorama.tsx:350–455 captures the old frame,
offsets a new corridor and retains a global outgoing index. Original
dioramaSceneLayout.ts:26–78 and sceneFrame.ts:103–131 supply the sorted window,
nearest-cluster tie, colour, fit and distance-opacity rules. Source checkout is
unchanged. Exact source fragment/function/host/font/lock hashes and source states
are recorded in `../diorama-fixtures/cut-stage-v1/scene.json`.

Original sequencer: eight synthetic outgoing lines at origin, seed Outgoing
mirror; original normal camera follows 0→6.8s (line4). New three-line corridor
uses seed Incoming mirror, original frame4 + pickTransitionOffset(seed,1), global
start8. Each line is `Synthetic outgoing/incoming line i`, starts700+i*1400ms,
ends1500+i*1400ms. Published-core SyncedLine/no words and original words:[] use
line-level timing fallback, not genuine word annotations. Same existing OFL
Latin Bold,128px em/700,DPR1,380/800×600,colours,orbit override and settings.
Actual Motion13.4.3/dom13.4.2/utils13.3.0,Chromium151.0.7922.34,Three0.185.1.

60/144 finite schedules include exact probes 0,.7,1.6,2.099,2.1,3.199,3.2,
3.201,3.499,3.5,4.5,4.6s:48 original frames. Source original text raster/layout,
window/material functions and three-layer Three composition run with verbatim
extracted CameraRig operators. Host explicitly supplies the new-song edge and
resets local incoming clock; outgoing window uses controlled media[0,3.2).
Existing analytic unit charges/delta0/breath1/audio0 are injected. No fog,
background, theme damping, real React/R3F/RAF envelope or device claim.

Native independently prepares real core, shaped geometry and targets. Its OWN
outgoing finite trajectory seeds its OWN incoming flight; original output poses,
rasters and PNGs never drive native. Adaptive media camera separately captures
its own outgoing state, including its prior reconstruction residual.

## Implemented and verified

`cutAt` captures value-only refs plus <=6 shared immutable PLAIN rasters:
outgoing-2/+1 and last two old lines required by incoming live-2 window. It
retains no old core objects, active/glow rasters, playback, camera track or job.
Snapshots cannot recursively chain. The original global index shift is preserved
for windows/materials and old/new transparent centre-depth order; active source
identity/click inverse remains incoming core. Plain rasters are reused, not copied.

At this matrix's 3.2s, current is global9: live window retains old7, not old6.
At3.5s/current10, old tail is gone. Six bounded immutable old rasters remain owned
for deterministic reverse seek, and become unreachable when their owner is
dropped; no global cache/ticker/timeout. This is an ownership bound, not measured
GPU/native-memory reclamation evidence. At the next eligible cut only the active
corridor's necessary plain planes are retained. Interrupted three-visible-corridor
cuts fail explicitly; full sequencer/gate/pruning/font reconfiguration is pending.

48 original pose controls: peak1.4210854715202004e-14 against unchanged1e-9 gate.
244 original neighbour rows: exact indices/cluster flags/draw order; fit, colour,
centre, opacity and depth peak2.842170943040401e-14 against1e-9. No missing nodes.
Adaptive media position residual .11002510718601854world units and orientation
1-|dot| .0000678149073041201 remain; these include outgoing history, not local-only.

Same source/own-finite/own-media ALL-nonbackground union, including lowest alpha;
no registration, delay, rejected frames or alpha cut:

| Method | Mean foreground MAE | Worst image | Worst64px |
| --- | ---: | ---: | ---: |
| Own finite | 6.331865 | 21.549652 | 23.651852 |
| Own adaptive media | 20.047587 | 60.231635 | 97.018091 |

No blank frames. Edge-touch source/finite/media41/41/40. Fixed contact inspected:
departing row4 stays visible at0, old tail7 appears during flight, incoming text
emerges with strong oblique angles and later crop. Source also crops these inputs;
native media/raster differences remain and readability is NOT accepted. This is
one new matrix/two methods, not a before/after improvement or an acceptance pass.

Pause/reverse seek/24,60,144 query independence, exact offset, reduced motion,
resize restoration, unmount/remount, original refs, cancelled preparation and
third-corridor rejection are tested. First test incorrectly guessed old6 remained
at3.2s; independent original fixture disproved it. Original TSV/PNG unchanged;
failed log/XML preserved `.work/diorama-cut-first*`. Boundary assertion now reads
the original window. Bounded-window coverage includes old songs1..128/10000 lines
without preparing giant songs. No font-change/late-ready/whole-scene acceptance.

Android library and Desktop sample compile. Targeted JVM stage plus shared
loop/core/offset and public Fume/Cappella/Pendolo regressions pass. Existing80
loop-stage native PNG hashes are identical after draw-stage refactor. No new
real Desktop window/audio/Android device/performance evidence.

```
node verification/classic-browser/capture-diorama-cut-stage.mjs
./gradlew :library:jvmTest --tests '*DioramaCutStageTest'
python verification/compare-diorama-flight.py --cut
```

Exporter refuses frozen source overwrite; owned browser/server close in finally.
Raw frames stay ignored in build/verification. Do not replace the existing loop
oracle or rerun its complete source capture to measure this cut.
