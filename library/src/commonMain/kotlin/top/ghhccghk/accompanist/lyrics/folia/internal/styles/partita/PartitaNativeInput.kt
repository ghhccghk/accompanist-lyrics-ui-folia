// SPDX-License-Identifier: AGPL-3.0-only
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.partita

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.PointerInputChange

/** Only capture a gesture after a submitted glyph hit. Blank downs remain
 * available to parent controls. Movement, another pointer or parent consumption
 * cancels both click and long press; an up never follows a long press with click. */
internal suspend fun PointerInputScope.partitaNativeGestures(
    hit:(Offset)->Boolean,click:()->Unit,longPress:()->Unit,
) = awaitEachGesture {
    val down=awaitFirstDown()
    if(!hit(down.position))return@awaitEachGesture
    down.consume()
    var cancelled=false
    var up:PointerInputChange?=null
    val ended=withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
        while(true) {
            val event=awaitPointerEvent()
            val change=event.changes.firstOrNull { it.id==down.id }
            if(change==null || change.isConsumed || event.changes.any { it.id!=down.id && it.pressed } ||
                (change.position-down.position).getDistance()>viewConfiguration.touchSlop || !hit(change.position)) {
                cancelled=true;break
            }
            if(!change.pressed){up=change;break}
        }
        true
    }
    if(ended==null&&!cancelled) {
        if(hit(down.position))longPress()
        do {val event=awaitPointerEvent();event.changes.forEach { it.consume() }} while(event.changes.any { it.pressed })
    } else if(!cancelled)up?.let { it.consume();click() }
}
