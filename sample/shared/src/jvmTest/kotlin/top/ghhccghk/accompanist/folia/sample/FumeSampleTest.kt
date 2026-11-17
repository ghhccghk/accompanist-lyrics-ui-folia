package top.ghhccghk.accompanist.folia.sample

import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import top.ghhccghk.accompanist.lyrics.folia.FoliaStyle
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class FumeSampleTest {
    @Test fun localSettingsSelectFumeWithoutStartingDemo()=runDesktopComposeUiTest(width=420,height=800){
        val session=SampleSession();val before=session.songs.snapshot
        setContent{FoliaSampleApp(session=session,settingsStore=null)}
        onNodeWithText("设置").performClick();onNodeWithText("浮名").performScrollTo().performClick()
        onNodeWithText("浮名：文章镜头校准预览。短桥接、总览与文字光栅仍有差异。").assertExists()
        runOnIdle{assertIs<FoliaStyle.Fume>(session.lyricStyle);assertFalse(session.demo);assertEquals(before,session.songs.snapshot);assertNull(session.songs.current)}
    }
    @Test fun demoPreservesMediaClockAcrossFumeChanges()=runDesktopComposeUiTest(width=800,height=700){
        val session=SampleSession(true).apply{lyricTheme=SampleLyricTheme.Fume;clock.seek(8150)}
        setContent{FoliaSampleApp(session=session,settingsStore=null)}
        onNodeWithContentDescription("演示进度").assertExists()
        runOnIdle{session.lyricTheme=SampleLyricTheme.Classic};waitForIdle()
        runOnIdle{session.lyricTheme=SampleLyricTheme.Fume};waitForIdle()
        runOnIdle{assertEquals(8150,session.clock.snapshot.positionMillis);assertFalse(session.clock.snapshot.isPlaying)}
    }
}
