package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.*
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.fume.*
import java.io.File
import java.util.Base64
import javax.imageio.ImageIO
import kotlin.math.*
import kotlin.test.*

/** Static full-article diagnostic only, not a claim about Fume's final camera or text effects. */
@OptIn(ExperimentalTestApi::class)
class FumeArticleFontTest {
    @Test fun short380() = capture(0, 380)
    @Test fun short800() = capture(0, 800)
    @Test fun short1440() = capture(0, 1440)
    @Test fun dense380() = capture(2, 380)
    @Test fun dense800() = capture(2, 800)
    @Test fun dense1440() = capture(2, 1440)
    private fun capture(scene: Int, width: Int) = runDesktopComposeUiTest(width = width, height = 600) {
        val lines = File("../verification/fume-fixtures/article-inputs.tsv").readLines()
            .filterNot { it.startsWith("#") || it.isBlank() }.map { it.split('\t') }.filter { it[0] == scene.toString() }
            .map { FumeLyric(String(Base64.getDecoder().decode(it[2]), Charsets.UTF_8), it[3].toDouble(), it[4].toDouble(), chorus = it[5].toBoolean()) }
        val expected = File("../verification/fume-fixtures/article-browser.tsv").readLines()
            .filterNot { it.startsWith("#") || it.isBlank() }.map { it.split('\t') }
            .filter { it[0] == scene.toString() && it[1] == width.toString() }
        val font = FontFamily(Font(File("../verification/assets/FoliaParityLatin-Bold.ttf"), FontWeight.Bold))
        var article: FumeArticle? = null
        setContent {
            val measurer = rememberTextMeasurer(cacheSize = 128)
            val measure = remember(measurer, font) { FumeTextMeasure(measurer,
                TextStyle(fontFamily = font, fontWeight = FontWeight.Bold, color = Color(0xffdfe5ec)), Density(1f)) }
            val a = remember {
                buildFumeArticle(lines, FumeViewport(width.toDouble(), 600.0), "FumeFixture", countLines = measure::countLines)!!
            }
            article = a
            val drawings = remember(a) { a.blocks.map { b -> b to measure.layout(b.line.text, b.hero, b.fontPx, b.innerWidth, b.lineHeight) } }
            Canvas(Modifier.fillMaxSize().background(Color(0xff162435))) {
                val scale = (min(width / a.width, 600 / a.height) * .92).toFloat()
                withTransform({ translate(width / 2f, 300f); scale(scale, scale, Offset.Zero); translate((-a.width / 2).toFloat(), (-a.height / 2).toFloat()) }) {
                    for ((b, layout) in drawings) {
                        drawRect(if (b.hero) Color(0xff54cae1) else Color(0xff52677f), Offset(b.x.toFloat(), b.y.toFloat()),
                            Size(b.width.toFloat(), b.height.toFloat()), style = Stroke(1 / scale))
                        drawText(layout, topLeft = Offset(b.x.toFloat(), b.y.toFloat()))
                    }
                }
            }
        }
        waitForIdle()
        val a = checkNotNull(article)
        val out = File("../build/verification/fume-article-kotlin").apply { mkdirs() }
        ImageIO.write(captureToImage().toAwtImage(), "png", out.resolve("$scene-$width.png"))
        val report = mutableListOf("# scene,width,index,sourceColumns,nativeColumns,sourceFont,nativeFont,sourceY,nativeY,sourceH,nativeH")
        a.blocks.forEachIndexed { i, b ->
            val e = expected[i]
            report += listOf(scene, width, b.sourceIndex, e[3], a.columns, e[14], b.fontPx, e[10], b.y, e[12], b.height).joinToString(",")
        }
        out.resolve("$scene-$width.csv").writeText(report.joinToString("\n") + "\n")
        assertEquals(expected.map { it[7].toInt() }, a.blocks.map { it.sourceIndex }, "Article shuffle/column selection")
        assertEquals(expected.map { it[8].toBoolean() }, a.blocks.map { it.hero })
        assertEquals(expected.first()[3].toInt(), a.columns)
        assertEquals(expected.first()[5].toDouble(), a.width, 1e-8)
        // Exact geometry is the initial goal, not a broad screenshot tolerance.
        expected.zip(a.blocks).forEach { (e, b) ->
            listOf(b.x, b.y, b.width, b.height, b.innerWidth, b.fontPx, b.lineHeight, b.lineCount.toDouble())
                .forEachIndexed { i, value -> assertEquals(e[9 + i].toDouble(), value, 1e-8, "scene=$scene width=$width block=${b.sourceIndex} field=$i") }
        }
    }
}
