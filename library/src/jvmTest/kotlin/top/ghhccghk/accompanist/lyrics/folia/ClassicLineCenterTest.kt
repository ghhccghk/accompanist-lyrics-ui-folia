package top.ghhccghk.accompanist.lyrics.folia

import top.ghhccghk.accompanist.lyrics.folia.internal.styles.classic.classicLineCenter
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.classic.classicHeightBudget
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class ClassicLineCenterTest {
    @Test fun actualOriginalFlexCentersAtOddAndEvenViewports() {
        val rows=File("../verification/classic-fixtures/line-centers.tsv").readLines()
            .filterNot {it.startsWith("#") || it.isBlank()}.map {it.split('\t')}
        assertEquals(36,rows.size)
        for(c in rows) {
            val actual=classicLineCenter(c[3].toFloat(),c[5].toFloat(),1f)
            assertEquals(c[8].toFloat(),actual,.0001f,c.toString())
            assertEquals(c[9].toFloat(),classicHeightBudget(c[3].toFloat(),1f),.0001f,c.toString())
            assertEquals(actual*2,classicLineCenter(c[3].toFloat()*2,c[5].toFloat()*2,2f),.0001f,"Density conversion: $c")
        }
    }
}
