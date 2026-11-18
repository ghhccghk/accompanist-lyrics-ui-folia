# Waiting overlays by theme — 2026-11-18

User requested removing waiting dots from themes where they obscure the retained
lyric stage. Diorama, Fume, Cappella and Pendolo now keep their own stage without
a separate waiting overlay. Classic, Partita, Tilt and Cadenza retain the existing
finite all-voice gap/exit-aware waiting dots. The breathing motion of lyric text
itself is unchanged. No upstream animation, source timing or fixture modified.

Validation: `:library:jvmTest --tests '*PublicDioramaTest.gapsKeepTextStageWithoutWaitingOverlay'
--tests '*WaitingStageTest' --tests '*ThemeWaitingPolicyTest' :library:compileAndroidMain`.
All three tests and Android library compilation passed. Public Diorama equals
the actual internal stage pixel-for-pixel at 4s intro and 17s interlude using the
same font/style, source lyrics, viewport, colors and disabled subtitle overlays.
Eight-theme checks confirm overlay presence/absence at both gaps. Existing
Classic offset/pause/reverse-seek/all-voice waiting regression remains unchanged.

Initial diagnostic failures: the first comparison included public default
subtitles absent from its internal control; the second captured an internal
re-mount before asynchronous preparation finished. Both test inputs/lifecycle
waits were corrected without relaxing the exact pixel assertion. No Android
device, real Desktop window, visual equivalence to Folia or performance claim.
