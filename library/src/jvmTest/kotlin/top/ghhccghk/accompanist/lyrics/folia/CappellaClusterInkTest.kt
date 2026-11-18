package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.*
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.cappella.*
import top.ghhccghk.accompanist.lyrics.folia.internal.text.*
import java.io.File
import java.util.Base64
import javax.imageio.ImageIO
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class CappellaClusterInkTest {
    @Test fun originalSpanPaintOwnsCompleteShapingClusters() = runDesktopComposeUiTest(width=800,height=300) {
        // Expectations come from original browser pixel equality/blank controls,
        // never from native cluster boundaries or native rendered data.
        val records=File("../verification/cappella-fixtures/cluster-ink-v1/paint.tsv").readLines().drop(1).map{it.split('\t')}
        val out=File("../build/verification/cappella-cluster-ink").apply{mkdirs()}
        val report=mutableListOf("# file wholeRowDifferentPixels paintedPixels owners")
        val failures=mutableListOf<String>()
        setContent {
            val measurer=rememberTextMeasurer();val resolver=LocalFontFamilyResolver.current
            SideEffect {
                for(r in records) {
                    val text=String(Base64.getDecoder().decode(r[5]),Charsets.UTF_8)
                    val style=TextStyle(fontFamily=FontFamily(Font(File("../verification/assets/${r[3]}"),FontWeight.Bold)),
                        fontWeight=FontWeight.Bold,fontStyle=if(r[4].toBoolean())FontStyle.Italic else FontStyle.Normal)
                    val measure=CappellaBubbleMeasure(measurer,style,Density(1f),CappellaBubbleStyle(fontPx=64.0,maxTextWidth=738.0),resolver)
                    val layout=measure.layout(text)
                    val edges=graphemeBoundaries(text).sorted()
                    val clusters=cappellaInkClusters(text,measure.style,resolver,Density(1f))
                    val runs=cappellaInkRuns(layout,edges.zipWithNext().mapIndexed{i,(a,b)->CappellaInkGlyph(i,a,b)},clusters)
                    val metrics=naturalFontMetrics(measure.style,resolver,Density(1f))
                    val origin=Offset(21f,17+cssFontBaseline(metrics.ascent,metrics.descent,92.8f)-paintedNaturalBaseline(layout,metrics))
                    fun render(reference:Boolean):ImageBitmap {
                        val image=ImageBitmap(800,300)
                        CanvasDrawScope().draw(Density(1f),LayoutDirection.Ltr,Canvas(image),Size(800f,300f)) {
                            if(reference)drawText(layout,Color.White,origin,shadow=Shadow.None)
                            else drawCappellaInk(layout,runs,runs.map{if(r[2].toInt()<0||it.index==r[2].toInt())1f else 0f},Color.White,origin)
                        }
                        return image
                    }
                    fun pixels(image:ImageBitmap)=IntArray(image.width*image.height).also{image.readPixels(it)}
                    val reference=pixels(render(true));val image=render(false);val actual=pixels(image)
                    ImageIO.write(image.toAwtImage(),"png",out.resolve(r[0]))
                    val different=actual.indices.count{actual[it]!=reference[it]};val count=actual.count{it ushr 24!=0}
                    report+="${r[0]}\t$different\t$count\t${runs.joinToString{it.index.toString()}}"
                    if(r[6].toBoolean()&&different!=0)failures+="${r[0]}: original owns full row; native loses $different pixels"
                    if(r[7].toBoolean()&&count!=0)failures+="${r[0]}: original continuation is empty; native paints $count pixels"
                }
            }
        }
        waitForIdle()
        out.resolve("coverage.tsv").writeText(report.joinToString("\n")+"\n")
        assertEquals(24,records.size)
        assertTrue(failures.isEmpty(),failures.joinToString("\n"))
    }

    @Test fun sharedPaintKeepsOriginalLogicalClockIndices() {
        val clocks=listOf(CappellaInkGlyph(7,0,1),CappellaInkGlyph(8,1,2),CappellaInkGlyph(9,2,4),CappellaInkGlyph(10,4,5))
        assertEquals(listOf(CappellaInkGlyph(7,0,2),clocks[2],clocks[3]),
            cappellaClusterOwners(clocks,listOf(CappellaInkCluster(0,2),CappellaInkCluster(2,4),CappellaInkCluster(4,5))))
        assertEquals(clocks,cappellaClusterOwners(clocks,emptyList()),"unavailable metadata retains the existing path")
        assertEquals(8,clocks[1].index,"source EGCs are not removed or renumbered")
    }

    @Test fun fontFeaturesMatchTheHostParagraphParser() = runDesktopComposeUiTest {
        setContent {
            val resolver=LocalFontFamilyResolver.current
            SideEffect {
                val style=TextStyle(fontFamily=FontFamily(Font(File("../verification/assets/FoliaParityLatin-Bold.ttf"),FontWeight.Bold)),
                    fontWeight=FontWeight.Bold,fontSize=64.sp)
                assertEquals(listOf(CappellaInkCluster(0,2)),cappellaInkClusters("fi",style,resolver,Density(1f)))
                assertEquals(listOf(CappellaInkCluster(0,1),CappellaInkCluster(1,2)),
                    cappellaInkClusters("fi",style.copy(fontFeatureSettings="liga 0"),resolver,Density(1f)))
                assertTrue(cappellaInkClusters("fi",style.copy(letterSpacing=1.sp),resolver,Density(1f)).isEmpty())
            }
        }
        waitForIdle()
    }
}
