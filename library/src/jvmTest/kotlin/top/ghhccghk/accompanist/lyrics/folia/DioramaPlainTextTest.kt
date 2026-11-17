package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama.*
import java.io.File
import java.util.Base64
import javax.imageio.ImageIO
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class DioramaPlainTextTest {
    @Test fun narrow() = capture(380)
    @Test fun wide() = capture(800)
    @Test fun fractional() = capture(801)

    private fun capture(width: Int) = runDesktopComposeUiTest(width = width, height = 600) {
        val rows = File("../verification/diorama-fixtures/text-planes.tsv").readLines()
            .filterNot { it.startsWith('#') || it.isBlank() }.map { it.split('\t') }.filter { it[2].toInt() == width }
        var current by mutableIntStateOf(0)
        var mounted by mutableStateOf(true)
        var prepared: DioramaPlainText? = null
        var builds = 0
        val path = buildDioramaPath(12, dioramaHashSeed("DioramaProbe"))
        val placement = dioramaTextPlacement(3, dioramaHashSeed("DioramaProbe"))
        setContent {
            val row = rows[current]
            val resolver = LocalFontFamilyResolver.current
            val raster = if (mounted) remember(row[1]) {
                val font = when (row[1]) {
                    "arabic" -> "FoliaParityBidi-Bold.ttf"
                    "cjk" -> "FoliaParityChatCJK-Bold.otf"
                    else -> "FoliaParityLatin-Bold.ttf"
                }
                val style = TextStyle(fontFamily = FontFamily(Font(File("../verification/assets/$font"), FontWeight.Bold)))
                val measurer = TextMeasurer(resolver, Density(1f), LayoutDirection.Ltr)
                builds++
                DioramaPlainText.prepare(String(Base64.getDecoder().decode(row[5]), Charsets.UTF_8), style, measurer, resolver)
            } else null
            SideEffect { prepared = raster }
            Canvas(Modifier.fillMaxSize().background(Color(0xff162435))) {
                fun vec(i: Int) = DioramaVec(row[i].toDouble(), row[i + 1].toDouble(), row[i + 2].toDouble())
                raster?.draw(this, DioramaPerspective(vec(10), vec(13), width.toDouble(), 600.0), path[3], placement)
            }
        }
        val out = File("../build/verification/diorama-text-plane-kotlin").apply { mkdirs() }
        fun pixels(): IntArray {
            waitForIdle(); val image = captureToImage()
            return IntArray(image.width * image.height).also { image.readPixels(it) }
        }
        val captured = mutableMapOf<Int, IntArray>()
        for (i in rows.indices) {
            runOnIdle { current = i }; waitForIdle()
            val p = assertNotNull(prepared)
            val row = rows[i]
            assertEquals(row[6].toInt(), p.advancePx, "${row[0]} advance")
            assertEquals(row[7].toInt(), p.fontPx, "${row[0]} font")
            assertEquals(row[8].toInt(), p.image.width, "${row[0]} canvas width")
            assertEquals(row[9].toInt(), p.image.height, "${row[0]} canvas height")
            assertTrue(p.hasTypographicMetrics)
            assertEquals(row[16].toDouble(), p.middleBaseline, 1e-6, "${row[0]} original Canvas middle")
            captured[i] = pixels()
            assertTrue(captured[i]!!.any { it != Color(0xff162435).toArgb() })
            ImageIO.write(captureToImage().toAwtImage(), "png", out.resolve(row[0]))
            if (i % 2 == 1) assertEquals((i + 1) / 2, builds, "camera moves must not rebuild text")
        }
        runOnIdle { current = 0 }; val restored = pixels()
        assertTrue(captured[0]!!.contentEquals(restored), "camera/content restore")
        val beforePause = builds
        mainClock.advanceTimeBy(1000)
        assertTrue(restored.contentEquals(pixels()), "no autonomous animation")
        assertEquals(beforePause, builds)
        runOnIdle { mounted = false }; pixels(); assertNull(prepared)
        runOnIdle { mounted = true }
        assertTrue(restored.contentEquals(pixels()), "remount restores complete shaped plane")
    }
}
