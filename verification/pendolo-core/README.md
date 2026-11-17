# Published lyrics-core to Pendolo model bridge

`PendoloCoreAdapter.kt` consumes this repository's actual lyrics-core0.5.0
normalized model and preserves the original `ISyncedLine` instance/SourceRef.
No original lyrics-ui dependency, source mutation, invented callback object,
classic text grouping or audio clock is introduced. Subtitle fields map to
translation/romanization; explicit chorus indices remain opt-in.

Each parser fragment retains its time. Only boundaries cutting an EGC are
coalesced into a display word, with original pieces retained as nested syllables.
The existing Pendolo timing mapper reconciles those pieces against intact
graphemes. Combining-mark and ZWJ controls check both nested source clocks and
display EGC clocks. Complete shaped rows remain the renderer's responsibility.

Declared source start/end remain separate from fragment-expanded normalization
diagnostics and the original render-polish end. Shared normalizer ordering and
duplicate/unsupported-line diagnostics are preserved, not replaced. Inputs
without word timing keep an empty word list, allowing original Pendolo's even
line-grapheme interpolation; this deliberately differs from Cadenza's instant
line-only adaptation. No artificial timed word is added.

`PendoloRenderHints.kt` ports original `utils/lyrics/renderHints.ts` at
cc6226e464050a456a2913e062374f23d3844cc5 (AGPL-3.0-only).56 actual-source cases
cover short/micro/normal thresholds at multiple offsets. Initial integer-us
reuse failed:1.18-1.0 is below.18 in source double arithmetic, selecting fast
and renderEnd1.2496s, while integer classification gave1.42s. The original
fixture/expectation was retained; the production bridge now uses original
seconds arithmetic. Failure log `.work/pendolo-core-integer-hint-failure.log`.

Tests use actual published `SyncedLine`, `MainKaraokeLine`, `KaraokeSyllable` and
`SyncedLyrics`, checking identity, subtitles, source immutability, empty inputs,
out-of-line diagnostics, nested raw pieces, display timing and raw/render bounds.
Three JVM logic tests, Android APK assembly and Desktop compile pass:
`.work/pendolo-core-adapter-final.log`. No new UI was added, so no new image or
device acceptance is claimed. This is an internal bridge; FoliaLyricsView's
public Pendolo branch, typography and transformed interactions are still pending.

```
node verification/classic-browser/generate-pendolo-core-render.mjs
./gradlew :library:jvmTest --tests '*PendoloCoreAdapterTest*'
```
