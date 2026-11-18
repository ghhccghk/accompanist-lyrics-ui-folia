# Actual mounted Diorama text / camera — 2026-11-18

Unlike the isolated orbit matrices, this mounts the untouched original
**CameraRig and DioramaScene** React/R3F components together. It is not the
VisualizerDiorama parent, readiness gate, timeout or complete background theme.
Seven public previews unchanged; Diorama remains internal and unaccepted.

## Reproducible conditions and seams

Pinned Folia cc6226e464050a456a2913e062374f23d3844cc5 / AGPL-3.0-only.
Eight outgoing / three incoming synthetic line-level lyrics, integer-ms timings,
seeds, font, colours, width380/800,height600,DPR1 and cut at outgoing6.8s are
the same inputs as `../diorama-cut-stage`. This matrix instead uses the ORIGINAL
default weighted shot picker, real original active envelopes/breath, native
line-raster cache, fog, R3F renderer defaults and actual component callback order.
Do not compare its mean with the isolated-orbit mean as an improvement.

Host supplies a ready-song append edge, global read head, media time reset to0,
and wall time continuing6.8+t. R3F frameloop=never / advance supplies each real
useFrame interval. One pending original cache RAF batch runs per turn, under a
React flush; new callbacks wait for the next turn. No source hook/ref/target,
envelope, breath or material values are overwritten. Passive Fiber/ref inspection
reads the original values. Both60/144 schedules include the same12 exact incoming
probes:48 PNGs and4686 complete camera callback records, no omitted intervals.
No real-browser RAF cadence or full parent orchestration acceptance is claimed.

Original source checkout remains clean. Motion13.4.3/dom13.4.2/utils13.3.0 and
R3F9.8.1 plus **every direct runtime dependency** match the original lock; source,
host, lock, font hashes and actual versions are recorded in
`../diorama-fixtures/mounted-cut-v1/scene.json`. Three0.185.1,React19.3.0,
Chromium151.0.7922.34. R3F only added to reference tooling, not the library/sample.
Private provisional diagnostic used newer Babel/Zustand before the mismatch was
found; it was not promoted. Scoped overrides pin7.29.7/5.0.15 before final capture.

Dedicated server4188/cache and one sequential headless browser; owned resources
close in finally. Source unmount checks every condition: R3F roots0/cache RAFs0.
This is original browser teardown, not Android/native GPU memory measurement.

## Findings and controls

1. CameraRig runs BEFORE DioramaScene. The camera reads the previously published
   active width; text then publishes the new measured width. On the first incoming
   frame this is the old corridor's width; at a line change it is the previous line.
   Pre/post widths and every integration interval are retained. No fixed-frame lag
   was added to the production media contract to imitate this source scheduling.
2. Real R3F default `toneMapping=4` (ACESFilmic), output sRGB, fog12/30. Prior raw
   Three text probes used NoToneMapping. Native material colour encoding presently
   lacks that ACES step. This is a concrete remaining renderer difference, not a
   reason to relabel the old source oracle or silently change its renderer.
3. Real original Float32 light/soul envelopes reset on line changes and use actual
   callback delta; original breath uses continuous wall time. Native analytic
   media envelope and breath1 do not assert equivalence to that history.

Each actual mounted interval is also checked against the verbatim original
extracted operators with its independently observed incoming refs/width. Peak0.
Those pure checks never supply values to the original mounted components.

Native separately prepares published-core objects, full-shaped text, default
shots/targets and its OWN outgoing+incoming finite histories. The finite CONTROL
reproduces original callback order with its OWN prior measured width. All4686
original states/rendered poses pass1e-9; peak6.039613253960852e-14, width peak
8.881784197001252e-16. Source poses/widths are assertions, never renderer inputs.
Production adaptive media is compared independently; position residual up to
.1305875347511929world units (includes outgoing history/coupling, not local-only).
Finite camera success does not validate material or production-media equivalence.

Same source/own-finite-camera/own-media ALL-nonbackground union including lowest
alpha, no registration/phase shift/cutoff/excluded frames:

| Method (native analytic materials) | Mean foreground MAE | Worst image | Worst64 |
| --- | ---: | ---: | ---: |
| Own finite camera with actual source callback order | 7.251753 | 20.492229 | 24.617612 |
| Own adaptive media camera | 20.348141 | 50.141831 | 87.853968 |

No blank frames; edge-touch source/finite/media36/38/38. Fixed contact inspected:
plain departing text persists, incoming angled text emerges, source and native
both crop some views. Source/native material and cropping differences remain.
No pixel criterion was relaxed. Source fields/window/cache availability remain
visible in raw evidence rather than inferred from the images.

Two JVM stage tests pass, including media FPS independence, pause, reverse seek,
remount and original-source identity. First harness failures (duplicate bundled
Three caused `mat.color.copy` error; wrong passive ref index) and native initial
compile failure (fog requires encoded Color) remain in `.work/diorama-mounted-*`.
They were repaired in the harness/test, not upstream. All temporary browsers
closed on failure. No new Android build/runtime, audio, real Desktop window or
performance conclusion; production is unchanged from c4d5680.

```
node verification/classic-browser/capture-diorama-mounted.mjs
./gradlew :library:jvmTest --tests '*DioramaMountedStageTest'
python verification/compare-diorama-flight.py --mounted
```

Exporter refuses frozen fixture overwrite. `steps.tsv` retains every original
camera state/delta/coupling input; raw duplicated material trees/PNGs stay ignored
under build/verification. Fix ACES from actual shader controls next, using this
same frozen matrix and preserving its before images/report. Do not inject a
fixed UI frame delay or substitute the isolated orbit stage for this evidence.
