// SPDX-License-Identifier: AGPL-3.0-only
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama

import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import java.nio.ByteBuffer

@Composable
internal actual fun rememberDioramaRasterDiskFactory():()->DioramaRasterDisk {
    val context=LocalContext.current.applicationContext
    return remember(context){{DioramaFileRasterDisk(context.cacheDir)}}
}

internal fun dioramaNativeRasterBytes(image:ImageBitmap):ByteArray {
    val bitmap=image.asAndroidBitmap()
    check(bitmap.config==Bitmap.Config.ARGB_8888){"镜台纹理缓存仅支持ARGB8888"}
    val buffer=ByteBuffer.allocate(bitmap.byteCount);bitmap.copyPixelsToBuffer(buffer)
    val raw=buffer.array();val row=image.width*4
    return if(bitmap.rowBytes==row)raw else ByteArray(row*image.height).also{out->
        for(y in 0 until image.height)raw.copyInto(out,y*row,y*bitmap.rowBytes,y*bitmap.rowBytes+row)
    }
}

internal fun dioramaNativeRasterFromBytes(width:Int,height:Int,bytes:ByteArray):ImageBitmap {
    val bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888)
    try {
        val row=width*4
        val raw=if(bitmap.rowBytes==row)bytes else ByteArray(bitmap.byteCount).also{out->
            for(y in 0 until height)bytes.copyInto(out,y*bitmap.rowBytes,y*row,y*row+row)
        }
        bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(raw))
        return bitmap.asImageBitmap()
    }catch(error:Exception){bitmap.recycle();throw error}
}
