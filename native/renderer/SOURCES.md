# Renderer source and dependency ledger

License: AGPL-3.0-only, same as lyrics-ui-folia. No reference application files,
fonts, icons, player, SPW, cryoglyph patch or application dependencies are copied.

| Reference | Fixed revision / path | Destination / use |
| --- | --- | --- |
| Folia-rust-android | d7ff03e083d28069c973cf204b4a8f0a9d5c8594, app/src/main/java/io/github/techuouo520/folia/MonetLyrics.kt | native-renderer-android/.../NativeLyricsSurface.kt: independently implemented per-view worker, TextureView retain/release ordering and worker Choreographer; replaces single poster with per-instance versioned sessions, no player reads |
| Folia-rust-android | same revision, crates/folia-bridge/src/monet.rs | src/android.rs: independently implemented NativeWindow/surface/device lifecycle using wgpu 27; no application bridge or iced dependency |
| Folia-rust-android | same revision, styles/partita.rs and styles/glow.rs in crates/folia-visualizer | inspected, not copied: enlarged translucent text halo is unsuitable for equivalent Partita glow |
| lyrics-ui-folia | fdcc1e5, native/experimental-mask/rust-environment.ps1 | producer build reuses local tool lookup only; CPU experiment runtime is not linked |
| lyrics-ui-folia | fdcc1e5, PartitaLine.kt, partita samplers, classic/GlowPrimitives.kt | FoliaRendererBridge.kt + PartitaNativeScene.kt reuse current prepared cells and samplers; src/gpu.rs and blur/halo/draw.wgsl independently implement the existing three-box kernel/phase, small Gaussian and integer halo alpha on GPU. GPU conformance tests use an independent naive f64 implementation; no reference simplified halo copied |

Transitive Rust dependency versions, sources and checksums are fixed by Cargo.lock.
`cargo metadata --locked` provides the complete dependency graph and license fields.
Before a release candidate, archive dependency license texts alongside corresponding
source and binary hashes. A lockfile alone is not a complete third-party notice bundle.

The reference lock pins its upstream Folia-rust to
39e33e8e89980289edcb01ad1fa9dde2b676ba9f; this renderer does not depend on that repository.
