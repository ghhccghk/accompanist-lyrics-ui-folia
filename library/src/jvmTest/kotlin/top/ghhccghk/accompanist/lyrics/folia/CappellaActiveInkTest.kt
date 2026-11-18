package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.cappella.*
import top.ghhccghk.accompanist.lyrics.folia.internal.text.*
import java.io.File
import java.util.Base64
import javax.imageio.ImageIO
import kotlin.random.Random
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class CappellaActiveInkTest {
    @Test fun sourceFadesAndUniformOpacityPreserveWholeShapedInk()=runDesktopComposeUiTest(width=800,height=300) {
        val frames=File("../verification/cappella-fixtures/active-ink-v1/frames.tsv").readLines()
            .filterNot{it.isBlank()||it.startsWith('#')}.map{it.split('\t')}
        assertEquals(30,frames.size)
        var record by mutableStateOf(frames.first())
        var reference by mutableStateOf(false)
        val config=CappellaBubbleStyle(fontPx=64.0,maxTextWidth=738.0)
        val out=File("../build/verification/cappella-active-ink").apply{mkdirs()}
        val checked=mutableSetOf<String>()
        setContent {
            val r=record;val name=r[1];val uniform=r[2].toBoolean();val time=r[3].toDouble()
            val text=String(Base64.getDecoder().decode(r[5]),Charsets.UTF_8)
            val chars=cappellaGraphemes(text)
            val line=remember(text,uniform){CappellaLine(text,0.0,1.0,chars.mapIndexed{i,c->
                CappellaWord(c,if(uniform)0.0 else i.toDouble()/chars.size*.8,if(uniform)1.0 else (i+1.0)/chars.size*.8)})}
            val plan=remember(line){CappellaRevealPlan(line)}
            assertEquals(r[4].toInt(),plan.visibleCount(time),"source visible EGC count")
            val expected=r[6].takeIf{it.isNotEmpty()}?.split(',')?.map(String::toDouble).orEmpty()
            assertEquals(plan.visibleCount(time),expected.size)
            for((i,alpha)in expected.withIndex())assertEquals(alpha,cappellaEaseOut(plan.progress(i,time)),1e-6,"source CSS alpha $name/$time/$i")
            val font=when(name){"arabic","mixed"->"FoliaParityBidi-Bold.ttf";"cjk"->"FoliaParityChatCJK-Bold.otf";else->"FoliaParityLatin-Bold.ttf"}
            val base=remember(name){TextStyle(fontFamily=FontFamily(Font(File("../verification/assets/$font"),FontWeight.Bold)),fontWeight=FontWeight.Bold,
                fontStyle=if(name=="italic")FontStyle.Italic else FontStyle.Normal,textDirection=TextDirection.ContentOrLtr,textAlign=TextAlign.Left)}
            val resolver=LocalFontFamilyResolver.current;val measurer=rememberTextMeasurer()
            SideEffect {
                if(checked.add(name)) {
                    val measure=CappellaBubbleMeasure(measurer,base,Density(1f),config,resolver)
                    val paragraph=measure.layout(text)
                    val edges=graphemeBoundaries(text).sorted()
                    val runs=cappellaInkRuns(paragraph,edges.zipWithNext().mapIndexed{i,(a,b)->CappellaInkGlyph(i,a,b)})
                    assertTrue(runs.isNotEmpty())
                    for(x in -50..(paragraph.size.width+50)) {
                        assertEquals(1,runs.count{(it.left==null||x>=it.left)&&(it.right==null||x<it.right)},"disjoint whole-ink ownership $name/$x")
                    }
                    for(scale in listOf(.75f,1f,1.3f)) {
                        fun raster(partitioned:Boolean):IntArray {
                            val image=ImageBitmap(1100,300)
                            CanvasDrawScope().draw(Density(1f),LayoutDirection.Ltr,Canvas(image),androidx.compose.ui.geometry.Size(1100f,300f)) {
                                withTransform({translate(23.25f,17.5f);scale(scale,scale,pivot=Offset.Zero)}) {
                                    if(!partitioned)drawText(paragraph,Color.White,shadow=Shadow.None)
                                    else drawCappellaInk(paragraph,runs,List(runs.size){1f},Color.White)
                                }
                            }
                            return IntArray(image.width*image.height).also{image.readPixels(it)}
                        }
                        val whole=raster(false);val shaded=raster(true)
                        assertEquals(0,whole.indices.count{whole[it]!=shaded[it]},"single alpha field preserves exact transformed shaped ink $name/$scale")
                    }
                }
            }
            if(reference) {
                val measure=remember(base,resolver){CappellaBubbleMeasure(measurer,base,Density(1f),config,resolver)}
                val paragraph=remember(base,resolver,text){measure.layout(text)}
                val metrics=remember(base,resolver){naturalFontMetrics(measure.style,resolver,Density(1f))}
                Canvas(Modifier.fillMaxSize()) {
                    val top=17+cssFontBaseline(metrics.ascent,metrics.descent,92.8f)-paintedNaturalBaseline(paragraph,metrics)
                    drawText(paragraph,Color.White,Offset(21f,top),alpha=expected.first().toFloat(),shadow=Shadow.None)
                }
            } else CappellaBubbleTarget(line,{time},base,CappellaBubbleColors(Color.Transparent,Color.Transparent,Color.White),
                Modifier.fillMaxSize(),config=config,active=true,sizeOverride=CappellaBubbleSize(780.0,160.0))
        }
        fun pixels():IntArray {waitForIdle();val image=captureToImage();return IntArray(image.width*image.height).also{image.readPixels(it)}}
        val report=mutableListOf("# file uniformDifferentPixels")
        val failures=mutableListOf<String>();val states=linkedMapOf<List<String>,IntArray>()
        for(r in frames) {
            runOnIdle{record=r;reference=false};val actual=pixels();states[r]=actual
            ImageIO.write(captureToImage().toAwtImage(),"png",out.resolve(r[0]))
            if(r[2].toBoolean()||r[3].toDouble()>1) {
                runOnIdle{reference=true};val expected=pixels()
                val difference=expected.indices.count{expected[it]!=actual[it]}
                report+="${r[0]}\t$difference"
                if(difference!=0)failures+="${r[0]} clips or duplicates $difference complete-run pixels"
                ImageIO.write(captureToImage().toAwtImage(),"png",out.resolve("reference-${r[0]}"))
            }
        }
        for((r,expected)in states.entries.shuffled(Random(548))) {
            runOnIdle{record=r;reference=false};assertContentEquals(expected,pixels(),"random seek ${r[0]}")
        }
        val frozen=pixels();mainClock.advanceTimeBy(1500);assertContentEquals(frozen,pixels(),"paused fade")
        out.resolve("coverage.tsv").writeText(report.joinToString("\n")+"\n")
        assertTrue(failures.isEmpty(),failures.joinToString("\n"))
    }
}
