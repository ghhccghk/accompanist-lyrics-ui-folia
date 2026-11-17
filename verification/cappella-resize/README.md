# Continuously mounted original bubble resize

Generate `node verification/classic-browser/capture-cappella-resize.mjs` with
the existing local fixture server. Run
`gradlew.bat :library:jvmTest --tests '*CappellaBubbleResizeTest*'`, then
`python verification/compare-cappella-resize.py`.

Actual pinned AnimatedBubbleFrame remains mounted with a stable React key and
receives source prefix targets, including the original0.2s width lookahead.
The original Motion width/height animation and RAF queue run at exact original
reveal events plus60Hz/sample read points; no native values are supplied to source.
Glyph CSS fades alone are explicitly media-anchored for reproducible text phase.
Inputs: two synthetic Latin lines,160px text width,24.12px bold OFL Latin face,
both sides,480x300,DPR1. All text is covered by the font.

Native CappellaBubbleResize prepares immutable width/height curves at those
reveal events and samples any media time directly. Source lookahead is applied
once, not added again to the animation. Real paragraph reflow/clipping occurs
inside the interpolated frame.92 screenshots,92 shuffled revisits and pause
pixel checks. Contact inspected. Max original DOM width residual0.243535px,
height0.07211px,foreground RGB MAE0.879876. DOM1/64px dimensions,Motion RAF ms
sampling,layout timing and native text/corner raster differences remain; this
is not an exact dynamic geometry pass or a tolerance relaxation. Settled source
target geometry assertions remain0.001px, unchanged. Raw source states and
conditions are in build/verification/cappella-resize-browser.

This seam is a continuously active isolated bubble. Active→passed size/style,
row entry/exit,stack relocation,springs and timestamp appearance still require
whole-stage integration/comparison before any public Cappella selector.
