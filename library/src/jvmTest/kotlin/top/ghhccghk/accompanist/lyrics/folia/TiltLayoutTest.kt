package top.ghhccghk.accompanist.lyrics.folia

import top.ghhccghk.accompanist.lyrics.folia.internal.styles.tilt.*
import java.io.File
import java.util.Base64
import kotlin.test.*

class TiltLayoutTest {
    @Test fun actualOriginalSplitResplitFitAndTypographyChoice() {
        fun decode(value:String)=String(Base64.getDecoder().decode(value),Charsets.UTF_8)
        File("../verification/tilt-fixtures/layout.tsv").useLines { rows ->
            rows.filterNot{it.startsWith("#")||it.isBlank()}.forEach { row ->
                val f=row.split('\t');val text=decode(f[0])
                val layout=buildTiltLayout(text,f[1].toDouble(),TiltConfig(f[4].toDouble(),f[5].toDouble()),
                    f[2].toDouble(),f[3].toDouble()){s,tilt,size->s.length*size*(if(tilt).61 else .57)}
                assertEquals(f[6].toDouble(),layout.scale,.00000001,row)
                val expected=f[7].split(';').map { part ->
                    val e=part.split(',');TiltSegment(decode(e[0]),e[1].toBoolean(),e[2].toBoolean(),e[3].toInt())
                }
                assertEquals(expected,layout.segments,row)
            }
        }
    }
    @Test fun originalPulseAndRetainedTail() {
        File("../verification/tilt-fixtures/pulse.tsv").readLines().filterNot{it.startsWith("#")||it.isBlank()}.forEach { row ->
            val f=row.split('\t').map(String::toDouble)
            assertEquals(f[3],tiltPulse(f[0],f[1],f[2]),.00000001,row)
        }
    }
}
