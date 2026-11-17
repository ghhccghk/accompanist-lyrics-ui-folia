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
class DioramaActiveLayersTest {
    @Test fun narrow() = capture(380)
    @Test fun wide() = capture(800)
    @Test fun fractional() = capture(801)
    private fun capture(width: Int) = runDesktopComposeUiTest(width = width, height = 600) {
        val rows = File("../verification/diorama-fixtures/active-layers.tsv").readLines()
            .filterNot { it.startsWith('#') || it.isBlank() }.map { it.split('\t') }.filter { it[2].toInt() == width }
        val expected = File("../verification/diorama-fixtures/active-layers-materials.tsv").readLines()
            .filterNot { it.startsWith('#') }.map { it.split('\t') }.groupBy { it[0] }
        var materialPeak = 0.0
        fun decode(value: String) = String(Base64.getDecoder().decode(value), Charsets.UTF_8)
        var current by mutableIntStateOf(0)
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
            SideEffect { prepared = stage }
            Canvas(Modifier.fillMaxSize().background(Color(0xff162435))) {
                fun color(i: Int) = r[i].split(',').map(String::toDouble).let { DioramaLinearColor(it[0], it[1], it[2]) }
                fun vec(i: Int) = DioramaVec(r[i].toDouble(), r[i + 1].toDouble(), r[i + 2].toDouble())
                stage?.draw(this, r[3].toDouble(), DioramaPerspective(vec(9), vec(12), width.toDouble(), 600.0), path[3], placement, color(7), color(8),
                    DioramaUnitTuning(soulActive = r[15].toBoolean()))
            }
        }
        val out = File("../build/verification/diorama-active-layers-kotlin").apply { mkdirs() }
        fun pixels(): IntArray { waitForIdle(); val image = captureToImage(); return IntArray(image.width * image.height).also { image.readPixels(it) } }
        val captures = mutableMapOf<Int, IntArray>()
        for (i in rows.indices) {
            runOnIdle { current = i }; waitForIdle()
            val p = assertNotNull(prepared); val r = rows[i]
            assertEquals(r[6].toDouble() * 128 / .62, p.lineAdvance, .001, "${r[0]} complete-shaped width")
            assertEquals(ceilExpectedUnits(r[1]), p.slices.size)
            assertTrue(p.slices.all { it.unit.timeline.isNotEmpty() })
            val primary = r[7].split(',').map(String::toDouble).let { DioramaLinearColor(it[0], it[1], it[2]) }
            val accent = r[8].split(',').map(String::toDouble).let { DioramaLinearColor(it[0], it[1], it[2]) }
            for ((index, slice) in p.slices.withIndex()) {
                val state = dioramaUnitFrame(r[3].toDouble(), slice.unit.start, slice.unit.end,
                    dioramaMediaEnvelope(r[3].toDouble(), slice.unit.start, slice.unit.end),
                    primary, accent, DioramaUnitTuning(soulActive = r[15].toBoolean()))
                val values = listOf(state.fill.red, state.fill.green, state.fill.blue, state.baseOpacity,
                    state.glowOpacity, if (state.glowVisible) 1.0 else 0.0, state.soulOpacity,
                    if (state.soulVisible) 1.0 else 0.0, state.soulLift, state.soulScale)
                val source = expected.getValue(r[0])[index].drop(2).map(String::toDouble)
                for (field in values.indices) {
                    materialPeak = maxOf(materialPeak, kotlin.math.abs(source[field] - values[field]))
                    assertEquals(source[field], values[field], 1e-7, "${r[0]} unit$index field$field")
                }
                if (width == 800 && r[3].toDouble() == 0.0 && r[15].toBoolean()) {
                    ImageIO.write(slice.image.toAwtImage(), "png", out.resolve("${r[1]}-unit$index-base.png"))
                    ImageIO.write(slice.halo.toAwtImage(), "png", out.resolve("${r[1]}-unit$index-halo.png"))
                }
                assertEquals(slice.image.width, slice.halo.width)
                assertEquals(slice.image.height, slice.halo.height)
            }
            captures[i] = pixels()
            ImageIO.write(captureToImage().toAwtImage(), "png", out.resolve(r[0]))
        }
        out.resolve("materials-$width.txt").writeText("materialPeak=$materialPeak\n")
        assertEquals(3, builds, "media probes cannot reprepare line rasters")
        for (i in listOf(38, 0, 43, 19, 34, 2)) {
            runOnIdle { current = i }; assertTrue(captures[i]!!.contentEquals(pixels()), "reverse seek ${rows[i][0]}")
        }
        val freeze = pixels(); val before = builds; mainClock.advanceTimeBy(1000)
        assertTrue(freeze.contentEquals(pixels())); assertEquals(before, builds)
        runOnIdle { mounted = false }; pixels(); assertNull(prepared)
        runOnIdle { mounted = true }; assertTrue(freeze.contentEquals(pixels()), "remount")
    }
    private fun ceilExpectedUnits(scene: String) = when (scene) { "arabic" -> 2; "cjk" -> 7; else -> 3 }
}
