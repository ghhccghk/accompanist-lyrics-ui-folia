package top.ghhccghk.accompanist.lyrics.folia

import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import top.ghhccghk.accompanist.lyrics.folia.internal.model.normalize
import top.ghhccghk.accompanist.lyrics.folia.internal.timeline.VisualTimeline
import kotlin.test.*

class VisualTimelineTest {
    @Test fun tiltRetainsEveryExitForTheOriginal450Milliseconds() {
        val lines=normalize(SyncedLyrics(List(30) { index ->
            SyncedLine("$index",null,index*10,index*10+1)
        })).lines
        val classic=VisualTimeline(lines)
        val tilt=VisualTimeline(lines,fixedExitDurationUs=450_000)
        val leaving=tilt.at(10_000).exiting.single()
        assertEquals(0,leaving.line)
        assertEquals(10_000,leaving.leaveUs)
        assertTrue(tilt.at(459_999).exiting.contains(leaving))
        assertFalse(tilt.at(460_000).exiting.contains(leaving))
        assertFalse(classic.at(459_999).exiting.contains(leaving))
        assertEquals((0..28).toList(),tilt.at(300_000).exiting.map{it.line})
        val saved=tilt.at(459_999)
        repeat(100){tilt.at(it*15_731L)}
        assertSame(saved,tilt.at(459_999))
        assertTrue(tilt.at(2_000_000).exiting.isEmpty())
    }
    @Test fun intervalResultsAreReusedAndEqualStartPrioritySurvivesTail() {
        val timeline = VisualTimeline(normalize(SyncedLyrics(listOf(
            SyncedLine("first", null, 0, 1_000), SyncedLine("second", null, 0, 1_000)
        ))).lines)
        assertSame(timeline.at(300_000), timeline.at(300_001))
        assertEquals(0, timeline.at(1_000_000).primary?.line)
        val before = timeline.at(999_999)
        repeat(100) { timeline.at(it * 17_321L) }
        assertSame(before, timeline.at(999_999))
    }
    @Test fun overlapResumptionProducesIndependentEpochAndCanonicalExit() {
        val timeline = VisualTimeline(normalize(SyncedLyrics(listOf(SyncedLine("long", null, 0, 2_000),
            SyncedLine("short", null, 500, 1_000)))).lines)
        assertEquals(0, timeline.at(0).primary?.line)
        assertEquals(1, timeline.at(500_000).primary?.line)
        assertEquals(1, timeline.at(510_000).exiting.size)
        val resumed = timeline.at(1_000_000).primary!!
        assertEquals(0, resumed.line)
        assertEquals(1_000_000, resumed.enterUs)
        timeline.at(0)
        assertEquals(resumed, timeline.at(1_000_000).primary)
    }
    @Test fun gapsNeverResurrectAnOlderTailAndDenseExitsFinishTheirTransitions() {
        val timeline = VisualTimeline(normalize(SyncedLyrics(List(30) { index ->
            SyncedLine("$index", null, index * 10, index * 10 + 1)
        })).lines)
        assertEquals((18..28).toList(), timeline.at(300_000).exiting.map { it.line })
        assertEquals(12, timeline.maxExitingCount)
        assertNull(timeline.at(2_000_000).primary)
        assertTrue(timeline.at(2_000_000).exiting.isEmpty())
    }
}
