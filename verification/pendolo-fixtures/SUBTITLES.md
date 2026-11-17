# Pendolo subtitle-track preparation

Same pinned source revision as README.md. Reproduce with
`node verification/classic-browser/generate-pendolo-subtitles.mjs` and
`gradlew :library:jvmTest --tests "*PendoloSubtitles*"`.

The unchanged original pendoloSubtitleTracks/alternateText/subtitleTrackStyle
exports produce 528 configurations / 168 non-empty tracks. Native matches role,
text, order, font rounding, 1.5 line-height, .25em per-row gap, first/secondary
weight500/400, .88 secondary size and .82 opacity. Both/focal/idle/hidden/scale,
translation/romanization fallback, duplicate content, placeholder filtering,
Unicode L/N (including supplementary-plane letters/numbers), FEFF and NEL trim
semantics are covered. A direct placeholder suppresses alternates exactly as
source does; unreadable romanization does not demote the first real track.

Stack-height summation uses an explicitly controlled text-only height callback;
it does **not** claim real wrapping/reserved-space/rendered-subtitle agreement.
That integration and its screen evidence remain required with the wheel stage.
All Pendolo JVM tests, Android debug APK and Desktop compilation pass
(.work/pendolo-subtitles.log). Unicode-property runtime behavior was executed on
Desktop; old Android devices are not verified. No new public theme selector.
