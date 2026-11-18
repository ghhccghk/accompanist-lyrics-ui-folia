# Diorama public experimental debug preview — 2026-11-18

User authorized opening the existing real text/camera renderer for debugging.
`FoliaStyle.Diorama` routes real published core0.5.0 objects into the existing
perspective/text/follower stage. This is integration, not a replacement upstream
oracle. Original mounted/48-frame/compositing evidence remains frozen in the
previous verification directories. Visual parity is still unaccepted.

`PublicDioramaTest` compares the public entry with the same internal stage
pixel-for-pixel, checks real source refs and actual base-ink projected gesture
targets, blank fallthrough, pause/seek/offset, resize/system font scale,
reduced motion, switch/remount, oversize error and replacement recovery.
Budget bypass has a public option, sample toggle and persisted preference;
switching it rebuilds without changing in-budget rendering. Neither disabled
checks nor the tests allocate deliberate over-budget textures.

`public-800.png` and `public-380.png` use stage widths800/380 inside the same
800x600 root (unused right side stays background), media1600ms and the same
bundled redistributable Latin font. See `validation.json` for hashes/conditions.
These are deterministic offscreen Compose, not actual device or real window
screenshots. No source-facing acceptance threshold was changed.

Failures retained:

- Initial offset equality exposed stale fallback pixels: non-state sampler
  completion did not invalidate Canvas. Completion now publishes its prepared
  index; paused pixels redraw after owners finish. Offset equality then passed.
- Equal-value replacement test used default structural Compose state, so the
  new object was never delivered. The harness now uses referential state to
  actually supply that input; source-identity requirement is unchanged.
- Budget rebuild exposed an old ready count briefly paired with Preparing.
  Preparation callback now clears counters atomically; readiness waits for
  positive count and no preparing diagnostic. Narrow screenshots are captured
  after preparation, not the loading placeholder. Original failure retained.
- First compile exposed the new allocation callback stealing a trailing
  cancellation lambda. Existing prepare signature restored; bounded preparation
  has a separately named internal function. Original cancellation test remains.

Run the six suites in `validation.json`, Android library/shared compilation and
Desktop compilation, then `python verification/diorama-public/record-validation.py`.
Maven-coordinate independent consumer includes a separate Diorama preparation
test. No Android install, playback-process device test, performance measurement,
Android fragment/F16 equivalence or full depth/cross-song sequencing is claimed.
