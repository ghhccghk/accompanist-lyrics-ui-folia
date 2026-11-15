# Third-party sources and dependencies

No lyrics, covers, icons or fonts have been copied from reference applications.
Synthetic sample text is written for this repository. Samples use platform defaults;
verification/assets contains unmodified Noto Sans Bold and Noto Sans Arabic Bold,
plus a renamed Latin face, separate body/subtitle CJK subsets and a merged mixed-direction comparison
face under SIL OFL 1.1 with their license,
copyright metadata and provenance. They are used by the comparison harness only.

| Component | Version | License / role |
| --- | --- | --- |
| Folia | v0.7.26 / cc6226e464050a456a2913e062374f23d3844cc5 | AGPL-3.0 as declared; handled as version 3 only. See docs/upstream.md. |
| Gradle wrapper | 9.5.0 | Apache-2.0; official Gradle distribution scripts and wrapper JAR |
| Kotlin | 2.4.10 | Apache-2.0; compiler and standard library |
| Compose Multiplatform | 1.12.0 | Apache-2.0; UI and Desktop runtime |
| Android Gradle Plugin | 9.3.2 | Apache-2.0; build tooling |
| AndroidX Activity Compose | 1.13.0 | Apache-2.0; Android sample entry |
| lyrics-core | 0.5.0 | Apache-2.0; published model dependency, not lyrics-ui |
| ICU4J | 78.3 | Unicode-3.0; grapheme segmentation, same implementation on Android/JVM |
| kotlinx.coroutines | 1.11.0 | Apache-2.0; asynchronous sample import and settings I/O |
| Tailwind CSS | 4.3.3 | MIT; verification-only compiler/generated subtitle utilities, matching the pinned Folia lockfile |

This inventory covers direct dependencies. Transitive binary/source notices and
packaging must be audited before distribution. No remote publication is configured.
