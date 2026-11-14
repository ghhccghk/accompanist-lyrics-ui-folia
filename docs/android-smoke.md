# Android runtime smoke check — 2026-11-14

Environment: existing probe34 AVD, sdk_gphone64_x86_64 / API 34, x86_64,
WHPX, SwiftShader, 1024MiB guest RAM, two CPU cores. Headless, read-only AVD
session; no physical-device security setting or original AVD data was changed.
Viewport 1080×2200 and temporary 2200×1080 override; default platform fonts.
This is functional/visual smoke coverage, not GPU/frame-time measurement.

Verified using ADB input, screenshots, UI hierarchy and AndroidRuntime log:

- APK installation and cold launch succeed; no AndroidRuntime fatal error.
- Play/pause and progress seek display actual classic glyph transforms, halo,
  translation, phonetics and next-line preview.
- Portrait and short landscape use their intended layouts. Landscape initially
  exposed an oversized cover; compact metadata now keeps controls on screen.
- OpenDocument selects an original UTF-8 enhanced/standard LRC from Downloads.
  Imported title and 13s no-audio clock appear; cancel preserves imported content.
- Window size/configuration recreation retains the imported session.
- From a fresh scene: play ~0.7s, Home, stay background 2s, bring task forward:
  clock remains at 0:01 / 22s and button is Play (explicit resume required).
- Toggle translation, force-stop only this sample, relaunch: the preference is
  restored. Imported capabilities are session-only and are not restored after
  process death.

Diagnostic screenshots/hierarchies live only in ignored .work/android-* files.
They contain original synthetic text and platform UI, not reference media assets.
The temporary screen-size override was reset after verification.

Separately, earlier installation on a physical model 2509FPN0BC / API 37 returned
INSTALL_FAILED_USER_RESTRICTED. No physical-device run is claimed or bypassed.
Still unverified: low-API devices, alternate providers, revoked URI access, native
cancel gestures, large-font accessibility, long-session memory, thermal/GPU/frame
performance and pinned-upstream visual equivalence.
