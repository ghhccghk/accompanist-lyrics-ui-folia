package top.ghhccghk.accompanist.lyrics.folia

import top.ghhccghk.accompanist.lyrics.folia.internal.motion.*
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.pendolo.*
import java.io.File
import kotlin.math.*
import kotlin.random.Random
import kotlin.test.*

class PendoloFollowerTest {
    private fun rows(name:String)=File("../verification/pendolo-fixtures/follower-v1/$name.tsv").readLines()
        .filterNot{it.startsWith('#')||it.isBlank()}.map{it.split('\t')}
    private fun response(r:List<String>):DampedSpring {
        val c=pendoloTickConfig(PendoloIntensity.valueOf(r[0].uppercase()),r[1].toDouble())
        return DampedSpring(r[2].toDouble(),r[3].toDouble(),r[4].toDouble(),c.stiffness,c.damping,c.mass,.001,.01)
    }
    @Test fun originalFollowerLimitsAndContinuousFirstRest() {
        val residual=mutableListOf("# intensity snap from to velocity sourceStop nativeStop error")
        for(r in rows("stops")) {
            assertEquals(.001,r[7].toDouble());assertEquals(.01,r[8].toDouble())
            val spring=response(r);val stop=r[5].toDouble()
            residual+=(r.take(5)+listOf(stop,spring.continuousDuration,abs(stop-spring.continuousDuration))).joinToString("\t")
            assertEquals(stop,spring.continuousDuration,1e-9,"independent original first rest $r")
            assertEquals(stop,r[6].toDouble(),1e-9,"independent 100/200us root isolation agrees")
            assertFalse(spring.generator(stop-1e-6).done,"before original rest")
            assertTrue(spring.generator(stop+1e-6).done,"after original rest")
            assertTrue(spring.sampleContinuous(stop+1e-6).done)
            assertEquals(r[3].toDouble(),spring.sampleContinuous(stop+.02).value)
            assertEquals(0.0,spring.sampleContinuous(stop+.02).velocity)
            for(t in listOf(.4,0.0,stop+.02,.1,stop-1e-6,3.0).shuffled(Random(81)))
                assertEquals(spring.sampleContinuous(t),response(r).sampleContinuous(t),"arbitrary seek/remount")
        }
        File("../build/verification/pendolo-follower").apply{mkdirs()}.resolve("stops.tsv").writeText(residual.joinToString("\n")+"\n")
    }
    @Test fun unchangedOriginalFollowerGeneratorAtArbitraryTimes() {
        val stops=rows("stops").associateBy{it.take(5)}
        var boundaryRepresentations=0;var peak=0.0
        for(r in rows("response")) {
            val spring=response(r);val t=r[5].toDouble();val actual=spring.generator(t)
            val sourceStop=stops.getValue(r.take(5))[5].toDouble()
            val velocityError=abs(r[7].toDouble()-actual.velocity);peak=max(peak,velocityError)
            assertEquals(r[7].toDouble(),actual.velocity,1e-9,"original physical velocity $r")
            if(actual.done!=r[8].toBoolean()) {
                // Bisection's boundary has two adjacent floating-point
                // representations. Keep/count it, and test both stable sides
                // above, rather than interpreting a snapped value as raw ink.
                boundaryRepresentations++
                assertEquals(sourceStop,t,0.0);assertEquals(sourceStop,spring.continuousDuration,1e-12)
            } else {
                val error=abs(r[6].toDouble()-actual.value);peak=max(peak,error)
                assertEquals(r[6].toDouble(),actual.value,1e-9,"original physical value $r")
            }
        }
        File("../build/verification/pendolo-follower").apply{mkdirs()}.resolve("raw-summary.tsv")
            .writeText("# peakRawError boundaryRepresentations\n$peak\t$boundaryRepresentations\n")
    }
    @Test fun semanticTimelineRetargetsUseFollowerRatherThanDispatcher() {
        val inputs=mapOf(
            "regular" to listOf(PendoloLineTime(1.0,2.5,2.8),PendoloLineTime(3.1,4.0),PendoloLineTime(5.0,6.0,6.4)),
            "rapid" to listOf(.7 to .73,.78 to .8,.83 to .86,.87 to .9).map{PendoloLineTime(it.first,it.second)},
            "overlap" to listOf(.7 to 3.0,.78 to .81,1.1 to 1.18,1.22 to 2.7,2.0 to 2.04).map{PendoloLineTime(it.first,it.second)},
            "tied" to listOf(0.0 to .4,0.0 to .2,.4 to .6).map{PendoloLineTime(it.first,it.second)},
            "empty" to emptyList(),"mounted" to (0 until 8).map{PendoloLineTime(.7+it*4,2.8+it*4)})
        var peak=0.0
        for((key,queries)in rows("track").groupBy{it.take(3)}) {
            val lines=inputs.getValue(key[0]);val intensity=PendoloIntensity.valueOf(key[1].uppercase());val snap=key[2].toDouble()
            val track=PendoloTickTrack(lines,intensity,snap)
            for(q in queries+queries.shuffled(Random(171))) {
                val t=q[3].toDouble();val actual=track.at(t)
                val expected=q.drop(4).map(String::toDouble)
                for((i,pair)in expected.zip(listOf(actual.target,actual.value,actual.velocity,actual.wheelRotation,actual.textCorrection)).withIndex()) {
                    peak=max(peak,abs(pair.first-pair.second));assertEquals(pair.first,pair.second,1e-8,"$key/$t/$i")
                }
                assertEquals(actual,track.at(t),"pause")
                assertEquals(actual,PendoloTickTrack(lines,intensity,snap).at(t),"remount")
                val reduced=track.at(t,true);assertEquals(actual.target,reduced.value);assertEquals(0.0,reduced.velocity)
            }
        }
        File("../build/verification/pendolo-follower").apply{mkdirs()}.resolve("track-summary.tsv").writeText("# peakTrackError\n$peak\n")
        val legacyDifferences=mutableListOf("# scene intensity snap time field legacyExpected correctedNative error")
        val legacyRows=File("../verification/pendolo-fixtures/tick-track.tsv").readLines()
            .filterNot{it.startsWith('#')||it.isBlank()}.map{it.split('\t')}
        for((key,queries)in legacyRows.groupBy{it.take(3)}) {
            val track=PendoloTickTrack(inputs.getValue(key[0]),PendoloIntensity.valueOf(key[1].uppercase()),key[2].toDouble())
            for(q in queries){val a=track.at(q[3].toDouble())
                assertEquals(q[5].toDouble(),a.target,"legacy target semantics stay exact")
                for((i,pair)in q.drop(5).map(String::toDouble).zip(listOf(a.target,a.value,a.velocity,a.wheelRotation,a.textCorrection)).withIndex())
                    if(abs(pair.first-pair.second)>1e-8)legacyDifferences+=(q.take(4)+listOf(i,pair.first,pair.second,abs(pair.first-pair.second))).joinToString("\t")
            }
        }
        File("../build/verification/pendolo-follower/legacy-differences.tsv").writeText(legacyDifferences.joinToString("\n")+"\n")
    }
    @Test fun originalDriverSeparatesMountedSamplingFromSemanticMediaTime() {
        val mounted=File("../verification/pendolo-fixtures/tick-stage-frames.tsv").readLines()
            .filterNot{it.startsWith('#')||it.isBlank()}.map{it.split('\t')}.associateBy{it[0]}
        for(r in rows("driver").filter{it[1]=="60"&&it[2]in listOf("7","7.3")}) {
            val key="latin-380-${r[0]}-${(r[2].toDouble()*10000).roundToInt()}"
            val source=mounted.getValue(key)
            assertEquals(source[6].toDouble(),r[4].toDouble(),1e-12,"actual source driver vs unchanged mounted value")
            assertEquals(source[8].toDouble(),r[6].toDouble(),1e-12,"actual source driver vs unchanged mounted rotation")
            assertEquals(if(r[2]=="7")183.0 else 483.0,r[9].toDouble(),"actual rounded generator elapsed, not200/500ms")
        }
        for((_,group)in rows("driver").filter{it[2]=="7"}.groupBy{it[0]})
            assertEquals(3,group.map{it[6]}.distinct().size,"original source itself differs by RAF schedule")
    }
    @Test fun continuousRestIsLatchedAndHandlesCriticalDamping() {
        for((k,c)in listOf(1.0 to 2.0,4.0 to 4.0,100.0 to 1.0,1.0 to 4.0))for(v in listOf(-12.0,0.0,12.0)) {
            val spring=DampedSpring(0.0,1.0,v,k,c,1.0,.001,.01)
            val end=spring.continuousDuration
            assertTrue(end.isFinite()&&end>0)
            assertFalse(spring.generator(end-1e-6).done,"first crossing $k $c $v")
            assertTrue(spring.generator(end+1e-6).done)
            assertEquals(SpringSample(1.0,0.0,true),spring.sampleContinuous(end+10))
        }
    }
}
