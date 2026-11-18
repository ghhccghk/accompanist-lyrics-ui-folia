# Diorama internal bridge follows the existing public offset contract

This fixes the internal core text playback bridge; Diorama remains unpublished.
There is no new upstream visual-parity, device or performance claim.

The independent authority is the existing `docs/DESIGN.md` media-time contract
(`lyricTimeUs = positionMillis*1000 - offsetMillis*1000`) and BOTH existing sample
click handlers (`line.start + options.offsetMillis`). All seven public themes
already follow that sign. The original Folia text component receives lyric time
from its host, so its camera/word formulas and source fixtures are unchanged.

`DioramaTextPlayback.lyricTime` mistakenly used media+offset; `mediaStart` used
lineStart-offset. Its earlier internal self-test repeated this reverse sign and
therefore did not establish compatibility with the public host.

New `DioramaOffsetContractTest` was run BEFORE modifying production. It fails
at media1.7/offset+200ms: the second source object was returned400ms early relative
to the required boundary2.1s. Log/XML remain in
`.work/diorama-offset-contract-before.{log,xml}`. The old self-test source is
preserved in `.work/DioramaTextPlaybackTest-before-offset.kt`.

| input | required lyric time | required source |
| --- | ---: | --- |
|media1.7s, offset+200ms|1.5s|first, start.7s|
|media1.7s, offset-200ms|1.9s|second, start1.9s|
|media2.1s, offset+200ms|1.9s|second, original object|
|click second, offset+200ms|media2.1s|inverse returns lyric1.9s|
|draw lyric1s, offset+200ms|media1.2s|same complete pixels as offset0/media1s|

The minimal repair changes those two signs. Only the bounded camera input is
clamped to[0,endTime]; source timestamps and returned media click targets remain
unmodified. The old self-test's reverse-direction assertions were corrected
with this independent migration reason; no upstream fixture, image or numeric
precision gate changed, and no new public convention was introduced.

Tests cover positive/negative/zero offsets,60 independent media samples and
source identities across lyric starts/gaps/tail, inverse click round trips,
complete actual-core text+camera pixel equality across five offsets, reduced
motion, pause, no reprepare, and the existing resize/remount/reverse-seek bridge.
8 targeted JVM checks pass, including camera-track and public Tilt cross checks;
Android library/Desktop compilation pass. Log
`.work/diorama-offset-contract-after.log`. No device or true-window run here.
Since every original Diorama oracle uses offset0, no source matrix was needlessly
recaptured; their numerical/visual residuals remain, not inherited as acceptance.

```
./gradlew :library:jvmTest --tests '*DioramaOffsetContractTest' --tests '*DioramaTextPlaybackTest' --tests '*DioramaCameraTrackTest' --tests '*PublicTiltTest' --tests '*TiltSubtitleStageTest' :library:compileAndroidMain :sample:desktop-app:compileKotlin
```
