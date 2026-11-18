// SPDX-License-Identifier: AGPL-3.0-only
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.ceil

/** Temporary viewport-sized F16 RGBA payload bound, excluding GPU/driver and
 * any nested raster buffers. No song/image cache or frame history is owned. */
internal fun dioramaTextSurfaceBytes(size:Size):Long? {
    if(!size.width.isFinite()||!size.height.isFinite()||size.width<=0||size.height<=0)return null
    if(size.width>8192||size.height>8192)return null
    val bytes=ceil(size.width.toDouble()).toLong()*ceil(size.height.toDouble()).toLong()*8
    return bytes.takeIf{it<=32L*1024*1024}
}

/** Executes exactly once on an available bounded transparent F16 surface, then
 * composites over the host. False means draw was NOT called: use real fallback.
 * Never catches a drawing error or retains a surface after the draw returns. */
internal expect fun DrawScope.tryDioramaTextSurface(draw:()->Unit):Boolean
