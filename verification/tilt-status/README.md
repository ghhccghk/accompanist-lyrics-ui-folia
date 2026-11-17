# Tilt quality-change status regression

Native seven-theme window probe found a public status failure on switching
Standard -> Minimal while paused on the same prepared line. Requested/actual
quality changed, but mainUnitCount became0 indefinitely. A focused public-entry
test independently reproduces expected9 / actual0 before the fix. Failure XML/log
are preserved in ignored `.work/tilt-status-before-fix.{xml,log}`.

Cause: FoliaLyricsView starts a new status epoch when options.quality changes.
TiltLine keeps its correctly prepared shaping/raster and previously notified only
on a new prepared object. The repair republishes the existing count for the new
quality epoch, separately from the unchanged layout observer. It does not prepare
text again or modify source animation/layout/visual acceptance expectations.

`PublicTiltTest` now checks Minimal/Reduced/Standard/Minimal status and retains
the preexisting actual drawing, pause/seek/offset/resize/callback/450ms-tail checks.
All3 public Tilt tests pass after the fix; Android debug APK including the existing
sample Rust packaging builds successfully. Logs `.work/tilt-status-after-fix.log`
and `.work/tilt-status-android-build.log`. No Android runtime/device claim. This is
a host-status repair; original Folia has no FoliaStatus API, so no upstream visual
improvement is claimed. The separate native-window follow-up still needs accurate
async-preparation readiness before its narrow Fume frame can be accepted.

```
./gradlew :library:jvmTest --tests '*PublicTiltTest'
./gradlew :sample:android-app:assembleDebug
```
