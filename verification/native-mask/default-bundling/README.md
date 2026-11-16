# Default sample native packaging — 2026-11-16

Android sample default includes arm64-v8a/x86_64 JNI in all variants; debug built here.
Windows x64 Desktop sample defaults to a JAR-contained DLL. Library unaffected.
Runtime switch still defaults Kotlin. No user Desktop Gradle configuration edited.

- JDK21.0.5 and JBR25.0.4: verifyBundledDesktopMask passed with empty java.library.path,
  resource protocol jar, actual ABI and 1x1 convolution, three toggle/release cycles.
- Three ExperimentalMaskControlsTest UI checks passed; missing DLL forced explicitly
  so failure remains tested even when native resources exist.
- Default Android debug APK: both JNI exports, 16KiB ELF and ZIP alignment, signature pass.
- Consecutive true/default -> false: packaged JAR/APK contain no folia_mask_experiment.
- No Android device/UI/runtime or installer launch tested this round.
- JBR25 warns about future native-access restrictions; current real JNI calls pass.

APK before memory changes: build/outputs/folia-sample-native-default-debug.apk
SHA256: 5819a5fdc9884bbf40db7021c400201e1327a73c5bac26ba1369b25cb6d752c3

Local full build logs: .work/bundled-native-build.txt, .work/bundled-native-jbr25.txt,
.work/native-disabled-build.txt (ignored; paths are local execution evidence only).

Follow-up host support and Cargo discovery:
- No PATH cargo, CARGO_HOME/RUSTUP_HOME unset: both Windows build helpers find
  existing project-private rustup, real Android two-ABI/Desktop DLL builds pass.
  Process environment restored after helper. No global PATH mutation/install.
- Gradle default (no foliaRustCargo override): Android assemble and packaged DLL
  probe pass. Platform mapping tests cover Windows/Linux/macOS x64/arm64 paths.
- Unix sh helpers tested under Git Bash using mock compiler/uname in isolated
  temporary paths with spaces; Linux/Darwin routing, two ABI copy and explicit
  nonzero compiler exit propagation pass. These are NOT real host builds.
- Linux/macOS native compilation, dynamic loading, UI and installer remain unverified.
