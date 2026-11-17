package top.ghhccghk.accompanist.lyrics.folia.internal.styles.cappella

import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.Density
import kotlin.math.min

/** Coordinates already include both actual row/group transforms. Shadow and
 * rounded empty corners are not content; title/transparent rows have no source. */
internal fun cappellaHitTest(rows:List<CappellaRowLayout>,point:Offset):Int? =
    rows.asReversed().firstOrNull { r ->
        if(r.message.line==null||r.opacity<=0||r.scale<=0)false else {
            val x=point.x-r.bubbleX;val y=point.y-r.bubbleY
            val w=r.bubbleWidth;val h=r.bubbleHeight
            if(x<0||y<0||x>=w||y>=h)false else {
                val right=r.message.sender.side==CappellaSide.RIGHT
                val normalization=min(1.0,min(w,h)/(48*r.scale))
                val small=6*r.scale*normalization;val large=24*r.scale*normalization
                val radius=if(y<h/2)large else if((x>=w/2)==right)small else large
                val cx=if(x<w/2)radius else w-radius
                val cy=if(y<h/2)radius else h-radius
                val dx=x-cx;val dy=y-cy
                x in radius..(w-radius)||y in radius..(h-radius)||dx*dx+dy*dy<=radius*radius
            }
        }
    }?.message?.line

@Composable
internal fun CappellaInput(rows:List<CappellaRowLayout>,lines:List<CappellaLine>,density:Density,
    modifier:Modifier,onClick:((Int)->Unit)?,onLongPress:((Int)->Unit)?) {
    val latest by rememberUpdatedState(rows)
    val click by rememberUpdatedState(onClick);val longPress by rememberUpdatedState(onLongPress)
    val input=if(onClick==null&&onLongPress==null)Modifier else Modifier.pointerInput(lines,density) {
        awaitEachGesture {
            val down=awaitFirstDown(requireUnconsumed=false)
            fun hit(point:Offset)=cappellaHitTest(latest,point/density.density)
            val index=hit(down.position)?:return@awaitEachGesture
            down.consume()
            val outcome=withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis){true to waitForUpOrCancellation()}
            if(outcome==null){
                if(hit(down.position)==index)longPress?.invoke(index)
                do{val event=awaitPointerEvent();event.changes.forEach{it.consume()}}while(event.changes.any{it.pressed})
            }else outcome.second?.let{up->if(hit(up.position)==index)click?.invoke(index);up.consume()}
        }
    }
    Box(modifier.then(input).semantics {
        val visible=rows.filter{it.message.line!=null&&it.opacity>0&&it.scale>0}
        contentDescription=visible.joinToString("\n"){it.message.text}
        customActions=visible.flatMap{r->buildList{
            val i=r.message.line!!
            if(onClick!=null)add(CustomAccessibilityAction(r.message.text){click?.invoke(i);true})
            if(onLongPress!=null)add(CustomAccessibilityAction("长按 ${r.message.text}"){longPress?.invoke(i);true})
        }}
    })
}
