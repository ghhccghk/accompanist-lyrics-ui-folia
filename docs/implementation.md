# Implementation and verification log

Date: 2026-11-14. This file describes actual implementation; design documents
remain the roadmap and must not be treated as an existing API support list.

## Starting point

- Independent repository initialized on `codex/initial-implementation`.
- Four existing design documents saved unchanged in af528a8.
- No applicable AGENTS.md found in this directory or its ancestors.
- Accompanist is read-only. Its pre-existing .idea modifications/untracked files
  were observed before work; do not remove, stage or commit them.
- User selected AGPL-3.0-only after fixed-source declaration review.

## P0 — scaffold built

Independent library, shared sample and two platform entry points created.
No Folia renderer port, animation, import, performance claims or Rust backend yet.
Verified `:sample:desktop-app:classes :sample:android-app:assembleDebug` with the
independent wrapper: BUILD SUCCESSFUL. Both platform entry points compile;
actual window/device interaction is pending the first renderer milestone.

Initial host: Windows x64, Android SDK platform 37; an adb device is available.
First attempt with JBR 25.0.4 failed native allocation under host memory pressure.
Retried successfully with Temurin JDK 21.0.5, two workers, in-process Kotlin and
1536 MiB Gradle heap. No local runtime/SDK path
is committed. Distribution binaries are obtained through the independent wrapper.

## Outstanding acceptance

Classic shaping/layout/visual comparison, deterministic animation, playback,
import, lifecycle, publication consumption and performance remain to implement.
minSdk 21 is the library target; Android sample starts at 23. Actual manifest
merging and device checks determine the verified matrix. Low-API Unicode and
blur behavior have not yet been experimentally verified.

## P1a — core adaptation and interval queries

Implemented actual 0.5.0 karaoke, synced and unchecked models; source identity,
nested accompaniment, exact text and phonetic separators are retained. Unknown
types and reversed lines produce diagnostics. Line-only lyrics retain line-only
timing. Normalized intervals cover outlying fragments without rewriting sources.

Sweep deltas with 64-boundary checkpoints support unordered overlapping inputs,
half-open endpoints, gaps and random seek without a per-frame full-song scan.
Verified `:library:jvmTest :library:compileAndroidMain`: five tests passed,
including 2,000 seeded random queries against a naive overlap oracle.
Visual display/epochs are not yet implemented at this milestone.
