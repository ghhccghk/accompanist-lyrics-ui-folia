# Tilt main text and actual original subtitle overlay

Folia v0.7.26 / cc6226e464050a456a2913e062374f23d3844cc5, AGPL-3.0-only.
This follows the scale repair64d5619. No further production movement/collision
policy was invented. Original Tilt uses a centered70vh text region and an
independent bottom subtitle; it has no main/subtitle collision solver.

## Reference and public path

`startReferenceServer(4195,{realSubtitles:true})` imports the REAL unchanged
`VisualizerSubtitleOverlay.tsx` rather than the existing no-subtitle alias.
Default server behavior and all older source fixtures remain unchanged. Main
host loads the existing synthetic wrap scene, explicit source translation
"Copper translates the quiet light", actual valid single-translation layout,
same licensed parity font/weight700, body line-height1.5,112px bottom, default
subtitle opacity0.6, no backdrop. Source current lyric and original Motion row,
character, parent exit and subtitle behaviors all execute. Shell/background
engine/chrome remain isolated. No artificial collision avoidance, alignment,
source formula replacement or rewritten expected values.

`../tilt-fixtures/subtitles-v2/conditions.json` contains source/host/font/lock
hashes, actual Chromium151.0.7922.34 and Motion13.4.3/dom13.4.2/utils13.3.0.
380/800×600,DPR1,scale1/1.4/2,media1000/1500/2400/3200/3500ms,10ms media/RAF,
30 frames,110 main and30 subtitle records. Native uses actual published
lyrics-core `MainKaraokeLine` translation and the public FoliaLyricsView, not
an internal image masquerading as public output. Internal stage is checked
pixel-for-pixel only as a supplemental wiring assertion, then public pixels
are saved for the source comparison.

The2400ms geometry checks pass with unchanged font .011px,width .51px,
height .03125px and top .1px gates: observed peaks .000001/.20275/.01565/.03125px.
Main top removes ONLY the source's measured row y from its box to compare
static geometry. Animated transforms are not registered away in any image.
Pause/reverse seek/offset once/remount preserve public pixels exactly. Two
new Tilt tests pass (.work/tilt-subtitle-stage-v2.log); no new device/window or
continuous playback/performance claim. Prior Android/Desktop compile evidence
for64d5619 still applies to unchanged production code, not a fresh device run.

## Overlap exists in the actual original too

At2400ms main/subtitle current-transform BOX intersection is:

| viewport width | scale1 | scale1.4 | scale2 |
| --- | ---: | ---: | ---: |
|380|0px|10.421295px|74.265625px|
|800|0px|38.483795px|60px|

These are actual source rectangles, not claimed glyph intersection areas.
The fixed contact visibly shows overlapping lettering at the large settings
on BOTH sides. Native near-equal boxes do not turn that into a readable or
accepted result. It is an observed upstream behavior, not a platform exemption
or permission to redefine the acceptance standard. Any future readability
policy must be explicit; this change does not shrink or move the source text
silently. Wide/narrow large settings are still tracked as readability limits.

All30 source/public pairs include lowest-alpha text/shadows on one fixed
non-background union. Mean foreground MAE11.1192605; worst64px152.3333333.
Every frame/channel/local result and hash is retained in comparison.json.
There is no before/after benefit claim from this new matrix, and it is not final
theme visual acceptance. Root800px native canvas is cropped only at its known
unused right viewport margin; no content alignment or rescaling is applied.
Contacts are labelled thumbnails; raw30+30 images remain under build/verification.

## Retained invalid first capture

The first capture accidentally set source `subtitleOverlayOpacity=1`, while
the library/source default is0.6. Its pixels cannot be used as an equal-config
comparison. Complete inputs/images/incorrect comparison/contact are preserved
under `.work/tilt-subtitles-opacity1`. Original v1 conditions SHA256
`80aa7f286d31cc258d79a413b8f3540ca1bee7a7ced3bb06606e704557fe38ff`,
incorrect comparison SHA256
`335103beed1cd76ac6d70b5a0c1ba62f36b751737a72d81f17ef71f7b7aaaa31`.
No production change was made to chase that mistake. v2 removes the erroneous
source override, with exactly the same lyrics/fonts/viewports/scales/times;
all30 are retained, not a chosen subset. Mean19.0982868 from the invalid run
must NOT be described as a production improvement to11.1192605.

```
node verification/classic-browser/capture-tilt-subtitles.mjs
./gradlew :library:jvmTest --tests '*TiltSubtitleStageTest' --tests '*TiltFontScaleTest'
python verification/compare-tilt-subtitles.py
```

Capture refuses to overwrite its source fixture. This is a completed bounded
comparison, not a newly completed theme or a subtitle collision repair.
