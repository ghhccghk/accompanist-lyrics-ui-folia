package top.ghhccghk.accompanist.lyrics.folia

import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.*
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import top.ghhccghk.accompanist.lyrics.folia.internal.model.*
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama.*
import java.io.File
import java.util.Base64
import kotlin.test.*

class DioramaTextTest {
    private fun rows(name: String) = File("../verification/diorama-fixtures/$name.tsv").readLines()
        .filterNot { it.startsWith('#') || it.isBlank() }.map { it.split('\t') }
    private fun text(value: String) = String(Base64.getDecoder().decode(value), Charsets.UTF_8)

    @Test fun actualOriginalTimingAndWordGrouping() {
        val expected = rows("text").groupBy { it[1] }
        var glyphs = 0
        var units = 0
        var peak = 0.0
        for ((key, input) in rows("text-inputs").groupBy { it[1] }) {
            val source = input.first { it[0] == "L" }
            val words = input.filter { it[0] == "W" }.map { w ->
                val syllables = input.filter { it[0] == "S" && it[3] == w[2] }.map { s ->
                    DioramaTimedText(text(s[4]), s[5].toDouble(), s[6].toDouble())
                }
                DioramaTimedText(text(w[3]), w[4].toDouble(), w[5].toDouble(), syllables)
            }
            val line = DioramaLyric(text(source[3]), source[4].toDouble(), source[5].toDouble(), words)
            val timeline = dioramaLineTimeline(line)
            val groups = dioramaUnits(line, timeline)
            val original = expected[key].orEmpty()
            val gs = original.filter { it[0] == "G" }
            val us = original.filter { it[0] == "U" }
            assertEquals(gs.size, timeline.size, "$key grapheme count")
            assertEquals(us.size, groups.size, "$key unit count")
            for ((i, g) in timeline.withIndex()) {
                val e = gs[i]
                assertEquals(text(e[3]), g.text, "$key/$i text")
                assertEquals(e[4].toDouble(), g.start, 1e-12, "$key/$i start")
                assertEquals(e[5].toDouble(), g.end, 1e-12, "$key/$i end")
                peak = maxOf(peak, kotlin.math.abs(e[4].toDouble() - g.start), kotlin.math.abs(e[5].toDouble() - g.end))
                assertEquals(e[6].takeUnless { it == "-" }?.toInt(), g.wordIndex, "$key/$i word identity")
            }
            for ((i, u) in groups.withIndex()) {
                val e = us[i]
                assertEquals(text(e[3]), u.text, "$key/$i unit text")
                assertEquals(e[4].toInt(), u.charStart, "$key/$i UTF16 start")
                assertEquals(e[5].toInt(), u.charEnd, "$key/$i UTF16 end")
                assertEquals(e[6].toDouble(), u.start, 1e-12, "$key/$i unit start")
                assertEquals(e[7].toDouble(), u.end, 1e-12, "$key/$i unit end")
                peak = maxOf(peak, kotlin.math.abs(e[6].toDouble() - u.start), kotlin.math.abs(e[7].toDouble() - u.end))
                assertEquals(u.text, u.timeline.joinToString("") { it.text })
                assertEquals(line.text.substring(u.charStart, u.charEnd), u.text)
            }
            glyphs += timeline.size
            units += groups.size
        }
        assertEquals(641, glyphs)
        assertEquals(226, units)
        File("../build/verification/diorama-text").apply { mkdirs() }.resolve("numeric.txt")
            .writeText("glyphs=$glyphs units=$units timePeak=$peak\nText splitting/timing only; NOT visual acceptance\n")
    }

    @Test fun actualCorePreservesSourceAndEveryInternalClockAcrossUnsafeSplits() {
        val pieces = listOf("A", "́ ", "👩", "‍🚀", "tail")
        val backing = KaraokeLine.AccompanimentKaraokeLine(listOf(KaraokeSyllable("●●", 100, 600)),
            null, KaraokeAlignment.Unspecified, 100, 600)
        val source = KaraokeLine.MainKaraokeLine(pieces.mapIndexed { i, text ->
            KaraokeSyllable(text, i * 100, (i + 1) * 100)
        }, "translated", KaraokeAlignment.Unspecified, 0, 500, accompanimentLines = listOf(backing))
        val original = SyncedLyrics(listOf(source))
        val mapped = dioramaCoreLines(normalize(original))
        val main = mapped.first { it.ref.accompanimentIndex == null }
        val nested = mapped.first { it.ref.accompanimentIndex != null }
        assertSame(source, main.source)
        assertSame(backing, nested.source)
        assertEquals(SourceRef(0, 0), nested.ref)
        assertEquals("translated", main.translation)
        assertSame(source, original.lines.single())
        assertEquals(listOf("Á ", "👩‍🚀", "tail"), main.lyric.words.map { it.text })
        val times = dioramaLineTimeline(main.lyric)
        assertEquals(listOf("Á", " ", "👩‍🚀", "t", "a", "i", "l"), times.map { it.text })
        assertEquals(listOf(0.0, .1), times[0].clocks.map { it.start })
        // Keep original seconds arithmetic (.1 + (.2-.1)/2), including IEEE rounding.
        assertEquals(listOf(.1, .1 + (.2 - .1) / 2), times[0].clocks.map { it.end })
        // The standalone parser piece ZWJ+rocket contains TWO EGCs before
        // reconciliation. Preserve both, not just the parent syllable span.
        assertEquals(listOf(.2, .3, .3 + (.4 - .3) / 2), times[2].clocks.map { it.start })
        assertEquals(listOf(.3, .3 + (.4 - .3) / 2, .4), times[2].clocks.map { it.end })
        val units = dioramaUnits(main.lyric, times)
        assertEquals(listOf("Á", "👩‍🚀", "tail"), units.map { it.text })
        assertEquals(4, units.last().timeline.size, "grouping cannot discard internal grapheme clocks")
        assertEquals(listOf("●", "●"), dioramaUnits(nested.lyric).map { it.text })
        for (u in units) for (g in u.timeline) for (clock in g.clocks) {
            assertTrue(clock.charStart >= u.charStart && clock.charEnd <= u.charEnd)
        }
    }

    @Test fun declaredTimesFallbackAndUnsortedParserEndpointsAreNotInvented() {
        val source = KaraokeLine.MainKaraokeLine(listOf(KaraokeSyllable("ab", 300, 800),
            KaraokeSyllable("cd", 0, 100)), null, KaraokeAlignment.Unspecified, 100, 500)
        val document = normalize(SyncedLyrics(listOf(source)))
        assertTrue(document.diagnostics.isNotEmpty())
        val line = dioramaCoreLines(document).single().lyric
        assertEquals(.1, line.start)
        assertEquals(.5, line.end)
        assertEquals(.8, line.words.first().end)
        assertEquals(.1, line.words.last().end)
        val plain = SyncedLine("AB 中文", null, 1000, 3000)
        val fallback = dioramaCoreLines(normalize(SyncedLyrics(listOf(plain)))).single()
        assertTrue(fallback.lyric.words.isEmpty())
        assertEquals(1.0, dioramaLineTimeline(fallback.lyric).first().start)
        assertEquals(3.0, dioramaLineTimeline(fallback.lyric).last().end)
        assertEquals(listOf("AB", "中", "文"), dioramaUnits(fallback.lyric).map { it.text })
        assertFailsWith<IllegalArgumentException> { dioramaUnits(line, emptyList()) }
        assertTrue(dioramaCoreLines(normalize(SyncedLyrics(emptyList()))).isEmpty())
    }
}
