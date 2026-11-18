// SPDX-License-Identifier: AGPL-3.0-only
// Independent bounded residency adapter; not part of the upstream camera model.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap

/** Getter never reads disk. Published on the drawing thread between frames. */
internal class DioramaRaster(image:ImageBitmap) {
    val width=image.width
    val height=image.height
    val bytes=width.toLong()*height*4
    internal var resident:ImageBitmap?=image
    internal var diskId:Int?=null
    val isResident:Boolean get()=resident!=null
    val image:ImageBitmap get()=checkNotNull(resident){"镜台纹理尚未加载"}
}

internal interface DioramaRasterDisk {
    fun write(image:ImageBitmap):Int
    fun read(id:Int):ImageBitmap
    fun close()
}

/** Internal deterministic failure/lifecycle control, not a public cache path. */
internal val LocalDioramaRasterDiskFactory=staticCompositionLocalOf<(() -> DioramaRasterDisk)?>{null}

internal class DioramaRasterWindow(internal val images:Map<DioramaRaster,ImageBitmap>)

/** One owner per content/font/viewport preparation. No global lyric cache and
 * no readback, decompression, shaping or file access in Canvas/hit testing. */
internal expect class DioramaRasterCache(memoryLimit:Long,residentLimit:Long=96L*1024*1024,
    enforce:Boolean=true,diskFactory:()->DioramaRasterDisk) {
    val spilled:Boolean
    val residentBytes:Long
    val totalBytes:Long
    fun retain(image:ImageBitmap):DioramaRaster
    fun load(required:List<DioramaRaster>,prefetch:List<DioramaRaster>,checkpoint:()->Unit={}):DioramaRasterWindow
    fun publish(window:DioramaRasterWindow)
    fun close()
}

@Composable
internal expect fun rememberDioramaRasterDiskFactory():()->DioramaRasterDisk
