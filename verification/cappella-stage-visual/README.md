# Cappella complete target stage (2026-11-17)

Pinned Folia cc6226e464050a456a2913e062374f23d3844cc5, unchanged whole
VisualizerCappella/MessageRow/BubbleParts, text-only configuration, color avatars.
No reference avatar assets are copied. All synthetic text, including title and
timestamps, passes check-cappella-fonts.py for the recorded OFL faces.

Run the owned local fixture server (`node verification/classic-browser/serve.mjs`),
then `node verification/classic-browser/capture-cappella-stage-visual.mjs`.
Run `:library:jvmTest --tests '*Cappella*'` and
`python verification/compare-cappella-stage.py` from the repository root.
The scripts use the pinned local checkout and installed verification dependencies;
see classic-browser/README.md for environment setup. Images are under
build/verification/cappella-stage-visual-{browser,kotlin}; contact/report are here.

This is a TARGET stage calibration: source geometry is settled with 210 original
RAF ticks at fixed media. Only actual CSS character fades and glow sweep are
explicitly paused/anchored with WAAPI to the same media elapsed time as native.
The original wall-clock behavior is not silently called media deterministic.
No alignment, phase search, pixel registration, palette fitting or expected-value
relaxation is applied. Original target-geometry fixtures remain unchanged.

60 image pairs, four fonts/viewports, three intensities, five times. Native
pause/remount and 60 shuffled revisits are pixel-identical. Initial title wrap
failure is retained in initial-diagnostics.json/initial-contact.png. The corrected
native constraint rounds up its fractional pixel bound rather than truncating a
valid fit-content single line; size-oracle tolerance remains 0.001px.

Known differences: row entry/exit/layout springs, interpolated frame sizes and
timestamp appearance response are not implemented in this target renderer.
Shaping-preserving EGC clips differ from original codepoint DOM spans for some
complex text. CSS versus native outer-shadow kernel, gradient/border coverage,
text antialiasing and timestamp baseline residuals remain. The report measures
them; its average is not a complete-theme pass criterion. Android only builds,
with actual paragraph fallback metrics; no device runtime/performance claim.
