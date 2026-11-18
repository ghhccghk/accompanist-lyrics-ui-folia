package top.ghhccghk.accompanist.folia.sample

import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import kotlinx.coroutines.runBlocking
import top.ghhccghk.accompanist.folia.sample.importing.loadSongLyrics
import top.ghhccghk.accompanist.folia.sample.playback.*
import java.io.ByteArrayInputStream
import java.io.File
import java.security.MessageDigest
import javax.imageio.ImageIO
import javax.sound.sampled.*
import kotlin.math.abs
import kotlin.test.*

/** Explicit real-device task. UI is offscreen; it does NOT certify native file
 * dialogs or replace the separate real-window / original visual matrices. */
@OptIn(ExperimentalTestApi::class)
object LocalPlaybackThemesProbe {
    @JvmStatic fun main(args:Array<String>) {
        val out=File("build/verification/local-playback-themes").apply{mkdirs()}
        out.resolve("SUCCESS").delete()
        val log=out.resolve("events.tsv").apply{writeText("event\ttheme\tposition_ms\tplaying\tdiscontinuity\n")}
        val wav=out.resolve("OwnedSilentClip.wav")
        val format=AudioFormat(48000f,16,1,true,false)
        AudioInputStream(ByteArrayInputStream(ByteArray(12*48000*2)),format,12L*48000).use {
            AudioSystem.write(it,AudioFileFormat.Type.WAVE,wav)
        }
        val lyricFile=out.resolve("OwnedLyrics.lrc").apply{writeText(
            "[00:00.70]Amber echoes beyond\n[00:03.60]Quiet lanterns gather\n[00:06.50]Silver waters return\n[00:10.00]End of the synthetic verse\n")}
        val translatedFile=out.resolve("OwnedTranslation.lrc").apply{writeText(
            "[00:00.70]Synthetic translation one\n[00:03.60]Synthetic translation two\n[00:06.50]Synthetic translation three\n[00:10.00]Synthetic translation four\n")}
        val session=SampleSession()
        try {
            runDesktopComposeUiTest(width=1100,height=740) {
                // Only controls the UI scheduler. Native position always comes
                // from Clip.microsecondPosition; no preview/wall clock feeds it.
                mainClock.autoAdvance=false
                fun settle(){
                    // Actual UI clicks leave a hovered button; compare identical
                    // input state as well as media (never mask control pixels).
                    onAllNodes(isRoot()).onLast().performMouseInput{moveTo(Offset(1080f,20f))}
                    mainClock.advanceTimeBy(640);waitForIdle()
                }
                fun click(text:String){onNodeWithText(text).performClick();settle()}
                fun record(event:String){
                    val s=session.songs.snapshot
                    log.appendText(listOf(event,session.lyricTheme,s.positionMillis,s.isPlaying,s.discontinuity).joinToString("\t")+"\n")
                    assertFalse(session.demo);assertFalse(session.clock.snapshot.isPlaying)
                    assertEquals(0,session.clock.snapshot.positionMillis,"Preview clock must never drive real playback")
                    assertNull(session.songs.error)
                }
                fun pixels():IntArray{waitForIdle();val im=captureToImage();return IntArray(im.width*im.height).also{im.readPixels(it)}}
                fun seek(ms:Long){
                    onNodeWithContentDescription("播放进度").performSemanticsAction(SemanticsActions.SetProgress){it(ms.toFloat())}
                    settle();assertTrue(abs(session.songs.snapshot.positionMillis-ms)<=2,"real native paused seek")
                }
                fun awaitLyrics(){
                    val query=onAllNodes(hasContentDescription("Amber echoes beyond",substring=true),useUnmergedTree=true)
                    val deadline=System.nanoTime()+15_000_000_000L
                    while(query.fetchSemanticsNodes().isEmpty()) {
                        if(System.nanoTime()>=deadline) {
                            val name=session.lyricTheme.toString()
                            ImageIO.write(captureToImage().toAwtImage(),"png",out.resolve("$name-not-ready.png"))
                            out.resolve("$name-not-ready.txt").writeText(onRoot(useUnmergedTree=true).printToString())
                            error("Actual renderer did not expose source lyrics: $name")
                        }
                        Thread.sleep(25);mainClock.advanceTimeByFrame();waitForIdle()
                    }
                    onNodeWithText("正在准备浮名文章…").assertDoesNotExist()
                }
                setContent{FoliaSampleApp(session=session,settingsStore=null)}
                settle();onNodeWithText("选择歌曲").assertIsDisplayed()
                click("选择歌曲");onNodeWithText("播放所选歌曲").assertIsNotEnabled();click("取消")
                assertNull(session.songs.current);record("cancel-empty")
                val audio=DesktopSongFile(wav)
                runOnIdle {
                    assertTrue(runBlocking{session.songs.open(SongFiles(audio),::prepareDesktopAudio)})
                    session.songs.control{it.pause()}
                }
                settle()
                val player=session.songs.current!!.player
                assertIs<DesktopAudioPlayer>(player);assertEquals(12000,player.durationMillis)
                onNodeWithText("未加载歌词").assertIsDisplayed()
                onNodeWithContentDescription("等待演唱").assertDoesNotExist();record("native-open-no-lyrics")
                seek(1000);val before=session.songs.snapshot
                click("播放");val start=System.nanoTime();Thread.sleep(300)
                mainClock.advanceTimeByFrame();waitForIdle()
                val moving=session.songs.snapshot;val elapsed=(System.nanoTime()-start)/1_000_000
                assertTrue(moving.isPlaying&&moving.positionMillis-before.positionMillis>=100,"real device cursor must advance")
                assertTrue(moving.positionMillis-before.positionMillis<=elapsed+1000,"UI test time must not advance audio")
                record("native-playing-no-lyrics")
                click("暂停");val paused=session.songs.snapshot;val frozen=pixels()
                Thread.sleep(220);settle();assertEquals(paused,session.songs.snapshot)
                assertTrue(frozen.contentEquals(pixels()),"paused full sample changes without media")
                record("native-pause-no-lyrics")
                seek(4500);assertTrue(session.songs.snapshot.discontinuity>paused.discontinuity);record("slider-seek")
                runOnIdle {
                    assertTrue(runBlocking{session.songs.attachLyrics(SongFiles(audio,DesktopSongFile(lyricFile),DesktopSongFile(translatedFile)),::loadSongLyrics)})
                }
                settle();assertSame(player,session.songs.current!!.player)
                out.resolve("parsed-times.tsv").writeText(session.songs.current!!.lyrics!!.lines.joinToString("\n"){"${it.start}\t${it.end}"}+"\n")
                onNodeWithText("未加载歌词").assertDoesNotExist();record("attach-actual-parser-translation")
                val labels=listOf("流光","云阶","倾诉","心象","时计","群唱","浮名")
                for((i,theme)in SampleLyricTheme.entries.withIndex()) {
                    seek(1600)
                    click("设置")
                    val label=labels[i]+if(session.lyricTheme==theme)" ✓" else ""
                    // Scroll semantics await the UI animation clock. Allow it
                    // to run ONLY while real audio is paused; otherwise a
                    // below-fold theme can block forever on a frozen test clock.
                    assertFalse(session.songs.snapshot.isPlaying)
                    mainClock.autoAdvance=true
                    try {onNodeWithText(label).performScrollTo().performClick()}
                    finally {mainClock.autoAdvance=false}
                    settle()
                    click("完成");assertEquals(theme,session.lyricTheme)
                    awaitLyrics();settle();val image=captureToImage()
                    ImageIO.write(image.toAwtImage(),"png",out.resolve("$theme-paused.png"))
                    val baseline=pixels();val state=session.songs.snapshot
                    Thread.sleep(180);settle();assertEquals(state,session.songs.snapshot)
                    assertTrue(baseline.contentEquals(pixels()),"pause media-driven $theme")
                    record("theme-pause")
                    click("播放");Thread.sleep(220);mainClock.advanceTimeByFrame();waitForIdle()
                    assertTrue(session.songs.snapshot.isPlaying&&session.songs.snapshot.positionMillis>state.positionMillis+60)
                    assertSame(player,session.songs.current!!.player);record("theme-native-advance")
                    click("暂停");seek(7000);seek(1600);awaitLyrics();settle()
                    ImageIO.write(captureToImage().toAwtImage(),"png",out.resolve("$theme-restored.png"))
                    record("theme-seek-restored")
                    assertTrue(baseline.contentEquals(pixels()),"same native seek reconstructs $theme")
                    record("theme-reverse-seek")
                }
                click("重新选歌");click("取消");assertSame(player,session.songs.current!!.player)
                record("cancel-keeps-native-session")
                runOnIdle{session.songs.close()};settle();onNodeWithText("选择歌曲").assertIsDisplayed()
                assertFailsWith<IllegalStateException>{player.play()}
                record("closed-native-clip")
            }
            out.resolve("conditions.txt").writeText("Production real DesktopAudioPlayer/Java Sound Clip, 12s zero-valued owned PCM48000Hz/16bit mono. Full FoliaSampleApp, settingsStore=null, real parser/core, theme controls and transport actions.\nUI=offscreen Compose1100x740; native file picker NOT automated. Device position is real; UI scheduler steps are NOT media time. No preview clock. No Android/original visual/performance/physical-output claim.\nOS=${System.getProperty("os.name")};Java=${System.getProperty("java.version")};waveSHA256="+
                MessageDigest.getInstance("SHA-256").digest(wav.readBytes()).joinToString(""){"%02x".format(it)}+"\n")
            out.resolve("SUCCESS").writeText("Actual native audio, full sample UI and seven themes passed.\n")
        } finally {session.songs.close();session.clock.pause()}
        println("LOCAL_PLAYBACK_THEMES_VERIFIED")
    }
}
