# Cappella shared shaping-cluster opacity (2026-11-18)

Pinned Folia v0.7.26 / cc6226e464050a456a2913e062374f23d3844cc5,
`CappellaBubbleParts.tsx:133–159` ActiveCappellaText and its unchanged original
grapheme fade plan. AGPL-3.0-only. The earlier early-fi observation in
`../cappella-active-ink/early-fi-observation.json` is retained BEFORE this fix.
This repairs paint ownership, not the unresolved 22.188593px dense projection.

## Independent source evidence and cause

The previous painter gave each EGC a disjoint caret interval even when several
EGCs shared ONE shaped glyph. Native caret metrics split fi in half. The original
inline DOM spans instead paint the complete shared glyph with the first LOGICAL
span's opacity. A transparent i must not cut off the right half of fi. This is
a semantic difference, not merely a different rasterizer or accepted exception.

The new 24 original controls use the ACTUAL unchanged ActiveCappellaText, fixed
800×300/DPR1/64px/700 fonts and host geometry from the previous active matrix.
Only original CSS animation currentTime is paused at opaque/transparent endpoints
to isolate each span. No original component, shaping, timing, projection or
geometry algorithm is patched. The full set includes normal/italic fi, ffi, ffl,
RTL lam-alef, separate fj and separate Arabic letters. For each of the five
ligature cases, first-only and all-opaque original PNG hashes are EXACTLY equal;
every continuation-only image is blank. The separate-letter controls remain
nonblank per span and are not merged. Raw hashes, every opacity/DOM rectangle,
browser151.0.7922.34, Motion13.4.3/dom13.4.2/utils13.3.0, both locks and font/source/
host hashes are in `../cappella-fixtures/cluster-ink-v1/conditions.json`.
`paint.tsv` is generated solely from ORIGINAL PNG equality/empty-alpha checks.
It contains no native expected geometry or native rendered data.

The unchanged existing original30 frames are reused; they were NOT recaptured.
Baseline b40c9da native frames are retained in `.work/cappella-cluster-ink-before`;
the caret-only control set is in `.work/cappella-cluster-controls-before`.
The comparator retains one original/before/after union of EVERY nonzero-alpha
pixel in premultiplied RGBA. No registration, low-alpha removal, fitted frame
delay, picked recapture or changed source threshold.

## Production change and platform limits

Preparation reads complete-row shaping cluster source ranges, independently of
the reveal plan. Only paint ownership is merged. A cluster spanning two/three
EGCs keeps the first logical EGC's original clock index; later EGC clocks and
their reveal/count events remain present. The existing complete Compose paragraph
is still drawn ONCE with the alpha field, preserving shaping and overhang.
There is no per-UTF16 shaping, whole-song mask, per-frame platform shaping or
additional native/Rust dependency.

JVM uses the existing Skia public Shaper/RunHandler metadata and closes all font/
shaper handles. Glyph coordinates are NOT transplanted into Compose. Different
fallback faces/unresolved glyphs do not supply merges. Nonzero tracking retains
the existing caret path. Font features use the SAME parseW3 parser as installed
Compose ParagraphBuilder, including `liga 0`, rather than the incompatible
HarfBuzz `liga=0` syntax. Host paragraph and cluster metadata are kept consistent.

Android uses public Paint.getTextRunAdvances on API29+ over complete bidi runs,
with the resolved typeface, size, font features and locale. Android's documented
ligature rule places the complete cluster advance on its first logical character
and zero on continuations. Below29 the existing complete-row/caret path remains;
no hidden API is invoked. This branch COMPILES but has NOT been run on a device
in this work. Custom fallback/locale/font-feature edge cases and API21–28 cluster
parity remain outstanding, not declared intentional final equivalence.

## Results and retained failures

The strict original equality/blank controls first fail the old caret path at12
specific checks (538/702px fi, 1158px ffi, 1207px ffl, 365px lam-alef loss and
their erroneous continuation ink). Failure XML/log are retained as
`.work/cappella-cluster-caret-control-failure.{xml,log}`. All now pass exactly
against the native COMPLETE shaped-row control; this does not mean Chromium and
Skia raster pixels are identical. The preliminary exporter used the Windows
default decoder and garbled Arabic; its invalid TSV/images are preserved under
`.work/*cluster*invalid*`. Explicit UTF-8 fixes the reader without recapturing or
changing original data. A mistaken manual23-case count also failed; the actual
source contains24 observations. That harness failure is retained separately.

| Fixed matrix | Before MAE | After MAE | Before worst64 | After worst64 |
| --- | ---: | ---: | ---: | ---: |
| Original24 span controls | 102.784197 | 7.142305 | 250.666667 | 223.833333 |
| Original30 active fades | 11.677862 | 6.984958 | 223.259843 | 93.000000 |
| Same original italic100ms frame | 143.168309 | 6.700328 | 223.259843 | 21.333333 |

Every frame is in comparison.json. No original30 frame's union MAE regresses.
The worst control remains a separate Arabic-letter paint/raster boundary and
must not be hidden by the ligature improvement. Fixed contact inspected; old
source reports and failures remain. Uniform coverage, transformed whole ink,
pause and arbitrary-order seek, core/source callbacks and real dense presence
regressions pass. All240 dense native PNG hashes and six geometry tables remain
EXACTLY the b40c9da versions; 22.188593px projection is still unresolved.

The production paths also passed one105-request actual DIRECT3D window pass
(focusable=false; all hashes unchanged from b40c9da since its fixture has no
shared ligature), and an independent local Maven JVM render/core identity plus
Android consumer compile and notice/boundary audit. See integration.json.
One ADB check found no attached device. No new Android execution, native picker/
audio, continuous source-theme visual acceptance or performance result is claimed.

## Reproduce

```
node verification/classic-browser/capture-cappella-cluster-ink.mjs
python verification/compare-cappella-cluster-ink.py --export-source
./gradlew :library:jvmTest --tests '*CappellaClusterInkTest' --tests '*CappellaActiveInkTest' --tests '*CappellaCompleteTextTest' --tests '*CappellaPresenceTest' --tests '*PublicCappellaTest' --tests '*CappellaRevealTest' --tests '*CappellaCoreAdapterTest' :library:compileAndroidMain :sample:desktop-app:compileKotlin
python verification/compare-cappella-cluster-ink.py
./gradlew :sample:desktop-app:verifyThemesWindow
python scripts/verify-consumer.py
```

The original exporter refuses overwrite and closes only its own ephemeral-port
server/browser. Preserve original directories before any intentional new capture.
The comparison requires the named preserved pre-fix matrices. Logs:
`.work/cappella-cluster-complete-regression.log`, `cappella-cluster-features.log`,
`cappella-cluster-native-window.log` and `cappella-cluster-consumer.log`.
