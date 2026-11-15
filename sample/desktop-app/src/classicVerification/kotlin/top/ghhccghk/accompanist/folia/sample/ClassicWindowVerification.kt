package top.ghhccghk.accompanist.folia.sample

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.*
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import top.ghhccghk.accompanist.lyrics.folia.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicReference
import javax.imageio.ImageIO
import kotlin.math.roundToInt

private data class WindowFrame(val width: Int = 800, val avoid: Boolean = false, val ms: Long = 750, val id: Long = 0, val repeatText:Int=1)
private data class DrawnWindow(val id: Long, val width: Int, val height: Int)

/** Native window/render-backend integration. Recorded Compose layer != OS/window screenshot.
 * No OS automation/input, hidden settings changes, audio, system fonts or upstream runtime dependency. */
fun main(args:Array<String>) {
    val subtitle="--subtitle" in args
    var failure: Throwable? = null
    var completed = false
    application(exitProcessOnExit = false) {
        var request by remember { mutableStateOf(WindowFrame()) }
        val state = rememberWindowState(width = 800.dp, height = 600.dp)
        Window(onCloseRequest = ::exitApplication, state = state, undecorated = true,
            title = "Folia classic native-window verification") {
            val nativeDensity = LocalDensity.current
            val drawn = remember { AtomicReference<DrawnWindow?>(null) }
            val layer = rememberGraphicsLayer()
            val fontFile = remember { File("verification/assets/${if(subtitle) "FoliaParitySubtitle-Bold.otf" else "FoliaParityLatin-Bold.ttf"}") }
            val font = remember { FontFamily(Font(fontFile, FontWeight.Bold)) }
            val lyrics = remember(request.repeatText) { if(subtitle) SyncedLyrics(
                File("verification/classic-fixtures/subtitle-cjk-scenes.tsv").readLines()
                    .filterNot {it.isBlank()||it.startsWith('#')}.map {row->
                        val c=row.split('\t');SyncedLine(c[2].repeat(request.repeatText),c[3],c[0].toInt(),c[1].toInt())
                    }) else SyncedLyrics(listOf(KaraokeLine.MainKaraokeLine(listOf(
                KaraokeSyllable("Amber ",700,1300), KaraokeSyllable("echo",1300,1900)),
                null,KaraokeAlignment.Unspecified,700,1900))) }
            CompositionLocalProvider(LocalDensity provides Density(1f,1f)) {
                FoliaLyricsView(lyrics, { FoliaPlaybackSnapshot(request.ms,false,request.id) },
                    Modifier.fillMaxSize().drawWithContent {
                        layer.record {
                            drawRect(Color(0xFF162435))
                            this@drawWithContent.drawContent()
                        }
                        drawLayer(layer)
                        val latest=DrawnWindow(request.id,size.width.roundToInt(),size.height.roundToInt())
                        if (drawn.get()?.id != latest.id) println("NATIVE_DRAW: $latest awt=${window.size} density=$nativeDensity backend=${window.renderApi}")
                        drawn.set(latest)
                    },
                    typography=FoliaTypography(secondary=TextStyle(fontFamily=font,fontWeight=FontWeight.Bold),
                        subtitle=TextStyle(fontFamily=font,fontWeight=FontWeight.Bold)),
                    colors=FoliaColors(text=if(subtitle) Color.Transparent else FoliaColors().text,
                        active=if(subtitle) Color.Transparent else Color.White),
                    options=FoliaOptions(showTranslation=subtitle,showPhonetic=false,showNextLine=subtitle),
                    style=FoliaStyle.Classic(ClassicTuning(breathingFloatMultiplier=0f,avoidOverlap=request.avoid)))
            }
            LaunchedEffect(Unit) {
                val out=File("build/verification/${if(subtitle) "subtitle" else "classic"}-native-window").apply {mkdirs()}
                out.resolve("SUCCESS").delete()
                val manifest=out.resolve("frames.tsv")
                manifest.writeText("width\tmedia_ms\tavoid\trender_api\tawt_width\tawt_height\tplatform_density\tpaused_identical\trevisit\tsha256\trepeat_text\n")
                try {
                    withTimeout(180_000) {
                        withTimeout(15_000) { while (!window.isShowing) withFrameNanos {} }
                        // Let initial AWT size notifications finish before requesting the verification viewport.
                        delay(100)
                        var token=0L
                        val observed=mutableMapOf<String,IntArray>()
                        for(width in listOf(800,380,800)) for(variant in listOf(false,true)) {
                            val avoid=variant&&!subtitle
                            val repeatText=if(subtitle&&variant) 2 else 1
                            val times=if(subtitle) listOf(1500L,3500L,5500L) else listOf(750L,1000L,1500L,1900L,2100L)
                            for(ms in times+times.reversed()) {
                                state.size = DpSize((width / nativeDensity.density).dp, (600 / nativeDensity.density).dp)
                                request=WindowFrame(width,avoid,ms,++token,repeatText)
                                withTimeout(15_000) {
                                    while(drawn.get()!=DrawnWindow(token,width,600)) withFrameNanos {}
                                }
                                check(window.isShowing) {"Native window is not showing"}
                                val image=layer.toImageBitmap()
                                check(image.width==width && image.height==600) {"Wrong viewport: ${image.width}x${image.height}"}
                                val pixels=IntArray(width*600).also {image.readPixels(it)}
                                delay(1000)
                                val repeat=IntArray(pixels.size).also {layer.toImageBitmap().readPixels(it)}
                                check(pixels.contentEquals(repeat)) {"Native-layer pause drift at $width/$avoid/$ms"}
                                val key="$width-$avoid-$ms-$repeatText"
                                observed[key]?.let {check(it.contentEquals(pixels)) {"Native-layer reverse seek/resize drift at $width/$avoid/$ms"}}
                                val revisited=key in observed
                                observed[key]=pixels
                                val digest=MessageDigest.getInstance("SHA-256").digest(image.toAwtImage().let { bitmap ->
                                    java.io.ByteArrayOutputStream().use {bytes -> ImageIO.write(bitmap,"png",bytes);bytes.toByteArray()}
                                }).joinToString("") {"%02x".format(it)}
                                ImageIO.write(image.toAwtImage(),"png",out.resolve("$width-${if(subtitle) "repeat$repeatText" else if(avoid) "avoid" else "default"}-${ms}ms.png"))
                                manifest.appendText(listOf(width,ms,avoid,window.renderApi,window.width,window.height,
                                    nativeDensity.density,true,revisited,digest,repeatText).joinToString("\t")+"\n")
                            }
                        }
                    }
                    val fontHash=MessageDigest.getInstance("SHA-256").digest(fontFile.readBytes()).joinToString("") {"%02x".format(it)}
                    out.resolve("conditions.txt").writeText("Native ComposeWindow, undecorated, real AWT resize800->380->800; public FoliaLyricsView; fixed synthetic ${if(subtitle) "CJK translation+next, repeat1/2" else "baseline"}; density/fontScale1; fontSHA=$fontHash; actual backend per frames.tsv. Capture is GraphicsLayer.toImageBitmap after native draw, NOT an OS screenshot or native mouse/keyboard test. No performance claim.\n")
                    out.resolve("runtime.txt").writeText("OS=${System.getProperty("os.name")} ${System.getProperty("os.version")} ${System.getProperty("os.arch")}\nJava=${System.getProperty("java.version")}\n")
                    val sources = listOf("library/src/commonMain", "library/src/jvmMain",
                        "sample/desktop-app/src/classicVerification").flatMap { path ->
                        File(path).walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
                    } + listOf(File("sample/desktop-app/build.gradle.kts"), File("gradle/libs.versions.toml"))
                    out.resolve("source-sha256.tsv").writeText("path\tsha256\n" + sources.sortedBy { it.invariantSeparatorsPath }.joinToString("\n") {
                        val hash = MessageDigest.getInstance("SHA-256").digest(it.readBytes()).joinToString("") { b -> "%02x".format(b) }
                        "${it.invariantSeparatorsPath}\t$hash"
                    } + "\n")
                    out.resolve("SUCCESS").writeText("Native window creation/draw/resize; ${if(subtitle) 36 else 60} paused requests; exact reverse reconstruction.\n")
                    completed = true
                    println("FOLIA_NATIVE_WINDOW_VERIFIED: ${out.path}; renderApi=${window.renderApi}")
                } catch(t:Throwable) {
                    failure=t
                    System.err.println("FOLIA_NATIVE_WINDOW_FAILED: $t; request=$request; drawn=${drawn.get()}; awt=${window.size}; density=$nativeDensity; backend=${window.renderApi}")
                } finally { exitApplication() }
            }
        }
    }
    failure?.let {throw IllegalStateException("Native-window verification failed",it)}
    check(completed) { "Native-window verification was closed before completion" }
}
