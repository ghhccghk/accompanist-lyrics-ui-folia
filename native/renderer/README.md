# Optional Android Rust renderer (experimental)

This is a rendering-only crate. Android minSdk 26, arm64-v8a and x86_64; Vulkan
and RGBA32Float render attachments are required. GLES and Windows/JVM hosts are
not implemented. Kotlin shaping/layout and the existing Partita media samplers
prepare coverage. Rust runs actual blur/glow/group composition. Subtitles,
secondary lines and finite waiting dots still use Kotlin overlays. There is no
native player, SPW, alternate media clock or per-frame full-screen readback.

## Producer

Verified producer host: Windows. Install JDK 21, Android SDK with NDK
28.2.13676358, Python >=3.11 and Rust 1.91.1 with
`aarch64-linux-android,x86_64-linux-android` targets. Tool discovery may use a
project-local Rust installation; see `build-support/rust-environment.ps1` in the
source archive or `../experimental-mask/rust-environment.ps1` in the checkout.
Do not change the global PATH or upgrade unrelated Kotlin/Compose/Gradle tools.

From the repository root:

```powershell
./gradlew.bat :library:publishAllPublicationsToVerificationRepository :native-renderer-android:publishAllPublicationsToVerificationRepository -PfoliaExperimentalMask=false
./scripts/test-renderer-android.ps1 -Sdk <sdk> -Serial <task-owned-emulator>
python scripts/verify-native-consumer.py --sdk <sdk> --java-home <jdk> --serial <task-owned-emulator>
```

Normal builds use `--locked`. The first producer build needs access to the exact
crates in Cargo.lock; subsequent dependency archiving uses locked, offline Cargo
metadata for both ABIs. The archive step verifies every registry `.crate` against
Cargo.lock, retains actual license text, and fails closed if text is missing.
Several workspace crates omit license files in their crates.io tarballs; checked
upstream files from the exact `.cargo_vcs_info.json` revisions are retained in
`third-party-license-supplements/SOURCES.json`. hexf-parse declares CC0 in its
verified manifest; pinned SPDX standard text is supplied with that declaration.
No versions are inferred from a floating branch.

The Maven `rust-sources` ZIP contains this crate, WGSL, lock, build scripts, source
ledger, repository notices, dependency declarations/licenses and exact source
tarballs for all selected Android dependencies. `native-symbols` contains the
same build's libraries before `--strip-debug`, including line tables. It is a
diagnostic artifact; it is not bundled in the AAR. The AAR includes precompiled
runtime libraries, JNI keep rules and dependency license texts. Producer source
archives also need the corresponding repository Kotlin sources to rebuild the
whole AAR; the ordinary sources JAR supplies the wrapper, and the repository
commit supplies Gradle/core sources. See the source ledger and release evidence.

## Consumer

Consume `top.ghhccghk.accompanist:lyrics-ui-folia-native-android:0.1.0-SNAPSHOT`
from the explicitly configured local Maven repository for verification; this is
not a statement that the coordinate has been uploaded remotely. Consumers need
no Rust, Cargo, NDK build task, checkout, sample classes or local runtime path.
Use `FoliaNativeLyricsView` with the real `SyncedLyrics` and a host-owned
`FoliaPlaybackSnapshot`. `Kotlin` is the default. `NativePreferred` reports its
reason and falls back to the current Kotlin state. `NativeRequired` reports an
explicit failure. Unsupported themes do not silently execute as native.

The per-prepared-line CPU coverage-copy limit defaults to 64MiB, configurable by
`FoliaNativeRendererConfig`. Native resident textures/scratch have a separate
192MiB cap. These are not total process/driver/staging/transition memory budgets.
Runtime errors are bounded; Kotlin cannot recover a native abort/driver crash.

Submission IDs/sample times are diagnostics, not physical present IDs or FPS.
Interaction currently follows the latest acknowledged submission; its exact
physical-display correspondence under a backed-up compositor remains unverified.
No hardware speedup or final visual-equivalence claim has been established.
Keep experimental/default Kotlin until the design's gates actually pass.

## Rebuilding the Rust source archive alone

Extract the ZIP in a new directory. `build-android.ps1` expects the repository
layout by default; when using this standalone archive, place its
`build-support/rust-environment.ps1` at `../experimental-mask/rust-environment.ps1`
or use the same locked Cargo command with your explicit target linker/NDK.
`third-party/registry-sources/*.crate` are checksum-verified upstream tarballs,
not a Cargo vendor source-replacement directory. They provide corresponding
sources; configuring a fresh offline Cargo registry/index is a separate step.
No claim of a network-free clean producer rebuild is made. See checked hashes
and the fixed manifest/lock, rather than fetching a reference application's main.
