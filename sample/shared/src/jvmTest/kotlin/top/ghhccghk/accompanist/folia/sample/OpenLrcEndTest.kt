package top.ghhccghk.accompanist.folia.sample

import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.*
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import top.ghhccghk.accompanist.folia.sample.importing.*
import kotlin.test.*

class OpenLrcEndTest {
    @Test fun actualPublishedParserSentinelUsesNativeDurationWithoutMutatingSource() {
        val raw=parseSongLyrics("[00:00.70]Amber\n[00:10.00]Return".encodeToByteArray(),
            "[00:00.70]First translation\n[00:10.00]Last translation".encodeToByteArray())
        assertEquals(Int.MAX_VALUE,raw.lines.last().end,"actual core0.5.0 sentinel")
        val display=resolveOpenLrcEnds(raw,12000)
        assertSame(raw.lines.first(),display.lines.first())
        assertEquals(listOf(700,10000),display.lines.map{it.start})
        assertEquals(12000,display.lines.last().end)
        assertEquals("Last translation",(display.lines.last() as SyncedLine).translation)
        assertEquals(Int.MAX_VALUE,raw.lines.last().end)
        assertEquals(14000,resolveOpenLrcEnds(raw,14000).lines.last().end)
        assertEquals(12000,display.lines.last().end,"different song duration cannot mutate a prior display copy")
    }
    @Test fun finiteLyricsAndTimedWordsKeepTheirIdentityAndDoNotClipToAudio() {
        val line=SyncedLine("Finite",null,700,15000,"Phonetic","en")
        val words=KaraokeLine.MainKaraokeLine(listOf(KaraokeSyllable("Timed",900,14000)),null,KaraokeAlignment.Unspecified,900,14000)
        val raw=SyncedLyrics(listOf(line,words))
        assertSame(raw,resolveOpenLrcEnds(raw,12000))
        val mixed=SyncedLyrics(raw.lines+SyncedLine("Open",null,16000,Int.MAX_VALUE,"Sound","en"))
        val display=resolveOpenLrcEnds(mixed,12000)
        assertSame(line,display.lines[0]);assertSame(words,display.lines[1])
        assertEquals(SyncedLine("Open",null,16000,16000,"Sound","en"),display.lines[2])
        assertEquals(Int.MAX_VALUE,mixed.lines[2].end)
        assertSame(raw,resolveOpenLrcEnds(raw,1))
        assertFailsWith<IllegalArgumentException>{resolveOpenLrcEnds(raw,0)}
    }

    @Test fun demoUsesItsFiniteImportTailAndRetainsRawSingleLine() {
        val scene=parseLrc("Owned single-line LRC", "[00:02.00]Single line".encodeToByteArray(), 1)
        assertEquals(7000,scene.durationMillis)
        assertEquals(Int.MAX_VALUE,scene.lyrics.lines.single().end)
        val display=resolveOpenLrcEnds(scene.lyrics,scene.durationMillis)
        assertEquals(2000,display.lines.single().start)
        assertEquals(7000,display.lines.single().end)
        assertEquals(Int.MAX_VALUE,scene.lyrics.lines.single().end)
        assertSame(display,resolveOpenLrcEnds(display,9000),"do not re-bound a finite display model")
        assertEquals(Int.MAX_VALUE,resolveOpenLrcEnds(scene.lyrics,Long.MAX_VALUE).lines.single().end,
            "the core model's Int timestamp must not overflow")
    }
}
