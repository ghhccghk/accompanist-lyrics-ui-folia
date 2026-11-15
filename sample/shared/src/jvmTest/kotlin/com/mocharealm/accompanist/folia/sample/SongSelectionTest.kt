package com.mocharealm.accompanist.folia.sample

import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.graphics.toAwtImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.*
import androidx.compose.runtime.*
import com.mocharealm.accompanist.folia.sample.playback.*
import com.mocharealm.accompanist.folia.sample.importing.*
import com.mocharealm.accompanist.lyrics.folia.FoliaPlaybackSnapshot
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalTestApi::class)
class SongSelectionTest {
    @Test fun defaultIsSelectionAndCancelNeverStartsDemoClock()=runDesktopComposeUiTest(width=390,height=740) {
        val session=SampleSession()
        setContent{FoliaSampleApp(session=session,settingsStore=null)}
        onNodeWithText("选择歌曲").performClick()
        onNodeWithText("音频文件（必选）").assertIsDisplayed()
        onNodeWithText("歌词文件（可选）").assertIsDisplayed()
        onNodeWithText("播放所选歌曲").assertIsNotEnabled()
        onNodeWithText("翻译文件（可选）").assertIsNotEnabled()
        onNodeWithText("取消").performClick()
        runOnIdle{assertNull(session.songs.current);assertFalse(session.demo);assertFalse(session.clock.snapshot.isPlaying)}
        val out=File("build/verification/local-audio").apply{mkdirs()}
        ImageIO.write(captureToImage().toAwtImage(),"png",File(out,"waiting-narrow.png"))
        onNodeWithText("开发／演示").performClick()
        onNodeWithText("演示时钟 · 无音频").assertExists()
        onNodeWithText("返回本地歌曲").performClick()
        onNodeWithText("选择歌曲").assertIsDisplayed()
    }
    @Test fun lyricsCanAttachWithoutReplacingPausedAudio()=runDesktopComposeUiTest(width=900,height=600) {
        val session=SampleSession()
        val file=object:LyricsSource{override val name="Original PCM";override suspend fun read()=byteArrayOf()}
        val player=object:LocalAudioPlayer {
            override val metadata=SongMetadata("Original PCM")
            override val durationMillis=12000L
            override var snapshot by mutableStateOf(FoliaPlaybackSnapshot(0,false,1))
            override fun poll(){}
            override fun play(){snapshot=snapshot.copy(isPlaying=true)}
            override fun pause(){snapshot=snapshot.copy(isPlaying=false)}
            override fun seek(positionMillis:Long){snapshot=snapshot.copy(positionMillis=positionMillis,discontinuity=snapshot.discontinuity+1)}
            override fun close(){}
        }
        runBlocking{session.songs.open(SongFiles(file),{player});session.songs.control{it.seek(1500);it.pause()}}
        setContent{FoliaSampleApp(session=session,settingsStore=null)}
        onNodeWithText("未加载歌词").assertIsDisplayed()
        onNodeWithText("补选歌词／翻译").performClick()
        onNodeWithText("应用歌词").assertIsDisplayed()
        onNodeWithText("取消").performClick()
        runOnIdle {
            val before=session.songs.snapshot
            runBlocking {session.songs.attachLyrics(SongFiles(file)){parseSongLyrics("[00:01.00]Amber\n[00:06.00]Echo".encodeToByteArray())}}
            assertEquals(before,session.songs.snapshot);assertSame(player,session.songs.current!!.player)
        }
        onNodeWithText("未加载歌词").assertDoesNotExist()
        onNodeWithContentDescription("Amber").assertIsDisplayed().performClick()
        runOnIdle {assertEquals(1000,session.songs.snapshot.positionMillis);assertFalse(session.songs.snapshot.isPlaying);assertEquals(0,session.clock.snapshot.positionMillis)}
        // Capture the visible active phase, not the transparent entrance boundary reached by clicking.
        runOnIdle {session.songs.control {it.seek(1500)}}
        onNodeWithContentDescription("Amber").assertIsDisplayed()
        val out=File("build/verification/local-audio").apply{mkdirs()}
        ImageIO.write(captureToImage().toAwtImage(),"png",File(out,"selected-lyrics-wide.png"))
        runOnIdle {
            runBlocking {session.songs.attachLyrics(SongFiles(file)) {
                parseSongLyrics("[00:20.00]Beyond the audio\n[00:25.00]Later".encodeToByteArray())
            }}
            session.songs.control {it.seek(12000)}
        }
        onNodeWithText("播放结束").assertIsDisplayed()
        onNodeWithContentDescription("等待演唱").assertDoesNotExist()
        runOnIdle{session.songs.close()}
    }
}
