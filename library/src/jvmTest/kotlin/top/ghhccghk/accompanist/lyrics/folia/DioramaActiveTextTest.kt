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
import androidx.compose.ui.unit.*
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama.*
import java.io.File
import java.util.Base64
import javax.imageio.ImageIO
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class DioramaActiveTextTest {
    @Test fun narrow() = capture(380)
    @Test fun wide() = capture(800)
    @Test fun fractional() = capture(801)
    private fun capture(width: Int) = runDesktopComposeUiTest(width = width, height = 600) {
        val rows = File("../verification/diorama-fixtures/active-base.tsv").readLines()
            .filterNot { it.startsWith('#') || it.isBlank() }.map { it.split('\t') }.filter { it[2].toInt() == width }
        fun decode(value: String) = String(Base64.getDecoder().decode(value), Charsets.UTF_8)
        var current by mutableIntStateOf(0)
        var sampling by mutableStateOf(false)
        val renderer=DioramaActiveBaseRenderer()
        var mounted by mutableStateOf(true)
        var builds = 0
        var prepared: DioramaActiveText? = null
        val path = buildDioramaPath(12, dioramaHashSeed("DioramaProbe"))
        val placement = dioramaTextPlacement(3, dioramaHashSeed("DioramaProbe"))
        setContent {
            val r = rows[current]; val resolver = LocalFontFamilyResolver.current
            val stage = if (mounted) remember(r[1]) {
                val font = when (r[1]) { "arabic" -> "FoliaParityBidi-Bold.ttf"; "cjk" -> "FoliaParityChatCJK-Bold.otf"; else -> "FoliaParityLatin-Bold.ttf" }
                val words = r[5].split(';').map { w -> w.split(',').let { DioramaTimedText(decode(it[0]), it[1].toDouble(), it[2].toDouble()) } }
                val line = DioramaLyric(decode(r[4]), .7, words.last().end, words)
                val style = TextStyle(fontFamily = FontFamily(Font(File("../verification/assets/$font"), FontWeight.Bold)))
                builds++
                DioramaActiveText.prepare(line, style, TextMeasurer(resolver, Density(1f), LayoutDirection.Ltr), resolver)
            } else null
            SideEffect { prepared = stage;renderer.prepare(stage) }
            Canvas(Modifier.fillMaxSize().background(Color(0xff162435))) {
                fun color(i: Int) = r[i].split(',').map(String::toDouble).let { DioramaLinearColor(it[0], it[1], it[2]) }
                fun vec(i: Int) = DioramaVec(r[i].toDouble(), r[i + 1].toDouble(), r[i + 2].toDouble())
                stage?.drawBase(this, r[3].toDouble(), DioramaPerspective(vec(9), vec(12), width.toDouble(), 600.0), path[3], placement, color(7), color(8),
                    baseRenderer=if(sampling)renderer else null)
            }
        }
        val out = File("../build/verification/diorama-active-base-kotlin").apply { mkdirs() }
        fun pixels(): IntArray { waitForIdle(); val image = captureToImage(); return IntArray(image.width * image.height).also { image.readPixels(it) } }
        val captures = mutableMapOf<Int, IntArray>()
        val sampled=mutableMapOf<Int,IntArray>()
        val samplerOut=File("../build/verification/diorama-active-base-sampler-kotlin").apply{mkdirs()}
        for (i in rows.indices) {
            runOnIdle { current = i;sampling=false }; waitForIdle()
            val p = assertNotNull(prepared); val r = rows[i]
            assertEquals(r[6].toDouble() * 128 / .62, p.lineAdvance, .001, "${r[0]} complete-shaped width")
            assertEquals(ceilExpectedUnits(r[1]), p.slices.size)
            assertTrue(p.slices.all { it.unit.timeline.isNotEmpty() })
            captures[i] = pixels()
            ImageIO.write(captureToImage().toAwtImage(), "png", out.resolve(r[0]))
            runOnIdle{sampling=true};sampled[i]=pixels()
            ImageIO.write(captureToImage().toAwtImage(),"png",samplerOut.resolve(r[0]))
        }
        assertEquals(3, builds, "media probes cannot reprepare line rasters")
        for (i in listOf(18, 0, 23, 9, 14, 2)) {
            runOnIdle { current = i;sampling=false }; assertTrue(captures[i]!!.contentEquals(pixels()), "reverse seek ${rows[i][0]}")
            runOnIdle{sampling=true};assertContentEquals(sampled[i],pixels(),"sampled reverse seek ${rows[i][0]}")
        }
        val freeze = pixels(); val before = builds; mainClock.advanceTimeBy(1000)
        assertTrue(freeze.contentEquals(pixels())); assertEquals(before, builds)
        runOnIdle { mounted = false }; pixels(); assertNull(prepared)
        assertEquals(0,renderer.residentCount);assertEquals(0L,renderer.residentBytes)
        runOnIdle { mounted = true }; assertTrue(freeze.contentEquals(pixels()), "remount")
        renderer.close()
    }
    private fun ceilExpectedUnits(scene: String) = when (scene) { "arabic" -> 2; "cjk" -> 7; else -> 3 }
}
