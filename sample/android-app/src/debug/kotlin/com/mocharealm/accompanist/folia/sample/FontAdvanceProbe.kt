package com.mocharealm.accompanist.folia.sample

import android.content.res.AssetManager
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

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
