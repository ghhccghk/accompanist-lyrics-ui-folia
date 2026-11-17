package top.ghhccghk.accompanist.lyrics.folia

import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.*
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import top.ghhccghk.accompanist.lyrics.folia.internal.model.*
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.cappella.*
import java.io.File
import java.util.Base64
import kotlin.random.Random
import kotlin.test.*

class CappellaCoreAdapterTest {
    private fun decode(s:String)=if(s=="-")""else String(Base64.getDecoder().decode(s),Charsets.UTF_8)
    private fun rows(name:String)=File("../verification/cappella-fixtures/$name.tsv").readLines().filterNot{it.startsWith('#')||it.isBlank()}.map{it.split('\t')}
    @Test fun realCoreLineTimingAndAgentsMatchPinnedFunctions() {
        val expected=rows("core")
        for((key,input)in rows("core-inputs").groupBy{it[0]}){
            val original=input.map{SyncedLine(decode(it[2]),null,it[3].toInt(),it[4].toInt())}
            val document=normalize(SyncedLyrics(original))
            val agents=input.mapNotNull{r->decode(r[5]).takeIf{it.isNotEmpty()}?.let{SourceRef(r[1].toInt()) to it}}.toMap()
            val mapped=cappellaCoreLines(document,agents);val lines=mapped.map{it.stage}
            for((i,m)in mapped.withIndex()){
                assertSame(original[i],m.source);assertEquals(SourceRef(i),m.ref);assertTrue(m.stage.words.isEmpty())
                val gs=expected.filter{it[0]=="G"&&it[1]==key&&it[2]=="$i"}
                val plan=CappellaRevealPlan(m.stage)
                assertEquals(gs.map{decode(it[4])},plan.characters)
                for((r,g)in gs.zip(plan.timeline)){
                    assertEquals(r[5].toDouble(),g.start,1e-10);assertEquals(r[6].toDouble(),g.end,1e-10)
                }
                assertEquals(expected.single{it[0]=="R"&&it[1]==key&&it[2]=="$i"}[3].toDouble(),m.stage.renderEnd,1e-10)
                val probes=listOf(m.stage.start-.01,m.stage.start,m.stage.end,m.stage.renderEnd)
                val counts=probes.associateWith(plan::visibleCount)
                for(t in probes.shuffled(Random(409)))assertEquals(counts[t],plan.visibleCount(t))
            }
            for(intensity in CappellaIntensity.entries){
                val messages=buildCappellaMessages(lines,"Synthetic title",intensity).filter{it.line!=null}
                val source=expected.filter{it[0]=="M"&&it[1]==key&&it[2]==intensity.name.lowercase()}
                for((r,m)in source.zip(messages)){
                    assertEquals(r[3].toInt(),m.line);assertEquals(r[4],m.sender.side.name.lowercase());assertEquals(r[5].toInt(),m.sender.avatar)
                }
                assertEquals(source.size,messages.size)
            }
        }
    }
    @Test fun parserCutsKeepCompleteClustersAndNestedRawClocks() {
        val parts=listOf("A","́ ","👩","‍🚀","tail")
        val source=KaraokeLine.MainKaraokeLine(parts.mapIndexed{i,s->KaraokeSyllable(s,i*100,(i+1)*100)},
            "translated",KaraokeAlignment.Unspecified,0,500,"phonetic")
        val mapped=cappellaCoreLines(normalize(SyncedLyrics(listOf(source)))).single()
        assertSame(source,mapped.source);assertEquals("translated",mapped.translation);assertEquals("phonetic",mapped.phonetic)
        assertEquals(listOf("Á ","👩‍🚀","tail"),mapped.stage.words.map{it.text})
        assertEquals(parts.take(2),mapped.stage.words[0].syllables.map{it.text})
        assertEquals(parts.subList(2,4),mapped.stage.words[1].syllables.map{it.text})
        val plan=CappellaRevealPlan(mapped.stage)
        assertEquals(listOf("Á"," ","👩‍🚀","t","a","i","l"),plan.characters)
        assertEquals(0.0,plan.timeline[0].start);assertEquals(.15,plan.timeline[0].end,1e-12)
        assertEquals(.2,plan.timeline[2].start);assertEquals(.4,plan.timeline[2].end)
        assertNull(mapped.stage.agent)
    }
    @Test fun nestedVoicesPreserveRefAndHaveNoGuessedIdentity() {
        val back=KaraokeLine.AccompanimentKaraokeLine(listOf(KaraokeSyllable("back",300,900)),null,KaraokeAlignment.Unspecified,300,900)
        val lead=KaraokeLine.MainKaraokeLine(listOf(KaraokeSyllable("lead",100,800)),null,KaraokeAlignment.Unspecified,100,800,accompanimentLines=listOf(back))
        val lyrics=SyncedLyrics(listOf(lead));val document=normalize(lyrics)
        val mapped=cappellaCoreLines(document)
        assertEquals(listOf(SourceRef(0),SourceRef(0,0)),mapped.map{it.ref})
        assertSame(lead,mapped[0].source);assertSame(back,mapped[1].source)
        assertTrue(mapped.all{it.stage.agent==null})
        val explicit=cappellaCoreLines(document,mapOf(SourceRef(0) to " lead ",SourceRef(0,0) to "guest"))
        assertEquals(listOf("lead","guest"),explicit.map{it.stage.agent})
        assertSame(back,explicit[1].source)
        assertTrue(cappellaCoreLines(normalize(SyncedLyrics(emptyList()))).isEmpty())
    }
    @Test fun sourceBoundsAndRawLastEndSurviveDiagnosticsAndEgcUnion() {
        val source=KaraokeLine.MainKaraokeLine(listOf(KaraokeSyllable("A",0,900),KaraokeSyllable("́",100,200)),
            null,KaraokeAlignment.Unspecified,100,500)
        val document=normalize(SyncedLyrics(listOf(source)))
        assertTrue(document.diagnostics.isNotEmpty())
        val mapped=cappellaCoreLines(document).single().stage
        assertEquals(.1,mapped.start);assertEquals(.5,mapped.end)
        assertEquals(.9,mapped.words.single().end)
        assertEquals(.5,mapped.renderEnd,"raw final fragment .2 must not become merged .9 + hold/exit")
        // Reuse the actual pinned renderHints fixture, not a native expectation.
        for(r in File("../verification/pendolo-fixtures/core-render.tsv").readLines().filterNot{it.startsWith('#')||it.isBlank()}){
            val c=r.split('\t');val line=SyncedLine("AB",null,c[0].toInt(),c[1].toInt())
            assertEquals(c[2].toDouble(),cappellaCoreLines(normalize(SyncedLyrics(listOf(line)))).single().stage.renderEnd,1e-10,r)
        }
    }
}
