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

## Subtitle renderer recheck — 2026-11-15

Latest subtitle-motion/layout APK installed with adb install -r into this run's
read-only probe34, emulator-5558, API34 / 1080x2200 / density420 / SwiftShader.
Cold launch and paused drag to 0:02 show translation, romanization and blurred
upcoming text; native Typeface font-metric resolution does not throw. AndroidRuntime
contains no fatal/exception. Screenshot: .work/folia-subtitle.png. No pinned font
is installed in the sample; this is a runtime smoke check, not browser pixel parity.
Low API / physical hardware acceptance remains outstanding.

## API30 software-layer runtime — 2026-11-15

Downloaded official default x86_64 API30 r11 system image, verified SHA1
5e4de3946d46f88856c35efcc4d797b381456347. Separate temporary classic30 AVD under
ignored .work, emulator-5560, read-only/no snapshots, 800x600/density160, 2 cores,
2GiB RAM, WHPX/SwiftShader. No existing AVD configuration or user files changed.
Actual getprop SDK=30. Latest fallback APK installs/launches and paused seek at
0:02 shows lyrics, translation, romanization and blurred upcoming text. No
AndroidRuntime fatal/exception. Two paused screenshots match exactly in the app
area (24px status bar excluded): .work/folia-api30{,-b}.png. This verifies the
actual low-API branch runs. Native sample font differs from the pinned browser
fixture; full Android pixel parity and frame-time/memory measurements remain open.
API21..29 and physical devices are not claimed verified.

## Fractional subtitle ink baseline recheck — 2026-11-15

Current baseline-fix APK rebuilt, installed and cold-launched in the same API34
and API30 sessions. Paused progress drag shows current lyrics, translation,
romanization and preview without crash. Captures:
.work/emulator-{5558,5560}-baseline.png. AndroidRuntime contains no application
exception. These use system fonts and sample layout, so validate native execution
and absence of clipped subtitle rows, not fixed-font original-browser parity.
