package top.ghhccghk.accompanist.lyrics.folia

import top.ghhccghk.accompanist.lyrics.folia.internal.animation.*
import java.io.File
import kotlin.test.*

class PartitaWordFilterTest {
    @Test fun unchangedZeroBlurTargetContinuesThroughShortWordEnd() {
        val scenes=(File("../verification/classic-fixtures/scenes.tsv").readLines()+
            File("../verification/partita-fixtures/scenes.tsv").readLines()).filterNot{it.startsWith("#")||it.isBlank()}.map{it.split('\t')}
        File("../verification/partita-fixtures/word-filter.tsv").readLines().filterNot{it.startsWith("#")||it.isBlank()}.forEach { row ->
            val f=row.split('\t');val words=scenes.filter{it[0]==f[0]};val word=words[f[2].toInt()]
            val enter=word[1].toLong()*1000;val end=word[2].toLong()*1000
            val frame=sampleWord(f[1].toLong()*1000,enter,word[3].toLong()*1000,word[4].toLong()*1000,
                renderProfile(enter,end),Pose(0f,0f,1f,0f,0f),false)
            assertEquals(f[3].toDouble(),frame.blur.toDouble(),.0001,row)
        }
    }
}
