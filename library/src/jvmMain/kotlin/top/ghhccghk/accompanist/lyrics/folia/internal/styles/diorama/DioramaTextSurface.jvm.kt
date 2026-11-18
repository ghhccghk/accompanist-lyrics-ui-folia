// SPDX-License-Identifier: AGPL-3.0-only
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama

import androidx.compose.ui.graphics.skiaCanvas
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Rect

/** Original transparent WebGL canvas accumulates normal/additive RGBA BEFORE
 * page-background composition. F16 avoids intermediate8-bit alpha/color loss.
 * This is an internal opt-in text surface, not an Android or public3D engine. */
internal actual fun DrawScope.tryDioramaTextSurface(draw:()->Unit):Boolean {
    if(dioramaTextSurfaceBytes(size)==null)return false
    val canvas=drawContext.canvas.skiaCanvas
    // Bounds are in LOCAL pixels. Account for the actual host matrix/density
    // before budgeting the DEVICE allocation; reject a projective horizon.
    val matrix=canvas.localToDeviceAsMatrix33.mat
    val corners=listOf(0.0 to 0.0,size.width.toDouble() to 0.0,
        0.0 to size.height.toDouble(),size.width.toDouble() to size.height.toDouble())
    val denominators=corners.map{(x,y)->matrix[6]*x+matrix[7]*y+matrix[8]}
    if(denominators.any{!it.isFinite()||it<=0})return false
    val points=corners.mapIndexed { i,(x,y)->
        (matrix[0]*x+matrix[1]*y+matrix[2])/denominators[i] to
            (matrix[3]*x+matrix[4]*y+matrix[5])/denominators[i]
    }
    if(dioramaTextSurfaceBytes(Size((points.maxOf{it.first}-points.minOf{it.first}).toFloat(),
            (points.maxOf{it.second}-points.minOf{it.second}).toFloat()))==null)return false
    val count=canvas.save()
    try{
        val bounds=Rect.makeWH(size.width,size.height)
        canvas.clipRect(bounds,org.jetbrains.skia.ClipMode.INTERSECT,false)
        canvas.saveLayer(Canvas.SaveLayerRec(bounds=bounds,
            saveLayerFlags=Canvas.SaveLayerFlags(Canvas.SaveLayerFlagsSet.F16ColorType)))
        draw()
    }finally{canvas.restoreToCount(count)}
    return true
}
