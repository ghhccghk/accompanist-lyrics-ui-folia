package com.mocharealm.accompanist.folia.sample

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.*
import java.io.File

/** Debug control: intact Compose paragraph on hardware canvas vs the same cached software input. */
class ForegroundCoverageActivity:ComponentActivity() {
    private data class Request(val size:Float,val cached:Boolean,val dark:Boolean,val token:Long,val blank:Boolean)
    private fun requested(intent:Intent)=Request(intent.getFloatExtra("size",19.74f).also{require(it in 10f..80f)},
        intent.getBooleanExtra("cached",false),intent.getBooleanExtra("dark",true),intent.getLongExtra("token",0),
        intent.getBooleanExtra("blank",false))
    private lateinit var request:MutableState<Request>
    override fun onCreate(state:Bundle?) {
        super.onCreate(state)
        @Suppress("DEPRECATION")
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        request=mutableStateOf(requested(intent))
        val fontFile=File(filesDir,"coverage-FoliaParityLatin-Bold.ttf")
        assets.open("FoliaParityLatin-Bold.ttf").use {input->fontFile.outputStream().use(input::copyTo)}
        val font=FontFamily(Font(fontFile,weight=FontWeight.Bold))
        setContent {
            val r=request.value
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                val layout=rememberTextMeasurer().measure("Synthetic",TextStyle(fontFamily=font,fontWeight=FontWeight.Bold,
                    fontSize=r.size.sp,textMotion=TextMotion.Animated),softWrap=false)
                val origin=Offset(128f,191f-layout.firstBaseline)
                val bitmap=remember(layout) {
                    ImageBitmap(512,320).also {image->
                        CanvasDrawScope().draw(Density(1f),LayoutDirection.Ltr,Canvas(image),Size(512f,320f)) {
                            drawText(layout,Color.White,origin,shadow=Shadow.None)
                        }
                    }
                }
                Layout(content={},modifier=Modifier.drawBehind {
                    drawRect(if(r.dark)Color(0xFF162435) else Color.Black)
                    check(drawContext.canvas.nativeCanvas.isHardwareAccelerated) {"Expected actual hardware surface"}
                    if(!r.blank) {
                        if(r.cached)drawImage(bitmap) else drawText(layout,Color.White,origin,shadow=Shadow.None)
                    }
                }) {_,constraints->layout(constraints.maxWidth,constraints.maxHeight){}}
            }
            LaunchedEffect(r) {
                withFrameNanos{};withFrameNanos{}
                check(android.os.Build.VERSION.SDK_INT>=29)
                window.decorView.viewTreeObserver.registerFrameCommitCallback {
                    Log.i("FoliaCoverage","READY token="+r.token)
                }
                window.decorView.invalidate()
            }
        }
    }
    override fun onNewIntent(intent:Intent) {super.onNewIntent(intent);setIntent(intent);request.value=requested(intent)}
}
