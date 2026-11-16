// Frozen ebcaffd SoftwareBlur.kt for byte-exact regression; do not update with production.
package top.ghhccghk.accompanist.lyrics.folia

import top.ghhccghk.accompanist.lyrics.folia.internal.rendering.*

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import kotlin.math.*



/** Finite Gaussian convolution in premultiplied sRGB, with transparent exterior.
 * Crop only transparent padding; keep three sigma of support on every edge.
 * Straight RGB must not bleed from transparent pixels into visible coloured text.
 * Match the three-box CSS/Skia coverage kernel at sigma >= 2; retain the finite
 * Gaussian for small sigmas where the integer box approximation is unsuitable.
 * This correctness fallback deliberately makes no real-time performance promise. */
internal fun legacyBlurPremultiplied(source: ImageBitmap, sigma: Float): BlurredPixels {
    require(sigma.isFinite() && sigma > 0f)
    val full = IntArray(source.width * source.height)
    source.readPixels(full)
    var left = source.width; var right = -1; var top = source.height; var bottom = -1
    for (y in 0 until source.height) for (x in 0 until source.width) {
        if (full[y * source.width + x] ushr 24 != 0) {
            left = minOf(left,x); right = maxOf(right,x); top = minOf(top,y); bottom = maxOf(bottom,y)
        }
    }
    if (right < left) return BlurredPixels(bitmapFromArgb(1,1,intArrayOf(0)), Offset.Zero)
    val radius = ceil(3 * sigma).toInt().coerceAtLeast(1)
    left = maxOf(0,left-radius); right = minOf(source.width-1,right+radius)
    top = maxOf(0,top-radius); bottom = minOf(source.height-1,bottom+radius)
    val width = right-left+1; val height = bottom-top+1
    val pixels = IntArray(width*height) { i -> full[(top+i/width)*source.width+left+i%width] }
    val kernel = DoubleArray(radius*2+1) { i -> exp(-.5*(i-radius).toDouble().pow(2)/(sigma*sigma)) }
    val norm = kernel.sum(); for (i in kernel.indices) kernel[i] /= norm
    val input = DoubleArray(pixels.size)
    val temporary = DoubleArray(pixels.size)
    val output = DoubleArray(pixels.size)
    val alpha = DoubleArray(pixels.size)
    val result = IntArray(pixels.size)
    for (channel in intArrayOf(24,16,8,0)) {
        for (i in pixels.indices) {
            val a = pixels[i] ushr 24
            input[i] = if (channel == 24) a.toDouble() else ((pixels[i] ushr channel) and 255)*a/255.0
        }
        val filtered = if (sigma >= 2f) blurBoxCoverage(input,temporary,width,height,sigma) else {
            for (y in 0 until height) for (x in 0 until width) {
                var sum = 0.0
                for (d in maxOf(-radius,-x)..minOf(radius,width-1-x)) sum += input[y*width+x+d]*kernel[d+radius]
                temporary[y*width+x] = sum
            }
            for (y in 0 until height) for (x in 0 until width) {
                var sum = 0.0
                for (d in maxOf(-radius,-y)..minOf(radius,height-1-y)) sum += temporary[(y+d)*width+x]*kernel[d+radius]
                output[y*width+x] = sum
            }
            output
        }
        for (i in pixels.indices) {
            val value = if (channel == 24) { alpha[i] = filtered[i]; filtered[i] }
                else if (alpha[i] > 0) filtered[i]*255/alpha[i] else 0.0
            result[i] = result[i] or (floor(value+.5).toInt().coerceIn(0,255) shl channel)
        }
    }
    return BlurredPixels(bitmapFromArgb(width,height,result),Offset(left.toFloat(),top.toFloat()))
}
