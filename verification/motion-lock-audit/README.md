# Exact original Motion transitive lock recertification

Pinned Folia cc6226e464050a456a2913e062374f23d3844cc5.
Original package-lock:framer-motion13.4.3,motion-dom13.4.2,motion-utils13.3.0.
Historical verification installed range-updated transitives13.5.1/13.5.0;
not every historical server cache version is proven. This audit validates the
CURRENT source captures with exact installed versions and a new isolated server.

Original capture hosts, fonts, inputs, clocks/probes and source algorithms unchanged.
serve.mjs exports startReferenceServer(port), isolated port4177/cache; capture
scripts accept FOLIA_REFERENCE_URL, default remains4173. Existing user/reference
servers are untouched. One headless browser at a time. All close in finally.
Previous full source images/states/fixture bytes preserved in ignored
.work/motion-lock-audit. Audit refuses to overwrite a baseline; optional argv2
selects another plain run-directory name. DO NOT repeat to choose better frames.

Pendolo public-stage:45 PNG hashes unchanged; all four input/frame/row/conditions
files unchanged. This recertifies that original public45-frame matrix only;
older dense tick/chorus/resize matrices have not all been re-certified.
Cappella regular playback/public:210 frames,20 PNG hashes differ. Numeric
frame/geometry/presence/opacity TSV bytes unchanged. Raw source pixel differences
are17..143 pixels per changed frame,peak channel34,full image MAE at most.00075841.
Three passive glyph-count observations differ (wrap1.6s/6s/14s); all other observed
states identical. These are retained, NOT ignored as a threshold exemption.
Fresh cache/DOM event delivery/graphics/dependency cause is not individually isolated.
No claim that changed dependencies alone caused these tiny glyph-edge differences.
All hashes, raw differences and state paths retained in comparison.json.
Source-contact compares predefined changed frames, no registration/alpha cut.

Actual public Cappella JVM capture/check rerun after exact source recapture:
MAE2.32597280485,geometry peak3.63720031497px,opacity.01371552733,presence mismatches0.
Previous public MAE2.32595410327/report/contact retained alongside this README.
Foreground union changes slightly; DO NOT call the aggregate difference a fixed-mask
improvement. Existing public vs internal pixel equality checks unchanged/passed.
Dense240-frame Cappella pressure matrix is NOT re-certified by this regular210 run;
22.188593px historical pressure residual remains unresolved, not accepted.
No changed source geometry expected values, no production code edits, no gate changes.
No Android runtime/real Desktop window/performance acceptance.

```
node verification/classic-browser/audit-motion-lock.mjs
python verification/compare-motion-lock.py
./gradlew :library:jvmTest --tests '*PublicCappellaStageTest'
python verification/compare-cappella-playback.py --public
```
First command is a one-shot preserve-and-capture audit; baseline must not already
exist (use a new optional run name for an intentional later audit, retain old evidence).
Fresh Gradle JVM compile and restored Fume regressions also pass in
.work/fume-restored-and-motion-lock.log. All255 source capture pages ran in actual
Chromium headless; this is not an Android or real application window run.


## Dense Cappella exact-lock follow-up (2026-11-18)

The prior pending dense240 statement is historical as of this section. The older
Pendolo dense tick/chorus/resize matrices remain pending; do not recapture unrelated
regular210, Pendolo public45 or Fume mounted92 without a concrete reason.

`audit-cappella-pressure-lock.mjs` preserves the COMPLETE prior source/native
frames, states, tables and comparison under.work/cappella-pressure-lock-v1 BEFORE
recapture. Same original240 probes/999 rows, rapid+overlap, three intensities,
380x480/DPR1/font/inputs/source algorithms/clocks; new dedicated4182 cache/server.
Actual Chromium151.0.7922.34, Motion13.4.3/dom13.4.2/utils13.3.0, both lock/font
hashes and conditions are in cappella-pressure.json. No source formula changed.

All three fixture TSV hashes (inputs, frames and geometry/presence/alpha) unchanged;
all passive states unchanged.38 of240 PNGs change8..1578 pixels, raw channel peak42,
full-image MAE peak0.01725877193. Compare retains EVERY image hash/raw difference;
source-contact shows the first three changed filenames, no cherry-picking,
registration or alpha suppression. Visual inspection shows small glyph/glow-edge
changes while the large stacking error remains a separate Kotlin problem. The
cause has NOT been isolated to dependency versions. This is not final acceptance.

Run the preserved240 JVM matrix against the exact source, including random seek,
pause, remount and presence/order checks. Its numeric22.1885925046px residual is
still required work; no regular210 result substitutes for it. Native geometry
exports now append rowTop/rowHeight for diagnosis; original compared columns and
all assertions stay unchanged. Old reports/images remain preserved.

```
node verification/classic-browser/audit-cappella-pressure-lock.mjs
python verification/compare-cappella-pressure-lock.py
./gradlew :library:jvmTest --tests '*CappellaPresenceTest' --tests '*CappellaPresenceTrackTest'
python verification/compare-cappella-playback.py --presence
```

The historical pressure comparison uses a>8 foreground cutoff. Keep that legacy
metric for continuity; it is not sufficient for current low-alpha visual acceptance.
Any repair comparison must use a SINGLE original/before/after all-nonbackground
union (including halo), retaining the old report and numeric geometry failures.
No Android/device/real Desktop-window/performance conclusion from this audit.

## Pendolo dense tick / chorus / CSS-width follow-up (2026-11-18)

The earlier pending statements for these three matrices are historical as of
this section. `audit-pendolo-motion-lock.mjs` preserves complete old source/native
images, states, fixtures and reports under `.work/pendolo-motion-lock-v1` before
capture. A dedicated4184 server/cache and one sequential browser capture at a
time leave existing servers untouched. The actual original hook/subtree generator
is unchanged. Motion13.4.3/dom13.4.2/utils13.3.0, Chromium151.0.7922.34, both lock
hashes, all three font hashes, generated-source hash and original conditions are
recorded in `pendolo-dense.json`.

All numeric/input/condition fixture bytes and observed states are identical.
Tick144 frames:4 PNGs differ3..19 pixels. Chorus168 full frames:6 PNGs differ2..19
pixels. The168 halo-only PNGs are identical. Maximum raw channel difference4,
full-image MAE0.0000486112. No registration, alpha cutoff or selected recapture;
the contact shows the first two changed names per matrix. Small edge differences
are retained and are NOT proven to result from a dependency change. Isolated
width-response probes are actual Blink CSS interpolation/reversal, not a Motion
spring matrix; their142 event/query rows are byte-identical as well.

JVM recapture:6 tick-wheel and7 chorus-wheel cases, plus the separate width tests,
pass with existing pause/random-seek/remount assertions. Logs:
`.work/pendolo-exact-lock-wheel-test.log`, `.work/pendolo-exact-lock-test.log`.
Same residuals remain: primary tick geometry1.364706px, wheel0.325106deg,
source-rotation isolation0.105115px; chorus6.019072px/isolation0.231502px.
Recomputed historical>8 foreground means13.137673/11.918339 are not a production
improvement or new acceptance gate. Full results replace only current comparison
reports; complete old reports are preserved. No Android/visible-window/physical
performance conclusion. The public theme was already integrated in b0ed98e.

```
node verification/classic-browser/audit-pendolo-motion-lock.mjs
python verification/compare-pendolo-motion-lock.py
```

The audit refuses to overwrite its baseline. Do not rerun the full capture without
a new concrete reason. Comparators run AFTER capture completion, never concurrently.
