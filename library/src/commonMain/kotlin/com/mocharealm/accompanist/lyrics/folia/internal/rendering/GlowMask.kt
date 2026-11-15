package com.mocharealm.accompanist.lyrics.folia.internal.rendering

import androidx.compose.ui.graphics.ImageBitmap

internal expect fun bitmapFromArgb(width: Int, height: Int, pixels: IntArray): ImageBitmap

/** Three bounded separable box passes approximate a fixed Gaussian halo, only during preparation. */
internal fun blurredMask(source: ImageBitmap, radius: Int): ImageBitmap {
    return blurredMasks(source, intArrayOf(radius)).single()
}

/** Reads the source once and reuses CPU scratch arrays for all independent radii.
 * bitmapFromArgb copies the completed pixels; returned masks do not share scratch storage. */
internal fun blurredMasks(source: ImageBitmap, radii: IntArray): List<ImageBitmap> {
    require(radii.all { it in 1..4096 })
    if (radii.isEmpty()) return emptyList()
    val width = source.width
    val height = source.height
    val pixels = IntArray(width * height)
    source.readPixels(pixels)
    val alpha = IntArray(pixels.size)
    val temporary = IntArray(pixels.size)
    return radii.map { radius ->
        for (index in pixels.indices) alpha[index] = pixels[index] ushr 24
        blurAlpha(alpha, temporary, width, height, radius)
        for (index in alpha.indices) alpha[index] = (alpha[index] shl 24) or 0xFFFFFF
        bitmapFromArgb(width, height, alpha)
    }
}

/** CSS applies the span opacity after painting all its shadows. Flatten first so
 * the overlapping inner/outer shadows receive one opacity during the sweep. */
internal fun compositeGlowMasks(masks: List<ImageBitmap>): ImageBitmap {
    require(masks.isNotEmpty())
    if (masks.size == 1) return masks.single()
    val width = masks.first().width
    val height = masks.first().height
    require(masks.all { it.width == width && it.height == height })
    val coverage = IntArray(width * height)
    val pixels = IntArray(coverage.size)
    masks.forEach { mask ->
        mask.readPixels(pixels)
        for (i in coverage.indices) {
            val a = pixels[i] ushr 24
            coverage[i] = a + (coverage[i] * (255 - a) + 127) / 255
        }
    }
    for (i in coverage.indices) pixels[i] = (coverage[i] shl 24) or 0xFFFFFF
    return bitmapFromArgb(width, height, pixels)
}

private fun blurAlpha(alpha: IntArray, temporary: IntArray, width: Int, height: Int, radius: Int) {
    val window = radius * 2 + 1
    repeat(3) {
        for (y in 0 until height) {
            var total = 0
            for (x in -radius..radius) if (x in 0 until width) total += alpha[y * width + x]
            for (x in 0 until width) {
                temporary[y * width + x] = total / window
                if (x - radius >= 0) total -= alpha[y * width + x - radius]
                if (x + radius + 1 < width) total += alpha[y * width + x + radius + 1]
            }
        }
        for (x in 0 until width) {
            var total = 0
            for (y in -radius..radius) if (y in 0 until height) total += temporary[y * width + x]
            for (y in 0 until height) {
                alpha[y * width + x] = total / window
                if (y - radius >= 0) total -= temporary[(y - radius) * width + x]
                if (y + radius + 1 < height) total += temporary[(y + radius + 1) * width + x]
            }
        }
    }
}
