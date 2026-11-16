package top.ghhccghk.accompanist.lyrics.folia.internal.rendering

import androidx.compose.ui.graphics.ImageBitmap
import kotlin.math.*
import top.ghhccghk.accompanist.lyrics.folia.ExperimentalFoliaMaskProcessor

internal expect fun bitmapFromArgb(width: Int, height: Int, pixels: IntArray): ImageBitmap

/** CSS shadow radius is twice Gaussian sigma. No per-pass integer truncation. */
internal fun blurredMask(source: ImageBitmap, radius: Int): ImageBitmap =
    blurredMasks(source, intArrayOf(radius)).single()

internal fun blurredMasks(source: ImageBitmap, radii: IntArray): List<ImageBitmap> =
    blurredMasks(source, FloatArray(radii.size) { radii[it].toFloat() })

/** Independent separable implementation of the CSS/SVG three-box approximation.
 * Keep integer sums exact through all three passes of each axis, quantizing
 * only the completed horizontal and vertical masks, as observed in Chromium.
 * A pair of opposite half-pixel even windows keeps the kernel centred. */
internal fun blurredMasks(source: ImageBitmap, sigmas: FloatArray,
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
    val bounds = alphaBounds(pixels,width,height)
    if (bounds == null) return sigmas.map { bitmapFromArgb(width,height,IntArray(pixels.size) { 0xFFFFFF }) }
    // The three finite box passes cannot carry ink beyond this support. Retain
    // enough zero exterior for every intermediate pass, including even-window
    // phase offsets. Use the largest radius so scratch is reused across masks.
    val window = boxWindow(sigmas.maxOrNull()!!)
    val support = 3 * (window / 2) - if (window % 2 == 0) 1 else 0
    val left = maxOf(0,bounds.left-support)
    val top = maxOf(0,bounds.top-support)
    val right = minOf(width,bounds.right+support)
    val bottom = minOf(height,bounds.bottom+support)
    val cropWidth = right-left
    val cropHeight = bottom-top
    val fullSurface = cropWidth==width && cropHeight==height
    val a = DoubleArray(cropWidth*cropHeight)
    val b = DoubleArray(a.size)
    return sigmas.map { sigma ->
        if (fullSurface) for (i in pixels.indices) a[i] = (pixels[i] ushr 24).toDouble()
        else for (y in 0 until cropHeight) {
            val sourceRow=(top+y)*width+left
            val targetRow=y*cropWidth
            for (x in 0 until cropWidth) a[targetRow+x]=(pixels[sourceRow+x] ushr 24).toDouble()
        }
        val input = blurBoxCoverage(a,b,cropWidth,cropHeight,sigma)
        val result = if (fullSurface) IntArray(pixels.size) { (input[it].toInt() shl 24) or 0xFFFFFF }
        else IntArray(pixels.size) { 0xFFFFFF }.also { output ->
            for (y in 0 until cropHeight) {
                val sourceRow=y*cropWidth
                val targetRow=(top+y)*width+left
                for (x in 0 until cropWidth) output[targetRow+x]=(input[sourceRow+x].toInt() shl 24) or 0xFFFFFF
            }
        }
        bitmapFromArgb(width,height,result)
    }
}

private fun boxWindow(sigma: Float): Int = maxOf(1, floor(sigma * 3 * sqrt(2 * PI) / 4 + .5).toInt())

/** Shared coverage kernel for the pre-31 premultiplied colour fallback. */
internal fun blurBoxCoverage(a: DoubleArray, b: DoubleArray, width: Int, height: Int, sigma: Float): DoubleArray {
    val window = boxWindow(sigma)
    val widths = intArrayOf(window, window, window + if (window % 2 == 0) 1 else 0)
    var input = a
    var output = b
    for (horizontal in listOf(true, false)) {
        widths.forEachIndexed { pass, count ->
            boxPass(input, output, width, height, count, horizontal,
                if (count % 2 == 0 && pass == 1) -1 else 0)
            val swap = input; input = output; output = swap
        }
        val divisor = widths.fold(1.0) { product, count -> product * count }
        for (i in input.indices) input[i] = floor(input[i] / divisor + .5).coerceIn(0.0, 255.0)
    }
    return input
}

private fun boxPass(input: DoubleArray, output: DoubleArray, width: Int, height: Int,
    window: Int, horizontal: Boolean, origin: Int) {
    val length = if (horizontal) width else height
    val lines = if (horizontal) height else width
    val stride = if (horizontal) 1 else width
    val left = window / 2 + origin
    val right = window - left - 1
    if (!horizontal) {
        // Keep the exact per-column recurrence, but write adjacent pixels in
        // each row. The previous output row holds the rolling sums. This avoids
        // strided column traversal without another buffer or changed rounding;
        // fractional premultiplied channels use the same addition/subtraction
        // order, not a prefix-sum or lower-precision approximation.
        for (x in 0 until width) {
            var total = 0.0
            for (y in 0..minOf(right, height - 1)) total += input[y * width + x]
            output[x] = total
        }
        for (y in 1 until height) {
            val previous = (y - 1) * width
            val current = y * width
            val remove = y - 1 - left
            val add = y + right
            for (x in 0 until width) {
                var total = output[previous + x]
                if (remove >= 0) total -= input[remove * width + x]
                if (add < height) total += input[add * width + x]
                output[current + x] = total
            }
        }
        return
    }
    repeat(lines) { line ->
        val base = if (horizontal) line * width else line
        var total = 0.0
        for (p in 0..minOf(right, length - 1)) total += input[base + p * stride]
        repeat(length) { p ->
            output[base + p * stride] = total
            if (p - left >= 0) total -= input[base + (p - left) * stride]
            if (p + right + 1 < length) total += input[base + (p + right + 1) * stride]
        }
    }
}

/** Each CSS shadow carries the ink alpha before source-over. The animated span
 * opacity belongs to the flattened pair and is applied later, once, in draw. */
internal fun compositeGlowMasks(masks: List<ImageBitmap>, inkAlpha: Float = 1f): ImageBitmap {
    require(masks.isNotEmpty())
    require(inkAlpha.isFinite() && inkAlpha in 0f..1f)
    if (masks.size == 1 && inkAlpha == 1f) return masks.single()
    val width = masks.first().width
    val height = masks.first().height
    require(masks.all { it.width == width && it.height == height })
    val coverage = IntArray(width * height)
    val pixels = IntArray(coverage.size)
    masks.forEach { mask ->
        mask.readPixels(pixels)
        for (i in coverage.indices) {
            val a = ((pixels[i] ushr 24) * inkAlpha).roundToInt()
            coverage[i] = a + (coverage[i] * (256 - a)) / 256
        }
    }
    for (i in coverage.indices) pixels[i] = (coverage[i] shl 24) or 0xFFFFFF
    return bitmapFromArgb(width, height, pixels)
}
