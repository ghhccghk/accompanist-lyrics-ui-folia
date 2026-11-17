# Public Cappella actual-core calibration

Pinned original Folia cc6226e464050a456a2913e062374f23d3844cc5. Reuses the
unchanged actual-original complete JSX capture and six conditions of
../cappella-playback/README.md: 210 frames, media0..14s, three intensities,
380/800/1200 logical widths, same covered OFL font bytes, DPR1, same colors.
Original selection/timestamps/row presence/size/spring/projection run normally;
only the documented fixture clocks and CSS phase control are injected.

`PublicCappellaStageTest` constructs **actual lyrics-core0.5.0 KaraokeLine and
KaraokeSyllable objects** from these synthetic integer-ms inputs. Original and
bridged line/render boundaries and internal EGC timeline are checked, then each
FoliaLyricsView frame must equal the independently mounted internal chat stage
pixel-for-pixel. Native public PNGs remain in build/verification/cappella-public-kotlin.
Geometry in the report is from that independently mounted pixel-equal stage;
the public component does not receive any browser geometry. No host/input/style
modification was made to the existing original capture for this public check.

Subtitles are disabled on both sides of this main-stage matrix. Public subtitle
integration uses the shared overlay and is **not** source-subtitle visual acceptance.
Explicit nested agent identity and transformed input have separate tests.

Reproduce after the original fixture server/capture is available:

```
node verification/classic-browser/capture-cappella-playback.mjs
gradlew :library:jvmTest --tests "*PublicCappella*"
gradlew :sample:shared:jvmTest --tests "*CappellaSampleTest*" --tests "*SettingsTest*"
python -X utf8 verification/compare-cappella-playback.py --public
```

Report retains all nonbackground RGB pixels including arbitrarily low alpha;
no registration, phase shift, dropped pixels or source-derived positions in the
renderer. MAE2.325954, geometry peak3.637201px, opacity peak.013716, source
presence/order exact. Per-frame full-screen MAE, channel peak and worst64px tile
are recorded. This foreground metric differs from older >8 reports; no improvement
is claimed by comparing them. Contact.png inspected; glyph/raster differences remain.

Public interaction/lifecycle tests check original object click/long press,
transformed rounded corners/blank parent input, nested source+explicit agents,
offset once, document replacement, theme/resize restore, pause, seek,
reduced motion/Minimal quality and unmount/remount. Sample tests select the real
theme without starting demo and render paused demo through the production entry.
All five older public theme interaction tests plus Android APK/Desktop compilation
pass (.work/cappella-public-final.log). No Android device or visible Desktop window.

**Not final visual acceptance.** The independent240-frame pressure matrix retains
22.188593px peak layout discrepancy and all failure evidence. Preview availability,
source geometry tests, graphics fidelity and device performance remain separate.
