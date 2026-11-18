package org.example.foliaconsumer

import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import kotlin.test.*
import top.ghhccghk.accompanist.lyrics.folia.*

@OptIn(ExperimentalTestApi::class)
class ConsumerTest {
    @Test fun publishedDioramaDebugPreviewPreparesFromCoordinates()=runDesktopComposeUiTest(width=800,height=600){
        var status:FoliaStatus?=null
        setContent{Consumer(style=FoliaStyle.Diorama(DioramaTuning(enforceRasterBudget=false)),onStatus={status=it})}
        waitUntil(timeoutMillis=30000){(status?.mainUnitCount?:0)>0&&status!!.diagnostics.none{it.startsWith("DioramaPrepar")}}
        onNodeWithContentDescription(originalLine.content).assertExists()
        assertEquals(FoliaQuality.Minimal,status!!.actualQuality)
        assertTrue(captureToImage().width>0)
    }
    @Test fun publishedVariantRendersAndPreservesCoreIdentity() = runDesktopComposeUiTest(width = 800, height = 600) {
        var clicked: Any? = null
        setContent { Consumer(onClick = { clicked = it }) }
        onNodeWithContentDescription("Independent consumer").performClick()
        assertSame(originalLine, clicked)
        onNodeWithText("Original synthetic text").assertExists()
        assertTrue(captureToImage().width > 0)
    }
}
