package top.ghhccghk.accompanist.lyrics.folia

import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.*
import top.ghhccghk.accompanist.lyrics.folia.internal.model.normalize
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.cadenza.*
import java.io.File
import java.util.Base64
import kotlin.test.*

class CadenzaFragmentsTest {
    private fun rows(name: String)=File("../verification/cadenza-fixtures/$name.tsv").readLines()
        .filterNot{it.startsWith('#')||it.isBlank()}.map{it.split('\t')}
    private fun decode(s:String)=String(Base64.getDecoder().decode(s),Charsets.UTF_8)
    private val lyrics=rows("fragmentInputs").associate{f->f[0] to CadenzaLyric(decode(f[1]),f[2].toDouble(),f[3].toDouble(),
        if(f[4]=="-")emptyList()else f[4].split('|').map{d->val w=d.split('~');CadenzaWord(decode(w[0]),w[1].toDouble(),w[2].toDouble(),
            if(w[3].isEmpty())emptyList()else w[3].split(';').map{s->val v=s.split('@');CadenzaWord(decode(v[0]),v[1].toDouble(),v[2].toDouble())})})}
    private var maximum=0.0
    private fun numbers(e:String,actual:List<Double>,label:String){val expected=e.split(',').map(String::toDouble)
        assertEquals(expected.size,actual.size,label);expected.zip(actual).forEachIndexed{i,(a,b)->
            maximum=maxOf(maximum,kotlin.math.abs(a-b));assertEquals(a,b,1e-8,"$label field$i")}}
    @Test fun actualBrowserSegmentsBuildSourceWordRangesFragmentsAndPlacements() {
        val segments=rows("segments").groupBy{it[0]};val wraps=rows("wraps").groupBy{it[0]}
        val ranges=rows("wordRanges").groupBy{it[0]};val placements=rows("realPlacements").groupBy{it[0]}
        var total=0;var split=0
        for(f in rows("geometryInputs")){
            val line=lyrics.getValue(f[1]);val text=decode(f[4]);val size=cadenzaPreparationSize(line,f[2].toDouble(),text,f[3].toDouble())
            numbers(f.subList(5,8).joinToString(","),listOf(size.fontPx,size.lineHeight,size.maxWidth),"${f[0]} size")
            val ss=segments[f[0]].orEmpty().map{s->CadenzaTextSegment(decode(s[2]),s[3].toDouble(),if(s[4]=="-")null else s[4].split(',').map(String::toDouble))}
            val graphemes=ss.flatMap{cadenzaGraphemes(it.text)};assertEquals(text,graphemes.joinToString(""))
            val rs=findCadenzaWordRanges(line,graphemes);val expectedRanges=ranges[f[0]].orEmpty();assertEquals(expectedRanges.size,rs.size)
            rs.zip(expectedRanges).forEach{(r,e)->
                assertEquals(listOf(e[1].toInt(),e[2].toInt(),e[3].toInt()),listOf(r.wordIndex,r.start,r.end));assertEquals(e[4],r.color)
                val timings=if(e[5]=="-")emptyList()else e[5].split(';');assertEquals(timings.size,r.timings.size)
                r.timings.zip(timings).forEach{(a,b)->val t=b.split(',');assertEquals(decode(t[0]),a.text);numbers(t.drop(1).joinToString(","),listOf(a.start,a.end),"${f[0]} glyph")}
            }
            val ls=wraps[f[0]].orEmpty().map{w->CadenzaWrappedLine(w[2].toInt(),w[3].toInt(),w[4].toDouble())}
            val fs=buildCadenzaFragmentLines(ss,ls,rs)
            for(frag in fs.flatMap{it.fragments}){
                assertEquals(frag.word.text,frag.word.syllables.takeIf{it.isNotEmpty()}?.joinToString(""){it.text}?:frag.word.text)
                if(frag.split){split++;assertTrue(frag.countInWord>1)}
            }
            val ps=buildCadenzaPlacements(fs,size.fontPx,size.lineHeight,size.maxWidth,CadenzaIntensity.NORMAL,line.start*1000,false)
            val es=placements[f[0]].orEmpty();assertEquals(es.size,ps.size,"${f[0]} placement count")
            ps.zip(es).forEach{(p,e)->
                assertEquals(decode(e[2]),p.id);assertEquals(e[3].toInt(),p.fragment.wordIndex);assertEquals(decode(e[4]),p.fragment.text)
                assertEquals(e.subList(5,8).map(String::toInt),listOf(p.fragment.startInWord,p.fragment.endInWord,p.fragment.graphemeCount))
                numbers(e[8],listOf(p.x,p.y,p.width,p.height,p.scale,p.passedRotation,p.passedDriftX,p.passedDriftY,p.emphasis),"${f[0]} ${p.id}");total++
            }
        }
        assertTrue(split>0,"matrix must exercise wrapped word fragments")
        File("../build/verification/cadenza-fragments-kotlin").apply{mkdirs()}.resolve("comparison.txt")
            .writeText("placements=$total\nsplit_fragments=$split\nmaximum_absolute_error=$maximum\n")
    }
    @Test fun explicitAnnotationsMatchActualIngestionAndDoNotOverrideCustomHints(){
        for(f in rows("annotations")){
            val a=annotateCadenzaLine(lyrics.getValue(f[0]));val h=assertNotNull(a.hints)
            numbers(f.subList(1,3).joinToString(","),listOf(h.rawDuration,h.renderEnd),"annotation ${f[0]}")
            assertEquals(f[3],h.transition.name.lowercase());assertEquals(f[4],h.reveal.name.lowercase())
            assertSame(a,annotateCadenzaLine(a))
        }
    }
    @Test fun coreAdapterPreservesRawFragmentsWhileCoalescingUnsafeGraphemeSplits(){
        val parts=listOf("A","́ ","👩","‍🚀","tail")
        val source=KaraokeLine.MainKaraokeLine(parts.mapIndexed{i,text->KaraokeSyllable(text,i*100,(i+1)*100)},
            null,KaraokeAlignment.Unspecified,0,500)
        val line=cadenzaLyric(normalize(SyncedLyrics(listOf(source))).lines.single())
        assertEquals(parts.joinToString(""),line.text)
        assertEquals(listOf("Á ","👩‍🚀","tail"),line.words.map{it.text})
        assertEquals(listOf("A","́ "),line.words[0].syllables.map{it.text})
        val times=cadenzaWordTimings(line.words[0]);assertEquals(listOf("Á"," "),times.map{it.text})
        numbers("0,0.15,0.15,0.2",times.flatMap{listOf(it.start,it.end)},"coalesced accent")
        val astronaut=cadenzaWordTimings(line.words[1]).single();assertEquals("👩‍🚀",astronaut.text)
        numbers("0.2,0.4",listOf(astronaut.start,astronaut.end),"coalesced astronaut")
        assertNotNull(line.hints)
    }
}
