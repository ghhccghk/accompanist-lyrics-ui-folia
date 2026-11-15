package com.mocharealm.accompanist.lyrics.folia

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.*
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.*
import com.mocharealm.accompanist.lyrics.folia.internal.model.normalize
import com.mocharealm.accompanist.lyrics.folia.internal.styles.classic.ClassicLine
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.*

/** Public entry must render the same core already compared against original browser DOM. */
@OptIn(ExperimentalTestApi::class)
class PublicClassicParityTest {
    @Test fun viewport800() = checkViewport(800)
    @Test fun viewport380() = checkViewport(380)
    private fun checkViewport(width:Int) = runDesktopComposeUiTest(width=width,height=600) {
        val original=KaraokeLine.MainKaraokeLine(listOf(KaraokeSyllable("Amber ",700,1300),
            KaraokeSyllable("echo",1300,1900)),"Synthetic subtitle",KaraokeAlignment.Unspecified,700,1900)
        val lyrics=SyncedLyrics(listOf(original))
        val line=normalize(lyrics).lines.single()
        val font=FontFamily(Font(File("../verification/assets/NotoSans-Bold.ttf"),FontWeight.Bold))
        val base=TextStyle(fontFamily=font,fontWeight=FontWeight.Bold)
        val expected=base.copy(fontSize=(width*.06f).coerceIn(36f,72f).sp)
        var public by mutableStateOf(false)
        var captions by mutableStateOf(false)
        var avoid by mutableStateOf(false)
        var snapshot by mutableStateOf(FoliaPlaybackSnapshot(1500,false))
        setContent {
            val tuning=ClassicTuning(breathingFloatMultiplier=0f,avoidOverlap=avoid)
            val options=FoliaOptions(showTranslation=captions,showPhonetic=false,showNextLine=false)
            Box(Modifier.fillMaxSize().background(Color(0xFF162435)),contentAlignment=Alignment.Center) {
                if(public) FoliaLyricsView(lyrics,{snapshot},Modifier.fillMaxSize(),
                    typography=FoliaTypography(secondary=base),options=options,style=FoliaStyle.Classic(tuning))
                else Box(Modifier.fillMaxWidth().padding(horizontal=32.dp),contentAlignment=Alignment.Center) {
                    ClassicLine(line,{snapshot},expected,FoliaColors(),options,tuning,356.dp)
                }
            }
        }
        val output=File("../build/verification/classic-public").apply{mkdirs()}
        for(mode in listOf(false,true)) for(time in listOf(750L,1000L,1500L,1900L,2100L)) {
            runOnIdle {avoid=mode;public=false;captions=false;snapshot=FoliaPlaybackSnapshot(time,false)}
            waitForIdle()
            val reference=captureToImage()
            val expectedPixels=IntArray(width*600).also{reference.readPixels(it)}
            runOnIdle{public=true}
            waitForIdle()
            val actual=captureToImage()
            val pixels=IntArray(width*600).also{actual.readPixels(it)}
            assertContentEquals(expectedPixels,pixels,"Public/core mismatch: width=$width avoid=$mode time=$time")
            ImageIO.write(actual.toAwtImage(),"png",File(output,"$width-${if(mode) "avoid" else "default"}-${time}ms.png"))
            if(time==1500L) {
                runOnIdle{captions=true}
                waitForIdle()
                val withCaption=IntArray(width*600).also{captureToImage().readPixels(it)}
                assertContentEquals(pixels.copyOf(width*440),withCaption.copyOf(width*440),"Subtitle shifted the main line")
                assertFalse(pixels.contentEquals(withCaption),"Subtitle must actually be visible")
            }
        }
    }
}
