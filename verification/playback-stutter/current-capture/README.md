# Current Android playback capture — 2026-11-16

Physical 2509FPN0BC / arm64 / API37; existing debuggable application, process
unchanged throughout. Installed APK SHA-256 is in summary.json; do not assume
it equals the latest source or prior tested APK. User confirmed Android was
playing. No install, tap, pause, seek, file access, screenshot or settings change.
Partita inferred from stacks; actual quality and Rust toggle were not captured.

Two valid 15-second simpleperf recordings, cpu-clock:u, 250Hz: 2,894 and 2,676
recorded samples, zero lost. Text export contains 2,890 and 2,675 stacks.
Initial cpu-clock attempt produced zero samples and is excluded. A subsequent
stat diagnostic reported restricted kernel samples; user-only recording worked
without changing system permissions. Do not infer the exact cause of the first
empty capture solely from that diagnostic.

Inclusive exported-stack fractions (overlap; not additive):

| Function | Capture 1 | Capture 2 |
| --- | ---: | ---: |
| SoftwareBlur.blurPremultiplied | 34.394% | 21.047% |
| ClassicLayer.record | 48.720% | 33.159% |
| GlowMask.blurredMasks | 10.692% | 10.206% |
| Main thread, all stacks | 91.799% | 89.533% |

These are different song intervals, not before/after measurements. Source has
unvalidated zero-opacity culling work in progress; it was not built or installed.

Capture 1's frame ring had expired before export: no corresponding frame
statistics claimed. Capture 2 collected six gfxinfo rings at roughly two-second
intervals during/after sampling. Deduplicate by intended/completed timestamps,
keep only fully contained records: 349 frames, P50 14.774ms, P95 20.072ms,
maximum 966.892ms intended-VSync-to-completion. All raw flags are 32; no
platform-specific interpretation or session jank/FPS percentage is asserted.
The ring collection began partway into sampling and is incomplete.

The longest retained frame overlaps 235 exported samples: 222 contain
ClassicLayer.record, 164 software blur, 223 Partita, 8 GC-named frames. This
supports investigating the nested software drawing/blur path; it is temporal
overlap, not exclusive wall-clock attribution. Other 174.643–188.421ms records
overlap 26–30 GlowMask.blurredMasks samples each. Some frames and their sample
windows overlap each other, so do not add those counts or durations.

PSS snapshots span 153,869–400,840KiB, RSS 302,360–550,324KiB (roughly
150–391MiB PSS / 295–537MiB RSS). Snapshots are not continuous peaks and
include different playback/preparation moments. No leak or improvement claim.

Next: remove provably invisible drawing work, then bound the nested software
fallback and move/bound synchronous mask preparation. Require same-pixel
pause/seek checks and controlled same-song/device A/B before claiming a fix.
Rust mask toggle does not accelerate SoftwareBlur or the whole drawing tree.

Raw traces, symbol reports and frame/memory exports remain in the ignored
`.work/stutter-*` capture directory. No lyrics, filenames or media are committed.
summary.json contains the sanitized CPU/frame/memory aggregates;
long-frame-overlap.json contains relative timestamps and stack counts only.

Recording command (use a distinct local output for each run):

```sh
adb shell simpleperf record --app top.ghhccghk.accompanist.folia.sample \
  -e cpu-clock:u -f 250 -g --duration 15 -o /data/local/tmp/folia-capture.data
adb shell simpleperf report-sample -i /data/local/tmp/folia-capture.data --show-callchain
adb shell dumpsys gfxinfo top.ghhccghk.accompanist.folia.sample framestats
adb shell dumpsys meminfo top.ghhccghk.accompanist.folia.sample
```

Collect gfxinfo repeatedly while recording. Do not use a much later overwritten
ring or historical gfxinfo histogram as corresponding frame evidence.
