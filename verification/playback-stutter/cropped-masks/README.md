# Bounded Kotlin glow preparation — 2026-11-16

The real-song Partita stacks in `../current-capture` include synchronous
`GlowMask.blurredMasks` preparation (10.2–10.7% inclusive samples). Some
175–188ms frame windows overlap that preparation. These are observations, not
exclusive elapsed-time attribution.

The Kotlin fallback now finds the exact nontransparent bounds and expands them
by the three-box kernel's finite support. For window `w`, that support on each
axis is `3 * (w / 2) - (if w is even then 1 else 0)` pixels. This accounts for
the existing opposite half-pixel offsets. Scratch buffers cover the union of
all requested radii; output images retain their original dimensions, origin
and every ARGB pixel. Fully transparent input needs no convolution scratch.
Dense input keeps the existing flat conversion path. Kernel arithmetic,
quantization, radius, opacity and shaping are unchanged. The optional Rust
processor still receives full input and uses its existing implementation;
this change speeds up the Kotlin path only.

## Verification

- Frozen full-surface preparation versus bounded preparation: **345** masks,
  exact dimensions and all-pixel equality. Covers 1-pixel axes, sparse corners,
  low alpha, transparent colour, dense and padded random inputs, fractional
  sigmas, repeated/unsorted radii and sigma 4096. The frozen preparation uses
  the unchanged production kernel; existing independent kernel/reference
  tests and 135 software colour-blur comparisons also pass.
- Bounding rectangle matches an independent complete scan in 36 cases.
- `scene-comparison.json` records freshly recaptured narrow normal Partita
  wrap, Arabic and word-group images against the hashes committed in
  `ca9e07c`. Existing upstream geometry, pause and reverse-seek assertions
  remain active. This is regression parity, not a new claim that all prior
  browser-to-Compose raster differences are resolved.
- `preparation.csv`: Windows x64, Intel i7-12650H, JDK21, offscreen Skia;
  two masks (sigma 10/20), 8 paired warmups, 21 alternating measured pairs,
  including bitmap creation. JVM bytes use ThreadMXBean and exclude native
  bitmap storage. One run under uncontrolled host load, not Android playback.
  At 640x500 padded input: P50 16.7305 → 6.5249 ms; JVM allocation
  12,802,264 → 8,008,920 bytes. Dense input P50 16.8813 → 16.636 ms and
  allocation effectively unchanged. No timing threshold is used as a test.

Commands (use `gradlew.bat` on Windows):

```sh
./gradlew :library:jvmTest --tests '*AlphaBoundsTest*' --tests '*CroppedMaskTest*' --tests '*GlowMaskTest*' --tests '*SoftwareBlurTest*'
./gradlew :library:jvmTest --tests '*PartitaParityTest.narrowNormalWrap' --tests '*PartitaParityTest.narrowNormalArabic' --tests '*PartitaParityTest.narrowNormalGroup' --tests '*NativeBlurParityTest*' :sample:shared:compileKotlinJvm :sample:shared:compileAndroidMain :sample:android-app:assembleDebug
```

Local logs: `.work/cropped-mask-tests.log` and
`.work/cropped-mask-scenes-build.log`. No user lyrics/audio or device identifiers
are copied into this evidence. Android runtime remains unverified: the physical
device was disconnected. The APK build/package checks are recorded separately
in `checks.json`; no installation or playback-session A/B occurred.

## Remaining work

Visible nested software layers, particularly positive opacity with tiny sigma,
still rasterize synchronously. Cropping does not remove final full-sized masks
or guarantee a session memory bound. Neither this change nor zero-opacity
culling establishes that the recorded 967ms stall is fixed. Next validation is
same-device, same-song, same-interval/settings A/B including Kotlin/Rust selection;
do not substitute this JVM micro-measurement for frame/PSS measurements.
