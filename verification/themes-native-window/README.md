# Seven public themes in a real Desktop window

2026-11-18, Windows10/JDK21.0.5, actual DIRECT3D backend; native desktop density1.5,
content density1/fontScale1 or1.4. Dedicated visible undecorated ComposeWindow,
800x600 ->380x740 ->800x600 real AWT resizing; public FoliaLyricsView and published
lyrics-core models. Three original synthetic verses, licensed fixed Latin font
(hash in conditions.txt); no user songs, settings files, system audio or OS input.

105 requests completed across Classic/Partita/Tilt/Cadenza/Pendolo/Cappella/Fume.
Each tests native drawing,120ms pause equality, forward/reverse seek, responsive
restore, system font scale, reduced-motion/Minimal setting restore, offset applied
once, document replacement/restore, detach/remount, reverse theme revisits. Eight
restored snapshots per theme equal their own baseline pixels. Manifest includes
all105 hashes. This is SELF-determinism, not equality to an original browser.

Initial failure: Tilt prepared count9 ->0 indefinitely on a quality change,
confirmed separately and fixed in059087b. Original failure log/XML retained in
`.work/tilt-status-before-fix.*` and `.work/themes-window-initial-*`.
The first completed run then exposed a HARNESS readiness error on image inspection:
Fume narrow frame was a preparation label while previous viewport's ready count
was still held. That run is invalid for full seven-theme completion, preserved
under `.work/themes-native-window-incomplete-readiness`. The corrected verifier
clears readiness only for actual async Fume preparation input changes, then waits
for new positive status and absence of FumePreparing. It still requires positive
status and actual nonblank drawing rather than relaxing the gate. Final complete
run `.work/themes-window-ready-runtime.log` succeeds in34s; inspected Fume narrow
now contains actual lyric article. Contact contains the SAME final run's fixed
baseline/narrow/large-font variants for all themes, no selective recaptures.

These are GraphicsLayer snapshots after the real window draws, NOT OS screenshots.
No window mouse/keyboard, native file-picker, real-audio selection/playback,
continuous animation, resource/heap profiling or Android-device validation is
claimed. Existing oversized Fume camera framing, Tilt large-font subtitle overlap,
and theme raster/motion residuals remain visible and require source-conditioned
follow-up. Do not treat this integration pass as visual acceptance or hide these
frames. No attempt was made to resize/register/recolor images to match an oracle;
only the labelled contact thumbnails are scaled for viewing.

```
./gradlew :sample:desktop-app:verifyThemesWindow
python verification/summarize-themes-window.py
```

The task is in the existing isolated classicVerification source set; none of its
fixtures/entry points are packaged into the sample app. It closes only its own
window; existing user browser/player windows are untouched. Raw28 PNGs, SUCCESS,
checks and runtime log remain in build/verification and .work. Compact contact,
checks, conditions and source hashes are tracked. Android default debug APK
(including existing Rust sample runtime) also builds after059087b; no connected
device was available in the earlier single device check this turn.
