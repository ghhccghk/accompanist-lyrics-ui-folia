package top.ghhccghk.accompanist.lyrics.folia

import top.ghhccghk.accompanist.lyrics.folia.internal.styles.tilt.*
import java.io.File
import java.util.Base64
import kotlin.test.*

class TiltTimingTest {
    @Test fun actualOriginalWordRangeAndInternalCharacterTiming() {
        fun decode(value:String)=String(Base64.getDecoder().decode(value),Charsets.UTF_8)
        File("../verification/tilt-fixtures/timings.tsv").readLines().filterNot{it.startsWith("#")||it.isBlank()}.forEach { row ->
            val f=row.split('\t');val text=decode(f[0]);val offset=f[2].toInt();val segment=decode(f[3])
            val words=f[1].split('|').map { descriptor ->
                val w=descriptor.split('~')
                val syllables=w[3].takeIf{it.isNotEmpty()}?.split('+')?.map { s ->
                    val v=s.split('@');TiltWord(decode(v[0]),v[1].toDouble(),v[2].toDouble())
                }?:emptyList()
                TiltWord(decode(w[0]),w[1].toDouble(),w[2].toDouble(),syllables)
            }
            assertEquals(f[6].toInt() until f[7].toInt(),tiltWordRange(offset,segment,text,words),row)
            val actual=buildTiltCharTimings(offset,segment,f[4].toDouble(),f[5].toDouble(),text,words)
            val expected=f[8].takeIf{it.isNotEmpty()}?.split(';')?.map { t ->
                val v=t.split(',');TiltCharTime(v[0].toInt(),v[1].toDouble(),v[2].toDouble())
            }?:emptyList()
            assertEquals(expected.size,actual.size,row)
            actual.zip(expected).forEach { (a,e) ->
                assertEquals(e.index,a.index,row)
                assertEquals(e.start,a.start,.00000001,row)
                assertEquals(e.end,a.end,.00000001,row)
            }
        }
    }
}
