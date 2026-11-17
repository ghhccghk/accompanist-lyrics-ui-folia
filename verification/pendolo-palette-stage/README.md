# Pendolo caller palette: real original wheel control

Pinned cc6226e464050a456a2913e062374f23d3844cc5 / AGPL-3.0-only.
Original `VisualizerPendolo.tsx` primary/secondary/accent inputs now map to
internal `PendoloPalette` in `PendoloWheelTarget.kt`. Main/idle text,
subtitles, chorus mix/halo and both post-mask shadow passes use that palette.
Keyword ranges retain their independent configured colors; only unmatched
ink uses the new mixed fallback. No hardcoded color remains in drawing calls.

Original JSX/component remains unchanged. Separate host changes only three
caller colors to primary#71b8ed/secondary#e2b66f/accent#93f1a1. Same covered
fixture fonts, synthetic lyrics, widths380/800,height600,DPR1,normal intensity.
Latin/CJK/Arabic at8 times:24 full pairs and24 source-rotation isolation pairs.
854 numeric fields:peak content-box error.068329px. Foreground MAE7.946432,
peak12.017226; raster differences remain. These are new conditions, not an
improvement comparison with the default-palette matrix. `contact.png` inspected.

Palette disable/restore must visibly change/recover pixels; random seek,
pause,remount and exact row order checks pass. All1202 older native PNG hashes
are unchanged. Full Pendolo suite,Android debug APK and Desktop compile pass
(`.work/pendolo-palette-final.log`); no device or window launched.

Reproduce using the already-generated pinned JSX and private fixture server:

```
node verification/classic-browser/capture-pendolo-palette-stage.mjs
./gradlew :library:jvmTest --tests '*Pendolo*'
python verification/compare-pendolo-palette-stage.py
```

Raw PNGs: `build/verification/pendolo-palette-*`; raw conditions/inputs are
`verification/pendolo-fixtures/palette-stage-*`. Same halo-isolation seam is
captured for follow-up, but this report only evaluates full text-wheel pairs.
Still an internal integration step, not public Pendolo support/final acceptance.
Next: typography/lyrics-core integration, transformed hit areas and a labelled
calibration preview, while preserving documented dense-tick/raster differences.
