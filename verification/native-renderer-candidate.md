# Local Maven release candidate

The Android coordinates use group `top.ghhccghk.accompanist` and the same
version. An Android application can use `lyrics-ui-folia-android` by itself.
To opt in, add `lyrics-ui-folia-native-android` and explicitly select a native
backend for Partita or Diorama. The core remains the default. The optional
AAR declares minSdk 26, packages `arm64-v8a` and `x86_64`, and needs a usable
Vulkan surface adapter. Consumers build with the Android SDK and JDK. They do
not run Cargo or need the NDK. NativePreferred falls back to Kotlin on an
unavailable runtime or recoverable renderer failure. NativeRequired surfaces
the failure. A process abort remains outside this recovery boundary.

## Producer and consumers

| Version | Product source | Evidence directory under the November 19 review |
| --- | --- | --- |
| `0.1.0-rc.1` | `71593c24e0f75c5a929bde8b2f8879a2f352ae5d` | `local-candidate` |
| `0.1.0-rc.2` | `8e38cfddd6c87cd328057aefa61b97e5c6b14c04` | `local-candidate-rc2` |

The product source is identical between these commits. The later changes cover
verification fixtures. Both versions use Rust 1.91.1 and NDK 28.2.13676358.
RC.2 producer publication ran with `--no-build-cache --rerun-tasks` and executed
all 76 tasks, including both Kotlin Android compilation tasks, both Rust target
builds and both Android AAR bundles. The Gradle producer uses SDK 37 and the
local Java 25 daemon with the configured Kotlin compiler toolchain.

For RC.1, both independent consumers passed Release/R8 instrumentation on
API 35 x86_64 with `PAGE_SIZE=16384`. The native consumer passed the complete
Partita lifecycle and the test-only post-submit fault probe. The first
Kotlin-only run encountered an emulator system-process restart. The first
fault probe encountered a System UI ANR window covering the activity. Serial
reruns passed after the emulator recovered and the window was dismissed.

RC.2 also passed both Release/R8 probes on the API 35 x86_64 16 KiB image.
The already-built APKs were installed and tested serially. The records are
`core-app-api35-runtime.log` and `app-api35-runtime.log`, with the SDK and
page size in `device-api35.txt`. The native probe includes the 2160×2400
Partita transition that previously exhausted the scratch budget.

| RC.2 file | SHA-256 |
| --- | --- |
| Kotlin Android AAR | `f6481f3e6064d7fa10c51ddb30c0ad56acd5ab8847a4fd102a6e3948e3901cec` |
| Native Android AAR | `542db2846417207d6c23723561032cd0a6756cd92512503b50f4d260c5a2d943` |
| Native symbols | `26c72aaf1dcdd3fb6fefa46bf3d64dce7f696b5ef89e414378c3f3cee3dea1d2` |
| Rust sources | `d1ab55d8faafde2db40258300f6792a542d39805dc2ff0fab6e316afb6a420bb` |
| Cargo.lock | `6b16724c504fe8955afbd81efbec2f8f97173257836b98a4a703b73f29d4509a` |
| Kotlin-only Release APK | `4ce0dc228dec285fdbb9970b99748bd414699b562f21074c0e29de458458dee1` |
| Native Release APK | `dce3e1ec03112a05821102551b53ab59a453869291280e3ab594fbd0d2d56aaf` |

The publication uses build ID `bfb7e08ae6102a47227147aac2089f04a2abd5f7`
for ARM64 and `f024d80822069907be559f667f8351e59775d1aa` for x86_64.
`package-evidence.json` records these IDs and the remaining hashes.

The paired package checker reads the Kotlin AAR, its nested JARs, the common
metadata and JVM JARs, and the POMs and Gradle metadata. It verifies the core
contains no native runtime or native Folia dependency and the core-only APK
omits the Folia shared library. Other dependency runtimes can still be present
in the APK, including `libandroidx.graphics.path.so`. It checks packaged ELF
bytes, runtime/symbol build IDs, ELF and APK 16 KiB alignment, and hashes the
AARs, manifests and classifiers. The Rust source archive and AAR include
checksummed license texts for 63 Android crates.

## Source rebuild and release boundary

The contents of the two `rust-sources` archives match byte for byte across
all 217 files after extraction. The archived Rust sources were rebuilt outside
the producer checkout for both target ABIs. The first offline invocation with
only the 63 Android crate archives failed due to missing `windows v0.58.0`.
Cargo's resolver on the Windows host needed nine additional crate archives.
After adding the nine from crates.io and verifying their checksums against
Cargo.lock, the locked offline build succeeded. Both rebuilt ELFs meet 16 KiB
alignment. Their hashes and build IDs differ from the publication. The
publication's unstripped symbols match its own runtime libraries.

| ABI | Rebuilt ELF build ID | Rebuilt ELF SHA-256 |
| --- | --- | --- |
| `arm64-v8a` | `48b42bdcf0f4076044f551d288602af15c971505` | `b7093e7a629e40ec0e9bd9f53b726743bc634940f43f557dbc8f11ec2ece05fe` |
| `x86_64` | `e1a694c2618c8cbfa1bdf4af0b666aecd17b8d94` | `394f0e13fdb431e4f56ae0d10eb9f82162dc54650c90029ae7e0c314f3d6e1cf` |

A complete AAR rebuild also uses the producer checkout's core and native
Kotlin modules, Gradle settings, wrapper, version catalog and build scripts.
The archive rebuild above covers Rust. The displayed AARs came from the
producer builds listed in the table.

This candidate targets an ordinary file-backed Maven repository. A Central
submission would need the maintainers to confirm the project URL, SCM and
maintainer metadata, namespace ownership and signing key. Applicable API
documentation artifacts, signatures and a Central validation run remain
outstanding. The candidate stayed local.

## Repeat the package check

Use a new output directory and version for another producer build. Use a
task-owned emulator for the optional runtime probe. On Windows, with
`JAVA_HOME`, `ANDROID_HOME`, the pinned NDK and the Rust toolchain configured:

```powershell
$version = '0.1.0-rc.2'
$repo = '<new local Maven directory>'
$work = '<external consumer directory>'
.\gradlew.bat --no-daemon --no-build-cache --rerun-tasks "-PfoliaLocalVersion=$version" "-PfoliaLocalRepository=$repo" :library:publishAllPublicationsToVerificationRepository :native-renderer-android:publishAllPublicationsToVerificationRepository
python scripts/verify-native-consumer.py --sdk $env:ANDROID_HOME --java-home $env:JAVA_HOME --repository $repo --version $version --work $work --core-only --serial emulator-5580
python scripts/verify-native-consumer.py --sdk $env:ANDROID_HOME --java-home $env:JAVA_HOME --repository $repo --version $version --work $work --serial emulator-5580
python scripts/verify-renderer-package.py --repository $repo --version $version --readelf <llvm-readelf.exe> --zipalign <zipalign.exe> --apk <native-release.apk> --core-apk <core-only-release.apk>
```

Use a separate consumer directory with `--availability session-fault` to
include the probe's R8 rule. For the API 26, 28 and 29 missing-runtime control,
use `--availability absent`. On a supported device, `--availability present`
requires a frame submission. The same-process Vulkan ownership regression
runs with `scripts/test-renderer-android.ps1 -Sdk <sdk> -Serial <emulator>`.
