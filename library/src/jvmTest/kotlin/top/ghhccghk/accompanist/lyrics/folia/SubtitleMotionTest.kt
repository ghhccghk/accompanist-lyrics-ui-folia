package top.ghhccghk.accompanist.lyrics.folia

import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import top.ghhccghk.accompanist.lyrics.folia.internal.model.normalize
import top.ghhccghk.accompanist.lyrics.folia.internal.timeline.*
import top.ghhccghk.accompanist.lyrics.folia.internal.animation.sampleSubtitle
import java.io.File
import kotlin.test.*

class SubtitleMotionTest {
    @Test fun mountedOriginalSubtitleMotion() {
        val lines=File("../verification/classic-fixtures/subtitle-scenes.tsv").readLines().filterNot{it.startsWith("#")||it.isBlank()}.map {row->
            val c=row.split('\t');normalize(SyncedLyrics(listOf(SyncedLine(c[2],c[3],c[0].toInt(),c[1].toInt())))).lines.single().copy(phonetic=c[4])
        }
        val index=VisualTimeline(lines);val history=SubtitleHistory(lines)
        for(row in File("../verification/classic-fixtures/subtitle-motion.tsv").readLines().filterNot{it.startsWith("#")||it.isBlank()}) {
            val c=row.split('\t');val time=c[2].toLong()*1000;val epoch=index.at(time).primary
            val roles=if(c[1]=="translation") listOf(SubtitleSource.Translation) else listOf(SubtitleSource.Current,SubtitleSource.Translation,SubtitleSource.Phonetic)
            val content=resolveSubtitleContent(lines,epoch?.line,history.recent(time,epoch?.line),roles)
            val f=sampleSubtitle(time,epoch?.enterUs?:content.line?.let{lines[it].startUs},content.rows.isNotEmpty(),false)
            listOf(f.outerAlpha,f.outerY,f.innerAlpha,f.innerY).forEachIndexed {i,v->assertEquals(c[i+3].toDouble(),v.toDouble(),.0001,row)}
        }
    }
}
