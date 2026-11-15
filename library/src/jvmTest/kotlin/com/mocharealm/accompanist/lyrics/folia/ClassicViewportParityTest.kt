package com.mocharealm.accompanist.lyrics.folia

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.*
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertContentEquals

/** Public entry under the same odd/even viewports as the actual browser captures. */
@OptIn(ExperimentalTestApi::class)
class ClassicViewportParityTest {
    @Test fun wideOddAndEvenHeights() = capture("baseline",800)
    @Test fun narrowOddAndEvenHeights() = capture("wrap",380)
    private fun capture(scene:String,width:Int) = runDesktopComposeUiTest(width=800,height=602) {
        val syllables=if(scene=="baseline") listOf(KaraokeSyllable("Amber ",700,1300),KaraokeSyllable("echo",1300,1900))
        else File("../verification/classic-fixtures/scenes.tsv").readLines().map {it.split('\t')}
            .filter {it[0]==scene}.map {KaraokeSyllable(it[5],it[3].toInt(),it[4].toInt())}
        val source=SyncedLyrics(listOf(KaraokeLine.MainKaraokeLine(syllables,null,KaraokeAlignment.Unspecified,
            700,if(scene=="baseline") 1900 else 2800)))
        val family=FontFamily(Font(File("../verification/assets/FoliaParityLatin-Bold.ttf"),FontWeight.Bold))
        var height by mutableIntStateOf(600)
        var level by mutableStateOf(FoliaAnimationIntensity.Normal)
        var avoid by mutableStateOf(false)
        setContent {
            Box(Modifier.fillMaxSize().background(Color(0xFF162435))) {
                FoliaLyricsView(source,{FoliaPlaybackSnapshot(1500,false)},Modifier.requiredSize(width.dp,height.dp),
                    typography=FoliaTypography(secondary=TextStyle(fontFamily=family,fontWeight=FontWeight.Bold)),
                    options=FoliaOptions(showTranslation=false,showPhonetic=false,showNextLine=false,animationIntensity=level),
                    style=FoliaStyle.Classic(ClassicTuning(breathingFloatMultiplier=0f,avoidOverlap=avoid)))
            }
        }
        val out=File("../build/verification/line-centers-kotlin").apply{mkdirs()}
        for(intensity in FoliaAnimationIntensity.entries) for(mode in listOf(false,true)) {
            val seen=mutableMapOf<Int,IntArray>()
            for(h in listOf(600,601,602,601,600)) {
                runOnIdle {height=h;level=intensity;avoid=mode};waitForIdle()
                val frame=captureToImage()
                val pixels=IntArray(800*602).also {frame.readPixels(it)}
                mainClock.advanceTimeBy(1000);waitForIdle()
                assertContentEquals(pixels,IntArray(pixels.size).also {captureToImage().readPixels(it)},"Paused viewport $intensity/$mode/$h")
                seen[h]?.let {assertContentEquals(it,pixels,"Restored viewport $intensity/$mode/$h")}
                seen[h]=pixels
                ImageIO.write(frame.toAwtImage().getSubimage(0,0,width,h),"png",
                    out.resolve("${intensity.name.lowercase()}-$scene-${width}x${h}-${if(mode) "avoid" else "default"}.png"))
            }
        }
    }
}
