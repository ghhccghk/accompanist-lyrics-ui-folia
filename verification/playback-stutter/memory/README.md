# Software blur memory — 2026-11-16

Preserve shaped drawing, timing, sigma, premultiplication, kernels and rounding.
Reuse input for the vertical Gaussian output (reads only temporary); omit unused
Gaussian kernel for box blur. Reuse full ARGB when crop equals full dimensions,
otherwise retain contiguous cropped pixels: a strided four-channel candidate
saved more memory but the first device pair was slower and was rejected.
When a ClassicLayer returns from software to native recording, clear software
bitmap/painted references. GC decides reclamation; no immediate-PSS claim.

Windows x64 / JDK21 ThreadMXBean, opaque 640x320, sigma .5 and 6:
10,650,720 -> 8,193,088 bytes and 10,651,296 -> 8,193,368 bytes per call.
Saved ~2.34 MiB/call on full images. Cropped images retain the ARGB crop to avoid
strided scan costs, saving one 8-byte/pixel DoubleArray instead of 12 bytes/pixel.
Returned bitmap/native allocation is outside ThreadMXBean accounting.

Physical 2509FPN0BC, arm64/API37, separate app_process, no install/Activity/player
change or user media. Synthetic 97x43 and padded 1200x500, sigma .25/.5/2/6,
8 warmups and 15 operations; ART art.gc.bytes-allocated process delta per call.
Two baseline/candidate pairs in separate processes. All 8 output dimensions,
origins, ARGB hashes and SHA-256 agree in both pairs. Large padded image managed
allocation drops ~12.6%-13.7% (~0.82-1.04 MB/call), repeat confirms the reduction.
Timing varies substantially: first pair improved, repeat has a .25-sigma slowdown
and other sigmas near baseline. No stable CPU speedup, whole-playback improvement,
RSS/PSS reduction or full-device UI parity is claimed.

JVM: 405 frozen-legacy pixel combinations + colour/transparent analytic checks
pass. Existing 30 Partita scenes / 360 timestamps / pause / reverse-seek / upstream
geometry pass on first equivalent-buffer revision; final contiguous revision
rechecked exact pixel matrix and native/software CSS blur coverage. Both sample
platforms compile; default Android APK contains Rust. Not whole-theme acceptance.

Reproduce memory and pixel checks:
./gradlew.bat :library:jvmTest --tests '*SoftwareBlurTest*' --tests '*NativeBlurParityTest*'

Build SoftwareBlurDeviceProbe.java against API37 android.jar using javac then D8;
see parent README app_process command. Extra output managedBytesPerCall is managed
allocation only. compare-probes.py checks pixels and records memory/timing values.
The actual Android app has not been replaced during this round.
