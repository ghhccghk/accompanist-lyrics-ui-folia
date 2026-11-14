package org.example.foliaconsumer

import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class ConsumerTest {
    @Test fun publishedVariantRendersAndPreservesCoreIdentity() = runDesktopComposeUiTest(width = 800, height = 600) {
        var clicked: Any? = null
        setContent { Consumer(onClick = { clicked = it }) }
        onNodeWithContentDescription("Independent consumer").performClick()
        assertSame(originalLine, clicked)
        onNodeWithText("Original synthetic text").assertExists()
        assertTrue(captureToImage().width > 0)
    }
}
