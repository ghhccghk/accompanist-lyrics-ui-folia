package top.ghhccghk.accompanist.lyrics.folia

import top.ghhccghk.accompanist.lyrics.folia.internal.styles.cappella.*
import java.io.File
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.*

class CappellaResponseTest {
    @Test fun actualOriginalConfiguredMotionResponses() {
        val residual=mutableListOf("# intensity kind from to velocity time original native absoluteError rawSpringError velocityError")
        for(r in File("../verification/cappella-fixtures/response.tsv").readLines().filterNot{it.startsWith('#')||it.isBlank()}.map{it.split('\t')}) {
            val intensity=CappellaIntensity.valueOf(r[0].uppercase());val config=cappellaTransition(intensity)
            val from=r[2].toDouble();val to=r[3].toDouble();val velocity=r[4].toDouble();val t=r[5].toDouble()
            val actual:Double;var raw=0.0;var speed=0.0
            if(r[1]=="spring") {
                val spring=CappellaSpring(from,to,velocity,config)
                actual=spring.sample(t).value
                val state=spring.generator(t)
                raw=abs(state.value-r[7].toDouble());speed=abs(state.velocity-r[8].toDouble())
                assertEquals(r[9].toBooleanStrict(),state.done,"raw spring done $r")
            } else {
                val kind=CappellaCurve.valueOf(r[1].substringBefore('-').uppercase())
                actual=cappellaTween(from,to,t,kind,config).value
            }
            residual+=(r.take(6)+listOf(r[6],actual,abs(actual-r[6].toDouble()),raw,speed)).joinToString("\t")
        }
        val out=File("../build/verification/cappella-response").apply{mkdirs()}
        out.resolve("residual.tsv").writeText(residual.joinToString("\n")+"\n")
        for(r in residual.drop(1).map{it.split('\t')}) {
            assertEquals(r[6].toDouble(),r[7].toDouble(),1e-4,"Motion runtime ${r.take(6)}")
            assertTrue(r[9].toDouble()<1e-9,"raw spring ${r.take(6)}")
            assertTrue(r[10].toDouble()<1e-8,"raw velocity ${r.take(6)}")
        }
    }
    @Test fun immutableInterruptedTracksPreserveStateAndRebuildAnySeek() {
        val c=cappellaTransition(CappellaIntensity.NORMAL)
        for(curve in CappellaCurve.entries) {
            val track=CappellaScalarTrack(1.0,listOf(.1 to 2.0,.15 to 2.0,.2 to -.5,.21 to .7,.8 to 1.0),curve,c)
            val noDuplicate=CappellaScalarTrack(1.0,listOf(.1 to 2.0,.2 to -.5,.21 to .7,.8 to 1.0),curve,c)
            val times=(0..600).map{it*.007}+listOf(.1,.15,.2,.21,.8)
            val expected=times.associateWith(track::at)
            for(t in times.shuffled(Random(141))) {
                assertEquals(expected[t],track.at(t),"random seek $curve/$t")
                assertEquals(noDuplicate.at(t),track.at(t),"unchanged target must not restart $curve/$t")
            }
            for(t in listOf(.2,.21,.8))assertEquals(track.at(t-1e-10).value,track.at(t).value,1e-7,"continuous interruption $curve/$t")
        }
    }
}
