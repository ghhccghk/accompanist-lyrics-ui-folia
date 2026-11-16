package top.ghhccghk.accompanist.lyrics.folia

import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.*
import top.ghhccghk.accompanist.lyrics.folia.internal.model.normalize
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.fume.*
import java.io.File
import java.util.Base64
import kotlin.test.*

class FumeRevealTest {
    private fun rows(name: String) = File("../verification/fume-fixtures/$name.tsv").readLines()
        .filterNot { it.startsWith("#") || it.isBlank() }.map { it.split('\t') }
    private fun decode(text: String) = String(Base64.getDecoder().decode(text), Charsets.UTF_8)
    private fun number(text: String) = when (text) { "Infinity" -> Double.POSITIVE_INFINITY; "NaN" -> Double.NaN; else -> text.toDouble() }
    private fun inputs() = rows("inputs").associate { f ->
        f[0] to FumeLyric(decode(f[1]), f[2].toDouble(), f[3].toDouble(),
            if (f[4] == "-") emptyList() else f[4].split('|').map { descriptor ->
                val w = descriptor.split('~')
                FumeWord(decode(w[0]), w[1].toDouble(), w[2].toDouble(),
                    if (w[3].isEmpty()) emptyList() else w[3].split(';').map { s ->
                        val t = s.split('@'); FumeWord(decode(t[0]), t[1].toDouble(), t[2].toDouble())
                    })
            })
    }
    @Test fun exactSourceRangesAndInternalTimings() {
        val prepared = inputs().mapValues { FumeReveal(it.value) }
        val expected = rows("ranges")
        assertEquals(expected.size, prepared.values.sumOf { it.ranges.size })
        for (f in expected) {
            val range = prepared.getValue(f[0]).ranges[f[1].toInt()]
            assertEquals(f[2].toInt(), range.start, f.toString())
            assertEquals(f[3].toInt(), range.end, f.toString())
            val timings = if (f[4] == "-") emptyList() else f[4].split(';').map { t -> t.split(',').map(String::toDouble) }
            assertEquals(timings.size, range.timings.size)
            timings.zip(range.timings).forEach { (e, a) ->
                assertEquals(e[0], a.start, 1e-10, f.toString())
                assertEquals(e[1], a.finish, 1e-10, f.toString())
            }
        }
    }
    @Test fun printingAndContinuousFocusMatchSourceAfterReverseSeek() {
        val prepared = inputs().mapValues { FumeReveal(it.value) }
        val samples = rows("samples")
        // No accumulated events, frame rate or wall clock: identical after arbitrary history.
        for (f in samples + samples.asReversed()) {
            val a = prepared.getValue(f[0]); val t = f[1].toDouble()
            assertEquals(f[2].toInt(), a.count(t), f.toString())
            assertEquals(f[3].toDouble(), a.progress(t), 1e-10, f.toString())
            assertEquals(f[4].toDouble(), fumePassCutoff(a.line, null), 1e-10, f.toString())
            assertEquals(f[5].toDouble(), fumePassCutoff(a.line, 1.5), 1e-10, f.toString())
        }
    }
    @Test fun sourceRetainedTextAndGlowCutoffFunctions() {
        val inputs = inputs()
        for (f in rows("metrics")) {
            val actual = when (f[0]) {
                "progress" -> fumeVisualProgress(number(f[1]), number(f[2]), number(f[3]), number(f[4]))
                "dim" -> fumePassedDim(number(f[1]), number(f[2]), number(f[3]))
                "glow" -> fumeGlowEnvelope(number(f[1]), number(f[2]))
                "duration" -> fumePassedDuration(if (f[1] == "-") emptyList() else f[1].split(',').map(inputs::getValue), number(f[2]))
                "style" -> {
                    val style = fumePassedStyle(f[1].toBoolean(), f[2].toBoolean())
                    assertEquals(f.drop(3).map(::number), listOf(style.opacity, style.glow, style.shadowBase, style.shadowTrail))
                    continue
                }
                else -> error("Unknown fixture ${f[0]}")
            }
            val expected = number(f.last())
            if (expected.isInfinite()) assertEquals(expected, actual, f.toString())
            else assertEquals(expected, actual, 1e-10, f.toString())
        }
    }
    @Test fun coreAdapterKeepsParserTimingsAndProtectsCrossFragmentGraphemes() {
        val parts = listOf("A", "́ ", "👩", "‍🚀", "tail")
        val source = KaraokeLine.MainKaraokeLine(parts.mapIndexed { i, text -> KaraokeSyllable(text, i * 100, (i + 1) * 100) },
            null, KaraokeAlignment.Unspecified, 0, 500)
        val native = fumeLyric(normalize(SyncedLyrics(listOf(source))).lines.single())
        assertEquals(parts.joinToString(""), native.text)
        assertEquals(listOf("Á ", "👩‍🚀", "tail"), native.words.map { it.text })
        assertEquals(listOf("A", "́ "), native.words[0].syllables.map { it.text })
        val times = fumeWordTimings(native.words[0])
        assertEquals(0.0, times[0].start)
        assertEquals(.15, times[0].finish, 1e-10)
        assertEquals(2, times[0].end)
        val astronaut = fumeWordTimings(native.words[1]).single()
        assertEquals(.2, astronaut.start)
        assertEquals(.4, astronaut.finish)
        // Words are not classic sticky-retimed, and the display string stays whole for shaping.
        assertEquals(.4, native.words.last().start)
        assertEquals(7, FumeReveal(native).count(.5)) // Á, space, astronaut, t,a,i,l
    }
}
