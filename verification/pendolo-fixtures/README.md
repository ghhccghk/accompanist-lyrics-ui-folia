# Pendolo wheel geometry and timeline

Pinned Folia v0.7.26 / cc6226e464050a456a2913e062374f23d3844cc5.
`node verification/classic-browser/generate-pendolo-geometry.mjs` evaluates
unchanged `pendoloGeometry.ts` and `pendoloTimeline.ts` exports through the
locked Vite/pretext environment. The actual text-width declaration is extracted
unchanged from `VisualizerPendolo.tsx`; defaults are checked against `types.ts`.
Inputs are separate from expected outputs; source hashes are recorded.

252 scenarios / 781 wheel rows cover empty/single/long lists, narrow/wide/short
viewports, integer/fractional/outside anchors, ring offsets, measured-height
avoidance, custom tuning and angular offsets. 273 opacity queries check the
unwrapped 110-degree cutoff and 28-degree edge fade (no modulo). 2476 original
timeline calls cover ordinary, coincident, unsorted and overlapping lines,
inclusive render ends, intros/gaps/outros and long-line resumption.

`gradlew :library:jvmTest --tests "*PendoloGeometry*"` compares native numeric
results at 1e-9, opacity at 1e-12, checks reverse/shuffled seeks, reconstruction
after remount and viewport-size restoration. The source keeps a last-valid ref;
native rebuilds it from exact and right-limit boundary events. It never assumes
the last-started line is the last-observed line. No wall clock or RAF accumulator.
Empty lyrics remain empty; source no-lyrics instrumental/gear animation is not
ported or confused with a lyric gap.

The first run had one incorrect handwritten width assertion (174.8px: only the
46%-viewport bound). The original caller also applies the right-edge 48px bound,
which gives 172.4px at 380x480. All pure-export comparisons passed on that run.
The handwritten assertion was replaced by 252 actual original-caller results,
not by a looser tolerance or modified source fixture. Initial failure retained
in .work/pendolo-geometry-build.log; passing build in .work/pendolo-geometry-final.log.

Android debug assembly and Desktop compilation pass. This is an internal
geometry/history kernel, **not** text-layout, tick-animation, whole-theme visual
or real-device acceptance. Text wrapping, subtitles, sweep, tick response,
platform drawing and public/core/sample integration are still required.
