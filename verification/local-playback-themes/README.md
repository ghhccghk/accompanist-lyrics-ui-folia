# Real local playback and seven public previews

2026-11-18; base 51bdced. This is a sample integration repair, not a Fume camera
parity change. The explicit `verifyLocalPlaybackThemes` task opens the production
Java Sound `DesktopAudioPlayer` with an original 12s silent PCM WAV. The complete
sample uses actual core0.5.0 LRC parsing, translation attachment, transport and
theme controls. The UI is Compose **offscreen**, 1100x740. Native file dialogs,
audible physical output and Android runtime are not certified by this probe.

## Fault and narrow repair

Published `lyrics-core-0.5.0-sources.jar`,
`commonMain/com/mocharealm/accompanist/lyrics/core/parser/EnhancedLrcParser.kt:328–329`
sets an open final line's `end` to `Int.MAX_VALUE`. Actual parsing in
`OpenLrcEndTest` verifies this source behavior. The unchanged final 10s line
therefore reached Fume as a 24.8-day interval. Camera preparation failed with
`Fume continuous camera failed to converge` rather than showing lyrics.

`resolveOpenLrcEnds` creates a sample-owned display copy once per document and
host duration. ONLY open `SyncedLine` ends are resolved; start times, text,
translation/phonetics, explicit ends, karaoke syllables and untouched line
identity remain intact. `SongSession` keeps the raw parsed object. Real playback
uses the native duration; demo imports use their already-derived finite tail.
No parser, library timing contract, camera solver, threshold or original oracle
was changed. A line starting beyond the host duration receives a zero-length end
at its own start, not an inverted interval. This does not invent word annotation.

The pre-fix failure artifacts remain ignored at
`build/verification/local-playback-themes/Fume-not-ready.{png,txt}` and
`.work/local-playback-themes-fume-diagnostic.log`. Earlier harness hover/scroll
failures and the unbounded probe source are retained in `.work`; not product
parity failures. The test neutralizes pointer hover for full-image comparisons
and enables UI-clock advancement only for paused theme-selector scrolling.

## Results and reproduction

`result.json` retains conditions, native media events, input parser times and all
seven paused/restored PNG hashes. Actual native position advances with UI time
decoupled, freezes on pause, increments discontinuity on seek, and restores exact
full-sample pixels after forward/backward seek for all seven previews. No-lyrics
playback, translation attachment without replacing the player, cancellation and
closing the Clip also pass. The preview clock stays at zero and paused throughout.
All seven contact images were inspected; Partita/right-edge text and Fume camera
framing still have visible clipping. This task does not waive their source parity
and readability requirements. No user media is read or committed.

```
./gradlew :sample:shared:verifyLocalPlaybackThemes
./gradlew :sample:shared:jvmTest --tests '*OpenLrcEndTest' --tests '*ImportTest' --tests '*LocalSongTest' :sample:shared:compileAndroidMain :sample:desktop-app:compileKotlin
```

Windows uses `gradlew.bat`. Explicit audio-device task has a two-minute timeout
and releases its own session in `finally`; it is not in ordinary `jvmTest`.
Logs: `.work/local-playback-themes-bounded-end.log`,
`.work/open-lrc-end-verified.log`. Generated media, PNGs/contact and diagnostics
stay under ignored `build/verification/local-playback-themes`. This adds no new
visible-window, physical-device, upstream visual or performance pass.
