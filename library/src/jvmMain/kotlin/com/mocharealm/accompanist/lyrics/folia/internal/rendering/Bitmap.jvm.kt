package com.mocharealm.accompanist.lyrics.folia.internal.rendering

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ImageInfo

internal actual fun bitmapFromArgb(width: Int, height: Int, pixels: IntArray): ImageBitmap {
    val bytes = ByteArray(pixels.size * 4)
    pixels.forEachIndexed { index, pixel ->
        val alpha = pixel ushr 24
        bytes[index * 4] = (((pixel and 255) * alpha) / 255).toByte()
        bytes[index * 4 + 1] = ((((pixel ushr 8) and 255) * alpha) / 255).toByte()
        bytes[index * 4 + 2] = ((((pixel ushr 16) and 255) * alpha) / 255).toByte()
        bytes[index * 4 + 3] = alpha.toByte()
    }
    return Bitmap().apply {
        allocPixels(ImageInfo.makeS32(width, height, ColorAlphaType.PREMUL))
        installPixels(bytes)
    }.asComposeImageBitmap()
}
