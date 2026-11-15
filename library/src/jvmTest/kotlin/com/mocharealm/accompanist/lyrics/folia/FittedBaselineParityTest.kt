package com.mocharealm.accompanist.lyrics.folia

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.sp
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.mocharealm.accompanist.lyrics.folia.internal.text.*
import java.io.File
import kotlin.math.floor
import kotlin.test.*

/** Compare fitted-size baseline placement to the real original GlowWord. */
@OptIn(ExperimentalTestApi::class)
class FittedBaselineParityTest {
    @Test fun originalFittedFontBaselines() = runDesktopComposeUiTest(width=600,height=400) {
        val fitted=File("../verification/classic-fixtures/fit-baselines.tsv").readLines()
            .filterNot {it.startsWith("#") || it.isBlank()}.map {it.split('\t')}
            .map {listOf("FoliaParityLatin-Bold.ttf",it[0],it[1],it[2],"Amber")}
        val existing=File("../verification/classic-fixtures/body-layout.tsv").readLines()
            .filterNot {it.startsWith("#") || it.isBlank()}.map {it.split('\t')}
            .map {listOf(it[1],it[2],it[5],it[6],it[3])}
        val rows=fitted+existing
        var row by mutableStateOf(rows.first())
        var actual=0f
        var raw=""
        setContent {
            val family=remember(row[0]) {FontFamily(Font(File("../verification/assets/${row[0]}"),FontWeight.Bold))}
            val size=floor(row[1].toFloat()*100+.00001f)/100
            val style=TextStyle(fontFamily=family,fontWeight=FontWeight.Bold,fontSize=size.sp,textMotion=TextMotion.Animated)
            val layout=rememberTextMeasurer().measure(row[4],style,softWrap=false)
            val body=rememberTextMeasurer().measure(row[4],classicBodyRasterStyle(style),softWrap=false)
            val natural=naturalFontMetrics(style,LocalFontFamilyResolver.current,LocalDensity.current)
            val reported=cssBaseline(layout.firstBaseline,layout.multiParagraph.height,row[2].toFloat())
            val unrounded=cssFontBaseline(natural.ascent,natural.descent,row[2].toFloat())
            SideEffect {
                actual=unrounded
                raw="${row[0]}\t${row[1]}\t$size\t${layout.firstBaseline}\t${layout.multiParagraph.height}\t${natural.ascent}\t${natural.descent}\t${row[3]}\t$reported\t$unrounded\t${paintedLineBaseline(layout,0)}\t${paintedLineBaseline(body,0)}\t${paintedNaturalBaseline(layout,natural)}"
            }
        }
        val report=mutableListOf("font\tcssFontSize\trenderedSize\tparagraphBaseline\tparagraphHeight\tfontAscent\tfontDescent\toriginalBaseline\tparagraphDerived\tfontDerived\troundedReportedShadowBaseline\troundedReportedBodyBaseline\tnaturalPaintBaseline")
        val failures=mutableListOf<String>()
        for(c in rows) {
            runOnIdle {row=c};waitForIdle()
            report+=raw
            if(kotlin.math.abs(c[3].toFloat()-actual)>.0001f) failures+="Original ${c[3]}, Kotlin $actual at ${c[1]}px ${c[0]}"
        }
        File("../build/verification/fit-baselines-kotlin").apply {mkdirs()}
            .resolve("metrics.tsv").writeText(report.joinToString("\n")+"\n")
        assertTrue(failures.isEmpty(),failures.joinToString("\n"))
    }
}
