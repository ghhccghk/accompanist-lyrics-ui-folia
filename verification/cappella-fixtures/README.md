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


Bubble/text: run existing `node verification/classic-browser/serve.mjs`, then
`node verification/classic-browser/capture-cappella-bubble.mjs`,
`gradlew.bat :library:jvmTest --tests '*CappellaBubbleTest'`,
`python verification/compare-cappella-bubble.py`.
48 original-component/native pairs use identical synthetic cadenza scene inputs
and OFL font bytes,480x300,DPR1, two sides/six times. Conditions record scope:
frame TARGET size only, original CSS fades deliberately anchored with WAAPI to
media elapsed time. Does not test original row/stack/springs/resize/shadow/glow.
76 prefix sizes exact;233 computed CSS alpha samples within1e-6 (CSS serialization).
Mean foreground-union RGB MAE0.95330035, residuals in report/contact. Raw images
stay in build/verification; hashes and contact are versioned. Not a full theme.


Full-stage target geometry (not native full-stage visual acceptance):
`python verification/check-cappella-fonts.py`, then
`node verification/classic-browser/capture-cappella-stage.mjs`,
`gradlew.bat :library:jvmTest --tests '*Cappella*'`.
The capture mounts the ORIGINAL complete component with generated color avatars,
no emoji, fixed media/210 RAF steps.60 scenes/228 rows/2508 target fields have
max5.859375e-5px residual with independent native measurement;1e-3 bound unchanged.
248 actual browser Canvas probes match JVM exactly, including kerning/ligatures.
JVM static-font metric conversion is not implemented/validated for Android or
variable fonts: those use actual paragraph metrics. No false availability flag.

The new stage checks font cmap coverage for body/title/timestamps using existing
Bidi and separate ChatCJK fonts. Old bubble/Cadenza fonts/reports retained; the
old CJK subset missed some synthetic words, so they must not establish fallback-
free complex-text equivalence. Initial stage fallback/ceil failures remain in
verification/cappella-stage/initial-diagnostics.json. No tolerance relaxation.
