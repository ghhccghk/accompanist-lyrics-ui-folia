# Classic visual implementation and reference observations

## User-provided demonstrations — 2026-11-14

Examined six distributed frames per video and their durations (15.16s, 19.90s,
18.47s). These observations are a visual direction, not frame-accurate motion
verification or permission to redistribute their contents.

- https://github.com/user-attachments/assets/af806cf1-f67f-4b88-b2e7-57db507e9e81
- https://github.com/user-attachments/assets/fd27f4f0-64b9-4c57-8c3b-10df767f934b
- https://github.com/user-attachments/assets/704f195a-2194-434b-86e8-8f36290e5cc4

Observed text priorities: staggered group appearance, strong active-word scale,
soft colored halo around a readable body, subtle per-group angle/offset, slower
settling after a word, and whole-line fade away. Some frames show vertically
arranged words and layered previous/next text. Video alone does not establish the
style identifier or the exact Folia version; the pinned source remains normative.
Decorative particles, backdrop, artwork and original lyrics are outside the port.
Videos/frames are only in ignored development storage, not source or sample assets.

## Current implementation and unresolved differences — 2026-11-15

The pinned source, not the demonstrations, defines the target. Detailed source
locations, numerical fixtures, successive repairs and remaining image differences
are in [classic-parity.md](classic-parity.md). Historical observations below are
not claims that an earlier limitation still applies.

- Semantic/sticky grouping follows upstream cjkSemanticLayout and wordSegmentation.
  Display words retain source fragments and internal grapheme timing. Shaping runs
  remain complete; timed glow uses coverage ranges, not independently shaped UTF-16.
  Supplemental-plane letters/numbers, collapsed whitespace and contractions have
  direct original-function fixtures. ICU/Intl dictionary-version differences remain
  possible outside the tested inputs.
- Layout uses the original seed, fractional widths, margins, rotation reach,
  nine passed-angle samples, wrap planner and overlap fit (.5 minimum/.98 slack).
  Eight fixed-font scenes have browser DOM comparisons; Android's remaining
  fractional font advances can still move rounding boundaries and require review.
- Waiting/active/passed body and internal glow, spring/rest thresholds, color,
  continuous blur and whole-line entry/exit are compared with actual original
  Motion values. The 7s breathing keyframes/easing are retained. Curves are anchored
  to media time so pause freezes and seek reconstructs, as required by the host API.
- Full shaped body coverage is cached before word transforms. CSS blur sigma is
  converted at the platform boundary. API30 uses Kotlin software RGBA blur; API34
  and JVM use native layers where representable. Isolated 10/20px primitives match
  original alpha pixels; that does not prove full text-frame equality.
- Optional subtitle theme background/shadow uses the original gradients, padding,
  animation and pinned Tailwind 40px blur. Transparent remains the default. Actual
  blob baselines and fractional centering are accounted for. Whole subtitle frames
  still show raster/edge differences against the browser.
- Halo is cached in fixed masks using three separable box-blur passes.
  Standard uses two masks, Reduced one, Minimal none. Primary masks have 16MiB,
  while all unfinished exits share 16MiB, allocated by the document's peak exit
  count (at least four shares). Text is never dropped to fit a line-count cap.
  Insufficient mask capacity reduces Standard
  to Reduced, then Minimal. Primary fallback is reported through FoliaStatus.
  Retained visible mask pixels are bounded to 32MiB; scratch arrays, source
  rasters, text layouts, transient replaced objects awaiting GC and GPU/native
  overhead are excluded. This is not a whole-process memory ceiling.
- Reduced motion removes pose movement and retains timed color; it is a host
  accessibility option, not a substitute for normal-motion Folia acceptance.
- Pointer hit testing inverses the last drawn line/unit transforms against each
  visible shaped-run rectangle. Space between units does not click; whitespace
  within a preserved shaped run is still part of its rectangle. Semantics and
  Enter/Space activate the original source line. Active-region culling and
  background geometry-version preparation remain outstanding acceptance work.

## Reproducible in-process rendering check

`:library:jvmTest` renders 800×600, density 1, default system font, synthetic
"Hello light", paused 1500ms. It seeks to 2500ms and back and asserts exact pixel
reconstruction plus original callback identity. Screenshot outputs:
`library/build/verification/`. This is a Compose/Skia software test surface,
not a real-window or original-browser comparison. Opaque backdrop is used to
avoid misleading preview treatment of tiny-alpha pixels in transparent PNGs.

`:sample:shared:jvmTest` includes wide/narrow surface snapshots and verifies
scene, speed, loop and resize state. Current snapshots use platform fonts, so
are diagnostic artifacts rather than portable golden images.

Fixed-font browser/JVM and actual API30/API34 device captures now exist. Latest
Android captures include strict paused repeats and inverse seek reconstruction.
Remaining: full-frame visual acceptance, real-window continuous recording,
physical-device/API21..29 coverage, exhaustive RTL/font-scale checks and current
CPU/GPU/memory measurements. Previous performance numbers predate the raster/blur
repairs and do not describe the current renderer. Do not claim exact visual parity
or measured 60Hz.

## Continuous diagnostic sequence — 2026-11-15

Run `./gradlew :library:jvmTest --tests '*VisualSequenceTest*'` to produce
`library/build/verification/sequence/`: two 51-image GIF sequences, PNG checkpoints
every 500ms, pixel SHA-256 CSV and conditions.txt. Original synthetic English
lyrics are rendered at 480×360/density 1, Standard quality, default platform font,
opaque background, 0–5000ms in 100ms media steps. The second sequence enables
reduced motion. Randomly ordered seeks reconstruct every normal checkpoint exactly.

GIF delay represents sampled media time at 10 images/second; this is not measured
frame throughput. PNGs retain full color while GIF is indexed color. These outputs
permit continuous-motion inspection without shipping reference media. This older
system-font sequence does not replace the fixed-font original comparisons in
classic-parity.md; real-window continuous recording remains outstanding.
