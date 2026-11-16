# Experimental Rust coverage kernel

Optional CPU mask experiment authorized 2026-11-16. The sample now has a scoped
Kotlin/Rust switch; default remains Kotlin. Android sample now packages arm64-v8a / x86_64 JNI by default; Windows x64 sample
bundles the DLL in its JAR. Use `-PfoliaExperimentalMask=false` for sample builds
without Cargo/native. Library-only builds/publications still need no Cargo or native runtime. This is not the larger scene/animation ABI.
See [sample setup, verification, and device limitations](../../docs/native-mask-sample.md).

Rust 1.91.1, jni-sys exactly 0.3.0 with Cargo.lock. The kernel is independently
written from this project's Kotlin CPU coverage implementation. No Folia Rust,
Accompanist renderer, font, lyric, GPU implementation or player code copied.
jni-sys is MIT/Apache-2.0; see THIRD_PARTY_NOTICES.md and upstream registry source.

Standard Windows MSVC toolchain: `cargo build --manifest-path native/experimental-mask/Cargo.toml --release --locked`.
For an installed Rust Windows GNU host without GCC, from repository root use:

```powershell
./native/experimental-mask/build-windows-gnu.ps1
./gradlew.bat :library:jvmNativeMaskProbe '-PfoliaNativeMaskDll=../native/experimental-mask/target/release/folia_mask_experiment.dll'
```

`-Cargo <cargo.exe>` can select a private installation; the script derives paths
from rustc sysroot and does not edit user PATH, registry or Gradle runtime settings.
The validated local toolchain is x86_64-pc-windows-gnu, Rust LLD, self-contained CRT,
with explicit `DllMainCRTStartup` required by this linker combination. DLL depends
on Windows system libraries. Android r28c shared libraries are built/packaging-checked;
arm64/API37 sample loading and 15 exact CPU cases now pass on a real device.
Android full UI parity, x86_64 runtime and device performance remain unverified.
Cargo unit-test EXE linking failed on this environment (LLD cannot find import
libraries in EXE mode); actual JVM DLL loading and parity/boundary checks passed.
Do not report Cargo tests as passed or silently use a Kotlin fallback.

Probe: JDK21 `-Xcheck:jni`, native-order direct ByteBuffers, 30 random size/radius
cases bit-exact alpha, invalid/heap/misaligned/overlapping/short buffers rejected.
Read-only scratch/output ByteBuffers are rejected in JNI before mutable slices.
Fixed persistent scratch, synchronous call, no retained JNI address/native heap
state, no close handle needed. Panic maps to explicit status; JVM side requires OK.
Byte order is a caller contract of this private probe, not a public FFI guarantee.

Results: library/build/verification/native-mask-experiment.txt. Compare current
Kotlin, same contiguous scan with Kotlin, Rust+JNI, and preparation including
bitmap read/write. Twelve warmups and forty alternating measurements per path.
Report JVM allocations plus persistent direct buffers separately. GPU, font,
full-frame latency and native bitmap allocations remain unmeasured. Host load and
thermal state are uncontrolled; repeat across processes before conclusions.

Initial fair comparison favors reused contiguous Kotlin over Rust+JNI; the sample
switch is explicitly for device A/B testing, not a claim of stable native speedup. Current
candidate findings and platform limits are recorded in docs/performance.md.
