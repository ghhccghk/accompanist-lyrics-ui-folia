package top.ghhccghk.accompanist.lyrics.folia

import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.*
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import top.ghhccghk.accompanist.lyrics.folia.internal.model.*
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.fume.*
import java.io.File
import kotlin.test.*

class FumeCoreTimingTest {
    @Test fun rawBoundsAndLastParserEndAreNotReplacedByDisplayUnion() {
        val source=KaraokeLine.MainKaraokeLine(listOf(KaraokeSyllable("A",0,900),KaraokeSyllable("́",100,200)),
            "translation",KaraokeAlignment.Unspecified,100,500,"phonetic")
        val document=normalize(SyncedLyrics(listOf(source)));val normalized=document.lines.single()
        assertTrue(document.diagnostics.isNotEmpty());assertSame(source,normalized.source)
        val actual=fumeLyric(normalized)
        assertEquals(.1,actual.start);assertEquals(.5,actual.end)
        assertEquals(.9,actual.words.single().end,"display EGC union remains intact")
        assertEquals(.2,actual.rawLastWordEnd);assertEquals(.5,actual.renderEnd)
        assertEquals(listOf("A","́"),actual.words.single().syllables.map{it.text})
        assertEquals("translation",normalized.translation);assertEquals("phonetic",normalized.phonetic)
    }
    @Test fun realCoreLineTimingMatchesOriginalRenderHintsAtAllRecordedThresholds() {
        val inputs=File("../verification/pendolo-fixtures/core-render.tsv").readLines().filterNot{it.isBlank()||it.startsWith('#')}
        for(raw in inputs){
            val r=raw.split('\t');val source=SyncedLine("Amber echo",null,r[0].toInt(),r[1].toInt())
            val normalized=normalize(SyncedLyrics(listOf(source))).lines.single()
            assertSame(source,normalized.source)
            val actual=fumeLyric(normalized)
            assertEquals(r[2].toDouble(),actual.renderEnd,1e-10,raw)
            assertTrue(actual.words.isEmpty(),"line-only timing remains an explicit derived reveal")
            val reveal=FumeReveal(actual);val probes=listOf(actual.start-.01,actual.start,actual.end,actual.renderEnd)
            val expected=probes.associateWith(reveal::count)
            for(t in probes.reversed())assertEquals(expected[t],reveal.count(t))
        }
    }
}
