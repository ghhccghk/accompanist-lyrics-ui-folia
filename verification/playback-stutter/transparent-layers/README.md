# Exact-zero opacity culling — 2026-11-16

The captured Partita path recorded/filtered child layers even when their group
opacity was exactly zero. ClassicLayer now skips that record block and drawing,
clears software pixels and invalidates the previously recorded frame. Nonzero
opacity, kernels, shaping, rasterization, media time and layout remain unchanged.
Call-site review: media snapshot, hit geometry and semantic callbacks are outside
record blocks; nested drawing parameters are recomputed when visible again.

Windows x64, JDK21, offscreen Compose/Skia:

- Frozen 9f47417 recorder versus current: 120 nested opacity/backend/sigma/size
  and reverse-revisit cases, exact ARGB equality. Includes .0001 opacity, which
  still paints. Hidden descendants execute zero paint callbacks.
- Freeze the production ClassicLayer source at 9f47417, run 30 existing Partita
  scenes plus classic native/software and narrow subtitle native/software, then
  restore the candidate and rerun the identical selections. All **528 newly
  generated PNGs** identical. Existing wide subtitle files excluded from report.
  Original fixture geometry, pause and reverse-seek assertions pass. Digests in
  scene-comparison.json; representative synthetic screenshot partita-1150ms.png.
- Software zero-alpha 640x320 / sigma .25: work.csv compares the actual frozen
  recorder and current recorder, 4 warmups and 8 measured calls each. JVM managed
  allocation excludes bitmap/native storage; timings are one isolated run under
  uncontrolled host load, not device playback or whole-frame performance.
- Existing CSS/native/software blur spread checks and Android/JVM shared sample
  compilation pass. No installed APK changed, no Android runtime check this round.

pixels.csv records callback counts and image equality; work.csv records allocation
and timing. The source and timing are synthetic, unrelated to the user's lyrics.
checks.json records commands/log names and limitations. The temporary source
substitution restored the candidate in a finally block; no Git history changed.

Reproduce focused checks:

```sh
./gradlew :library:jvmTest --tests '*TransparentLayerTest*' --tests '*NativeBlurParityTest*'
```

The long scene pair passed at both checkpoints (7m33s / 9m18s test task wall
times). These task times are not evidence of speedup; they include raster
capture, waits, JIT and GC. The benefit established here is avoided hidden work
with unchanged pixels. Visible nested software drawing and synchronous mask
preparation remain unresolved; the sampled ~967ms playback stall is not claimed
fixed.
