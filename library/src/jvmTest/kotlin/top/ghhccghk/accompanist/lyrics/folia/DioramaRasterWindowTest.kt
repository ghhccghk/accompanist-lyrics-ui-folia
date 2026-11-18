package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.runtime.SideEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.*
import top.ghhccghk.accompanist.lyrics.folia.internal.model.normalize
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama.*
import top.ghhccghk.accompanist.lyrics.folia.internal.text.*
import java.io.File
import java.util.Base64
import kotlin.math.*
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class DioramaRasterWindowTest {
    @Test fun longCompleteShapingReconstructsAcrossEveryRasterWindow() =
        runDesktopComposeUiTest(width=800,height=600) {
            var resolver: FontFamily.Resolver? = null
            setContent { val r=LocalFontFamilyResolver.current; SideEffect { resolver=r } }
            waitForIdle()
            val sourceRows=File("../verification/diorama-fixtures/raster-window-v1/source.tsv").readLines()
                .filterNot { it.startsWith('#') || it.isBlank() }.map { it.split('\t') }
            val report=mutableListOf("# case advance units oldFullAtlasBytes retainedBaseAndHaloBytes widestUnit changedPixels")
            runOnIdle {
                for(bidi in listOf(false,true)) {
                    val name=if(bidi) "mixed" else "latin"
                    val words=List(32) { i ->
                        if(bidi) listOf("Aمرحبا", "12B", "بالعالم", "fiji")[i%4]
                        else listOf("Wavy", "fiji")[i%2]
                    }
                    val pieces=words.mapIndexed { i, word -> word+if(i<words.lastIndex) " " else "" }
                    val original=KaraokeLine.MainKaraokeLine(pieces.mapIndexed { i, word ->
                        KaraokeSyllable(word,700+i*100,800+i*100)
                    },null,KaraokeAlignment.Unspecified,700,3900)
                    val mapped=dioramaCoreLines(normalize(SyncedLyrics(listOf(original)))).single()
                    assertSame(original,mapped.source)
                    for(kind in listOf("G","U")) {
                        val source=sourceRows.filter { it[0]==kind && it[1]==name }
                        val values=if(kind=="G") {
                            var cursor=0
                            dioramaLineTimeline(mapped.lyric).map { g ->
                                val start=cursor;cursor+=g.text.length
                                DioramaUnit(g.text,start,cursor,g.start,g.end,listOf(g))
                            }
                        }else dioramaUnits(mapped.lyric)
                        assertEquals(source.size,values.size)
                        for((r,unit)in source.zip(values)) {
                            assertEquals(String(Base64.getDecoder().decode(r[7]),Charsets.UTF_8),unit.text)
                            assertEquals(r[3].toInt(),unit.charStart);assertEquals(r[4].toInt(),unit.charEnd)
                            assertEquals(r[5].toDouble(),unit.start,1e-12);assertEquals(r[6].toDouble(),unit.end,1e-12)
                        }
                    }
                    val font=if(bidi) "FoliaParityBidi-Bold.ttf" else "FoliaParityLatin-Bold.ttf"
                    val style=TextStyle(fontFamily=FontFamily(Font(File("../verification/assets/$font"),FontWeight.Bold)),
                        fontWeight=FontWeight.Bold,fontSize=128.sp,textAlign=TextAlign.Left,
                        textDirection=TextDirection.ContentOrLtr,color=Color.White,shadow=Shadow.None)
                    val density=Density(1f)
                    val fontResolver=assertNotNull(resolver)
                    val measurer=TextMeasurer(fontResolver,density,LayoutDirection.Ltr)
                    val layout=measurer.measure(AnnotatedString(mapped.lyric.text),style,softWrap=false)
                    val prepared=DioramaActiveText.prepare(mapped.lyric,style,measurer,fontResolver)
                    assertTrue(prepared.lineAdvance>8192,"stress must exceed one ordinary atlas")
                    assertEquals(dioramaUnits(mapped.lyric),prepared.slices.map { it.unit },"all source units and inner clocks")
                    val width=ceil(prepared.lineAdvance).toInt()+180
                    val metrics=naturalFontMetrics(style,fontResolver,density)
                    val baseline=180+prepared.middleBaseline-paintedNaturalBaseline(layout,metrics)
                    var changed=0L
                    for(left in 0 until width step 1024) {
                        val w=min(1024,width-left)
                        fun bitmap(draw: androidx.compose.ui.graphics.drawscope.DrawScope.()->Unit):ImageBitmap {
                            val image=ImageBitmap(w,360)
                            CanvasDrawScope().draw(density,LayoutDirection.Ltr,Canvas(image),Size(w.toFloat(),360f),draw)
                            return image
                        }
                        val expected=bitmap { drawText(layout,Color.White,Offset(90f-left,baseline.toFloat()),shadow=Shadow.None) }
                        val actual=bitmap {
                            for(slice in prepared.slices) {
                                val x=90+prepared.lineAdvance/2+slice.centerPx+slice.imageOffsetPx-slice.image.width/2.0-left
                                if(x<w && x+slice.image.width>0) drawImage(slice.image,Offset(x.toFloat(),0f))
                            }
                        }
                        val a=IntArray(w*360).also { expected.readPixels(it) }
                        val b=IntArray(w*360).also { actual.readPixels(it) }
                        changed+=a.indices.count { a[it]!=b[it] }
                        assertContentEquals(a,b,"complete shaped run, bidi=$bidi window=$left; no clipped seam or duplicated ink")
                    }
                    val retained=prepared.slices.sumOf { (it.image.width.toLong()*it.image.height+it.halo.width.toLong()*it.halo.height)*4 }
                    report+=listOf(name,prepared.lineAdvance,prepared.slices.size,width.toLong()*360*4,
                        retained,prepared.slices.maxOf { it.image.width },changed).joinToString("\t")
                }
            }
            File("../build/verification/diorama-raster-window").apply { mkdirs() }
                .resolve("coverage.tsv").writeText(report.joinToString("\n")+"\n")
        }
}
