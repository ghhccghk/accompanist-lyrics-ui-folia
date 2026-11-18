// SPDX-License-Identifier: AGPL-3.0-only
package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class ThemeWaitingPolicyTest {
    @Test fun retainedStagesDoNotOverlayWaitingDotsAndLineStagesKeepFiniteGaps()=runDesktopComposeUiTest(width=380,height=600){
        val core=SyncedLyrics(listOf(SyncedLine("First",null,8000,10000),SyncedLine("Next",null,22000,25000)))
        var time by mutableStateOf(4000L)
        var theme by mutableStateOf<FoliaStyle>(FoliaStyle.Classic())
        setContent{FoliaLyricsView(core,{FoliaPlaybackSnapshot(time,false)},Modifier.fillMaxSize(),style=theme)}
        val retained=listOf(FoliaStyle.Fume(),FoliaStyle.Cappella(),FoliaStyle.Pendolo(),FoliaStyle.Diorama())
        val transient=listOf(FoliaStyle.Classic(),FoliaStyle.Partita(),FoliaStyle.Tilt(),FoliaStyle.Cadenza())
        for(t in listOf(4000L,17000L))for(style in retained+transient){
            runOnIdle{time=t;theme=style}
            if(style in retained)onNodeWithContentDescription("等待演唱").assertDoesNotExist()
            else onNodeWithContentDescription("等待演唱").assertExists("$style gap $t")
        }
    }
}
