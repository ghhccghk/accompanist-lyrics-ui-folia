package top.ghhccghk.accompanist.folia.sample

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import top.ghhccghk.accompanist.folia.sample.importing.*
import top.ghhccghk.accompanist.folia.sample.playback.*
import top.ghhccghk.accompanist.lyrics.folia.FoliaPlaybackSnapshot
import java.io.File
import javax.imageio.ImageIO
import kotlinx.coroutines.runBlocking
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class EmbeddedLyricsUiTest {
    private class Player : LocalAudioPlayer {
        override val metadata = SongMetadata("Original tagged audio")
        override val durationMillis = 12000L
        override var snapshot by mutableStateOf(FoliaPlaybackSnapshot(0, false, 1))
        override fun poll() {}
        override fun play() { snapshot = snapshot.copy(isPlaying = true) }
        override fun pause() { snapshot = snapshot.copy(isPlaying = false) }
        override fun seek(positionMillis: Long) { snapshot = snapshot.copy(positionMillis = positionMillis, discontinuity = snapshot.discontinuity + 1) }
        override fun close() {}
    }
    private fun source(text: String? = null, notice: String? = null) = object : LyricsSource {
        override val name = "Original tags.wav"
        override suspend fun read(): ByteArray = error("must not read all audio as lyrics")
        override suspend fun embeddedLyrics() = EmbeddedLyrics(listOfNotNull(text), notice)
    }
    private fun save(image: androidx.compose.ui.graphics.ImageBitmap, name: String) {
        val directory = File("build/verification/embedded-lyrics").apply { mkdirs() }
        ImageIO.write(image.toAwtImage(), "png", File(directory, name))
    }

    @Test fun timedTagsUsePublicViewAndOffsetSeekAndAllowSupplementingTranslation() = runDesktopComposeUiTest(width = 420, height = 800) {
        val session = SampleSession(); val player = Player()
        session.options = session.options.copy(offsetMillis = 250)
        val audio = source("[00:01.00]Amber 光\n[00:03.00]Echo 回声")
        runBlocking {
            assertTrue(session.songs.openContent(SongFiles(audio), { player }, ::loadSongContent))
            session.songs.control { it.pause(); it.seek(1800) }
        }
        setContent { FoliaSampleApp(session = session, settingsStore = null) }
        onNodeWithText("内嵌计时歌词").assertIsDisplayed()
        onNodeWithText("未加载歌词").assertDoesNotExist()
        onNodeWithContentDescription("Amber 光").assertIsDisplayed().performClick()
        runOnIdle {
            assertEquals(1250, session.songs.snapshot.positionMillis)
            assertFalse(session.songs.snapshot.isPlaying)
            assertEquals(0, session.clock.snapshot.positionMillis)
            session.songs.control { it.seek(1800) }
        }
        save(captureToImage(), "embedded-timed-narrow.png")
        onNodeWithText("补选歌词／翻译").performClick()
        onNodeWithText("翻译文件（可选）").assertIsEnabled()
        onNodeWithText("取消").performClick()
        runOnIdle { assertSame(player, session.songs.current!!.player); session.songs.close() }
    }

    @Test fun plainTagsRemainReadableWithoutFakeTimelineAndCanBeReplaced() = runDesktopComposeUiTest(width = 900, height = 640) {
        val session = SampleSession(); val player = Player()
        val plain = "一行无时间歌词\nAnother original line"
        val audio = source(plain)
        runBlocking {
            session.songs.openContent(SongFiles(audio), { player }, ::loadSongContent)
            session.songs.control { it.pause(); it.seek(2300) }
        }
        setContent { FoliaSampleApp(session = session, settingsStore = null) }
        onNodeWithText("内嵌纯文本歌词").assertIsDisplayed()
        onNodeWithText(plain).assertIsDisplayed().assertHasNoClickAction()
        onNodeWithContentDescription("等待演唱").assertDoesNotExist()
        save(captureToImage(), "embedded-plain-wide.png")
        runOnIdle {
            val before = session.songs.snapshot
            val external = object : LyricsSource {
                override val name = "replacement.lrc"
                override suspend fun read() = "[00:01.00]Replacement".encodeToByteArray()
            }
            runBlocking { assertTrue(session.songs.attachContent(SongFiles(audio, external), ::loadSongContent)) }
            assertEquals(before, session.songs.snapshot)
            assertSame(player, session.songs.current!!.player)
        }
        onNodeWithText("外部歌词").assertIsDisplayed()
        onNodeWithText(plain).assertDoesNotExist()
        onNodeWithContentDescription("Replacement").assertIsDisplayed()
        runOnIdle { session.songs.close() }
    }

    @Test fun tagWarningDoesNotTurnPlayableAudioIntoSessionFailure() = runDesktopComposeUiTest(width = 420, height = 800) {
        val session = SampleSession(); val player = Player()
        runBlocking {
            assertTrue(session.songs.openContent(SongFiles(source(notice = "内嵌歌词读取失败：测试损坏标签")), { player }, ::loadSongContent))
            assertTrue(session.songs.snapshot.isPlaying)
            // Freeze deterministic UI capture; continuous real playback is covered by the device Clip probe.
            session.songs.control { it.pause() }
        }
        setContent { FoliaSampleApp(session = session, settingsStore = null) }
        onNodeWithText("内嵌歌词读取失败：测试损坏标签").assertIsDisplayed()
        onNodeWithText("未加载歌词").assertIsDisplayed()
        onNodeWithText("操作失败 · 原会话保留").assertDoesNotExist()
        runOnIdle {
            session.songs.control { it.play() }; assertTrue(session.songs.snapshot.isPlaying)
            session.songs.control { it.pause() }; assertNull(session.songs.error)
            session.songs.close()
        }
    }
}
