// SPDX-License-Identifier: AGPL-3.0-only
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.*
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.ColorAlphaType
import java.io.File

@Composable
internal actual fun rememberDioramaRasterDiskFactory():()->DioramaRasterDisk=remember {
    {DioramaFileRasterDisk(File(System.getProperty("java.io.tmpdir")))}
}

internal fun dioramaNativeRasterBytes(image:ImageBitmap):ByteArray =
    checkNotNull(image.asSkiaBitmap().readPixels(ImageInfo.makeS32(image.width,image.height,ColorAlphaType.PREMUL),image.width*4))

internal fun dioramaNativeRasterFromBytes(width:Int,height:Int,bytes:ByteArray):ImageBitmap {
    val bitmap=Bitmap()
    try {
        check(bitmap.installPixels(ImageInfo.makeS32(width,height,ColorAlphaType.PREMUL),bytes,width*4))
        return bitmap.asComposeImageBitmap()
    }catch(error:Exception){bitmap.close();throw error}
}
