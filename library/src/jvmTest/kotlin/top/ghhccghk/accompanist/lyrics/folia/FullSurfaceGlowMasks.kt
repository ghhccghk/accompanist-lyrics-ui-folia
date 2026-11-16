// Frozen full-surface preparation from 1b74fad; crop optimization reference.
package top.ghhccghk.accompanist.lyrics.folia.internal.rendering

import androidx.compose.ui.graphics.ImageBitmap
import top.ghhccghk.accompanist.lyrics.folia.ExperimentalFoliaMaskProcessor

internal fun fullSurfaceGlowMasks(source: ImageBitmap, sigmas: FloatArray,
    processor: ExperimentalFoliaMaskProcessor? = null): List<ImageBitmap> {
    require(sigmas.all { it.isFinite() && it in 1f..4096f })
    if (sigmas.isEmpty()) return emptyList()
    val width = source.width
    val height = source.height
    val pixels = IntArray(width * height)
    source.readPixels(pixels)
    processor?.blur(pixels, width, height, sigmas)?.let { masks ->
        require(masks.size == sigmas.size && masks.all { it.size == pixels.size }) { "Invalid experimental mask output" }
        return masks.map { bitmapFromArgb(width, height, it) }
    }
    val a = DoubleArray(pixels.size)
    val b = DoubleArray(pixels.size)
    return sigmas.map { sigma ->
        for (i in pixels.indices) a[i] = (pixels[i] ushr 24).toDouble()
        val input = blurBoxCoverage(a,b,width,height,sigma)
        val result = IntArray(pixels.size) { (input[it].toInt() shl 24) or 0xFFFFFF }
        bitmapFromArgb(width, height, result)
    }
}
