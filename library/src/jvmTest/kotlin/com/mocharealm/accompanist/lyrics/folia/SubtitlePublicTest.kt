package com.mocharealm.accompanist.lyrics.folia

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class SubtitlePublicTest {
    @Test fun gapsMarkersAndReverseSeek()=runDesktopComposeUiTest(width=800,height=600) {
        val lyrics=SyncedLyrics(listOf(SyncedLine("Amber echo","Soft amber",700,1900),SyncedLine("Silver light","//",3200,4100)))
        var snapshot by mutableStateOf(FoliaPlaybackSnapshot(1500,false))
        val font=FontFamily(Font(File("../verification/assets/FoliaParityLatin-Bold.ttf"),FontWeight.Bold))
        setContent {FoliaLyricsView(lyrics,{snapshot},Modifier.fillMaxSize().background(Color(0xFF162435)),
            typography=FoliaTypography(secondary=TextStyle(fontFamily=font),subtitle=TextStyle(fontFamily=font)),
            options=FoliaOptions(showPhonetic=false,showNextLine=false),style=FoliaStyle.Classic(ClassicTuning(breathingFloatMultiplier=0f)))}
        val output=File("../build/verification/subtitle-public").apply {mkdirs()}
        for(time in listOf(1500,2500,3500,4800,2500,0,1500)) {
            runOnIdle {snapshot=FoliaPlaybackSnapshot(time.toLong(),false,snapshot.discontinuity+1)};waitForIdle()
            if(time==1500 || time==2500) onNodeWithText("Soft amber").assertExists() else onNodeWithText("Soft amber").assertDoesNotExist()
            onNodeWithText("//").assertDoesNotExist()
            ImageIO.write(captureToImage().toAwtImage(),"png",output.resolve("${time}ms.png"))
        }
    }
}
