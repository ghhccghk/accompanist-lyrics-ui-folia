# Cappella source fixtures

Pinned Folia v0.7.26 / cc6226e464050a456a2913e062374f23d3844cc5, AGPL-3.0-only.
All text synthetic; no artwork/fonts/lyrics from source examples.

`node verification/classic-browser/generate-cappella-reveal.mjs` loads original
modules without algorithm rewriting. TSV UTF-8 text fields are base64. Inputs
contain full lines, words and syllables. Observations retain exact glyph starts,
ends, fades, timestamp readiness, visible/bubble counts. 55 cases,565 glyphs,3172
queries, including empty, unmatched words, repeated text, gaps, punctuation,
non-monotonic/zero/reversed timing, CJK, Arabic, Indic, combining and emoji.

`gradlew.bat :library:jvmTest --tests '*CappellaRevealTest'` compares native
chronological, shuffled and reverse queries. Does not establish rendered parity.
The conditions file records the source DOM codepoint/metrics grapheme discrepancy;
native deliberately retains full EGC display and internal timing, not that bug.
No ordinary-user theme selector exists until a real stage is verified.


`node verification/classic-browser/generate-cappella-messages.mjs` records original
text-only conversations and isolated original viewport decisions with controlled
heights:24 cases/774 messages/864 queries. `CappellaMessagesTest` compares exact
sides/ids/avatar indices/selection/order; not pixel or text measurement acceptance.
The no-image configuration is supported by the original itself. Agent IDs in these
fixtures are explicit source inputs, not guessed from lyrics-core metadata.
