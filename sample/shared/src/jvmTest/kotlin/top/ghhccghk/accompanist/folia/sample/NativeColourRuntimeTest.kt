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

@OptIn(ExperimentalTestApi::class)
class NativeColourRuntimeTest {
    @Test fun classicColourPixels() = compare(FoliaStyle.Classic(),"classic")
    @Test fun partitaColourPixels() = compare(FoliaStyle.Partita(),"partita")
    @Test fun settingsTogglePreservesClockAndOtherBackend() = runDesktopComposeUiTest(width=460,height=900) {
        val session=SampleSession(true)
        session.clock.seek(8000)
        setContent { FoliaSampleApp(session=session,settingsStore=null) }
        onNodeWithText("设置").performClick()
        onNodeWithContentDescription("实验 Rust 颜色卷积").performScrollTo().performClick()
        runOnIdle {
            assertTrue(session.colourBackend.requested)
            assertNull(session.colourBackend.loadError)
            assertNotNull(session.colourBackend.processor)
            assertFalse(session.maskBackend.requested)
            assertEquals(8000,session.clock.snapshot.positionMillis)
        }
        onNodeWithContentDescription("实验 Rust 颜色卷积").performScrollTo().performClick()
        onNodeWithText("颜色卷积：Kotlin").performScrollTo().assertExists()
        runOnIdle { assertNull(session.colourBackend.processor); assertEquals(8000,session.clock.snapshot.positionMillis) }
    }

    @Suppress("UNCHECKED_CAST")
    private fun compare(style:FoliaStyle,name:String) = runDesktopComposeUiTest(width=800,height=600) {
        // Test-only reflection forces the same production software tree on both
        // sides; no public debug switch or altered rendering expectations.
        val software=Class.forName("top.ghhccghk.accompanist.lyrics.folia.internal.rendering.ClassicLayerKt")
            .getMethod("getLocalClassicSoftwareLayers").invoke(null) as ProvidableCompositionLocal<Boolean>
        val input=File("verification/classic-fixtures/scenes.tsv").readLines().map { it.split('\t') }.filter { it[0]=="wrap" }
        val line=KaraokeLine.MainKaraokeLine(input.map { KaraokeSyllable(it[5],it[3].toInt(),it[4].toInt()) },
            null,KaraokeAlignment.Unspecified,700,2800)
        val lyrics=SyncedLyrics(listOf(line))
        val font=FontFamily(Font(File("verification/assets/FoliaParityLatin-Bold.ttf"),FontWeight.Bold))
        val session=SampleSession()
        var snapshot by mutableStateOf(FoliaPlaybackSnapshot(1500,false))
        var width by mutableIntStateOf(800)
        var attached by mutableStateOf(true)
        setContent {
            if(attached) CompositionLocalProvider(software provides true) {
                SampleMaskScope(session) {
                    Box(Modifier.size(width.dp,600.dp).background(Color(0xFF162435))) {
                        FoliaLyricsView(lyrics,{snapshot},Modifier.fillMaxSize(),style=style,
                            typography=FoliaTypography(main=TextStyle(fontFamily=font,fontSize=44.sp,fontWeight=FontWeight.Bold)),
                            options=FoliaOptions(showTranslation=false,showPhonetic=false,showNextLine=false))
                    }
                }
            }
        }
        val directory=File("build/verification/native-colour-runtime").apply { mkdirs() }
        fun pixels():IntArray { waitForIdle();val image=captureToImage();return IntArray(image.width*image.height).also { image.readPixels(it) } }
        var calls=0L
        var frames=0
        val invoked=mutableListOf<String>()
        for(w in listOf(800,380)) for(time in listOf(700L,950L,1500L,2700L,2900L,3100L)) {
            runOnIdle { session.colourBackend.selectRust(false);width=w;snapshot=FoliaPlaybackSnapshot(time,false,frames.toLong()) }
            val kotlin=pixels()
            val kotlinImage=captureToImage()
            runOnIdle {
                session.colourBackend.selectRust(true)
                assertNull(session.colourBackend.loadError)
                assertFalse(session.maskBackend.requested)
            }
            val rust=pixels()
            assertContentEquals(kotlin,rust,"$name width=$w time=$time exact ARGB")
            val stats=session.colourBackend.processor!!.stats()
            assertNull(stats.error);calls+=stats.calls
            if(stats.calls>0) {
                invoked += "width=$w time=$time JNI=${stats.calls}"
                ImageIO.write(kotlinImage.toAwtImage(),"png",File(directory,"$name-$w-$time-kotlin.png"))
                ImageIO.write(captureToImage().toAwtImage(),"png",File(directory,"$name-$w-$time-rust.png"))
            }
            mainClock.advanceTimeBy(1000)
            assertContentEquals(rust,pixels(),"pause")
            runOnIdle { snapshot=FoliaPlaybackSnapshot(850,false,100L+frames) };pixels()
            runOnIdle { snapshot=FoliaPlaybackSnapshot(time,false,200L+frames) }
            assertContentEquals(rust,pixels(),"reverse seek")
            runOnIdle { session.colourBackend.selectRust(false) }
            assertContentEquals(kotlin,pixels(),"switch back")
            frames++
        }
        assertTrue(calls>0,"Actual drawing must call colour JNI, not only load it")
        runOnIdle { width=800;snapshot=FoliaPlaybackSnapshot(1500,false,999) }
        val baseline=pixels()
        runOnIdle { session.colourBackend.selectRust(true) };pixels()
        val retained=session.colourBackend.processor!!
        assertNull(retained.convolve(intArrayOf(0),0,1,doubleArrayOf(0.0,1.0,0.0)))
        assertNotNull(retained.stats().error)
        runOnIdle { width=799 };pixels()
        runOnIdle { width=800 }
        assertContentEquals(baseline,pixels(),"visible-error Kotlin fallback")
        runOnIdle { attached=false };waitForIdle();retained.release()
        File(directory,"$name.txt").writeText("$frames exact whole-frame pairs; actual JNI calls=$calls; fixed Latin font44sp, 800/380x600.\n"+
            "Forced software tree both sides; pause/reverse seek/resize/switch/failure fallback/disposal passed.\n"+
            "Not Android UI or full playback performance evidence.\n"+invoked.joinToString("\n")+"\n")
    }
}
