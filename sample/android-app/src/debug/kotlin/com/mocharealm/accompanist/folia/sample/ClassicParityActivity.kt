package com.mocharealm.accompanist.folia.sample

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.*
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import com.mocharealm.accompanist.lyrics.folia.*
import java.io.File

/** Deterministic public-entry verification only. Not a sample screen or media clock. */
class ClassicParityActivity : ComponentActivity() {
    private data class Frame(val scene:String,val ms:Long,val width:Int,val avoid:Boolean,val backdrop:Boolean)
    private fun requested(intent:Intent)=Frame(intent.getStringExtra("scene")?:"baseline",
        intent.getLongExtra("mediaMs",1500),intent.getIntExtra("width",800).also {require(it in 200..1280)},
        intent.getBooleanExtra("avoid",false),intent.getBooleanExtra("backdrop",false))
    private lateinit var requestedFrame:MutableState<Frame>

    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        if(Build.VERSION.SDK_INT>=30) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.hide(WindowInsets.Type.systemBars())
        }
        requestedFrame=mutableStateOf(requested(intent))
        val families=listOf("FoliaParityLatin-Bold.ttf","NotoSansArabic-Bold.ttf","FoliaParityCJK-Bold.otf").associateWith {name->
            val file=File(filesDir,name)
            assets.open(name).use {input->file.outputStream().use(input::copyTo)}
            FontFamily(Font(file,weight=FontWeight.Bold))
        }
        val sceneRows=assets.open("scenes.tsv").bufferedReader().use {it.readLines()}.filter {it.isNotBlank()&&!it.startsWith('#')}.map {it.split('\t')}
        val subtitleRows=assets.open("subtitle-scenes.tsv").bufferedReader().use {it.readLines()}.filter {it.isNotBlank()&&!it.startsWith('#')}.map {it.split('\t')}
        setContent {
            val frame=requestedFrame.value
            val subtitle=frame.scene=="subtitle"
            val lyrics=remember(frame.scene) {
                fun line(a:String,b:String,s:Int,m:Int,e:Int)=KaraokeLine.MainKaraokeLine(
                    listOf(KaraokeSyllable(a,s,m),KaraokeSyllable(b,m,e)),null,KaraokeAlignment.Unspecified,s,e)
                when(frame.scene) {
                    "baseline"->SyncedLyrics(listOf(line("Amber ","echo",700,1300,1900),line("Silver ","light",3200,3650,4100)))
                    "subtitle"->SyncedLyrics(subtitleRows.map {SyncedLine(it[2],it[3],it[0].toInt(),it[1].toInt())})
                    else->{
                        val rows=sceneRows.filter {it[0]==frame.scene};require(rows.isNotEmpty()) {"Unknown parity scene ${frame.scene}"}
                        SyncedLyrics(listOf(KaraokeLine.MainKaraokeLine(rows.map {KaraokeSyllable(it[5],it[3].toInt(),it[4].toInt())},
                            null,KaraokeAlignment.Unspecified,rows.first()[1].toInt(),rows.first()[2].toInt())))
                    }
                }
            }
            val font=families.getValue(when(frame.scene) {"arabic"->"NotoSansArabic-Bold.ttf";"cjk"->"FoliaParityCJK-Bold.otf";else->"FoliaParityLatin-Bold.ttf"})
            val typography=FoliaTypography(secondary=TextStyle(fontFamily=font,fontWeight=FontWeight.Bold),
                subtitle=TextStyle(fontFamily=font,fontWeight=FontWeight.Bold))
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                Layout(content={
                    FoliaLyricsView(lyrics,{FoliaPlaybackSnapshot(frame.ms,false)},typography=typography,
                        colors=FoliaColors(text=if(subtitle) Color.Transparent else FoliaColors().text,
                            active=if(subtitle) Color.Transparent else Color.White,
                            secondary=if(subtitle) FoliaColors().secondary else Color.Transparent,
                            subtitleBackground=if(frame.backdrop) Color(0xFF48707E) else Color.Transparent),
                        options=FoliaOptions(showTranslation=subtitle,showPhonetic=false,showNextLine=false),
                        style=FoliaStyle.Classic(ClassicTuning(breathingFloatMultiplier=0f,avoidOverlap=frame.avoid)))
                },modifier=Modifier.drawBehind {drawRect(Color(0xFF162435))}) {children,constraints->
                    require(constraints.maxWidth>=frame.width && constraints.maxHeight>=600) {"Parity viewport exceeds device surface: $constraints"}
                    val child=children.single().measure(Constraints.fixed(frame.width,600))
                    layout(constraints.maxWidth,constraints.maxHeight) {child.place(0,0)}
                }
            }
            LaunchedEffect(frame) {
                withFrameNanos {};withFrameNanos {}
                check(Build.VERSION.SDK_INT>=29) {"Frame-commit parity capture requires API29+"}
                // A Choreographer tick precedes drawing. Wait for the actual GPU
                // frame commit so preparation/upload cannot masquerade as pause drift.
                window.decorView.viewTreeObserver.registerFrameCommitCallback {
                    Log.i("FoliaParity","READY ${frame.scene} ${frame.ms} ${frame.width} ${frame.avoid} ${frame.backdrop}")
                }
                window.decorView.invalidate()
            }
        }
    }
    override fun onNewIntent(intent:Intent) {
        super.onNewIntent(intent)
        requestedFrame.value=requested(intent)
    }
}
