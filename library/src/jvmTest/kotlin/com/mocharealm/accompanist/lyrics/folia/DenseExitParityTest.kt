package com.mocharealm.accompanist.lyrics.folia

import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.*
import com.mocharealm.accompanist.lyrics.folia.internal.model.normalize
import com.mocharealm.accompanist.lyrics.folia.internal.timeline.VisualTimeline
import com.mocharealm.accompanist.lyrics.folia.internal.animation.*
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

/** Actual mounted original Classic/AnimatePresence, including >4 simultaneous exits. */
@OptIn(ExperimentalTestApi::class)
class DenseExitParityTest {
    private fun lyrics()=SyncedLyrics(List(30) {i->
            val start=700+i*10
            KaraokeLine.MainKaraokeLine(listOf(KaraokeSyllable("D${i.toString().padStart(2,'0')}",start,start+1)),
                null,KaraokeAlignment.Unspecified,start,start+1)
        })
    @Test fun allVisibleOriginalExitEpochsSurviveDirectSeek() {
        val lines=normalize(lyrics()).lines
        val timeline=VisualTimeline(lines)
        val rows=File("../verification/classic-fixtures/dense-exits.tsv").readLines().filterNot {it.startsWith('#')||it.isBlank()}
        for(row in rows+rows.reversed()) {
            val c=row.split('\t');val time=c[0].toLong()*1000;val selection=timeline.at(time)
            val actual=(selection.exiting+listOfNotNull(selection.primary)).map {epoch->
                val line=lines[epoch.line]
                val profile=renderProfile(line.startUs,line.endUs,line.fragments.last().endUs)
                val leave=epoch.leaveUs.takeIf {time>=it}
                line.startUs/1000 to sampleLine(time,epoch.enterUs,leave,profile,false,0f,1f).alpha
            }.filter {it.second>0}
            val starts=if(c[1].isEmpty()) emptyList() else c[1].split(',').map{it.toLong()}
            assertEquals(starts,actual.map{it.first},"Visible original line epochs at ${c[0]}ms")
            if(c[2].isNotEmpty()) c[2].split(',').forEachIndexed{i,a->
                assertEquals(a.toDouble(),actual[i].second.toDouble(),.0001,"Original exit alpha at ${c[0]}ms / ${starts[i]}")
            }
        }
    }

    @Test fun publicEntryRendersDenseExitsAndRebuildsPausedFrames() = runDesktopComposeUiTest(width=800,height=600) {
        val content=lyrics()
        val font=FontFamily(Font(File("../verification/assets/FoliaParityLatin-Bold.ttf"),FontWeight.Bold))
        var snapshot by mutableStateOf(FoliaPlaybackSnapshot(0,false))
        setContent {
            FoliaLyricsView(content,{snapshot},Modifier.fillMaxSize().background(Color(0xFF162435)),
                typography=FoliaTypography(secondary=TextStyle(fontFamily=font,fontWeight=FontWeight.Bold)),
                colors=FoliaColors(secondary=Color.Transparent),
                options=FoliaOptions(showTranslation=false,showPhonetic=false,showNextLine=false),
                style=FoliaStyle.Classic(ClassicTuning(breathingFloatMultiplier=0f)))
        }
        val output=File("../build/verification/classic-dense-kotlin").apply{mkdirs()}
        val observed=mutableMapOf<Int,IntArray>()
        val times=listOf(801,901,1001,1051,1101,1201,1301)
        for(ms in times+times.reversed()) {
            runOnIdle {snapshot=FoliaPlaybackSnapshot(ms.toLong(),false,snapshot.discontinuity+1)};waitForIdle()
            val capture=captureToImage();val pixels=IntArray(800*600).also{capture.readPixels(it)}
            observed[ms]?.let{assertContentEquals(it,pixels,"Dense reverse seek at $ms ms")}
            observed[ms]=pixels
            mainClock.advanceTimeBy(1000);waitForIdle()
            val repeated=IntArray(800*600).also{captureToImage().readPixels(it)}
            assertContentEquals(pixels,repeated,"Paused dense scene at $ms ms")
            ImageIO.write(capture.toAwtImage(),"png",output.resolve("${ms}ms.png"))
        }
    }
}
