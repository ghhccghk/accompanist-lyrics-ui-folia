# lyrics-ui-folia

Independent Kotlin / Compose Multiplatform lyrics project. License:
**AGPL-3.0-only**; provenance and version decision: [docs/upstream.md](docs/upstream.md).

## Implementation status

The design is in PLAN.md and docs/. Work has started with an independent Android
and Desktop scaffold. A scaffold is not acceptance of the planned classic effects.
See [docs/implementation.md](docs/implementation.md) for verified capabilities.

## Toolchain

Locked: Gradle 9.5.0, Kotlin 2.4.10, Compose 1.12.0, AGP 9.3.2,
lyrics-core 0.5.0, Activity Compose 1.13.0. The selected combination follows an
available stable reference combination and is verified in this independent build.
Use JDK 21 or newer supported by Gradle/AGP. Android SDK platform 37 is required.
Configure `ANDROID_HOME` or an untracked `local.properties` (`sdk.dir=...`).
No reference checkout, Rust, NDK, SPW or external player is needed.

```powershell
.\gradlew.bat :sample:desktop-app:run
.\gradlew.bat :sample:android-app:assembleDebug
.\gradlew.bat :library:jvmTest :sample:shared:jvmTest
```

Default sample: synthetic lyrics, demonstration clock, **no audio**.
The built APK is in `sample/android-app/build/outputs/apk/debug/`.
Android build success and device execution are recorded separately in the log.
No push, remote repository or public publishing is configured.
