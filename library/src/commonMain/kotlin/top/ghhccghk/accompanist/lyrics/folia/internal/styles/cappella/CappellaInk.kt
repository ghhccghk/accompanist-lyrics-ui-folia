// SPDX-License-Identifier: AGPL-3.0-only
// Independent ink-ownership adaptation; original EGC clocks are unchanged.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.cappella

import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.geometry.Offset
import kotlin.math.*

internal data class CappellaInkGlyph(val index:Int,val begin:Int,val end:Int)
/** Null endpoints extend to the caller's canvas edge, retaining all overhang. */
internal data class CappellaInkRun(val index:Int,val left:Int?,val right:Int?)

/** Partition the ALREADY shaped row's coverage, never reshape individual EGCs.
 * Each texel column has exactly one clock owner, including ligature/bidi ink
 * outside a selection's caret box. A logical EGC can own disjoint visual runs.
 * Rebuilt only when the shown row layout changes, not on each fade tick. */
internal fun cappellaInkRuns(layout:TextLayoutResult,glyphs:List<CappellaInkGlyph>):List<CappellaInkRun> {
    if(glyphs.isEmpty())return emptyList()
    val intervals=glyphs.map{it.index to layout.getPathForRange(it.begin,it.end).getBounds()}
    val first=floor(intervals.minOf{it.second.left}).toInt()-1
    val last=ceil(intervals.maxOf{it.second.right}).toInt()+1
    val result=mutableListOf<CappellaInkRun>()
    var owner=-1;var start=first
    for(x in first until last) {
        val center=x+.5
        var closest=Double.POSITIVE_INFINITY;var next=intervals.first().first
        for((i,rect)in intervals) {
            val distance=max(0.0,max(rect.left-center,center-rect.right))
            if(distance<closest){closest=distance;next=i}
        }
        if(next!=owner) {
            if(owner>=0)result+=CappellaInkRun(owner,if(start==first)null else start,x)
            owner=next;start=x
        }
    }
    result+=CappellaInkRun(owner,if(start==first)null else start,null)
    return result
}

/** One alpha field over one shaped draw. Separate clipped text draws lose
 * subpixel coverage when the message is scaled: adjacent antialiased clips are
 * not complementary under source-over. Duplicate gradient stops form exact
 * steps while the complete glyph's own coverage is rasterized only once. */
internal fun cappellaInkBrush(layout:TextLayoutResult,runs:List<CappellaInkRun>,
    alphas:List<Float>,color:Color):Brush {
    require(runs.isNotEmpty()&&runs.size==alphas.size)
    val boundaries=runs.dropLast(1).map{it.right!!.toFloat()}
    val left=min(0f,boundaries.minOrNull()?:0f)
    val right=max(layout.size.width.toFloat(),max(left+1,boundaries.maxOrNull()?:0f))
    fun tone(i:Int)=color.copy(alpha=color.alpha*alphas[i])
    val stops=buildList {
        add(0f to tone(0))
        for((i,x)in boundaries.withIndex()) {
            if(alphas[i]==alphas[i+1])continue
            val at=(x-left)/(right-left)
            add(at to tone(i));add(at to tone(i+1))
        }
        add(1f to tone(runs.lastIndex))
    }
    return Brush.horizontalGradient(*stops.toTypedArray(),startX=left,endX=right)
}

internal fun DrawScope.drawCappellaInk(layout:TextLayoutResult,runs:List<CappellaInkRun>,
    alphas:List<Float>,color:Color,origin:Offset=Offset.Zero) {
    if(runs.isEmpty())return
    require(runs.size==alphas.size)
    // Keep uniform text on the original solid-paint glyph path. Even a
    // constant gradient can select a different Skia subpixel raster path.
    if(alphas.all{it==alphas.first()}) {
        if(alphas.first()>0)drawText(layout,color,topLeft=origin,alpha=alphas.first(),shadow=Shadow.None)
    } else drawText(layout,cappellaInkBrush(layout,runs,alphas,color),topLeft=origin,shadow=Shadow.None)
}
