package com.mocharealm.accompanist.lyrics.folia

import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.*
import com.mocharealm.accompanist.lyrics.folia.internal.model.normalize
import com.mocharealm.accompanist.lyrics.folia.internal.timeline.VisualTimeline
import com.mocharealm.accompanist.lyrics.folia.internal.waiting.*
import kotlin.random.Random
import kotlin.test.*

class WaitingTimelineTest {
    @Test fun strictThresholdHalfOpenIntroAndNoOutro() {
        val timeline=WaitingTimeline(normalize(SyncedLyrics(listOf(
            SyncedLine("one",null,5000,6000),
            SyncedLine("two",null,11000,12000),
            SyncedLine("three",null,17001,18000)))).lines)
        assertEquals(listOf(WaitingInterval(12_000_000,17_001_000)),timeline.intervals)
        assertNull(timeline.at(-1));assertNull(timeline.at(0))
        assertNotNull(timeline.at(12_000_000));assertNull(timeline.at(17_001_000))
        assertNull(timeline.at(99_000_000))
        assertTrue(WaitingTimeline(emptyList()).intervals.isEmpty())
    }

    @Test fun allVoicesIncludingNestedAndEarlierFragmentsCoverGaps() {
        val backing=KaraokeLine.AccompanimentKaraokeLine(listOf(KaraokeSyllable("back",4000,23000)),
            null,KaraokeAlignment.Unspecified,5000,22000)
        val main=KaraokeLine.MainKaraokeLine(listOf(KaraokeSyllable("main",10000,12000)),
            null,KaraokeAlignment.Unspecified,10000,12000,accompanimentLines=listOf(backing))
        val lines=normalize(SyncedLyrics(listOf(main,SyncedLine("overlap",null,20000,24000),
            SyncedLine("next",null,30000,32000)))).lines
        val timeline=WaitingTimeline(lines)
        assertEquals(listOf(WaitingInterval(24_000_000,30_000_000)),timeline.intervals)
        assertNull(timeline.at(15_000_000))
    }

    @Test fun visualTailAndExitSuppressDotsWithoutRestartingPhase() {
        val lines=normalize(SyncedLyrics(listOf(SyncedLine("first",null,10000,12000),
            SyncedLine("next",null,30000,32000)))).lines
        val timeline=WaitingTimeline(lines);val visual=VisualTimeline(lines)
        assertNotNull(timeline.visibleAt(5_000_000,visual.at(5_000_000)))
        assertNull(timeline.visibleAt(12_000_000,visual.at(12_000_000)))
        val time=(12000L..29999L).first {timeline.visibleAt(it*1000,visual.at(it*1000))!=null}*1000
        assertTrue(time>12_000_000,"Do not overlap outgoing lyric tail/exit")
        assertEquals(12_000_000,timeline.visibleAt(time,visual.at(time))!!.startUs)
    }

    @Test fun randomSeekMatchesAllVoiceUnionOracle() {
        val random=Random(681)
        repeat(100) {
            val raw=List(15) {
                val start=random.nextInt(-1000,120000)
                SyncedLine("voice",null,start,start+random.nextInt(1,18000))
            }
            val timeline=WaitingTimeline(normalize(SyncedLyrics(raw)).lines)
            repeat(200) {
                val t=random.nextInt(-1000,140000)
                val previousEnd=raw.filter {it.end<=t && it.end>0}.maxOfOrNull{it.end}?:0
                val nextStart=raw.filter {it.start>t}.minOfOrNull{it.start}
                val singing=raw.any{t>=it.start && t<it.end}
                val expected=t>=0 && !singing && nextStart!=null && nextStart-previousEnd>5000
                assertEquals(expected,timeline.at(t.toLong()*1000)!=null,"time=$t raw=$raw")
            }
        }
    }
}
