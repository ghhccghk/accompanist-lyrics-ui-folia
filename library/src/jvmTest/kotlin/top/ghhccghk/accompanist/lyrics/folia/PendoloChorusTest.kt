package top.ghhccghk.accompanist.lyrics.folia

import top.ghhccghk.accompanist.lyrics.folia.internal.styles.pendolo.*
import java.io.File
import kotlin.test.*

class PendoloChorusTest {
    @Test fun overlapsResumeHaloAndReconstructWithoutDisplayHistory() {
        val lines=listOf(PendoloLineTime(.7,4.0),PendoloLineTime(.86,.99),PendoloLineTime(1.02,1.1))
        fun create()=PendoloChorusTrack(lines,listOf(true,false,true),800.0,600.0,List(3){34.0})
        val track=create()
        assertTrue(track.at(0,.7).presentation.active)
        assertEquals(0.0,track.at(0,.7).haloOpacity)
        assertEquals(.96,track.at(0,.7).haloScale)
        assertTrue(track.at(0,.8).haloOpacity>0)
        assertFalse(track.at(0,.86).presentation.active)
        assertEquals(0.0,track.at(0,.86).haloOpacity,"unmount has no fade-out")
        assertTrue(track.at(0,1.0).presentation.active,"long line resumes after short overlap")
        assertTrue(track.at(2,1.1).presentation.active,"inclusive end remains current")
        assertTrue(track.at(0,1.10001).presentation.active,"resume reconstructs a new halo")
        val stable=track.at(0,1.2,true)
        assertEquals(1.026,stable.scale);assertEquals(.34,stable.haloOpacity);assertEquals(1.0,stable.haloScale)
        val times=listOf(0.0,.7,.8,.86,.99,1.0,1.02,1.1,1.10001,1.2,3.0,4.0,4.1)
        val expected=times.associateWith{t->(0..2).map{track.at(it,t)}}
        for(t in times.reversed())assertEquals(expected[t],(0..2).map{create().at(it,t)},"seek/remount $t")
    }
    @Test fun originalProfilesAndPostMaskFilterParameters() {
        val rows=File("../verification/pendolo-fixtures/chorus.tsv").readLines().filterNot{it.startsWith('#')||it.isBlank()}
        assertEquals(60,rows.size)
        for(row in rows){
            val c=row.split('\t');val p=pendoloChorus(c[1].toBooleanStrict(),c[2].toBooleanStrict(),PendoloIntensity.valueOf(c[0].uppercase()))
            assertEquals(c[4].toBooleanStrict(),p.active,row)
            assertEquals(c.subList(5,10).map(String::toDouble),listOf(p.accentMix,p.haloOpacity,p.haloScale,p.glowMultiplier,p.duration),row)
            val f=pendoloChorusFilter(c[3].toDouble(),p.glowMultiplier)
            assertEquals(c.subList(10,13).map(String::toDouble),listOf(f.firstSigma,f.secondSigma,f.padding.toDouble()),row)
        }
    }
}
