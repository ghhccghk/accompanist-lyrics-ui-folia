package top.ghhccghk.accompanist.lyrics.folia

import top.ghhccghk.accompanist.lyrics.folia.internal.styles.tilt.TiltSentences
import top.ghhccghk.accompanist.lyrics.folia.internal.text.graphemeBoundaries
import java.io.File
import java.util.Base64
import kotlin.test.*

class TiltSentencesTest {
    @Test fun originalPunctuationAndSeededPartitions() {
        fun decode(s:String)=String(Base64.getDecoder().decode(s),Charsets.UTF_8)
        File("../verification/tilt-fixtures/sentences.tsv").readLines().filterNot{it.startsWith("#")||it.isBlank()}.forEach { line ->
            val fields=line.split('\t');val text=decode(fields[0])
            val expected=fields[3].split('~').map(::decode)
            assertEquals(expected,TiltSentences.split(text,fields[1].toInt(),fields[2].toDouble()),line)
        }
    }
    @Test fun fallbackCannotCutAnIndivisibleCluster() {
        for(text in listOf("👩‍👩‍👧‍👦","Á","👩‍👩‍👧‍👦Á👨‍👩‍👧"))for(target in 1..4) {
            val actual=TiltSentences.split(text,target,.7)
            assertEquals(text,actual.joinToString(""))
            var offset=0
            actual.forEach {offset+=it.length;assertTrue(offset in graphemeBoundaries(text))}
            assertEquals(actual,TiltSentences.split(text,target,.7))
        }
    }
}
