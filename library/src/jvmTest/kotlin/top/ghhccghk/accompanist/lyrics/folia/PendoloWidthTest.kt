package top.ghhccghk.accompanist.lyrics.folia

import top.ghhccghk.accompanist.lyrics.folia.internal.styles.pendolo.*
import java.io.File
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.*

class PendoloWidthTest {
    @Test fun originalIdleCssCurveInterruptionAndRepeatedReversal() {
        val rows=File("../verification/pendolo-fixtures/width-response.tsv").readLines().filterNot{it.startsWith('#')||it.isBlank()}.map{it.split('\t')}
        val residual=mutableListOf("# scenario time field original native error")
        for((key,case)in rows.groupBy{it[1]}) {
            val spans=mutableListOf<PendoloWidthResponse>()
            for(e in case.filter{it[0]=="E"}){
                val t=e[2].toDouble();val next=e[3].toDouble();val last=spans.lastOrNull()
                if(last?.to==next)continue
                spans+=pendoloRetargetWidth(last,t,false,next,last!=null)
            }
            val queries=case.filter{it[0]=="Q"}
            for(q in queries+queries.shuffled(Random(929))){
                val t=q[2].toDouble();val span=spans.last{it.time<=t}
                val expected=q[3].toDouble();val actual=span.value(t)
                residual+=listOf(key,t,"maxWidth",expected,actual,abs(expected-actual)).joinToString("\t")
                assertEquals(expected,actual,.001,"$key/$t width")
                if(t<span.time+span.duration){
                    val duration=q[4].toDouble()
                    residual+=listOf(key,t,"duration",duration,span.duration,abs(duration-span.duration)).joinToString("\t")
                    assertEquals(duration,span.duration,1e-5,"$key/$t reverse duration")
                }
            }
        }
        File("../build/verification/pendolo-width").apply{mkdirs()}.resolve("response-residual.tsv").writeText(residual.joinToString("\n")+"\n")
    }
    @Test fun timelineWidthKeepsPauseSeekAndReducedMotionDeterministic() {
        val lines=listOf(PendoloLineTime(.7,3.0),PendoloLineTime(.78,.81),PendoloLineTime(1.1,1.18),PendoloLineTime(1.22,2.7))
        val track=PendoloWidthTrack(lines,380.0,600.0,List(4){110.0})
        val times=(0..120).map{it/30.0}+listOf(.7,.78,.81,.8100001,1.1,1.18,1.22,2.7,3.0,3.0001)
        val baseline=times.associateWith{track.at(it)}
        for(t in times.shuffled(Random(521))){
            assertEquals(baseline[t],track.at(t));assertEquals(track.at(t),PendoloWidthTrack(lines,380.0,600.0,List(4){110.0}).at(t))
            assertTrue(track.at(t).values.all{it.isFinite()&&it>0})
            val target=PendoloTimeline(lines).anchor(t)
            for(item in pendoloWheelLayout(4,target,0.0,380.0,600.0,radiusOffset=22.8,blockHeights=List(4){110.0}).filterNot{it.active})
                assertEquals(pendoloAvailableTextWidth(380.0,600.0)/item.scale,track.at(t,true).getValue(item.index))
        }
    }
}
