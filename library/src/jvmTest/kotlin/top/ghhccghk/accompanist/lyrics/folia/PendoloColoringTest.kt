package top.ghhccghk.accompanist.lyrics.folia

import top.ghhccghk.accompanist.lyrics.folia.internal.styles.pendolo.*
import java.io.File
import java.util.Base64
import kotlin.test.*

class PendoloColoringTest {
    @Test fun originalRangePriorityGraphemeCoverageAndAdjacentRuns() {
        fun decode(s:String)=if(s=="-")"" else String(Base64.getDecoder().decode(s),Charsets.UTF_8)
        val rows=File("../verification/pendolo-fixtures/coloring.tsv").readLines().filterNot{it.startsWith('#')||it.isBlank()}.map{it.split('\t')}
        for((key,all)in rows.groupBy{it[1]}){
            val c=all.single{it[0]=="C"};val text=decode(c[2]);val fallback=c[3]
            val entries=all.filter{it[0]=="E"}.map{PendoloKeyword(decode(it[2]),it[3])}
            val ranges=pendoloColorRanges(text,entries)
            assertEquals(all.filter{it[0]=="R"}.map{PendoloColorRange(it[2].toInt(),it[3].toInt(),it[4],it[5].toInt())},ranges,"$key ranges")
            val colors=pendoloGraphemeColors(text,ranges)
            assertEquals(all.filter{it[0]=="G"}.associate{it[2].toInt() to it[3]},colors,"$key colors")
            for(u in all.filter{it[0]=="U"}){
                val start=u[2].toInt();val row=decode(u[3]);val runs=pendoloColorRuns(row,start,colors,fallback)
                val expected=all.filter{it[0]=="P"&&it[2]==u[2]}.map{listOf(it[3],decode(it[4]),it[5])}
                assertEquals(expected,runs.map{listOf("${it.start}-${it.color}",it.text,it.color)},"$key/$start runs")
                assertEquals(row,runs.joinToString(""){it.text},"$key/$start shaping source retained")
            }
        }
    }
}
