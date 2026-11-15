package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.*
import top.ghhccghk.accompanist.lyrics.folia.internal.model.normalize
import top.ghhccghk.accompanist.lyrics.folia.internal.text.displayUnits
import top.ghhccghk.accompanist.lyrics.folia.internal.animation.*
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.classic.classicGeometry
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import kotlin.math.ceil
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class InterludeParityTest {
    private fun source(scene: String): SyncedLyrics {
        val rows = File("../verification/classic-fixtures/scenes.tsv").readLines()
            .filterNot { it.startsWith('#') || it.isBlank() }.map { it.split('\t') }.filter { it[0] == scene }
        return SyncedLyrics(listOf(KaraokeLine.MainKaraokeLine(rows.map {
            KaraokeSyllable(it[5], it[3].toInt(), it[4].toInt())
        }, null, KaraokeAlignment.Unspecified, 700, 1900)))
    }

    @Test fun independentDotDisplayGroupsArePreserved() {
        val line = normalize(source("interlude")).lines.single()
        val units = displayUnits(line)
        assertEquals(6, units.size, "Pinned classic has six independently animated display words")
        assertEquals(listOf(700L, 900L, 1100L, 1300L, 1500L, 1700L), units.map { it.startUs / 1000 })
        assertEquals(1, displayUnits(normalize(source("interlude-single")).lines.single()).size)
    }

    @Test fun originalGeometryAndMotion() = runDesktopComposeUiTest(width = 800, height = 600) {
        val font = FontFamily(Font(File("../verification/assets/FoliaParityLatin-Bold.ttf"), FontWeight.Bold))
        val geometries = mutableMapOf<Triple<String, Int, Boolean>, top.ghhccghk.accompanist.lyrics.folia.internal.styles.classic.ClassicGeometry>()
        setContent {
            val measurer = rememberTextMeasurer()
            for (scene in listOf("interlude", "interlude-single")) for (width in listOf(800, 380)) for (avoid in listOf(false, true)) {
                val line = normalize(source(scene)).lines.single()
                val units = displayUnits(line)
                val size = (width * .06f).coerceIn(36f, 72f)
                val widths = units.map {
                    val layout = measurer.measure(it.text, TextStyle(fontFamily = font, fontWeight = FontWeight.Bold, fontSize = size.sp), softWrap = false)
                    layout.getPathForRange(0, it.text.length).getBounds().width.toDouble()
                }
                val poses = units.indices.map { wordPose(line.startUs, it, true, true) }
                geometries[Triple(scene, width, avoid)] = classicGeometry(widths, widths.map { ceil(it * 64) / 64 },
                    size.toDouble(), width - 64.0, poses, line.startUs, .7f, avoid, false, 1.0, interlude = true)
            }
        }
        for (row in fixture("interlude-layout")) {
            val g = geometries.getValue(Triple(row[0], row[1].toInt(), row[2] == "avoid"))
            val i = row[3].toInt(); val box = g.boxes[i]
            val actual = listOf(box.left.toDouble(), box.top.toDouble(), box.width.toDouble(), box.height.toDouble(),
                g.margins[i], g.height.toDouble(), g.rowGap.toDouble(), g.paddingTop.toDouble(), g.paddingBottom.toDouble())
            actual.forEachIndexed { j, value -> assertEquals(row[j + 4].toDouble(), value, if (j == 4) .0001 else .51, "Original interlude geometry $row column$j") }
        }
        for (row in fixture("interlude-motion")) {
            val line = normalize(source(row[0])).lines.single()
            val word = displayUnits(line)[row[4].toInt()]
            val pose = wordPose(line.startUs, row[4].toInt(), true, true)
            val frame = sampleWord(row[3].toLong() * 1000, line.startUs, word.startUs, word.endUs,
                renderProfile(line.startUs, line.endUs), pose, false)
            listOf(frame.x, frame.y, frame.scale, frame.rotation, frame.alpha).forEachIndexed { j, value ->
                assertEquals(row[j + 5].toDouble(), value.toDouble(), .0001, "Original interlude motion $row column$j")
            }
        }
    }

    private fun fixture(name: String) = File("../verification/classic-fixtures/$name.tsv").readLines()
        .filterNot { it.startsWith('#') || it.isBlank() }.map { it.split('\t') }

    @Test fun widePublicFrames() = capture(800)
    @Test fun narrowPublicFrames() = capture(380)

    private fun capture(width: Int) = runDesktopComposeUiTest(width = width, height = 600) {
        val font = FontFamily(Font(File("../verification/assets/FoliaParityLatin-Bold.ttf"), FontWeight.Bold))
        var scene by mutableStateOf("interlude")
        var avoid by mutableStateOf(false)
        var snapshot by mutableStateOf(FoliaPlaybackSnapshot(0, false))
        val sources = listOf("interlude", "interlude-single").associateWith(::source)
        setContent {
            FoliaLyricsView(sources.getValue(scene), { snapshot }, Modifier.fillMaxSize().background(Color(0xFF162435)),
                typography = FoliaTypography(secondary = TextStyle(fontFamily = font, fontWeight = FontWeight.Bold)),
                colors = FoliaColors(secondary = Color.Transparent),
                options = FoliaOptions(showTranslation = false, showPhonetic = false, showNextLine = false),
                style = FoliaStyle.Classic(ClassicTuning(breathingFloatMultiplier = 0f, avoidOverlap = avoid)))
        }
        val label = System.getenv("FOLIA_INTERLUDE_LABEL") ?: "kotlin"
        require(label.matches(Regex("[a-z0-9-]+")))
        val output = File("../build/verification/classic-interlude-$label").apply { mkdirs() }
        for (name in sources.keys) for (mode in listOf(false, true)) {
            runOnIdle { scene = name; avoid = mode }
            val observed = mutableMapOf<Int, IntArray>()
            val times = listOf(750, 1000, 1500, 1900, 2300, 2500)
            for (ms in times + times.reversed()) {
                runOnIdle { snapshot = FoliaPlaybackSnapshot(ms.toLong(), false, snapshot.discontinuity + 1) }
                waitForIdle()
                val capture = captureToImage()
                val pixels = IntArray(width * 600).also { capture.readPixels(it) }
                observed[ms]?.let { assertContentEquals(it, pixels, "Interlude $name/$width/$mode reverse seek $ms") }
                observed[ms] = pixels
                mainClock.advanceTimeBy(1000); waitForIdle()
                assertContentEquals(pixels, IntArray(width * 600).also { captureToImage().readPixels(it) }, "Interlude paused $ms")
                ImageIO.write(capture.toAwtImage(), "png", output.resolve("$name-$width-${if (mode) "avoid" else "default"}-${ms}ms.png"))
            }
        }
    }
}
