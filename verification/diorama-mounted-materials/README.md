# Full mounted text material-history control — 2026-11-18

This supplements the frozen175dabe **original CameraRig + DioramaScene**
observations, not another isolated target or a browser rerun. All48 source PNGs,
original input, fonts, dimensions, Motion/R3F/Three lock and time schedules remain
unchanged. `export-diorama-mounted-materials.mjs` checks original light/soul/
material observations against the retained raw4686 callbacks, then exports192
unit snapshots. The actual pinned `buildLineGraphemeTimeline` and
`splitDioramaLyricUnits` produce44 independent grouping/timing rows. Hashes and
conditions are in `../diorama-fixtures/mounted-material-v1`.

Native prepares its own published-core lines, full-shaped rasters, camera and
unit clocks. The existing finite camera replays all4686 original intervals from
its own state, including actual camera-before-width-publication order. Native
unit state resets at the real global index/count changes, replays its own
Float32 exponential envelopes with those intervals and reads source-equivalent
`0.9+0.1*sin(wall*1.9)` breath and **its own camera** distance life. Original
pose/material/envelope values are assertions only, never renderer inputs.

All44 group/text/range/timing rows agree (time gate1e-12). At48 source probes,
192 independent unit states have peak **7.207234808959129e-12 <=1e-9** across
linear colour, alpha, glow and soul; envelope values are exactly equal. Camera
gate remains1e-9. `DioramaActiveText.drawFrames` renders resolved states through
the same full-shaped three-layer path as production; default playback still
resolves media states as before. All96 pre-refactor native finite/media PNGs and
610 existing direct-Three-policy PNGs remain hash-identical. There is no source
frame lag or accumulated finite envelope in the production media clock.

One shared **four-image** original / own-finite-camera-analytic-material /
own-finite-camera-finite-material / own-media all-nonbackground union, including
lowest alpha and every frame, no registration/filter/phase shift:

| Method | Mean foreground MAE | Worst image | Worst64 local |
| --- | ---: | ---: | ---: |
| Finite camera, analytic envelope/breath1 | 6.937855 | 17.308414 | 20.434397 |
| Finite camera, finite envelope/original wall breath | 6.817019 | 17.308414 | 20.434397 |
| Media camera, analytic envelope/breath1 | 20.240528 | 46.444817 | 83.400000 |

Do not compare these means directly with the ACES five-image-union matrix.
Finite materials improve30 frames, regress6, leave12 unchanged; no worst64
regression. All six regressions (the four700ms and two144Hz3499ms frames) stay in
comparison.json. Fixed six-probe contact inspected. Camera/envelope controls
separate causes but do NOT remove glyph raster/coverage/clipping or media-camera
residuals. Numeric material success is not final visual acceptance or evidence
of device performance. Diorama remains internal and unavailable in sample.

Both mounted JVM tests pass, including media schedule independence, paused
pixels, reverse queries and remount. Additional unit/material and existing stage
regressions protect default rendering. Android debug/Desktop compilation also
run; no new Android runtime/audio/real-window acceptance is claimed.

```powershell
# Refuses existing fixture; requires retained original steps-full.json.
node verification/classic-browser/export-diorama-mounted-materials.mjs
.\gradlew.bat :library:jvmTest --tests '*DioramaMountedStageTest'
python verification/compare-diorama-materials.py
```

Current comparison requires preserved c75652d96 native image hashes to confirm
the refactor did not change production. Original captures/expected values are
not rewritten for this control. Next: replace the still-constant production
glow breath with an explicit media-domain phase, retaining pause/seek, offset,
cut/loop continuity and reduced motion; state the necessary difference from
upstream wall-time evolution instead of claiming exact RAF equivalence.
