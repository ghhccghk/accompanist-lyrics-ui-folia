// Copyright (c) 2026 chthollyphile and contributors
// Folia cc6226e4 pendoloSubtitleTracks.ts, alternateText.ts, subtitleTrackStyle.ts.
// SPDX-License-Identifier: AGPL-3.0-only. See docs/upstream.md.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.pendolo

import kotlin.math.floor

internal enum class PendoloSubtitleMode { NONE, TRANSLATION, ROMANIZATION, BOTH }
internal enum class PendoloSubtitleRole { ROMANIZATION, TRANSLATION }
internal data class PendoloAlternateText(val role: PendoloSubtitleRole, val text: String)
internal data class PendoloSubtitleSource(val translation: String? = null, val romanization: String? = null,
    val alternates: List<PendoloAlternateText> = emptyList())
internal data class PendoloSubtitleTrack(val role: PendoloSubtitleRole, val text: String,
    val fontPx: Double, val lineHeight: Double, val weight: Int, val gapEm: Double, val opacity: Double)

private val readable = Regex("[\\p{L}\\p{N}]")
private fun jsSpace(c: Char) = c in "\t\n\u000b\u000c\r \u00a0\u1680\u2028\u2029\u202f\u205f\u3000\ufeff" || c in '\u2000'..'\u200a'
private fun String.trackText() = trim(::jsSpace).takeIf { it.isNotEmpty() }

/** Readable means any Unicode Letter or Number (including Nl/No), not just
 * ASCII or digits. Filtering precedes de-duplication and secondary-row sizing. */
internal fun pendoloSubtitleTracks(source: PendoloSubtitleSource, mode: PendoloSubtitleMode,
    hidden: Boolean = false, focal: Boolean = true, fontScale: Double = 1.0): List<PendoloSubtitleTrack> {
    if (hidden || mode == PendoloSubtitleMode.NONE) return emptyList()
    val roles = when (mode) {
        PendoloSubtitleMode.BOTH -> listOf(PendoloSubtitleRole.ROMANIZATION, PendoloSubtitleRole.TRANSLATION)
        PendoloSubtitleMode.ROMANIZATION -> listOf(PendoloSubtitleRole.ROMANIZATION)
        else -> listOf(PendoloSubtitleRole.TRANSLATION)
    }
    val base = (if (focal) 16 else 12) * fontScale
    val found = mutableSetOf<String>()
    return buildList {
        for (role in roles) {
            val text = (if (role == PendoloSubtitleRole.ROMANIZATION) source.romanization else source.translation)?.trackText()
                ?: source.alternates.firstNotNullOfOrNull { if (it.role == role) it.text.trackText() else null }
                ?: continue
            // Like source, a non-readable direct field suppresses its alternate;
            // fallback is only for empty direct values, not placeholder content.
            if (!readable.containsMatchIn(text) || !found.add(text)) continue
            val secondary = isNotEmpty()
            val font = floor((if (secondary) base * .88 else base) + .5)
            add(PendoloSubtitleTrack(role, text, font, font * 1.5,
                if (secondary) 400 else 500, .25, if (secondary) .82 else 1.0))
        }
    }
}

/** Caller provides each track's actual text-only wrap height with the same
 * resolved font/width as drawing. The first gap is .25 of that row's own em. */
internal fun pendoloSubtitleHeight(tracks: List<PendoloSubtitleTrack>, measure: (PendoloSubtitleTrack) -> Double): Double =
    tracks.sumOf { measure(it) + it.gapEm * it.fontPx }
