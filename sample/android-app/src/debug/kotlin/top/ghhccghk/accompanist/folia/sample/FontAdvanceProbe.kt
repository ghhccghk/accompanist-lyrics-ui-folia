package top.ghhccghk.accompanist.folia.sample

import android.content.res.AssetManager
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.text.drawText
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.unit.sp

/** Diagnostic only: native shaped advances, never a replacement for paragraph shaping. */
internal fun logNativeFontAdvances(assets:AssetManager,directory:File) {
    val face=Typeface.createFromAsset(assets,"FoliaParityLatin-Bold.ttf")
    val text="Synthetic"
    for(size in listOf(19.74f,19.744f,36f,48f,72f)) {
        for(mode in listOf("linear-no-hint","linear-hint","no-linear-no-hint","linear-64x")) {
            val scale=if(mode=="linear-64x") 64f else 1f
            val paint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
                typeface=face; textSize=size*scale
                isLinearText=mode!="no-linear-no-hint"
                hinting=if(mode=="linear-hint") Paint.HINTING_ON else Paint.HINTING_OFF
            }
            val advances=FloatArray(text.length)
            val total=paint.getTextRunAdvances(text.toCharArray(),0,text.length,0,text.length,false,advances,0)/scale
            val file=File(directory,"font-probe-$size-$mode.png")
            val bitmap=Bitmap.createBitmap(512,320,Bitmap.Config.ARGB_8888)
            try {
                paint.color=android.graphics.Color.WHITE
                Canvas(bitmap).apply {scale(1f/scale,1f/scale);drawText(text,128f*scale,191f*scale,paint)}
                file.outputStream().use {check(bitmap.compress(Bitmap.CompressFormat.PNG,100,it))}
            }finally {bitmap.recycle()}
            Log.i("FoliaMetrics",JSONObject().put("text",text).put("size",size.toDouble())
                .put("mode",mode).put("flags",paint.flags).put("hinting",paint.hinting)
                .put("raster",file.name)
                .put("total",total.toDouble()).put("advances",JSONArray(advances.map {(it/scale).toDouble()})).toString())
        }
    }
    Log.i("FoliaMetrics","DONE")
}

/** The actual Compose primitive/style used by the intact classic body paragraph. */
@Composable
internal fun ComposeFontRasterProbe(font:FontFamily,token:Long,directory:File) {
    val measurer=rememberTextMeasurer()
    LaunchedEffect(font,token,measurer) {
        for(size in listOf(19.74f,19.744f,36f,48f,72f)) {
            val layout=measurer.measure("Synthetic",TextStyle(fontFamily=font,fontWeight=FontWeight.Bold,
                fontSize=size.sp,textMotion=TextMotion.Animated),softWrap=false,
                density=androidx.compose.ui.unit.Density(1f))
            val bitmap=androidx.compose.ui.graphics.ImageBitmap(512,320)
            androidx.compose.ui.graphics.drawscope.CanvasDrawScope().draw(androidx.compose.ui.unit.Density(1f),
                androidx.compose.ui.unit.LayoutDirection.Ltr,androidx.compose.ui.graphics.Canvas(bitmap),
                androidx.compose.ui.geometry.Size(512f,320f)) {
                drawText(layout,androidx.compose.ui.graphics.Color.White,
                    androidx.compose.ui.geometry.Offset(128f,191f-layout.firstBaseline),
                    shadow=androidx.compose.ui.graphics.Shadow.None)
            }
            val file=File(directory,"font-probe-compose-$size.png")
            file.outputStream().use {check(bitmap.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it))}
            Log.i("FoliaRaster",JSONObject().put("size",size.toDouble()).put("raster",file.name)
                .put("baseline",layout.firstBaseline.toDouble())
                .put("advance",layout.getPathForRange(0,9).getBounds().width.toDouble()).toString())
        }
        Log.i("FoliaRaster","RASTER_DONE")
    }
}
