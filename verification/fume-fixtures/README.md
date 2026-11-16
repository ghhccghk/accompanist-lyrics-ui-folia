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
