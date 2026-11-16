# Fume timing oracle

Source: Folia v0.7.26, `cc6226e464050a456a2913e062374f23d3844cc5`,
AGPL-3.0-only. No upstream songs, artwork or fonts copied.

Regenerate with `node verification/classic-browser/generate-fume-reveal.mjs`.
The script verifies a clean pinned `.work/folia-major` and directly imports
`fumeReveal.ts`, `fumeTextStyle.ts`, `fumeMath.ts` and `renderHints.ts` through
the existing Vite reference loader. It does not rewrite the expected functions.

- `inputs.tsv`: 18 synthetic lines, base64 UTF-8 text, source words/syllables.
- `ranges.tsv`: 30 exact word ranges and internal grapheme clocks.
- `samples.tsv`: 1,584 boundary/random samples: integer printing, continuous
  focus progress, next-line cutoff and render-end cutoff.
- `metrics.tsv`: 494 cutoff/glow/retained-text cases, including non-finite
  guards and the source's last-by-start (not maximum-end) hold duration.

`FumeRevealTest` evaluates every sample forward and backward. Complex grapheme
fragments split by lyrics-core are tested separately: native adaptation joins
only unsafe boundaries and retains all original fragments inside the word.
This is not a screenshot fixture and proves no article/camera/render parity.

The source's global duration memo rounds keys to three decimal places; the
generator clears it before each duration query. The port computes the result
without that history-dependent memo. Printing still respects all source
rounding/minimum durations. Infinity in `resolvePassedDimAmount` returns 1
in the original; a later renderer must preserve the caller's hold-style logic
rather than interpreting this helper alone as the complete hold behavior.

## Article geometry

`node verification/classic-browser/generate-fume-article.mjs` executes the original
`fumeArticleLayout.ts` generator and the original `chooseSingleLineFontPx`
declaration. Only its text measurement import is replaced with a labelled seam:
each EGC advances `fontPx * .57`, hard newlines are honored, and each candidate
is measured directly. Unused render-detail imports return no render lines;
the source's placement loop and final block construction remain intact.
`article-inputs.tsv` and `article-layout.tsv` cover 96 layouts / 1,560 blocks,
including empty blocks, fallback heroes, chorus heroes, Unicode and long lines.
These are numerical algorithm fixtures, not a substitute for actual fonts.

With the existing browser server running, use
`node verification/classic-browser/capture-fume-article.mjs` for actual unmodified
`buildArticleLayout` and `createStaticBlockSnapshot`, the registered OFL Latin
font, DPR1, 380/800/1440 by600. Six articles / 150 real-font blocks are recorded
in `article-browser.tsv` and `build/verification/fume-article-browser`.
`FumeArticleFontTest` applies the same font and viewport in Compose, uses the
library's FumeTextMeasure, and writes candidate pictures/CSV into
`build/verification/fume-article-kotlin`. Article/block geometry and font sizes
are checked at 1e-8, without widening the goal after initial failures.

`python verification/compare-fume-article.py` creates the committed contact and
raw-position comparison in `verification/fume-article`. The source pictures
use actual original static snapshots; native pictures are a geometry diagnostic
using drawText, with no final camera, active glow, baseline calibration or
hero text inset. Both include diagnostic block outlines. Current mean RGB
foreground-union MAE70.5127 is **not accepted visual parity**. The source's
middle-baseline/static snapshot renderer remains the next rendering task.

The failed initial integer intrinsic-width candidate is retained in
`.work/fume-article-font-first.log`; the failed hanging-space-only and
fractional-width candidates are also retained. The passing candidate measures
fractional shaped line advance, hangs trailing ASCII spaces for fit only, and
uses hundredth-pixel font quantization. Source text/timings are never trimmed.
Browser font memo heuristics were not copied; more fonts/scripts and Android
runtime still require their own comparison.
