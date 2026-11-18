package top.ghhccghk.accompanist.folia.sample

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import kotlinx.coroutines.runBlocking
import top.ghhccghk.accompanist.folia.sample.importing.LyricsSource
import top.ghhccghk.accompanist.folia.sample.playback.*
import top.ghhccghk.accompanist.lyrics.folia.FoliaPlaybackSnapshot
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class RenderDebugUiTest {
    private class Host:SampleRenderDebugHost {
        var recording=false
        override val backend="TEST_ONLY_BACKEND"
        override val requestedBackend="TEST_ONLY_REQUEST"
        override val metric="TEST_ONLY_CALLBACK_METRIC"
        override fun setEnabled(enabled:Boolean){this.recording=enabled}
        override fun read()=RenderFrameStats(60,30.0,4.0,8.0,16.0,33.3,2)
    }
    @Test fun demoToggleVisibilityAndUnmountReleaseInstrumentation()=runDesktopComposeUiTest(width=900,height=740) {
        val host=Host();val session=SampleSession(true).apply{clock.seek(8150)}
        var visible by mutableStateOf(true)
        var mounted by mutableStateOf(true)
        setContent{CompositionLocalProvider(LocalSampleRenderDebugHost provides host){
            if(mounted)FoliaSampleApp(visible,session,settingsStore=null)
        }}
        runOnIdle{assertFalse(host.recording)}
        onNodeWithText("设置").performClick()
        onNodeWithText("调试信息（后端／FPS）").performScrollTo().performClick()
        onNodeWithText("完成").performClick()
        onNodeWithText("后端 TEST_ONLY_BACKEND（请求 TEST_ONLY_REQUEST）").assertIsDisplayed()
        onNodeWithText("窗口回调 FPS 30.0 · 60 样本 / 最近2秒").assertIsDisplayed()
        runOnIdle{assertTrue(host.recording);assertEquals(8150,session.clock.snapshot.positionMillis);visible=false}
        runOnIdle{assertFalse(host.recording);visible=true}
        runOnIdle{assertTrue(host.recording)}
        val out=File("../../build/verification/render-debug").apply{mkdirs()}
        ImageIO.write(captureToImage().toAwtImage(),"png",out.resolve("demo-debug-ui-test-host.png"))
        runOnIdle{mounted=false}
        runOnIdle{assertFalse(host.recording)}
    }
    @Test fun selectedAudioDebugToggleDoesNotStartDemoClock()=runDesktopComposeUiTest(width=420,height=800) {
        val session=SampleSession();val host=Host()
        val source=object:LyricsSource{override val name="Owned test input";override suspend fun read()=byteArrayOf()}
        val player=object:LocalAudioPlayer {
            override val metadata=SongMetadata("Debug UI test input")
            override val durationMillis=12000L
            override var snapshot by mutableStateOf(FoliaPlaybackSnapshot(1800,false,3))
            var starts=0
            override fun poll(){}
            override fun play(){starts++;snapshot=snapshot.copy(isPlaying=true)}
            override fun pause(){snapshot=snapshot.copy(isPlaying=false)}
            override fun seek(positionMillis:Long)=error("Debug must not seek")
            override fun close(){}
        }
        runBlocking{assertTrue(session.songs.open(SongFiles(source),{player}));session.songs.control{it.pause()}}
        val starts=player.starts
        var mounted by mutableStateOf(true)
        setContent{CompositionLocalProvider(LocalSampleRenderDebugHost provides host){
            if(mounted)FoliaSampleApp(session=session,settingsStore=null)
        }}
        runOnIdle{assertFalse(host.recording)}
        onNodeWithText("设置").performClick()
        onNodeWithText("调试信息（后端／FPS）").performScrollTo().performClick()
        onNodeWithText("完成").performClick()
        onNodeWithText("TEST_ONLY_CALLBACK_METRIC").assertIsDisplayed()
        runOnIdle{assertTrue(host.recording);assertEquals(1800,session.songs.snapshot.positionMillis);assertEquals(0,session.clock.snapshot.positionMillis)}
        onNodeWithText("关闭调试信息").performClick()
        onNodeWithText("TEST_ONLY_CALLBACK_METRIC").assertDoesNotExist()
        runOnIdle{assertFalse(host.recording);assertEquals(starts,player.starts);mounted=false;session.songs.close()}
    }
}
