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
import androidx.compose.ui.text.platform.FontLoadResult
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import top.ghhccghk.accompanist.lyrics.folia.internal.text.paintedLineBaseline
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.math.floor
import kotlin.test.*

/** Isolates actual paint positions without changing the original subtitle fixtures.
 * Production keeps complete shaped paragraphs; covered single glyphs are probes only. */
@OptIn(ExperimentalTestApi::class)
class SubtitlePaintBaselineTest {
    private data class Probe(val font: String, val size: Float, val text: String)

    @Test fun explicitLineHeightPaintMatchesNativeBaseline() = runDesktopComposeUiTest(width=160,height=160) {
        val probes = listOf(
            "FoliaParityLatin-Bold.ttf" to "M",
            "NotoSansArabic-Bold.ttf" to "م",
            "FoliaParityCJK-Bold.otf" to "合",
            "FoliaParitySubtitle-Bold.otf" to "合",
        ).flatMap { (font, glyph) ->
            listOf(14f,15.84f,16f,17.6f,18f,20f).map { Probe(font,it,glyph) }
        }
        var requested by mutableStateOf(probes.first())
        var layout: TextLayoutResult? = null
        var face: org.jetbrains.skia.Typeface? = null
        setContent {
            val family = remember(requested.font) {
                FontFamily(Font(File("../verification/assets/${requested.font}"),FontWeight.Bold))
            }
            val usedSize = floor(requested.size*64+.5f)/64
            val lineHeight = floor(usedSize*1.5f*64)/64
            val style = TextStyle(fontFamily=family,fontWeight=FontWeight.Bold,fontSize=requested.size.sp,
                textMotion=TextMotion.Animated,textAlign=TextAlign.Start,textDirection=TextDirection.Ltr,
                lineHeight=lineHeight.sp,
                lineHeightStyle=LineHeightStyle(LineHeightStyle.Alignment.Center,LineHeightStyle.Trim.None,LineHeightStyle.Mode.Tight))
            val measured = rememberTextMeasurer().measure("${requested.text}\n${requested.text}",style,softWrap=false)
            val loaded = LocalFontFamilyResolver.current.resolve(family,FontWeight.Bold,FontStyle.Normal,FontSynthesis.All).value as FontLoadResult
            SideEffect { layout=measured; face=loaded.typeface }
        }
        val out = File("../build/verification/subtitle-native-baseline-control").apply { mkdirs() }
        val rows = mutableListOf("font\tfontSize\tline\treportedBaseline\thelperBaseline\tactualBaseline\talphaError\trawAscent\trawDescent\tlineTop\tlineBottom\tboxTop\tboxBottom\tcursorTop\trawLeading")
        val errors = mutableListOf<String>()
        for (probe in probes) {
            runOnIdle { requested=probe }; waitForIdle()
            val measured = layout!!
            val reference = ImageBitmap(160,160)
            CanvasDrawScope().draw(Density(1f),LayoutDirection.Ltr,Canvas(reference),Size(160f,160f)) {
                drawText(measured,Color.White,Offset(32f,32f),shadow=Shadow.None)
            }
            val ref = reference.toAwtImage()
            val pixels = ref.getRGB(0,0,160,160,null,0,160)
            ImageIO.write(ref,"png",out.resolve("${probe.font}-${probe.size}-paragraph.png"))
            org.jetbrains.skia.Font(face!!,probe.size).use { font ->
                assertTrue(font.getStringGlyphs(probe.text).all { it.toInt()!=0 },"Probe must be covered by ${probe.font}")
                font.isSubpixel=true; font.isLinearMetrics=true; font.isBaselineSnapped=false
                font.edging=org.jetbrains.skia.FontEdging.ANTI_ALIAS
                font.hinting=org.jetbrains.skia.FontHinting.NORMAL
                org.jetbrains.skia.TextLine.make(probe.text,font).use { text ->
                    org.jetbrains.skia.Paint().use { paint ->
                        paint.color=-1
                        repeat(measured.lineCount) { line ->
                            val top=(32+measured.getLineTop(line)).toInt()
                            val bottom=(32+measured.getLineBottom(line)).toInt()
                            var best=Long.MAX_VALUE; var bestBaseline=-1
                            var bestImage: java.awt.image.BufferedImage? = null
                            for (baseline in 0..90) {
                                val candidate=ImageBitmap(160,160)
                                Canvas(candidate).skiaCanvas.drawTextLine(text,32f,32f+baseline,paint)
                                val im=candidate.toAwtImage()
                                val p=im.getRGB(0,0,160,160,null,0,160)
                                var error=0L
                                for (y in top until bottom) for (x in 0 until 160) {
                                    val i=y*160+x
                                    error+=abs((pixels[i] ushr 24)-(p[i] ushr 24))
                                }
                                if (error<best) { best=error; bestBaseline=baseline; bestImage=im }
                            }
                            val helper=paintedLineBaseline(measured,line)
                            val box=measured.getBoundingBox(measured.getLineStart(line))
                            rows+="${probe.font}\t${probe.size}\t$line\t${measured.getLineBaseline(line)}\t$helper\t$bestBaseline\t$best\t${-font.metrics.ascent}\t${font.metrics.descent}\t${measured.getLineTop(line)}\t${measured.getLineBottom(line)}\t${box.top}\t${box.bottom}\t${measured.getCursorRect(measured.getLineStart(line)).top}\t${font.metrics.leading}"
                            ImageIO.write(bestImage!!,"png",out.resolve("${probe.font}-${probe.size}-line$line-native.png"))
                            if (best!=0L) errors+="No exact glyph match for $probe line $line: alpha error $best"
                            if (bestBaseline.toFloat()!=helper) errors+="Actual $bestBaseline != helper $helper for $probe line $line"
                        }
                    }
                }
            }
        }
        out.resolve("metrics.tsv").writeText(rows.joinToString("\n")+"\n")
        assertTrue(errors.isEmpty(),errors.joinToString("\n"))
    }
}
