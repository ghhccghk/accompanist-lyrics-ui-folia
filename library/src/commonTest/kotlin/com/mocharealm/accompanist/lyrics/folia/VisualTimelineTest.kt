package com.mocharealm.accompanist.lyrics.folia

import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import com.mocharealm.accompanist.lyrics.folia.internal.model.normalize
import com.mocharealm.accompanist.lyrics.folia.internal.timeline.VisualTimeline
import kotlin.test.*

class VisualTimelineTest {
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
    @Test fun gapsNeverResurrectAnOlderTailAndDenseExitsAreBounded() {
        val timeline = VisualTimeline(normalize(SyncedLyrics(List(30) { index ->
            SyncedLine("$index", null, index * 10, index * 10 + 1)
        })).lines)
        assertTrue(timeline.at(300_000).exiting.size <= 4)
        assertNull(timeline.at(2_000_000).primary)
        assertTrue(timeline.at(2_000_000).exiting.isEmpty())
    }
}
