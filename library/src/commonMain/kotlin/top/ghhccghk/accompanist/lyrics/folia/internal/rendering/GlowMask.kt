package top.ghhccghk.accompanist.lyrics.folia.internal.rendering

import androidx.compose.ui.graphics.ImageBitmap
import kotlin.math.*

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
internal fun blurredMasks(source: ImageBitmap, sigmas: FloatArray): List<ImageBitmap> {
    require(sigmas.all { it.isFinite() && it in 1f..4096f })
    if (sigmas.isEmpty()) return emptyList()
    val width = source.width
    val height = source.height
    val pixels = IntArray(width * height)
    source.readPixels(pixels)
    val a = DoubleArray(pixels.size)
    val b = DoubleArray(pixels.size)
    return sigmas.map { sigma ->
        for (i in pixels.indices) a[i] = (pixels[i] ushr 24).toDouble()
        val input = blurBoxCoverage(a,b,width,height,sigma)
        val result = IntArray(pixels.size) { (input[it].toInt() shl 24) or 0xFFFFFF }
        bitmapFromArgb(width, height, result)
    }
}

/** Shared coverage kernel for the pre-31 premultiplied colour fallback. */
internal fun blurBoxCoverage(a: DoubleArray, b: DoubleArray, width: Int, height: Int, sigma: Float): DoubleArray {
    val window = maxOf(1, floor(sigma * 3 * sqrt(2 * PI) / 4 + .5).toInt())
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
