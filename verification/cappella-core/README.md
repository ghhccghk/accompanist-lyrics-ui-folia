# Published lyrics-core to Cappella bridge

Actual lyrics-core0.5.0 SyncedLine/MainKaraokeLine/AccompanimentKaraokeLine inputs,
original object identity and SourceRef (including nested accompaniment) retained.
Normalization ordering/diagnostics remain unchanged; source declared bounds and
render-polish bounds stay separate. No alignment-to-sender/chorus inference.
Optional internal agent mapping is by exact SourceRef. Blank/missing/single-agent
cases use the original seeded sender fallback; multiple explicit agents use its
stable mapping. No downloaded avatar or emoji/image assets are introduced.

Only source fragment cuts inside an EGC are coalesced into display words; nested
raw syllables/times remain intact. Complete clusters get the covering time union
in the existing Cappella timing mapper, preserving full shaping. Original source
word-syllable flattening can cut an EGC; that known malformed-input adaptation
is not claimed as identical to original DOM codepoint rendering. Translation and
phonetic remain available independently for later subtitle integration.

32 real-core-equivalent source lines,96 sender observations across three
intensities match unchanged pinned source functions. Original even-line EGC
clocks/renderHints match at1e-10. Additional identity/nested-voice/unsafe-parser/
diagnostic controls and56 existing actual-source renderHints thresholds pass.
Merged EGC end is NOT substituted for raw last-fragment end in renderHints:
A0..900ms + combining mark100..200ms, source100..500ms retains renderEnd.5s,
not a fabricated .9s+hold/exit. Cappella annotation accepts that raw endpoint;
its existing default semantics are unchanged for all internal stage callers.

Four bridge tests,existing reveal/messages tests and Android APK/Desktop compile
pass (.work/cappella-core-bridge.log). No public Cappella/sample entry or new UI/
visual acceptance in this change; pressure projection40.109467px remains open.

Reproduce:
```
node verification/classic-browser/generate-cappella-core.mjs
./gradlew :library:jvmTest --tests '*CappellaCoreAdapterTest*' --tests '*CappellaRevealTest*' --tests '*CappellaMessagesTest*'
```
Source: fixed cc6226e464050a456a2913e062374f23d3844cc5 / AGPL-3.0-only,
utils/lyrics/graphemeTiming.ts,renderHints.ts,cappellaMessageSenders.ts,
cappellaMessages.ts,cappellaIntensity.ts. Fixture source/conditions in
verification/cappella-fixtures/core*. No pixels or device/performance claims.
