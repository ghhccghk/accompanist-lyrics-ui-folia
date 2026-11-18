# Sample theme options — 2026-11-18

Seven existing public calibration previews now use a wrapping Material filter-chip
selector with Chinese + implementation names and selection semantics:
流光 Classic, 云阶 Partita, 倾诉 Tilt, 心象 Cadenza, 时计 Pendolo,
群唱 Cappella, 浮名 Fume. One shared controls component serves both real-playback
settings and the development/demo screen. Diorama is described as internal and
unavailable; it is not a selectable or fake theme. Enum persistence keys and
renderer mapping are unchanged; changing a theme does not start demo playback.

`ThemeOptionsTest` exercises all seven real renderer mappings at 320/900×700,
visible selection, exclusive selected state, Chinese/English labels, unchanged
playback snapshot and no placeholder Diorama entry. `SettingsTest` and existing
Pendolo/Cappella/Fume full-sample UI tests also pass: 17 tests in total.
Android shared compilation and Desktop app compilation pass. These are JVM
offscreen checks and compilation, not a new device/native-window/audio or
upstream theme visual acceptance claim.

`contact.png` contains the upper 230/150 pixels of the two raw menu captures
(no rescaling or pixel edits). Full raw captures stay in ignored
`build/verification/theme-options/`. Both widths and the actual dark full-sample
settings dialog were inspected. Initial isolated capture omitted a Material
Surface (black background); the test host was corrected before evidence capture.
Initial Gradle command used the nonexistent Desktop `compileKotlinJvm` task;
the corrected task is `compileKotlin`. Failure logs remain in `.work`.

```powershell
.\gradlew.bat :sample:shared:jvmTest --tests '*ThemeOptionsTest' --tests '*SettingsTest' --tests '*PendoloSampleTest' --tests '*CappellaSampleTest' --tests '*FumeSampleTest' :sample:shared:compileAndroidMain :sample:desktop-app:compileKotlin
```
