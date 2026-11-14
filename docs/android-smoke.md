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

## Latest renderer recheck — 2026-11-15

Built 555a26e APK, installed in another headless read-only probe34/API34 session
with the same viewport/density/SwiftShader conditions. After paused progress seek
to 0:03, tapping empty space inside the lyric area retained 0:03; tapping the
drawn transformed Chinese unit changed the clock to its original line start
0:02. Actual screenshots and UI hierarchies confirm both outcomes.

Temporary system font scale 1.5: metadata wraps, scene/settings/transport remain
visible. At Unicode scene 0:08 the Arabic source remains a connected whole run,
with Chinese subtitle and Indic preview readable. This checks RTL text shaping,
not an Android RTL locale or exhaustive script/accessibility acceptance.
No AndroidRuntime error was logged. Source-document import was not repeated.

Font scale restored to 1.0; only this session's temporary device files were
removed, then only the emulator launched for this check was stopped. Ignored
.work/android-current-* captures/hierarchies retain evidence. Physical/low-API
and long-session performance acceptance remain outstanding.
