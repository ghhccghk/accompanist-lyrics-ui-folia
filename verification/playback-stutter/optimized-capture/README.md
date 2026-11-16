# Optimized APK: real-song recapture — 2026-11-16

User connected the physical device with a song already configured and authorized
capture. Device 2509FPN0BC / arm64 / API37. Installed APK SHA-256:
`f548962a7213029486110799ab4ec5f76d3e196d8c0d4f49f3803b0559f4934f`.
This exactly matches the APK recorded in `../cropped-masks/checks.json`, built
with zero-opacity culling and bounded Kotlin glow preparation. Current source
commit: `8a45ecb`. No installation, taps, pause, seek, settings changes, screenshots
or media-file reads were performed. The existing process stayed alive throughout.

Two 15-second `cpu-clock:u`, 250Hz, callchain captures; gfxinfo frame rings every
approximately two seconds concurrently, four memory snapshots per capture.

| Measurement | Capture 1 | Capture 2 |
| --- | ---: | ---: |
| Recorded samples / lost | 2682 / 0 | 2708 / 0 |
| Exported stacks | 2677 | 2696 |
| Main-thread stacks | 89.578% | 86.165% |
| ClassicLayer.record, inclusive | 39.484% | 39.874% |
| SoftwareBlur.blurPremultiplied, inclusive | 19.612% | 20.030% |
| GlowMask.blurredMasks, inclusive | 5.603% | 4.933% |
| neon::bilerp, inclusive | 8.181% | 9.496% |
| Retained frame records | 635 | 628 |
| Intended-VSync-to-completion P50 | 13.000ms | 13.317ms |
| Intended-VSync-to-completion P95 | 50.517ms | 26.752ms |
| Longest retained frame | 602.520ms | 1059.384ms |
| Retained records exceeding 100ms | 30 | 22 |

Inclusive stack percentages overlap and must not be added. Frame rings are
deduplicated by intended/completed timestamps and restricted to records fully
inside the first-to-last exported sample interval. All retained flags are 32,
without assigning platform-specific jank meaning. Rings remain incomplete;
this is neither a session FPS nor a complete dropped-frame measurement.

Capture 1's longest frame overlaps 140 exported samples containing layer record,
78 software blur, 36 bilerp, zero glow preparation and zero GC-named stacks.
Capture 2's longest frame overlaps 201 layer record, 89 software blur, 72 bilerp,
31 glow preparation and zero GC-named stacks. This is temporal overlap, not
exclusive wall-time attribution. Adjacent long records may overlap each other;
their sample counts and durations must not be summed.

Eight PSS snapshots span 295301–462923KiB (approximately 288–452MiB), RSS
442448–592772KiB (432–579MiB). Not continuous peaks, allocation measurements or
evidence of a memory leak. No same-interval old-APK run was taken, so these data
cannot establish a memory reduction or an optimization speedup against earlier
captures. The application is debuggable; sampling and concurrent dumps add load.

Partita/云阶 is inferred from the stacks. Quality and runtime Rust selection
were not captured. Zero observed Rust-named stacks does not establish that the
toggle was off or that the packaged library was unavailable. The full rendering
tree and colour-blur fallback are outside that optional alpha-mask processor.

## Conclusion and next target

The optimized APK still has severe long frames. Visible nested software recording,
premultiplied blur and transformed bitmap sampling remain priority targets;
bounded glow preparation and hidden-work removal alone are insufficient.
Inspect the positive-opacity small-sigma fallback and descendant raster sizes,
then validate an actual fix with unchanged pixels/pause/seek and device captures.
Do not lower blur, opacity or raster quality as a substitute for the fix.

## Evidence and reproduction

`capture-1.json` / `capture-2.json`: sanitized aggregates; corresponding
`long-frames-*.json`: relative timestamps and overlapping stack counts only.
No song names, lyrics, media paths or device serials are committed.
Raw traces, full symbol exports, frame rings and memory snapshots are retained
in ignored `.work/stutter-retest-*` directories. Capture/analysis scripts are
retained under `.work/`; `analyze-capture.py` is a portable copy of the analysis.

```sh
adb shell simpleperf record --app top.ghhccghk.accompanist.folia.sample -e cpu-clock:u -f 250 -g --duration 15 -o /data/local/tmp/UNIQUE.data
adb shell simpleperf report-sample -i /data/local/tmp/UNIQUE.data --show-callchain
adb shell dumpsys gfxinfo top.ghhccghk.accompanist.folia.sample framestats
adb shell dumpsys meminfo top.ghhccghk.accompanist.folia.sample
python verification/playback-stutter/optimized-capture/analyze-capture.py .work/stutter-retest-CAPTURE
```

Take repeated frame rings *during* the CPU capture; do not use the cumulative
gfxinfo histogram as capture-specific results. The analysis expects the raw
directory layout produced by `.work/capture-stutter-retest.py`. Both real
captures were parsed successfully; no application code changed this round,
so no new application build or renderer visual-regression run was needed.
