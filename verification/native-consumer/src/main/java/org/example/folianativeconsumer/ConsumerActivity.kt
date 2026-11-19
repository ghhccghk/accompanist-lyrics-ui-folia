package org.example.folianativeconsumer

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.*
import top.ghhccghk.accompanist.lyrics.folia.*
import top.ghhccghk.accompanist.lyrics.folia.nativeandroid.*
import java.io.File

/** Uses only published APIs and synthetic/previously licensed test assets. */
class ConsumerActivity : ComponentActivity() {
    private data class Request(val ms:Long,val width:Int,val height:Int,val scene:String,val backend:FoliaRendererBackend,
        val reduced:Boolean,val fontScale:Float,val guides:Boolean,val theme:String,
        val offset:Int,val instances:Int,val active:Boolean,val budget:Long)
    private fun request(i:Intent)=Request(i.getLongExtra("mediaMs",1500),i.getIntExtra("width",800),i.getIntExtra("height",600),
        i.getStringExtra("scene")?:"baseline",FoliaRendererBackend.valueOf(i.getStringExtra("backend")?:"NativeRequired"),
        i.getBooleanExtra("reduced",false),i.getFloatExtra("fontScale",1f),i.getBooleanExtra("guides",true),i.getStringExtra("theme")?:"Partita",
        i.getIntExtra("offset",0),i.getIntExtra("instances",1).coerceIn(1,2),i.getBooleanExtra("active",true),i.getLongExtra("budget",64L*1024*1024))
    private lateinit var current:MutableState<Request>
    private var hostVisible by mutableStateOf(false)
    override fun onStart(){super.onStart();hostVisible=true}
    override fun onStop(){hostVisible=false;super.onStop()}
    override fun onNewIntent(intent:Intent){super.onNewIntent(intent);setIntent(intent);current.value=request(intent)}
    override fun onSaveInstanceState(out:Bundle){out.putParcelable("rendererRequest",intent);super.onSaveInstanceState(out)}
    override fun onCreate(state:Bundle?) {
        super.onCreate(state)
        @Suppress("DEPRECATION")
        state?.getParcelable<Intent>("rendererRequest")?.let{setIntent(it)}
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        window.setDecorFitsSystemWindows(false);window.insetsController?.hide(WindowInsets.Type.systemBars())
        current=mutableStateOf(request(intent))
        val root=FrameLayout(this)
        val label=TextView(this).apply{text="Preparing AAR";setTextColor(-1);textSize=10f}
        val statuses=mutableMapOf<Int,String>()
        fun status(slot:Int,text:String){statuses[slot]=text;label.text=statuses.toSortedMap().values.joinToString("\n")}
        val fonts=listOf("FoliaParityLatin-Bold.ttf","NotoSansArabic-Bold.ttf","FoliaParityCJK-Bold.otf","FoliaParityBidi-Bold.ttf").associateWith{name->
            val file=File(filesDir,name);assets.open(name).use{input->file.outputStream().use(input::copyTo)}
            FontFamily(Font(file,weight=FontWeight.Bold))
        }
        val rows=assets.open("scenes.tsv").bufferedReader().use{it.readLines()}.filter{it.isNotBlank()&&!it.startsWith('#')}.map{it.split('\t')}
        val content=ComposeView(this).apply{setContent{
            val r=current.value
            SideEffect {
                if (statuses.keys.removeAll { it >= r.instances }) {
                    label.text = statuses.toSortedMap().values.joinToString("\n")
                }
            }
            val source=remember(r.scene){
                fun line(a: String, b: String, start: Int, middle: Int, end: Int, translation: String? = null) =
                    KaraokeLine.MainKaraokeLine(listOf(KaraokeSyllable(a, start, middle),
                        KaraokeSyllable(b, middle, end)), translation, KaraokeAlignment.Unspecified, start, end)
                if(r.scene=="baseline")SyncedLyrics(listOf(line("Amber ","echo",700,1300,1900),line("Silver ","light",3200,3650,4100)))
                else if (r.scene == "subtitle") SyncedLyrics(listOf(
                    line("Amber ", "echo", 700, 1300, 2900, "Translation probe"),
                    line("Silver ", "light", 3200, 3650, 4100)))
                else if (r.scene == "emoji") SyncedLyrics(listOf(
                    line("Glow \uD83D\uDC69\uD83C\uDFFD\u200D\uD83C\uDFA4 ", "\uD83C\uDDF9\uD83C\uDDFC echo", 700, 1300, 2500)))
                else if(r.scene=="waiting")SyncedLyrics(listOf(line("Amber ","echo",10000,11000,12000),line("Silver ","light",36000,37000,38000)))
                else {
                    val selected=rows.filter{it[0]==r.scene};require(selected.isNotEmpty())
                    SyncedLyrics(listOf(KaraokeLine.MainKaraokeLine(selected.map{KaraokeSyllable(it[5],it[3].toInt(),it[4].toInt())},null,KaraokeAlignment.Unspecified,selected.first()[1].toInt(),selected.first()[2].toInt())))
                }
            }
            val family=fonts.getValue(when{r.scene=="arabic"->"NotoSansArabic-Bold.ttf";r.scene=="cjk"->"FoliaParityCJK-Bold.otf";r.scene.startsWith("bidi-")->"FoliaParityBidi-Bold.ttf";else->"FoliaParityLatin-Bold.ttf"})
            CompositionLocalProvider(LocalDensity provides Density(1f,r.fontScale)){
                Layout(content={repeat(r.instances){slot->key(slot){FoliaNativeLyricsView(source,{FoliaPlaybackSnapshot(r.ms,false,r.ms)},
                    typography=FoliaTypography(secondary=TextStyle(fontFamily=family,fontWeight=FontWeight.Bold)),
                    options=FoliaOptions(showTranslation=r.scene == "subtitle",showPhonetic=false,showNextLine=false,reducedMotion=r.reduced,offsetMillis=r.offset),
                    style=when (r.theme) {
                        "Partita" -> FoliaStyle.Partita(PartitaTuning(showGuideLines=r.guides))
                        "Diorama" -> FoliaStyle.Diorama()
                        else -> FoliaStyle.Classic()
                    },backend=r.backend,
                    active=r.active&&hostVisible,rendererConfig=FoliaNativeRendererConfig(r.budget),
                    diagnosticsIntervalFrames=1,
                    onRendererStatus={s->
                        status(slot,"slot=$slot ABI=${FoliaNativeRuntime.ABI_VERSION} ${s.actual} ${s.state} ${s.reason.orEmpty()} frame=${s.frameId} media=${r.ms} sampled=${s.sampledMediaTimeUs} uploads=${s.uploadedBytes}")
                        android.util.Log.i("FoliaNativeConsumer","STATE slot=$slot request=${r.backend} scene=${r.scene} media=${r.ms} sampled=${s.sampledMediaTimeUs} path=${s.actual} state=${s.state} frame=${s.frameId} reason=${s.reason}")
                    },
                    onLineClicked={line->status(slot,"CLICK source=${source.lines.any{it===line}} media=${r.ms}")},
                    onLinePressed={line->status(slot,"LONG source=${source.lines.any{it===line}} media=${r.ms}")})}}},
                    modifier=Modifier.drawBehind{drawRect(Color(0xFF162435))}.clickable{status(0,"BLANK media=${r.ms}")}){children,constraints->
                    val measured=children.map{it.measure(Constraints.fixed(r.width/r.instances,r.height))}
                    layout(constraints.maxWidth,constraints.maxHeight){measured.forEachIndexed { index,child->child.place(index*r.width/r.instances,0) }}
                }
            }
        }}
        root.addView(content,FrameLayout.LayoutParams(-1,-1));root.addView(label,FrameLayout.LayoutParams(-1,-2));setContentView(root)
    }
}
