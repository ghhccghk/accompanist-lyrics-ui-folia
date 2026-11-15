package top.ghhccghk.accompanist.lyrics.folia

import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.*
import top.ghhccghk.accompanist.lyrics.folia.internal.model.normalize
import top.ghhccghk.accompanist.lyrics.folia.internal.timeline.VisualTimeline
import top.ghhccghk.accompanist.lyrics.folia.internal.animation.*
import java.io.File
import kotlin.test.*
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
import javax.imageio.ImageIO

@OptIn(ExperimentalTestApi::class)
class InterruptedLineParityTest {
    private fun lyrics(duration: Int) = SyncedLyrics(List(6) { i ->
        val start = 700 + i * 60
        KaraokeLine.MainKaraokeLine(
            listOf(KaraokeSyllable("D${i.toString().padStart(2, '0')}", start, start + duration)),
            null, KaraokeAlignment.Unspecified, start, start + duration)
    })

    @Test fun fastPublicFrames() = capture(150)
    @Test fun normalPublicFrames() = capture(300)

    private fun capture(duration: Int) = runDesktopComposeUiTest(width = 800, height = 600) {
        val content = lyrics(duration)
        val font = FontFamily(Font(File("../verification/assets/FoliaParityLatin-Bold.ttf"), FontWeight.Bold))
        var snapshot by mutableStateOf(FoliaPlaybackSnapshot(0, false))
        setContent {
            FoliaLyricsView(content, { snapshot }, Modifier.fillMaxSize().background(Color(0xFF162435)),
                typography = FoliaTypography(secondary = TextStyle(fontFamily = font, fontWeight = FontWeight.Bold)),
                colors = FoliaColors(secondary = Color.Transparent),
                options = FoliaOptions(showTranslation = false, showPhonetic = false, showNextLine = false),
                style = FoliaStyle.Classic(ClassicTuning(breathingFloatMultiplier = 0f)))
        }
        val output = File("../build/verification/classic-interrupted-kotlin").apply { mkdirs() }
        val observed = mutableMapOf<Int, IntArray>()
        val times = listOf(731, 761, 791, 821, 851, 881, 941, 1001, 1041, 1101, 1241, 1401, 1701, 2001)
        for (ms in times + times.reversed()) {
            runOnIdle { snapshot = FoliaPlaybackSnapshot(ms.toLong(), false, snapshot.discontinuity + 1) }
            waitForIdle()
            val capture = captureToImage()
            val pixels = IntArray(800 * 600).also { capture.readPixels(it) }
            observed[ms]?.let { assertContentEquals(it, pixels, "Interrupted $duration reverse seek $ms") }
            observed[ms] = pixels
            mainClock.advanceTimeBy(1000)
            waitForIdle()
            val repeated = IntArray(800 * 600).also { captureToImage().readPixels(it) }
            assertContentEquals(pixels, repeated, "Interrupted $duration paused $ms")
            ImageIO.write(capture.toAwtImage(), "png", output.resolve("$duration-${ms}ms.png"))
        }
    }

    @Test fun interruptedEntryContinuesFromOriginalValueOnExit() {
        val rows=File("../verification/classic-fixtures/interrupted-lines.tsv").readLines()
            .filterNot{it.startsWith('#')||it.isBlank()}.map{it.split('\t')}
        for((duration,sceneRows) in rows.groupBy{it[0].toInt()}) {
            val lines=normalize(lyrics(duration)).lines
            val timeline=VisualTimeline(lines)
            for((ms,expected) in sceneRows.groupBy{it[1].toLong()}.let{it.entries.toList()+it.entries.reversed()}) {
                val time=ms*1000;val selection=timeline.at(time)
                val actual=(selection.exiting+listOfNotNull(selection.primary)).map {epoch->
                    val line=lines[epoch.line]
                    line.startUs/1000 to sampleLine(time,epoch.enterUs,epoch.leaveUs.takeIf{time>=it},
                        renderProfile(line.startUs,line.endUs,line.fragments.last().endUs),false,0f,1f)
                }.filter{it.second.alpha>0}
                val visible=expected.filter{it[2]!="-"}
                assertEquals(visible.map{it[2].toLong()},actual.map{it.first},"$duration/$ms visible original epochs")
                visible.forEachIndexed{i,c->
                    val frame=actual[i].second
                    listOf(frame.alpha,frame.scale,frame.blur).forEachIndexed{j,value->
                        assertEquals(c[j+3].toDouble(),value.toDouble(),.0001,"$duration/$ms/${c[2]} column${j+3}")
                    }
                }
            }
        }
    }
}
