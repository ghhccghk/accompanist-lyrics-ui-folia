package top.ghhccghk.accompanist.folia.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.*
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.*
import top.ghhccghk.accompanist.lyrics.folia.*
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.*

/** Invoked only by jvmNativeMaskRuntimeTest, with a real explicitly built DLL. */
@OptIn(ExperimentalTestApi::class)
class NativeMaskRuntimeTest {
    @Test fun demoSwitchLoadsAndReportsRealCalls() = runDesktopComposeUiTest(width = 460, height = 900) {
        val session = SampleSession(true)
        session.clock.seek(8000)
        setContent { FoliaSampleApp(session = session, settingsStore = null) }
        onNodeWithText("设置").performClick()
        onNodeWithContentDescription("实验 Rust 辉光遮罩").performScrollTo().performClick()
        waitUntil(timeoutMillis = 5000) { session.maskBackend.processor?.stats()?.calls?.let { it > 0 } == true }
        runOnIdle { assertNull(session.maskBackend.processor!!.stats().error); assertEquals(8000, session.clock.snapshot.positionMillis) }
        onNodeWithText("实际：Rust CPU 遮罩", substring = true).performScrollTo()
        val directory = File("build/verification/native-mask-sample").apply { mkdirs() }
        ImageIO.write(captureToImage().toAwtImage(), "png", File(directory, "settings-rust.png"))
        onNodeWithContentDescription("实验 Rust 辉光遮罩").performScrollTo().performClick()
        onNodeWithText("实际：Kotlin").assertExists()
        runOnIdle { assertFalse(session.maskBackend.requested); assertEquals(8000, session.clock.snapshot.positionMillis) }
    }

    @Test fun classicSwitchPixelsPauseSeekResize() = compare(FoliaStyle.Classic(), "classic")
    @Test fun partitaSwitchPixelsPauseSeekResize() = compare(FoliaStyle.Partita(), "partita")

    private fun compare(style: FoliaStyle, name: String) = runDesktopComposeUiTest(width = 800, height = 600) {
        val input = File("verification/classic-fixtures/scenes.tsv").readLines().map { it.split('\t') }.filter { it[0] == "wrap" }
        val line = KaraokeLine.MainKaraokeLine(input.map { KaraokeSyllable(it[5], it[3].toInt(), it[4].toInt()) },
            null, KaraokeAlignment.Unspecified, 700, 2800)
        val lyrics = SyncedLyrics(listOf(line))
        val session = SampleSession()
        val font = FontFamily(Font(File("verification/assets/FoliaParityLatin-Bold.ttf"), FontWeight.Bold))
        var snapshot by mutableStateOf(FoliaPlaybackSnapshot(1500, false))
        var width by mutableIntStateOf(800)
        var attached by mutableStateOf(true)
        setContent {
            if (attached) SampleMaskScope(session) {
                Box(Modifier.size(width.dp, 600.dp).background(Color(0xFF162435))) {
                    FoliaLyricsView(lyrics, { snapshot }, Modifier.fillMaxSize(), style = style,
                        typography = FoliaTypography(main = TextStyle(fontFamily = font, fontSize = 44.sp, fontWeight = FontWeight.Bold)),
                        options = FoliaOptions(showTranslation = false, showPhonetic = false, showNextLine = false))
                }
            }
        }
        val directory = File("build/verification/native-mask-sample").apply { mkdirs() }
        fun image() = captureToImage()
        fun pixels(): IntArray { waitForIdle(); val image = image(); return IntArray(image.width * image.height).also { image.readPixels(it) } }
        var frames = 0
        for (w in listOf(800, 380)) for (time in listOf(700L, 950L, 1500L, 2700L, 2900L, 3100L)) {
            runOnIdle { session.maskBackend.selectRust(false); width = w; snapshot = FoliaPlaybackSnapshot(time, false, frames.toLong()) }
            val kotlin = pixels()
            if (time == 1500L) ImageIO.write(image().toAwtImage(), "png", File(directory, "$name-$w-kotlin.png"))
            runOnIdle {
                session.maskBackend.selectRust(true)
                assertNull(session.maskBackend.loadError, "Real native load and startup self-test")
                assertNotNull(session.maskBackend.processor)
            }
            val native = pixels()
            assertContentEquals(kotlin, native, "$name $w time=$time Kotlin/Rust exact ARGB")
            val stats = session.maskBackend.processor!!.stats()
            assertNull(stats.error)
            if (time == 1500L) {
                assertTrue(stats.calls > 0, "Actual render preparation must invoke Rust")
                ImageIO.write(image().toAwtImage(), "png", File(directory, "$name-$w-rust.png"))
            }
            mainClock.advanceTimeBy(1000)
            assertContentEquals(native, pixels(), "Pause freezes media animation")
            runOnIdle { snapshot = FoliaPlaybackSnapshot(850, false, 100 + frames.toLong()) }; pixels()
            runOnIdle { snapshot = FoliaPlaybackSnapshot(time, false, 200 + frames.toLong()) }
            assertContentEquals(native, pixels(), "Backward seek reconstructs frame")
            runOnIdle { session.maskBackend.selectRust(false) }
            assertContentEquals(kotlin, pixels(), "Switch back invalidates native preparation")
            frames++
        }
        // Same injected boundary that JNI status/memory errors use: a visible,
        // explicit null fallback must preserve the default Kotlin image.
        runOnIdle { width = 800; snapshot = FoliaPlaybackSnapshot(1500, false, 999) }
        val original = pixels()
        runOnIdle { session.maskBackend.selectRust(true) }; pixels()
        val retained = session.maskBackend.processor!!
        retained.blur(intArrayOf(0), 0, 1, floatArrayOf(1f))
        assertNotNull(retained.stats().error)
        runOnIdle { width = 799 }; pixels()
        runOnIdle { width = 800 }
        assertContentEquals(original, pixels(), "Failure falls back to Kotlin")
        runOnIdle { attached = false }; waitForIdle()
        retained.release()
        File(directory, "$name.txt").writeText("Windows x64 actual JNI, fixed FoliaParityLatin-Bold 44sp, 800x600/380x600.\n" +
            "$frames Kotlin/Rust exact ARGB pairs; pause, reverse seek, switch-back, resize, invalid-input fallback and disposal passed.\n" +
            "Not Android runtime or a frame-performance measurement.\n")
    }
}
