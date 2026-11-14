package com.mocharealm.accompanist.lyrics.folia.internal.rendering

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

internal actual fun bitmapFromArgb(width: Int, height: Int, pixels: IntArray): ImageBitmap =
    Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888).asImageBitmap()
