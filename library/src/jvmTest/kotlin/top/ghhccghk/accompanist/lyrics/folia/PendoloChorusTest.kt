package top.ghhccghk.accompanist.lyrics.folia

import top.ghhccghk.accompanist.lyrics.folia.internal.styles.pendolo.*
import java.io.File
import kotlin.test.*

class PendoloChorusTest {
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
