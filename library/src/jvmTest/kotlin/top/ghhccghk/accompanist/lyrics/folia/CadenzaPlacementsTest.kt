package top.ghhccghk.accompanist.lyrics.folia

import top.ghhccghk.accompanist.lyrics.folia.internal.styles.cadenza.*
import java.io.File
import java.util.Base64
import kotlin.random.Random
import kotlin.test.*

class CadenzaPlacementsTest {
    private fun rows(name: String) = File("../verification/cadenza-fixtures/$name.tsv").readLines()
        .filterNot{it.startsWith('#')||it.isBlank()}.map{it.split('\t')}
    private fun decode(s:String)=String(Base64.getDecoder().decode(s),Charsets.UTF_8)
    private val fragments=rows("placementFragments").groupBy{it[0]}
    private val expected=rows("placements").groupBy{it[0]}
    private fun lines(a:List<String>):List<CadenzaFragmentLine> {
        val grouped=fragments[a[0]].orEmpty().groupBy{it[1].toInt()}
        return (0 until a[7].toInt()).map { row ->
            val fs=grouped.getValue(row)
            CadenzaFragmentLine(fs.first()[2].toDouble(),0,100,fs.map{f->
                val index=f[3].toInt();val text=decode(f[4]);val w=CadenzaWord(text,1+index*.1,1.5+index*.1)
                CadenzaFragment(index,w,decode(f[5]),"#f17b4d",f[6].toDouble(),f[7].toDouble(),f[8].toInt(),f[9].toInt(),
                    f[10].toInt(),listOf(CadenzaGlyphTime(text,w.start,w.end)),f[11].toInt(),f[12].toInt(),f[13].toBoolean(),f[14].toBoolean())
            })
        }
    }
    private fun build(a:List<String>,lines:List<CadenzaFragmentLine>)=buildCadenzaPlacements(lines,
        a[1].toDouble(),a[2].toDouble(),a[3].toDouble(),CadenzaIntensity.valueOf(a[4].uppercase()),a[5].toDouble(),a[6].toBoolean())
    @Test fun seededHeroAvoidanceFallbackAndDriftMatchOriginal() {
        var count=0;var maximum=0.0
        for(a in rows("placementInputs").shuffled(Random(117))) {
            val lines=lines(a);val before=lines.toString();val ps=build(a,lines);val es=expected[a[0]].orEmpty()
            assertEquals(es.size,ps.size,"scenario ${a[0]}")
            ps.zip(es).forEach { (p,e)->
                assertEquals(decode(e[2]),p.id);assertEquals(e[3].toInt(),p.fragment.wordIndex)
                assertEquals(e[5].toBoolean(),p.interlude)
                val actual=listOf(p.x,p.y,p.width,p.height,p.rotation,p.scale,p.passedRotation,p.passedDriftX,p.passedDriftY,p.entryX,p.entryY,p.emphasis)
                e[4].split(',').map(String::toDouble).zip(actual).forEachIndexed { i,(v,q)->
                    maximum=maxOf(maximum,kotlin.math.abs(v-q));assertEquals(v,q,1e-8,"case ${a[0]} ${p.id} field$i")
                }
                if(p.fragment.split||p.interlude)assertEquals(1.0,p.emphasis,"split word must not become hero")
                assertTrue(p.fragment.timings.isNotEmpty(),"placement retains internal timing")
                count++
            }
            assertEquals(before,lines.toString(),"inputs mutated");assertEquals(ps,build(a,lines),"repeat build")
        }
        val out=File("../build/verification/cadenza-placements-kotlin").apply{mkdirs()}
        out.resolve("comparison.txt").writeText("placements=$count\nmaximum_absolute_error=$maximum\n")
    }
}
