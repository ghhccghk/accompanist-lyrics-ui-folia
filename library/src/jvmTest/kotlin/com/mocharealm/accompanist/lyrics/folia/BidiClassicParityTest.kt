package com.mocharealm.accompanist.lyrics.folia

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.sp
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.*
import com.mocharealm.accompanist.lyrics.folia.internal.model.normalize
import com.mocharealm.accompanist.lyrics.folia.internal.text.*
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class BidiClassicParityTest {
    private fun source(scene: String): SyncedLyrics {
        val rows = File("../verification/classic-fixtures/scenes.tsv").readLines()
            .filterNot { it.startsWith('#') || it.isBlank() }.map { it.split('\t') }.filter { it[0] == scene }
        return SyncedLyrics(listOf(KaraokeLine.MainKaraokeLine(rows.map {
            KaraokeSyllable(it[5], it[3].toInt(), it[4].toInt())
        }, null, KaraokeAlignment.Unspecified, 700, 1900)))
    }
    private fun family() = FontFamily(Font(File("../verification/assets/FoliaParityBidi-Bold.ttf"), FontWeight.Bold))

    @Test fun originalIntactBodyCharacterPositions() = runDesktopComposeUiTest(width = 800, height = 600) {
        val layouts = mutableMapOf<Pair<String, Int>, TextLayoutResult>()
        setContent {
            val measurer = rememberTextMeasurer()
            for (scene in listOf("bidi-rtl", "bidi-ltr")) {
                val line = normalize(source(scene)).lines.single()
                val style = TextStyle(fontFamily = family(), fontWeight = FontWeight.Bold, fontSize = 48.sp)
                displayUnits(line).forEachIndexed { i, unit ->
                    layouts[scene to i] = measurer.measure(cssWordText(unit.text).text,
                        style.copy(textAlign = TextAlign.Left, textDirection = classicTextDirection(style, line.text)), softWrap = false)
                }
            }
        }
        val rows = File("../verification/classic-fixtures/bidi-body-layout.tsv").readLines()
            .filterNot { it.startsWith('#') || it.isBlank() }.map { it.split('\t') }
        for ((key, expected) in rows.groupBy { it[0] to it[1].toInt() }) {
            val layout = layouts.getValue(key)
            expected.filter { it[2] != "SPACE" }.forEachIndexed { index, c ->
                val box = layout.getBoundingBox(index)
                assertEquals(c[3].toDouble(), box.left.toDouble(), .51, "Original body left $key/$index ${c[2]}")
                assertEquals(c[4].toDouble(), box.right.toDouble(), .51, "Original body right $key/$index ${c[2]}")
            }
        }
        assertEquals(TextDirection.Rtl, classicTextDirection(TextStyle(textDirection = TextDirection.Rtl), "Glow"))
        assertEquals(TextDirection.Ltr, classicTextDirection(TextStyle(textDirection = TextDirection.Ltr), "مرحبا"))
    }

    @Test fun publicFrames() = runDesktopComposeUiTest(width = 800, height = 600) {
        var scene by mutableStateOf("bidi-rtl")
        var avoid by mutableStateOf(false)
        var snapshot by mutableStateOf(FoliaPlaybackSnapshot(0, false))
        val sources = listOf("bidi-rtl", "bidi-ltr").associateWith(::source)
        val font = family()
        setContent {
            FoliaLyricsView(sources.getValue(scene), { snapshot }, Modifier.fillMaxSize().background(Color(0xFF162435)),
                typography = FoliaTypography(secondary = TextStyle(fontFamily = font, fontWeight = FontWeight.Bold)),
                colors = FoliaColors(secondary = Color.Transparent),
                options = FoliaOptions(showTranslation = false, showPhonetic = false, showNextLine = false),
                style = FoliaStyle.Classic(ClassicTuning(breathingFloatMultiplier = 0f, avoidOverlap = avoid)))
        }
        val label = System.getenv("FOLIA_BIDI_LABEL") ?: "kotlin"
        require(label.matches(Regex("[a-z0-9-]+")))
        val output = File("../build/verification/classic-bidi-$label").apply { mkdirs() }
        for (name in sources.keys) for (mode in listOf(false, true)) {
            runOnIdle { scene = name; avoid = mode }
            val observed = mutableMapOf<Int, IntArray>()
            val times = listOf(750, 1000, 1200, 1500, 1900, 2100, 2300, 2500)
            for (ms in times + times.reversed()) {
                runOnIdle { snapshot = FoliaPlaybackSnapshot(ms.toLong(), false, snapshot.discontinuity + 1) }; waitForIdle()
                val capture = captureToImage()
                val pixels = IntArray(800 * 600).also { capture.readPixels(it) }
                observed[ms]?.let { assertContentEquals(it, pixels, "Bidi reverse seek $name/$mode/$ms") }
                observed[ms] = pixels
                mainClock.advanceTimeBy(1000); waitForIdle()
                assertContentEquals(pixels, IntArray(800 * 600).also { captureToImage().readPixels(it) }, "Bidi paused $ms")
                ImageIO.write(capture.toAwtImage(), "png", output.resolve("$name-${if (mode) "avoid" else "default"}-${ms}ms.png"))
            }
        }
    }
}
