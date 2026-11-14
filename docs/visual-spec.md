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

## Current implementation and deliberate differences

- Safe independent units derive from original fragments and ICU graphemes.
  In-word Latin fragments merge; contextual scripts remain full shaped runs.
  CJK uses conservative source grouping, not upstream semantic segmentation.
- Font size follows the component-width clamp, measured with Compose; four
  bounded fit attempts, minimum 0.5 scale. Very long content stays scrollable.
- Normal-intensity deterministic offsets/scale/rotation follow the source seed.
  Wrapping uses measured safe units plus conservative visual spacing; this is
  not yet the original overflow planner or eight-step rotation-envelope solver.
- Spring is closed-form mass=1. Passed starts from the sampled active endpoint.
  Whole-line epochs and up to four exits can be reconstructed on direct seek.
- Breathing uses a media-time sinusoid, rather than the original wall-clock
  keyframe sequence. Pause freezes it; sparse inputs are not interpolated.
- Halo is cached in fixed masks using three separable box-blur passes.
  Standard uses two masks, Reduced one, Minimal none. A per-line 16MiB mask
  ceiling falls back to Minimal and is reported through FoliaStatus.
  This ceiling is not a measured whole-process or whole-scene memory budget.
- Glow envelope remains provisional, not matched to an upstream exported fixture.
  Per-grapheme glow within a merged shaping unit and dynamic body blur are not
  implemented. Reduce motion removes pose movement and retains timed color.
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

Still required: licensed fixed-font browser comparison at pinned upstream,
continuous transition recordings, Android device checks, RTL/large-font UI and
quantified CPU/GPU/memory measurements. Do not claim visual parity or 60Hz yet.

## Continuous diagnostic sequence — 2026-11-15

Run `./gradlew :library:jvmTest --tests '*VisualSequenceTest*'` to produce
`library/build/verification/sequence/`: two 51-image GIF sequences, PNG checkpoints
every 500ms, pixel SHA-256 CSV and conditions.txt. Original synthetic English
lyrics are rendered at 480×360/density 1, Standard quality, default platform font,
opaque background, 0–5000ms in 100ms media steps. The second sequence enables
reduced motion. Randomly ordered seeks reconstruct every normal checkpoint exactly.

GIF delay represents sampled media time at 10 images/second; this is not measured
frame throughput. PNGs retain full color while GIF is indexed color. These outputs
permit continuous-motion inspection without shipping reference media. Fixed-font
upstream comparison and real-window recording remain outstanding.
