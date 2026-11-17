# Fume original React parent entry / public-core comparison

Pinned cc6226e464050a456a2913e062374f23d3844cc5. This supplements, rather than
replaces, the216-frame function-pipeline baseline in ../fume-public.

Original VisualizerFume.tsx:340–346 animates its entire canvas parent from .985
before first printed content to1 in.45s with Motion easeOut. The Kotlin public
entry omitted that transform. FumeStageEntry / FumeEntryEasing now reconstruct
it from media time; graphicsLayer transforms drawing and pointer coordinates,
with Canvas clipRect matching the original bitmap boundary. Reduced motion uses1.
Original printed state is mount/history driven; Kotlin reconstructs firstStart
on seek/remount so pause freezes and reverse queries are deterministic. This is
a stated media-time adaptation, not equivalence to every original mount history.

## Oracle conditions and scope

Actual untouched VisualizerFume and useFumeCanvasStage remain mounted, with
original layout, driver, RAF and Motion logic. Host injects synthetic lyrics,
font, theme and MotionValues. Media updates each tick; React host rerenders only
when selected line changes. Background opacity0; shell/subtitle/settings and
loading Hourglass isolated. No background-engine acceptance is claimed.
FirstFrameContext is a readiness signal; fiber inspection is passive. Native
ResizeObserver callback is unchanged and logged; an initial real screenshot
allows browser layout/observer delivery before controlled time starts.
RAF/performance use a deterministic clock, with a task boundary per original tick.
Original WAAPI startTime/finish callbacks are driven on that clock; no phase
registration, oracle algorithm rewrite, pixel alignment or alpha cutoff.

Same invented four lines, FoliaParityLatin-Bold SHA256
adbf4886a018c81066273f569b8ec612b5e75b883076a5aa4b1d33d5778d16d3,
weight700, DPR1, viewport380/800×600, seed FumeSceneFixture, hold.3,
smooth/stepped, glow1, float origin0, colors#162435/#dfe5ec/#f17b4d,
explicit amber echo color#30caff.23 probes per variant,92 pairs,60Hz RAF plus
exact probe times. Full conditions/geometry in ../fume-fixtures/mounted-observations.json.

## Results

comparison.json uses the SAME original/before/after all-nonbackground union,
including low alpha. Before is the same current Kotlin pipeline with only
stageEntryEnabled=false; after is actual-core FoliaLyricsView. All92 frames:
foreground MAE8.5536883831→8.1401108677; probes before1.15s:
3.6997089577→2.5106736010. Parent scale peak1.2849506581e-7 (gate1e-6).
Full means, local worst64px, channel peaks and all image hashes are retained.
contact.png was visually inspected; no moving/registration masks applied.
Camera screen peak15.933272752px and scale.0562212535 remain unchanged.
This repairs parent entry only, NOT final Fume visual acceptance.

Public tests cover paused mid-entry, reverse seek and remount exact pixels,
reduced motion, actual transformed click/long press/original identity, offset,
resize and theme restore, cancellation,120-line/system-fontScale restoration.
JVM offscreen and Android APK/Desktop compile passed; no real Desktop window,
Android device, live audio or device performance validation in this change.

## Dependency correction and retained failures

Earlier verification installed Motion-dom13.5.1/utils13.5.0 through ranges,
although the Folia package-lock uses13.4.2/13.3.0. pnpm-workspace.yaml now pins
those exact versions; reference.mjs rejects mismatches. All92 source frames and
observations were recaptured at exact versions:0 PNG hash/state changes.
dependency-audit.json retains both version sets and the previous summary.
This Fume result does not re-certify older Cappella/Pendolo Motion captures;
their source code is pinned, but historical transitive packages were newer.
Before final acceptance those affected Motion matrices need exact-lock review.
Cubic-bezier source13.3.0 and13.5.0 is byte-identical. Production port is attributed
to13.3.0; MIT Motion and original BezierEasing notices are in LICENSES.
The latter license was obtained from npm bezier-easing2.1.0 archive (no dependency).

Initial mathematically tighter40-step inversion failed the unchanged1e-6 scale
gate at717ms. Matching the original12-step/1e-7 bisection fixed the cause, without
changing fixtures/tolerance. .work/fume-entry-first.log retains failures;
second.log and final.log pass. Earlier harness boot/layout failures are retained
as .work/fume-mounted-first.log through seventh.log; exact lock run is
.work/fume-mounted-exact-lock.log. No product exceptions were suppressed.

## Reproduction

From repo root, pnpm install in verification/classic-browser (ignore-scripts), then:
```
node verification/classic-browser/generate-fume-css.mjs
node verification/classic-browser/capture-fume-mounted.mjs
./gradlew :library:jvmTest --tests '*FumeMountedPublicTest' --tests '*PublicFumeTest' --tests '*PublicFumeStageTest'
python verification/compare-fume-mounted.py
./gradlew :sample:android-app:assembleDebug :sample:desktop-app:compileKotlin
```
Windows uses gradlew.bat and configured JDK21/SDK. Dedicated4175 browser/server
close in finally; PNGs remain ignored under build/verification. PublicFumeStageTest
now writes fume-public-kotlin-entry to preserve the earlier pre-entry baseline.
