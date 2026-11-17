package top.ghhccghk.accompanist.lyrics.folia

import top.ghhccghk.accompanist.lyrics.folia.internal.styles.cadenza.*
import java.io.File
import java.util.Base64
import kotlin.random.Random
import kotlin.test.*

class CadenzaTimelineTest {
    private fun rows(name:String)=File("../verification/cadenza-fixtures/$name.tsv").readLines().filterNot{it.startsWith('#')||it.isBlank()}.map{it.split('\t')}
    @Test fun matchesActualSourceOnBoundariesOverlapsResumptionsAndGaps() {
        val inputs=rows("timelineInputs").groupBy{it[0]}
        for((key,expected)in rows("timeline").groupBy{it[0]}){
            val lines=inputs[key].orEmpty().map{r->CadenzaLyric(String(Base64.getDecoder().decode(r[2]),Charsets.UTF_8),r[3].toDouble(),r[4].toDouble(),emptyList(),
                CadenzaHints(0.0,r[5].toDouble(),CadenzaTransition.NORMAL,CadenzaReveal.NORMAL))}
            val track=CadenzaTimeline(lines)
            for(r in expected+expected.shuffled(Random(151))+expected.reversed()){
                val time=r[1].toDouble();val actual=track.at(time)
                assertEquals(r[2].toInt().takeIf{it>=0},actual.active,"active $key/$time")
                assertEquals(r[3].toInt().takeIf{it>=0},actual.recent,"recent $key/$time")
                assertEquals(r[4].toInt().takeIf{it>=0},actual.upcoming,"upcoming $key/$time")
                if(actual.active==null)assertNull(actual.entered)else assertTrue(assertNotNull(actual.entered)<=time)
            }
        }
    }
    @Test fun canonicalMountIsReconstructedWhenAnOlderLineResumes() {
        val lines=listOf(CadenzaLyric("long",1.0,8.0,emptyList()),CadenzaLyric("short",2.0,3.0,emptyList()))
        val timeline=CadenzaTimeline(lines)
        assertEquals(1.0,timeline.at(1.5).entered)
        assertEquals(2.0,timeline.at(3.0).entered)
        assertEquals(3.0,timeline.at(3.000001).entered)
        assertEquals(3.0,timeline.at(7.0).entered)
        assertEquals(1.0,timeline.at(1.5).entered)
        assertNull(timeline.at(8.000001).active)
    }
}
