package top.ghhccghk.accompanist.lyrics.folia

import com.mocharealm.accompanist.lyrics.core.model.*
import com.mocharealm.accompanist.lyrics.core.model.karaoke.*
import com.mocharealm.accompanist.lyrics.core.model.synced.*
import top.ghhccghk.accompanist.lyrics.folia.internal.model.*
import top.ghhccghk.accompanist.lyrics.folia.internal.timeline.TimelineIndex
import kotlin.random.Random
import kotlin.test.*

class ModelTimelineTest {
    @Test fun preservesTextPhoneticsAndSourceIdentity() {
        val back = KaraokeLine.AccompanimentKaraokeLine(listOf(KaraokeSyllable("和", 20, 80)),
            null, KaraokeAlignment.Unspecified, 20, 80)
        val main = KaraokeLine.MainKaraokeLine(listOf(KaraokeSyllable("á", 0, 30, "a"),
            KaraokeSyllable(" 👩‍🚀", 30, 100, "space", phoneticSeparatorBefore = " ")),
            "translation", KaraokeAlignment.Unspecified, 0, 100, accompanimentLines = listOf(back))
        val document = normalize(SyncedLyrics(listOf(main, back)))
        assertEquals(2, document.lines.size)
        val normalized = document.lines.first()
        assertSame(main, normalized.source)
        assertEquals("á 👩‍🚀", normalized.text)
        assertEquals("a space", normalized.phonetic)
        assertEquals(normalized.text.length, normalized.fragments.last().end)
        assertEquals(SourceRef(0, 0), document.lines[1].ref)
    }
    @Test fun diagnosesInvalidAndUnknownWithoutStringifying() {
        val unknown = object : ISyncedLine { override val start = 0; override val end = 10; override val duration = 10 }
        val document = normalize(SyncedLyrics(listOf(unknown, UncheckedSyncedLine("bad", null, 30, 10))))
        assertTrue(document.lines.isEmpty())
        assertEquals(listOf("UnsupportedLineType", "ReversedLineTime"), document.diagnostics.map { it.reason })
    }
    @Test fun halfOpenBoundariesGapsAndLineOnlyTiming() {
        val document = normalize(SyncedLyrics(listOf(SyncedLine("next", null, 20, 30), SyncedLine("first", null, 0, 10))))
        assertTrue(document.lines.all { it.fragments.isEmpty() })
        val timeline = TimelineIndex(document.lines)
        assertEquals(0, timeline.select(9_999).primary)
        assertNull(timeline.select(10_000).primary)
        assertEquals(1, timeline.select(10_000).next)
        assertEquals(1, timeline.select(20_000).primary)
        assertNull(timeline.select(30_000).next)
    }
    @Test fun randomSeeksMatchNaiveOverlapOracle() {
        val random = Random(7041)
        val sources = List(250) { index ->
            val start = random.nextInt(-100, 5_000)
            SyncedLine("line $index", null, start, start + random.nextInt(0, 1_000))
        }
        val document = normalize(SyncedLyrics(sources))
        val timeline = TimelineIndex(document.lines)
        repeat(2_000) {
            val time = random.nextLong(-200_000, 6_000_000)
            val expected = document.lines.indices.filter { time >= document.lines[it].startUs && time < document.lines[it].endUs }
            val result = timeline.select(time)
            assertEquals(expected.toSet(), (result.secondary + listOfNotNull(result.primary)).toSet())
            assertEquals(expected.maxWithOrNull(compareBy<Int>({ document.lines[it].startUs },
                { -document.lines[it].ref.index })), result.primary)
        }
    }
    @Test fun longConversionCannotOverflowInt() {
        val line = SyncedLine("far", null, Int.MAX_VALUE - 1, Int.MAX_VALUE)
        val document = normalize(SyncedLyrics(listOf(line)))
        assertEquals(Int.MAX_VALUE.toLong() * 1000, document.lines.single().endUs)
    }
}
