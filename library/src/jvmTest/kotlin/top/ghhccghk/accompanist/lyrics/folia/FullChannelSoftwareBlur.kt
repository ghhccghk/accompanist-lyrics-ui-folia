// Frozen 4962f62 four-channel implementation for exact parity and paired measurements.
package top.ghhccghk.accompanist.lyrics.folia.internal.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import kotlin.math.*

/** Finite Gaussian convolution in premultiplied sRGB, with transparent exterior.
 * Crop only transparent padding; keep three sigma of support on every edge.
 * Straight RGB must not bleed from transparent pixels into visible coloured text.
 * Match the three-box CSS/Skia coverage kernel at sigma >= 2; retain the finite
 * Gaussian for small sigmas where the integer box approximation is unsuitable.
 * This correctness fallback deliberately makes no real-time performance promise. */
internal fun fullChannelBlurPremultiplied(source: ImageBitmap, sigma: Float): BlurredPixels {
    require(sigma.isFinite() && sigma > 0f)
    // Android ImageBitmap dimensions cross into Bitmap/JNI. Keep those reads
    // outside pixel loops, including the padded transparent-area scan.
    val sourceWidth = source.width
    val sourceHeight = source.height
    val full = IntArray(sourceWidth * sourceHeight)
    source.readPixels(full, startX=0, startY=0, width=sourceWidth, height=sourceHeight,
        bufferOffset=0, stride=sourceWidth)
    var left = sourceWidth; var right = -1; var top = sourceHeight; var bottom = -1
    for (y in 0 until sourceHeight) for (x in 0 until sourceWidth) {
        if (full[y * sourceWidth + x] ushr 24 != 0) {
            left = minOf(left,x); right = maxOf(right,x); top = minOf(top,y); bottom = maxOf(bottom,y)
        }
    }
    if (right < left) return BlurredPixels(bitmapFromArgb(1,1,intArrayOf(0)), Offset.Zero)
    val radius = ceil(3 * sigma).toInt().coerceAtLeast(1)
    left = maxOf(0,left-radius); right = minOf(sourceWidth-1,right+radius)
    top = maxOf(0,top-radius); bottom = minOf(sourceHeight-1,bottom+radius)
    val width = right-left+1; val height = bottom-top+1
    val count = width*height
    // Preserve contiguous scans for ART. Reuse full when no crop is needed.
    val pixels = if (width == sourceWidth && height == sourceHeight) full else IntArray(count).also { crop ->
        for (y in 0 until height) {
            val start = (top+y)*sourceWidth+left
            full.copyInto(crop,y*width,start,start+width)
        }
    }
    // The vertical pass reads only temporary, so output can reuse input.
    val kernel = if (sigma < 2f) DoubleArray(radius*2+1) { i ->
        exp(-.5*(i-radius).toDouble().pow(2)/(sigma*sigma))
    }.also { values -> val norm=values.sum(); for (i in values.indices) values[i] /= norm }
        else doubleArrayOf()
    val input = DoubleArray(count)
    val temporary = DoubleArray(count)
    val alpha = DoubleArray(count)
    val result = IntArray(count)
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
                input[y*width+x] = sum
            }
            input
        }
        for (i in 0 until count) {
            val value = if (channel == 24) { alpha[i] = filtered[i]; filtered[i] }
                else if (alpha[i] > 0) filtered[i]*255/alpha[i] else 0.0
            result[i] = result[i] or (floor(value+.5).toInt().coerceIn(0,255) shl channel)
        }
    }
    return BlurredPixels(bitmapFromArgb(width,height,result),Offset(left.toFloat(),top.toFloat()))
}
