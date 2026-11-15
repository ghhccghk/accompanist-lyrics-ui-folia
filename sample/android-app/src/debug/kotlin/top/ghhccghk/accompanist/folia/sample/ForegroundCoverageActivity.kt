package top.ghhccghk.accompanist.folia.sample

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.*
import java.io.File

/** Debug control: intact Compose paragraph on hardware canvas vs the same cached software input. */
class ForegroundCoverageActivity:ComponentActivity() {
    private data class Request(val size:Float,val cached:Boolean,val dark:Boolean,val token:Long,val blank:Boolean,
        val font:String,val ink:Color,val surface:Color,val tinted:Boolean)
    private fun requested(intent:Intent)=Request(intent.getFloatExtra("size",19.74f).also{require(it in 10f..80f)},
        intent.getBooleanExtra("cached",false),intent.getBooleanExtra("dark",true),intent.getLongExtra("token",0),
        intent.getBooleanExtra("blank",false),
        (intent.getStringExtra("font")?:"latin").also{require(it in setOf("latin","arabic","cjk"))},
        parseColor(intent.getStringExtra("ink")?:"#ffffff"),
        parseColor(intent.getStringExtra("surface")?:if(intent.getBooleanExtra("dark",true))"#162435" else "#000000"),
        intent.getBooleanExtra("tinted",false))
    private fun parseColor(value:String):Color {
        require(Regex("#[0-9a-fA-F]{6}").matches(value))
        return Color(android.graphics.Color.parseColor(value))
    }
    private lateinit var request:MutableState<Request>
    override fun onCreate(state:Bundle?) {
        super.onCreate(state)
        @Suppress("DEPRECATION")
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        request=mutableStateOf(requested(intent))
        val fonts=mapOf("latin" to "FoliaParityLatin-Bold.ttf","arabic" to "NotoSansArabic-Bold.ttf","cjk" to "FoliaParityCJK-Bold.otf").mapValues {(_,asset)->
            val file=File(filesDir,"coverage-$asset")
            assets.open(asset).use {input->file.outputStream().use(input::copyTo)}
            FontFamily(Font(file,weight=FontWeight.Bold))
        }
        setContent {
            val r=request.value
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                val text=when(r.font){"arabic"->"مرحبا";"cjk"->"合成歌词";else->"Synthetic"}
                val layout=rememberTextMeasurer().measure(text,TextStyle(fontFamily=fonts.getValue(r.font),fontWeight=FontWeight.Bold,
                    fontSize=r.size.sp,textMotion=TextMotion.Animated,textAlign=TextAlign.Left,textDirection=TextDirection.Ltr),softWrap=false)
                val origin=Offset(128f,191f-layout.firstBaseline)
                val bitmap=remember(layout,r.ink,r.tinted) {
                    ImageBitmap(512,320).also {image->
                        CanvasDrawScope().draw(Density(1f),LayoutDirection.Ltr,Canvas(image),Size(512f,320f)) {
                            drawText(layout,if(r.tinted)Color.White else r.ink,origin,shadow=Shadow.None)
                        }
                    }
                }
                Layout(content={},modifier=Modifier.drawBehind {
                    drawRect(r.surface)
                    check(drawContext.canvas.nativeCanvas.isHardwareAccelerated) {"Expected actual hardware surface"}
                    if(!r.blank) {
                        if(r.cached)drawImage(bitmap,colorFilter=if(r.tinted)ColorFilter.tint(r.ink) else null)
                        else drawText(layout,r.ink,origin,shadow=Shadow.None)
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
