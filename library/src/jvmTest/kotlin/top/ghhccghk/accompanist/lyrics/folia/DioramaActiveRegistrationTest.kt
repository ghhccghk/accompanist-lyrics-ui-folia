// SPDX-License-Identifier: AGPL-3.0-only
package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.*
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama.*
import java.io.File
import java.util.Base64
import kotlin.math.ceil
import kotlin.test.*

/** Independent original captured pivots versus own complete-run geometry.
 * Pre-change values retained in verification/diorama-rtl-registration.
 * No image registration or source raster reuse. */
@OptIn(ExperimentalTestApi::class)
class DioramaActiveRegistrationTest {
    @Test fun originalUnitGeometry()=runDesktopComposeUiTest(width=800,height=200) {
        val probes=File("../verification/diorama-fixtures/active-base.tsv").readLines()
            .filterNot{it.startsWith('#')||it.isBlank()}.map{it.split('\t')}.filter{it[2]=="800"&&it[3]=="0"}
        val source=File("../verification/diorama-fixtures/active-base-sampler-input-v1/source.tsv").readLines()
            .filterNot{it.startsWith('#')||it.isBlank()}.map{it.split('\t')}.groupBy{it[0]}
        fun decode(s:String)=String(Base64.getDecoder().decode(s),Charsets.UTF_8)
        var observations=emptyList<String>()
        setContent {
            val resolver=LocalFontFamilyResolver.current
            val rows=remember {
                probes.flatMap { r ->
                    val font=when(r[1]){"arabic"->"FoliaParityBidi-Bold.ttf";"cjk"->"FoliaParityChatCJK-Bold.otf";else->"FoliaParityLatin-Bold.ttf"}
                    val words=r[5].split(';').map{it.split(',').let{DioramaTimedText(decode(it[0]),it[1].toDouble(),it[2].toDouble())}}
                    val style=TextStyle(fontFamily=FontFamily(Font(File("../verification/assets/$font"),FontWeight.Bold)))
                    val text=DioramaActiveText.prepare(DioramaLyric(decode(r[4]),.7,words.last().end,words),style,
                        TextMeasurer(resolver,Density(1f),LayoutDirection.Ltr),resolver)
                    assertEquals(r[6].toDouble()*128/.62,text.lineAdvance,.001)
                    text.slices.mapIndexed { i,s ->
                        val e=source.getValue(r[0])[i]
                        val expected=e[3].toDouble()*128/.62
                        if(r[1]=="arabic") {
                            assertEquals(expected,s.centerPx,.001,"Independent original RTL pivot")
                            assertEquals(e[4].toDouble()*128/.62,s.image.width.toDouble(),.001,"Independent original RTL canvas width")
                            assertEquals(0.0,s.imageOffsetPx,"Original word registration has no canonical phase offset")
                        }
                        val raw=(s.rangeRight-s.rangeLeft).toDouble()
                        // Independent source formula: RTL starts the ceil-sized
                        // raster at right-prefix-ceil(width), LTR at prefix.
                        val rounding=ceil(raw)-raw
                        listOf(r[1],i,s.rangeLeft,s.rangeRight,s.centerPx,s.imageOffsetPx,
                            s.image.width,expected,s.centerPx-expected,rounding,
                            s.centerPx-expected-rounding).joinToString("\t")
                    }
                }
            }
            SideEffect{observations=rows}
        }
        waitForIdle();assertEquals(12,observations.size)
        val out=File("../build/verification/diorama-active-registration-kotlin").apply{mkdirs()}
        out.resolve("geometry.tsv").writeText("# scene unit left right pivot canonicalImageOffset width sourcePivot pivotError ceilRounding errorMinusRounding\n"+observations.joinToString("\n")+"\n")
    }
}
