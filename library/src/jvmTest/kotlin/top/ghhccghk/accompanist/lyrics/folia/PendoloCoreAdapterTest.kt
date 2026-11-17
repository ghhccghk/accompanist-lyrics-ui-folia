package top.ghhccghk.accompanist.lyrics.folia

import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.*
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import top.ghhccghk.accompanist.lyrics.folia.internal.model.*
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.pendolo.*
import java.io.File
import kotlin.test.*

class PendoloCoreAdapterTest {
    @Test fun publishedCoreIdentitySubtitlesAndUnsafeGraphemeSplitsSurvive() {
        val pieces=listOf("A","́ ","👩","‍🚀","tail")
        val source=KaraokeLine.MainKaraokeLine(pieces.mapIndexed{i,s->KaraokeSyllable(s,i*100,(i+1)*100)},
            "translated",KaraokeAlignment.Unspecified,0,500)
        val original=SyncedLyrics(listOf(source))
        val document=normalize(original)
        val mapped=pendoloCoreLines(document,setOf(0)).single()
        assertSame(source,mapped.source);assertSame(source,original.lines.single())
        assertEquals(SourceRef(0),mapped.ref);assertTrue(mapped.stage.isChorus)
        assertEquals("translated",mapped.stage.subtitles.translation)
        val line=mapped.stage.lyric
        assertEquals(pieces.joinToString(""),line.text)
        assertEquals(listOf("Á ","👩‍🚀","tail"),line.words.map{it.text})
        assertEquals(pieces.take(2),line.words[0].syllables.map{it.text})
        assertEquals(pieces.subList(2,4),line.words[1].syllables.map{it.text})
        val times=pendoloLineTimeline(line)
        assertEquals(listOf("Á"," ","👩‍🚀","t","a","i","l"),times.map{it.text})
        assertEquals(0.0,times[0].start);assertEquals(.15,times[0].end,1e-12)
        assertEquals(.2,times[2].start);assertEquals(.4,times[2].end)
        assertEquals(0.0,line.start);assertEquals(.5,line.end)
        assertEquals(.74,mapped.stage.renderEnd,1e-6)
    }
    @Test fun lineOnlyUsesUpstreamEvenTimingAndOriginalRenderHints() {
        val rows=File("../verification/pendolo-fixtures/core-render.tsv").readLines().filterNot{it.startsWith('#')||it.isBlank()}
        for(row in rows){val c=row.split('\t');val start=c[0].toInt();val end=c[1].toInt()
            val source=SyncedLine("AB",translation=null,start=start,end=end)
            val stage=pendoloCoreLines(normalize(SyncedLyrics(listOf(source)))).single().stage
            assertTrue(stage.lyric.words.isEmpty());assertEquals(c[2].toDouble(),stage.renderEnd,1e-6,row)
            val gs=pendoloLineTimeline(stage.lyric)
            assertEquals(start/1000.0,gs[0].start);assertEquals((start+end)/2000.0,gs[0].end,1e-12)
            assertEquals(end/1000.0,gs[1].end)
        }
    }
    @Test fun diagnosticExpandedFragmentsDoNotOverwriteDeclaredLineBounds() {
        val source=KaraokeLine.MainKaraokeLine(listOf(KaraokeSyllable("early",0,200),KaraokeSyllable("late",300,900)),
            null,KaraokeAlignment.Unspecified,100,500)
        val document=normalize(SyncedLyrics(listOf(source)))
        assertTrue(document.diagnostics.isNotEmpty())
        val mapped=pendoloCoreLines(document).single()
        assertEquals(.1,mapped.stage.lyric.start);assertEquals(.5,mapped.stage.lyric.end)
        assertEquals(0.0,mapped.stage.lyric.words.first().start)
        assertEquals(.9,mapped.stage.lyric.words.last().end)
        assertTrue(mapped.stage.renderEnd>.9)
        assertEquals(emptyList(),pendoloCoreLines(normalize(SyncedLyrics(emptyList()))))
    }
}
