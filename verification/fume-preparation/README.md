# Whole-song preparation and camera boundary probe

120 synthetic line-only lyrics, three seconds per line, 800×600, DPR1/fontScale1,
same FoliaParityLatin-Bold.ttf bytes and FumePreparationProbe layout seed.
Windows10 amd64, i7-12650H (10 cores/16 logical), JDK21.0.5, Desktop offscreen.
Each before/after observation uses the same isolated Gradle test filter and input.
No Android, PSS, sound playback or sustained-frame performance is claimed.

```
gradlew :library:jvmTest --tests "*FumePreparationProbeTest*"
```

| Observation | Article layout ms | Drawing preparation ms | Camera preparation ms | Camera nodes |
| --- | ---: | ---: | ---: | ---: |
| Before caching | 1997 | 110 | 74202 | 114371 |
| After, isolated run | 1992 | 119 | 1308 | 114371 |

Before triggered the existing one-minute coroutine test deadline; failing log
.work/fume-preparation-before.log and metrics .work/fume-preparation-before.txt
are preserved. The deadline, input, integrator tolerance and expected source
values were not changed. The fix prepares each block's EGC/timing plan once
instead of reconstructing it during every derivative/gap candidate query.
Fourteen forward/reverse media queries remain exactly reproducible; after timer
resolution is0ms, not evidence of zero work. These are single-run observations,
not a statistical/device speedup claim. Total first preparation is still~3.4s;
the production entry needs cancellable asynchronous preparation before opening.

FumeCameraTrackTest still meets its unchanged adapted-source1e-5 gate;
FumeCameraStepTest meets the original operator gate. All216 native scene PNGs
and216 shared-source-camera isolation PNGs retain their old SHA256 exactly.
Both Android APK and Desktop compilation pass (.work/fume-cached-scenes-final.log).

## Original boundary evidence

`node verification/classic-browser/probe-fume-scene-boundaries.mjs` passively
exposes original `stepFumeCamera` state through the existing article/text fixture.
Only the test shell return object adds fields; no original algorithm is changed.
`../fume-fixtures/scene-boundaries.json` records actual retarget, pose and focus.

At3s source changes to line1 using a direct bridge, duration.13s, from the prior
finite-step pose (smooth x1622.138378, stepped x1615.537364). At3.15 the original
has already left that bridge. Existing native screen peak14.867149px occurs
there; continuous bridge/spring reconstruction remains a required calibration
task, not an intentional final difference. The source-step port is independently
exact on its fixed-geometry matrix, so the whole-scene residual cannot be called
a source operator parity result.

Overview threshold is9.690000000000001, first original60Hz observation9.7;
retarget duration.4s. Native semantic onset9.69 versus source9.7 contributes to
the overview restore difference at10s (.0169375); scale peak.055318 remains.
No frame delay or phase registration was added. The native continuous response
is an approximation still requiring source calibration. Moving original
wall-clock float to deterministic media time is the necessary pause/seek
adaptation; it does not excuse remaining spring/bridge error.

Fractional801px destination handling is already present and separately isolated
in ../fume-scene-source-camera/comparison.json. These diagnostics do not grant
the future public theme formal visual acceptance.
