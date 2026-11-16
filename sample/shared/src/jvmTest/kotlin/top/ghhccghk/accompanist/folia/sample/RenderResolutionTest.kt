package top.ghhccghk.accompanist.folia.sample

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.*
import top.ghhccghk.accompanist.folia.sample.fixtures.scenes
import top.ghhccghk.accompanist.lyrics.folia.*
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.roundToInt
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class RenderResolutionTest {
    @Test fun physicalPixelsShrinkWhileLogicalViewportAndInputStayAligned() = runDesktopComposeUiTest(width=400,height=300) {
        var resolution by mutableStateOf(RenderResolution.Full)
        var size = IntSize.Zero
        var cssWidth = 0f
        var spDp = 0f
        var tap: Offset? = null
        val host = object : Density {
            override val density = 2f
            override val fontScale = 1.5f
            override fun Dp.toSp() = (value*2).sp
            override fun TextUnit.toDp() = (value/2).dp
        }
        setContent {
            CompositionLocalProvider(LocalDensity provides host,LocalLayoutDirection provides LayoutDirection.Rtl) {
                SampleRenderStage(resolution,Modifier.fillMaxSize()) {
                    val density = LocalDensity.current
                    spDp = with(density) { 20.sp.toDp().value }
                    Canvas(Modifier.fillMaxSize().onSizeChanged {
                        size=it; cssWidth=it.width/density.density
                    }.pointerInput(Unit) { detectTapGestures { tap=it } }) { drawRect(Color.White) }
                }
            }
        }
        for (mode in RenderResolution.entries) {
            runOnIdle { resolution=mode; tap=null }
            waitForIdle()
            runOnIdle {
                assertEquals(IntSize((400*mode.scale).roundToInt(),(300*mode.scale).roundToInt()),size)
                assertEquals(200f,cssWidth)
                assertEquals(10f,spDp) // Preserve the host font converter, not just fontScale.
            }
            onRoot().performTouchInput { click(Offset(320f,210f)) }
            runOnIdle {
                assertNotNull(tap)
                assertEquals(320f*mode.scale,tap!!.x,.1f)
                assertEquals(210f*mode.scale,tap!!.y,.1f)
            }
        }
    }

    @Test fun realLyricsKeepFullResolutionPixelsAndDeterministicReducedFrames() = runDesktopComposeUiTest(width=800,height=540) {
        var resolution by mutableStateOf(RenderResolution.Full)
        var wrapped by mutableStateOf(false)
        var stageWidth by mutableStateOf(380)
        var style by mutableStateOf<FoliaStyle>(FoliaStyle.Classic())
        var snapshot by mutableStateOf(FoliaPlaybackSnapshot(8_150,false,0))
        val lyrics = scenes.first { it.id=="mixed" }.lyrics
        val font = FontFamily(Font(File("../../verification/assets/FoliaParityLatin-Bold.ttf"),FontWeight.Bold))
        val out = File("../../build/verification/render-resolution").apply { mkdirs() }
        setContent {
            val content: @Composable () -> Unit = {
                FoliaLyricsView(lyrics,{snapshot},Modifier.fillMaxSize(),style=style,
                    typography=FoliaTypography(main=TextStyle(fontFamily=font,fontSize=48.sp)))
            }
            Box(Modifier.fillMaxSize()) {
                val modifier=Modifier.size(stageWidth.dp,540.dp)
                if(wrapped) SampleRenderStage(resolution,modifier,content) else Box(modifier) { content() }
            }
        }
        fun pixels(): IntArray {
            val image=captureToImage()
            return IntArray(image.width*image.height).also { image.readPixels(it) }
        }
        val direct=pixels()
        runOnIdle { wrapped=true }
        waitForIdle()
        assertContentEquals(direct,pixels(),"100% must preserve the original stage pixels")
        for (theme in listOf(FoliaStyle.Classic(),FoliaStyle.Partita())) for (mode in RenderResolution.entries) {
            runOnIdle { style=theme; resolution=mode; stageWidth=380 }
            val visited=mutableMapOf<Long,IntArray>()
            for (time in listOf(8_000L,8_150L,8_600L,10_400L,13_000L)) {
                runOnIdle { snapshot=FoliaPlaybackSnapshot(time,false,snapshot.discontinuity+1) }
                waitForIdle()
                val first=pixels()
                assertContentEquals(first,pixels(),"Paused $theme $mode $time")
                visited[time]=first
                ImageIO.write(captureToImage().toAwtImage(),"png",out.resolve("${theme::class.simpleName}-${mode.name}-$time.png"))
            }
            for (time in visited.keys.reversed()) {
                runOnIdle { snapshot=FoliaPlaybackSnapshot(time,false,snapshot.discontinuity+1) }
                waitForIdle()
                assertContentEquals(visited.getValue(time),pixels(),"Reverse $theme $mode $time")
            }
            val beforeResize=pixels()
            runOnIdle { stageWidth=800 }; waitForIdle()
            runOnIdle { stageWidth=380 }; waitForIdle()
            assertContentEquals(beforeResize,pixels(),"Resize restoration $theme $mode")
        }
    }

    @Test fun settingsExposeResolutionWithoutChangingPausedClock() = runDesktopComposeUiTest(width=800,height=740) {
        val session=SampleSession(true).apply { clock.seek(8_150) }
        setContent { FoliaSampleApp(session=session,settingsStore=null) }
        onNodeWithText("设置").performClick()
        onNodeWithText("50%").performScrollTo().performClick()
        runOnIdle {
            assertEquals(RenderResolution.Half,session.renderResolution)
            assertEquals(8_150,session.clock.snapshot.positionMillis)
            assertFalse(session.clock.snapshot.isPlaying)
        }
    }

    @Test fun localSongSettingsExposeTheSameResolutionAndPreserveEmptySession() = runDesktopComposeUiTest(width=420,height=800) {
        val session=SampleSession()
        val before=session.songs.snapshot
        setContent { FoliaSampleApp(session=session,settingsStore=null) }
        onNodeWithText("设置").performClick()
        onNodeWithText("75%").performScrollTo().performClick()
        runOnIdle {
            assertEquals(RenderResolution.ThreeQuarters,session.renderResolution)
            assertEquals(before,session.songs.snapshot)
            assertNull(session.songs.current)
        }
    }
}
