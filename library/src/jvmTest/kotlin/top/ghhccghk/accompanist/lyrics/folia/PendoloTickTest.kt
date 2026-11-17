package top.ghhccghk.accompanist.lyrics.folia

import top.ghhccghk.accompanist.lyrics.folia.internal.styles.pendolo.*
import top.ghhccghk.accompanist.lyrics.folia.internal.motion.*
import java.io.File
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.*

class PendoloTickTest {
    private fun rows(name: String) = File("../verification/pendolo-fixtures/$name.tsv").readLines()
        .filterNot { it.isBlank() || it.startsWith('#') }.map { it.split('\t') }
    private val scenarios = mapOf(
        "regular" to listOf(PendoloLineTime(1.0,2.5,2.8),PendoloLineTime(3.1,4.0),PendoloLineTime(5.0,6.0,6.4)),
        "rapid" to listOf(.7 to .73,.78 to .8,.83 to .86,.87 to .9).map { PendoloLineTime(it.first,it.second) },
        "overlap" to listOf(.7 to 3.0,.78 to .81,1.1 to 1.18,1.22 to 2.7,2.0 to 2.04).map { PendoloLineTime(it.first,it.second) },
        "tied" to listOf(0.0 to .4,0.0 to .2,.4 to .6).map { PendoloLineTime(it.first,it.second) },
        "empty" to emptyList(),
    )
    @Test fun originalMotionDispatcherResponseAndGeneratorVelocity() {
        val residual = mutableListOf("# intensity snap field original native error")
        for (r in rows("tick-response")) {
            val intensity = PendoloIntensity.valueOf(r[0].uppercase()); val snap = r[1].toDouble()
            val c = pendoloTickConfig(intensity,snap)
            assertEquals(r[6].toDouble(),c.stiffness,1e-12); assertEquals(r[7].toDouble(),c.damping,1e-12); assertEquals(r[8].toDouble(),c.mass)
            val spring = DampedSpring(r[2].toDouble(),r[3].toDouble(),r[4].toDouble(),c.stiffness,c.damping,c.mass)
            val t = r[5].toDouble(); val raw = spring.generator(t); val runtime = spring.sample(t)
            val expected = listOf(r[9],r[10],r[11]).map(String::toDouble)
            for ((i,pair) in expected.zip(listOf(runtime.value,raw.value,raw.velocity)).withIndex()) {
                residual += listOf(r[0],snap,i,pair.first,pair.second,abs(pair.first-pair.second)).joinToString("\t")
                assertEquals(pair.first,pair.second,1e-9,"$r field$i")
            }
            assertEquals(r[12].toBoolean(),raw.done,"raw done $r"); assertEquals(r[13].toBoolean(),runtime.done,"runtime done $r")
        }
        File("../build/verification/pendolo-tick").apply { mkdirs() }.resolve("response-residual.tsv").writeText(residual.joinToString("\n")+"\n")
    }
    @Test fun originalTimelineAndInterruptedResponsesAtArbitraryMediaQueries() {
        val residual = mutableListOf("# scene intensity snap field original native error")
        for ((key,queries) in rows("tick-track").groupBy { it.take(3) }) {
            val lines = scenarios.getValue(key[0]); val intensity = PendoloIntensity.valueOf(key[1].uppercase()); val snap = key[2].toDouble()
            val track = PendoloTickTrack(lines,intensity,snap)
            val timeline = PendoloTimeline(lines)
            for (q in queries + queries.shuffled(Random(811))) {
                val t=q[3].toDouble(); val actual=track.at(t)
                assertEquals(q[4].toInt(),timeline.current(t))
                val values=listOf(actual.target,actual.value,actual.velocity,actual.wheelRotation,actual.textCorrection)
                for ((field,pair) in q.drop(5).map(String::toDouble).zip(values).withIndex()) {
                    residual += (key+listOf(field,pair.first,pair.second,abs(pair.first-pair.second))).joinToString("\t")
                    assertEquals(pair.first,pair.second,1e-8,"$key/$t field$field")
                }
                assertEquals(actual,PendoloTickTrack(lines,intensity,snap).at(t),"remount $key/$t")
                assertEquals(actual,track.at(t),"pause $key/$t")
                val reduced=track.at(t,true)
                assertEquals(actual.target,reduced.value); assertEquals(0.0,reduced.velocity); assertEquals(0.0,reduced.wheelRotation,0.0)
            }
        }
        File("../build/verification/pendolo-tick").apply { mkdirs() }.resolve("track-residual.tsv").writeText(residual.joinToString("\n")+"\n")
    }
    @Test fun inclusiveEndAndRightLimitAreDistinctWithoutEpsilonInProduction() {
        val timeline=PendoloTimeline(listOf(PendoloLineTime(1.0,2.0)))
        assertEquals(0.0,timeline.anchor(2.0)); assertEquals(1.0,timeline.anchor(2.0,true))
        assertEquals(-1.0,timeline.anchor(.5,true))
        assertEquals(-1.0,PendoloTimeline(emptyList()).anchor(20.0,true))
        val track=PendoloTickTrack(listOf(PendoloLineTime(1.0,2.0)))
        assertEquals(0.0,track.at(2.0).target); assertEquals(1.0,track.at(2.0001).target)
        assertEquals(track.at(2.0).value,track.at(2.0-1e-10).value,1e-8)
        assertEquals(track.at(2.0).value,track.at(2.0+1e-10).value,1e-8)
    }
}
