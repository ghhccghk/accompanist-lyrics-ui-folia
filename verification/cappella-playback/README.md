# Cappella continuous mounted-stage comparison

Pinned original: Folia v0.7.26 / cc6226e464050a456a2913e062374f23d3844cc5.
This is an internal media-time stage, not final theme acceptance or a public selector.

## Scope and reproduction

1. Run `node verification/classic-browser/serve.mjs` (loopback port 4173).
2. Run `node verification/classic-browser/capture-cappella-playback.mjs`.
3. Run `gradlew :library:jvmTest --tests "*CappellaPlaybackTest*"`.
4. Run `python verification/compare-cappella-playback.py`.

Original complete Cappella JSX stays mounted for 14 media seconds, with eight
synthetic text-only messages. Six font/viewport/intensity configurations have
35 probes each: Latin 380×480 in all three intensities, wrapping Latin and Arabic
800×600, CJK 1200×600. Both renderers use the same covered OFL fonts; the Kotlin
stage measures and prepares its own layout, without source geometry inputs.
Font coverage/source hashes are in the recorded capture conditions.

The source RAF/performance clock advances at 60 Hz plus exact probes, in separate
browser tasks to preserve Motion's microtask time cache. Media updates a
MotionValue. Host React props change on selected-line changes, as documented in
original `stores/usePlaybackStore.ts:31–34` and guarded in
`components/app/playback/bridges/usePlaybackVisualizerBridge.ts:239–242`.
This leaves original local prefix/timestamp subscriptions intact.

Synthetic RAF does not advance document.timeline. Original native WAAPI effects
therefore receive media elapsed time relative to their original startTime; their
original completion callback is delivered once at the computed end. CSS transition
creation time shares that clock; original glyph fade/sweep effects are explicitly
media anchored. This is controlled playback, not ordinary wall-clock browser
playback. The original repository files are unchanged. Exiting message identities
come from passive React props observation rather than guessed visible targets.

## Result and limits

210 pairs / 594 original rows; native random revisit of every probe, fixed-media
pause, unmount/remount, reduced-motion pause, and exact source presence/draw order
all pass. Mean foreground RGB MAE **3.390252085680098**; maximum bubble/avatar
geometry residual **3.6372003149749617 px**, group×row opacity residual
**0.013715527333574395**. These are measured residuals, not new acceptance bounds.
Existing settled-target 0.001 px assertions have not changed. See comparison.json,
residual.tsv and contact.png. Raw PNGs/conditions/states are under build/verification.

Native state includes source render-end annotation (last-word semantics), inclusive
end boundaries, prefix lookahead/resize, active/passed physical springs, row
entry/exit, popLayout flow removal with position compensation, stable drawing
order, separate avatar fade and timestamp response. No frame accumulation or
background animation coroutine drives these values; pause and arbitrary seeks
reconstruct directly from the prepared media events.

Remaining work: RAF millisecond/sample and CSS fractional flow differences;
complex shaping/raster/shadows; source layout projection interruptions under
overlap, rapid removal/reappearance and viewport changes; core/public bridge and
device validation. Text-only color avatars deliberately use no unknown assets;
emoji/image message support is absent, not an equivalent replacement.

## Diagnostics preserved

The initial source harness forced a parent React render every RAF. Source layout
projection then restarted during bubble resize, holding one row for about 0.2 s.
The original store explicitly separates those update streams. The forced-parent
scenario is still reproducible with `?parentRaf=1`; its 210 PNGs/states are retained
in build/verification/cappella-playback-browser-forced-parent-raf, and reports in
.work/cappella-playback-forced-parent-raf.*. It is a separate host stress case,
not silently overwritten as a visual improvement. Its native residual reached
38.727571 px after initial projection work; it remains unresolved.

Earlier synchronous multi-RAF task and unanchored document.timeline captures
were invalid oracles, retained with explicit invalid names under build/.work.
The latter kept entering rows invisible because WAAPI startTime was ahead of a
frozen document clock; its MAE is not an implementation assessment. No original
expected values or tolerances were edited to make those captures pass.
