package top.ghhccghk.accompanist.lyrics.folia.internal.model

import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import com.mocharealm.accompanist.lyrics.core.model.synced.UncheckedSyncedLine

internal data class SourceRef(val index: Int, val accompanimentIndex: Int? = null)
internal data class TimedFragment(val begin: Int, val end: Int, val startUs: Long, val endUs: Long)
internal data class NormalizedLine(
    val ref: SourceRef,
    val source: ISyncedLine,
    val text: String,
    val translation: String?,
    val phonetic: String?,
    val startUs: Long,
    val endUs: Long,
    val accompaniment: Boolean,
    val fragments: List<TimedFragment>,
)
internal data class Diagnostic(val ref: SourceRef, val reason: String)
internal data class NormalizedDocument(val lines: List<NormalizedLine>, val diagnostics: List<Diagnostic>)

/** No source mutation, no toString fallback, no invented word timing for line lyrics. */
internal fun normalize(lyrics: SyncedLyrics): NormalizedDocument {
    val result = mutableListOf<NormalizedLine>()
    val diagnostics = mutableListOf<Diagnostic>()
    val seen = mutableListOf<ISyncedLine>()
    fun add(source: ISyncedLine, ref: SourceRef) {
        if (seen.any { it === source }) return
        seen += source
        if (source.end < source.start) {
            diagnostics += Diagnostic(ref, "ReversedLineTime")
            return
        }
        val text: String
        val translation: String?
        val phonetic: String?
        val fragments: List<TimedFragment>
        when (source) {
            is KaraokeLine -> {
                text = source.syllables.joinToString("") { it.content }
                translation = source.translation
                phonetic = source.phonetic?.takeIf { it.isNotBlank() } ?: buildString {
                    source.syllables.forEach { syllable ->
                        syllable.phonetic?.takeIf { it.isNotBlank() }?.let {
                            if (isNotEmpty()) append(syllable.phoneticSeparatorBefore)
                            append(it)
                        }
                    }
                }.takeIf { it.isNotBlank() }
                var offset = 0
                fragments = source.syllables.map { syllable ->
                    val begin = offset
                    offset += syllable.content.length
                    if (syllable.start < source.start || syllable.end > source.end) {
                        diagnostics += Diagnostic(ref, "FragmentOutsideLine")
                    }
                    TimedFragment(begin, offset, syllable.start.toLong() * 1000, syllable.end.toLong() * 1000)
                }
            }
            is SyncedLine -> {
                text = source.content; translation = source.translation; phonetic = source.phonetic
                fragments = emptyList()
            }
            is UncheckedSyncedLine -> {
                text = source.content; translation = source.translation; phonetic = source.phonetic
                fragments = emptyList()
            }
            else -> {
                diagnostics += Diagnostic(ref, "UnsupportedLineType")
                return
            }
        }
        result += NormalizedLine(ref, source, text, translation, phonetic,
            minOf(source.start.toLong() * 1000, fragments.minOfOrNull { it.startUs } ?: Long.MAX_VALUE),
            maxOf(source.end.toLong() * 1000, fragments.maxOfOrNull { it.endUs } ?: Long.MIN_VALUE),
            source is KaraokeLine.AccompanimentKaraokeLine, fragments)
    }
    lyrics.lines.forEachIndexed { index, source ->
        add(source, SourceRef(index))
        (source as? KaraokeLine.MainKaraokeLine)?.accompanimentLines?.forEachIndexed { nested, line ->
            add(line, SourceRef(index, nested))
        }
    }
    return NormalizedDocument(result.sortedWith(compareBy({ it.startUs }, { it.ref.index },
        { it.ref.accompanimentIndex ?: -1 })), diagnostics)
}
