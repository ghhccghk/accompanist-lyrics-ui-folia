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
class DioramaCameraStageTest {
    @Test fun narrow() = capture(380)
    @Test fun wide() = capture(800)
    private fun capture(width: Int) = runDesktopComposeUiTest(width = width, height = 600) {
        val rows = File("../verification/diorama-fixtures/camera-targets.tsv").readLines()
            .filterNot { it.startsWith('#') || it.isBlank() }.map { it.split('\t') }.filter { it[2].toInt() == width }
        fun decode(value: String) = String(Base64.getDecoder().decode(value), Charsets.UTF_8)
        var current by mutableIntStateOf(0)
        var mounted by mutableStateOf(true)
        var builds = 0
        var prepared: DioramaActiveText? = null
        var drawnTarget: DioramaCameraTarget? = null
        var targetPeak = 0.0
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
                DioramaActiveText.prepare(line, style, TextMeasurer(resolver, Density(1f), LayoutDirection.Ltr), resolver) to DioramaCameraLine(line)
            } else null
            SideEffect { prepared = stage?.first }
            Canvas(Modifier.fillMaxSize().background(Color(0xff162435))) {
                fun color(i: Int) = r[i].split(',').map(String::toDouble).let { DioramaLinearColor(it[0], it[1], it[2]) }
                fun vec(i: Int) = DioramaVec(r[i].toDouble(), r[i + 1].toDouble(), r[i + 2].toDouble())
                if (stage != null) {
                    val rawWidth = stage.first.lineAdvance*.62/128
                    val fit = dioramaFrameFit(rawWidth,5.2,55.0,width.toDouble()/600)*placement.scale
                    val target = stage.second.target(r[3].toDouble(),path[3],placement,dioramaHashSeed("DioramaProbe"),3,
                        DioramaShot.valueOf(r[15]),rawWidth*fit,width.toDouble()/600)
                    drawnTarget = target
                    stage.first.draw(this,r[3].toDouble(),DioramaPerspective(target.eye,target.look,width.toDouble(),600.0),
                        path[3],placement,color(7),color(8))
                }
            }
        }
        val out = File("../build/verification/diorama-camera-targets-kotlin").apply { mkdirs() }
        fun pixels(): IntArray { waitForIdle(); val image = captureToImage(); return IntArray(image.width * image.height).also { image.readPixels(it) } }
        val captures = mutableMapOf<Int, IntArray>()
        for (i in rows.indices) {
            runOnIdle { current = i }; waitForIdle()
            val p = assertNotNull(prepared); val r = rows[i]
            assertEquals(r[6].toDouble() * 128 / .62, p.lineAdvance, .001, "${r[0]} complete-shaped width")
            assertEquals(ceilExpectedUnits(r[1]), p.slices.size)
            assertTrue(p.slices.all { it.unit.timeline.isNotEmpty() })
            captures[i] = pixels()
            val target = assertNotNull(drawnTarget)
            val actual = listOf(target.eye.x,target.eye.y,target.eye.z,target.look.x,target.look.y,target.look.z)
            for (field in actual.indices) {
                val expected = r[9+field].toDouble()
                targetPeak = maxOf(targetPeak,kotlin.math.abs(expected-actual[field]))
                assertEquals(expected,actual[field],1e-5,"${r[0]} own shaped trackWidth target$field")
            }
            ImageIO.write(captureToImage().toAwtImage(), "png", out.resolve(r[0]))
        }
        out.resolve("targets-$width.txt").writeText("worldTargetPeak=$targetPeak\n")
        assertEquals(3, builds, "media probes cannot reprepare line rasters")
        for (i in listOf(rows.lastIndex, 0, 23, 9, 14, 2)) {
            runOnIdle { current = i }; assertTrue(captures[i]!!.contentEquals(pixels()), "reverse seek ${rows[i][0]}")
        }
        val freeze = pixels(); val before = builds; mainClock.advanceTimeBy(1000)
        assertTrue(freeze.contentEquals(pixels())); assertEquals(before, builds)
        runOnIdle { mounted = false }; pixels(); assertNull(prepared)
        runOnIdle { mounted = true }; assertTrue(freeze.contentEquals(pixels()), "remount")
    }
    private fun ceilExpectedUnits(scene: String) = when (scene) { "arabic" -> 2; "cjk" -> 7; else -> 3 }
}
