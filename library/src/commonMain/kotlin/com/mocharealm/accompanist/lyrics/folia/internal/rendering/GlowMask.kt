package com.mocharealm.accompanist.lyrics.folia.internal.rendering

import androidx.compose.ui.graphics.ImageBitmap

internal expect fun bitmapFromArgb(width: Int, height: Int, pixels: IntArray): ImageBitmap

/** Three bounded separable box passes approximate a fixed Gaussian halo, only during preparation. */
internal fun blurredMask(source: ImageBitmap, radius: Int): ImageBitmap {
    val width = source.width
    val height = source.height
    val pixels = IntArray(width * height)
    source.readPixels(pixels)
    var alpha = IntArray(pixels.size) { pixels[it] ushr 24 }
    var temporary = IntArray(pixels.size)
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
    for (index in pixels.indices) pixels[index] = (alpha[index] shl 24) or 0xFFFFFF
    return bitmapFromArgb(width, height, pixels)
}
