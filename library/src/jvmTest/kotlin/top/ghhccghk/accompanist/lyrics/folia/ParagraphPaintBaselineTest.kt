package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.text.platform.FontLoadResult
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.*
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import top.ghhccghk.accompanist.lyrics.folia.internal.text.classicBodyRasterStyle
import top.ghhccghk.accompanist.lyrics.folia.internal.text.NaturalFontMetrics
import top.ghhccghk.accompanist.lyrics.folia.internal.text.paintedNaturalBaseline
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.floor
import kotlin.test.*

/** Native baseline control: compare one intact paragraph with a native TextLine.
 * One covered glyph isolates positioning; production shapes complete words. */
@OptIn(ExperimentalTestApi::class)
class ParagraphPaintBaselineTest {
    @Test fun measureActualNativeBaseline() = runDesktopComposeUiTest(width=160,height=160) {
        val fitted=File("../verification/classic-fixtures/fit-baselines.tsv").readLines()
            .filterNot {it.startsWith("#") || it.isBlank()}.map {Triple("FoliaParityLatin-Bold.ttf",it.split('\t')[0].toFloat(),"M")}
        val existing=File("../verification/classic-fixtures/body-layout.tsv").readLines()
            .filterNot {it.startsWith("#") || it.isBlank()}.map {it.split('\t')}
            .map {Triple(it[1],it[2].toFloat(),if(it[1].contains("Arabic")) "م" else "M")}
        val cases=(fitted+existing).distinct()
        var requested by mutableStateOf(cases.first())
        var layout:TextLayoutResult?=null
        var face:org.jetbrains.skia.Typeface?=null
        setContent {
            val family=remember(requested.first) {FontFamily(Font(File("../verification/assets/${requested.first}"),FontWeight.Bold))}
            val size=floor(requested.second*100+.00001f)/100
            val style=classicBodyRasterStyle(TextStyle(fontFamily=family,fontWeight=FontWeight.Bold,
                fontSize=size.sp,textMotion=TextMotion.Animated,textAlign=TextAlign.Left,textDirection=TextDirection.Ltr))
            val measured=rememberTextMeasurer().measure(requested.third,style,softWrap=false)
            val loaded=LocalFontFamilyResolver.current.resolve(family,FontWeight.Bold,FontStyle.Normal,FontSynthesis.All).value as FontLoadResult
            SideEffect {layout=measured;face=loaded.typeface}
        }
        val out=File("../build/verification/native-baseline-control").apply {mkdirs()}
        val rows=mutableListOf("font\tcssSize\tparagraphBaseline\tnativeBaseline\talphaError")
        val errors=mutableListOf<String>()
        for(probe in cases) {
            val size=probe.second
            runOnIdle {requested=probe};waitForIdle()
            val reference=ImageBitmap(160,160)
            CanvasDrawScope().draw(Density(1f),LayoutDirection.Ltr,Canvas(reference),Size(160f,160f)) {
                drawText(layout!!,Color.White,Offset(64f,64f),shadow=Shadow.None)
            }
            val ref=reference.toAwtImage()
            val pixels=ref.getRGB(0,0,160,160,null,0,160)
            var best=Long.MAX_VALUE;var bestBaseline=-1;var bestImage:java.awt.image.BufferedImage?=null
            var predicted=0f
            org.jetbrains.skia.Font(face!!,floor(size*100+.00001f)/100).use {font->
                font.isSubpixel=true;font.isLinearMetrics=true;font.isBaselineSnapped=false
                font.edging=org.jetbrains.skia.FontEdging.SUBPIXEL_ANTI_ALIAS
                font.hinting=org.jetbrains.skia.FontHinting.NORMAL
                predicted=paintedNaturalBaseline(layout!!,NaturalFontMetrics(-font.metrics.ascent,font.metrics.descent))
                org.jetbrains.skia.TextLine.make(probe.third,font).use {text->org.jetbrains.skia.Paint().use {paint->
                    paint.color=-1
                    for(baseline in 0..90) {
                        val candidate=ImageBitmap(160,160)
                        Canvas(candidate).skiaCanvas.drawTextLine(text,64f,64f+baseline,paint)
                        val im=candidate.toAwtImage();val p=im.getRGB(0,0,160,160,null,0,160)
                        var error=0L
                        for(i in pixels.indices) error+=kotlin.math.abs((pixels[i] ushr 24)-(p[i] ushr 24))
                        if(error<best) {best=error;bestBaseline=baseline;bestImage=im}
                    }
                }}
            }
            rows+="${probe.first}\t$size\t${layout!!.firstBaseline}\t$bestBaseline\t$best"
            ImageIO.write(ref,"png",out.resolve("${probe.first}-$size-paragraph.png"))
            ImageIO.write(bestImage!!,"png",out.resolve("${probe.first}-$size-native.png"))
            if(best!=0L) errors+="No exact native glyph match for $probe: error=$best"
            if(bestBaseline.toFloat()!=predicted) errors+="Actual baseline $bestBaseline, helper $predicted for $probe"
        }
        out.resolve("metrics.tsv").writeText(rows.joinToString("\n")+"\n")
        assertTrue(errors.isEmpty(),errors.joinToString("\n"))
    }
}
