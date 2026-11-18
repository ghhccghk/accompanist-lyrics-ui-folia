package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.partita.partitaNativeGestures
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class PartitaNativeInputTest {
    @Test fun blankPassesThroughAndMoveOrCancelNeverLongPresses()=runDesktopComposeUiTest(width=400,height=300) {
        var clicks=0;var longs=0;var parents=0
        setContent {
            Box(Modifier.fillMaxSize().clickable { parents++ }) {
                Box(Modifier.fillMaxSize().pointerInput(Unit) {
                    partitaNativeGestures({it.x in 100f..200f && it.y in 100f..200f},{clicks++},{longs++})
                })
            }
        }
        onRoot().performTouchInput { click(Offset(20f,20f)) }
        runOnIdle { assertEquals(1,parents);assertEquals(0,clicks) }
        onRoot().performTouchInput { click(Offset(150f,150f)) }
        runOnIdle { assertEquals(1,clicks);assertEquals(1,parents) }
        onRoot().performTouchInput { longClick(Offset(150f,150f)) }
        runOnIdle { assertEquals(1,longs);assertEquals(1,clicks) }
        onRoot().performTouchInput { down(Offset(150f,150f));moveTo(Offset(195f,150f));advanceEventTime(1500);up() }
        onRoot().performTouchInput { down(Offset(150f,150f));advanceEventTime(30);cancel() }
        runOnIdle { assertEquals(1,longs);assertEquals(1,clicks);assertEquals(1,parents) }
    }
}
