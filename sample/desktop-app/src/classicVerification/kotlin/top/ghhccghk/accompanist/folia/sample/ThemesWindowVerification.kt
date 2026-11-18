package top.ghhccghk.accompanist.folia.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import top.ghhccghk.accompanist.lyrics.folia.*
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicLong
import javax.imageio.ImageIO

private data class ThemeWindowRequest(
    val style: FoliaStyle = FoliaStyle.Classic(), val millis: Long = 1600,
    val width: Int = 800, val height: Int = 600, val fontScale: Float = 1f,
    val options: FoliaOptions = FoliaOptions(), val attached: Boolean = true,
    val replacement: Boolean = false, val token: Long = 0,
)

/** Public renderer through a real visible ComposeWindow. No OS input injection,
 * user files, audio/device access or settings changes. Recorded layer != OS screenshot. */
fun themesWindowVerificationMain() {
    var failure: Throwable? = null
    var completed = false
    application(exitProcessOnExit = false) {
        val state = rememberWindowState(width = 800.dp, height = 600.dp)
        var request by remember { mutableStateOf(ThemeWindowRequest()) }
        Window(onCloseRequest = ::exitApplication, state = state, undecorated = true, focusable = false,
            title = "Folia seven-theme verification") {
            val platformDensity = LocalDensity.current
            val layer = rememberGraphicsLayer()
            val drawn = remember { AtomicLong(-1) }
            var status by remember { mutableStateOf<FoliaStatus?>(null) }
            val fonts = remember { FontFamily(Font(File("verification/assets/FoliaParityLatin-Bold.ttf"), FontWeight.Bold)) }
            val lyrics = remember(request.replacement) {
                val text = if (request.replacement) listOf("Other ", "synthetic ", "verse") else listOf("Amber ", "echoes ", "beyond")
                SyncedLyrics((0..2).map { i ->
                    val start = 700 + i * 2900
                    KaraokeLine.MainKaraokeLine(text.mapIndexed { j, word -> KaraokeSyllable(word, start+j*700, start+(j+1)*700) },
                        "A synthetic translation", KaraokeAlignment.Unspecified, start, start+2100, "A synthetic phonetic line")
                })
            }
            CompositionLocalProvider(LocalDensity provides Density(1f, request.fontScale)) {
                Box(Modifier.fillMaxSize().background(Color(0xff162435)).drawWithContent {
                    layer.record { drawRect(Color(0xff162435)); this@drawWithContent.drawContent() }
                    drawLayer(layer)
                    drawn.set(request.token)
                }) {
                    if (request.attached) FoliaLyricsView(lyrics,
                        { FoliaPlaybackSnapshot(request.millis, false, request.token) }, Modifier.fillMaxSize(),
                        typography = FoliaTypography(main = TextStyle(fontFamily=fonts, fontWeight=FontWeight.Bold),
                            secondary=TextStyle(fontFamily=fonts), subtitle=TextStyle(fontFamily=fonts)),
                        style=request.style, options=request.options, onStatus={status=it})
                }
            }
            LaunchedEffect(Unit) {
                val out = File("build/verification/themes-native-window").apply { mkdirs() }
                out.resolve("SUCCESS").delete()
                val manifest = out.resolve("checks.tsv")
                manifest.writeText("theme\tcheck\tmedia_ms\tviewport\tfont_scale\trender_api\tsha256\n")
                try {
                    withTimeout(240_000) {
                        while (!window.isShowing) withFrameNanos { }
                        delay(100)
                        var serial = 0L
                        suspend fun capture(next: ThemeWindowRequest, label: String): IntArray {
                            // Async Fume preparation belongs to its input epoch.
                            // A previous viewport's ready count cannot certify a
                            // new viewport which still draws the preparing label.
                            val fumeReprepare = next.style is FoliaStyle.Fume &&
                                (!request.attached || request.style !is FoliaStyle.Fume ||
                                    request.width!=next.width || request.height!=next.height ||
                                    request.fontScale!=next.fontScale || request.replacement!=next.replacement ||
                                    request.options.reducedMotion!=next.options.reducedMotion)
                            if (fumeReprepare) status=null
                            request = next.copy(token=++serial)
                            state.size = DpSize((next.width/platformDensity.density).dp, (next.height/platformDensity.density).dp)
                            repeat(4) { withFrameNanos { } }
                            withTimeout(30_000) {
                                while (drawn.get()!=serial || layer.size.width!=next.width || layer.size.height!=next.height ||
                                    next.attached && ((status?.mainUnitCount?:0)==0 ||
                                        status?.diagnostics?.contains("FumePreparing")==true)) withFrameNanos { }
                            }
                            check(window.isShowing) { "Window closed before $label" }
                            val bitmap = layer.toImageBitmap()
                            val pixels = IntArray(bitmap.width*bitmap.height).also {bitmap.readPixels(it)}
                            if (next.attached) check(pixels.any {it!=pixels[0]}) { "Ready renderer is blank: ${next.style}/$label" }
                            delay(120)
                            val repeat = layer.toImageBitmap()
                            check(bitmap.width==repeat.width && bitmap.height==repeat.height)
                            val paused = IntArray(pixels.size).also {repeat.readPixels(it)}
                            check(pixels.contentEquals(paused)) { "Paused wall-time drift: ${next.style}/$label" }
                            val image = bitmap.toAwtImage()
                            val bytes = java.io.ByteArrayOutputStream().use { b -> ImageIO.write(image,"png",b);b.toByteArray() }
                            val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") {"%02x".format(it)}
                            val name = next.style.javaClass.simpleName
                            if (label in listOf("baseline", "narrow", "large-font", "reduced"))
                                ImageIO.write(image,"png",out.resolve("$name-$label.png"))
                            manifest.appendText(listOf(name,label,next.millis,"${bitmap.width}x${bitmap.height}",next.fontScale,window.renderApi,hash).joinToString("\t")+"\n")
                            println("THEME_WINDOW_CHECK: $name/$label; ${bitmap.width}x${bitmap.height}; ${window.renderApi}")
                            return pixels
                        }
                        val styles = listOf(FoliaStyle.Classic(), FoliaStyle.Partita(), FoliaStyle.Tilt(), FoliaStyle.Cadenza(),
                            FoliaStyle.Pendolo(), FoliaStyle.Cappella(), FoliaStyle.Fume())
                        val baselines = mutableMapOf<FoliaStyle,IntArray>()
                        for (style in styles) {
                            val base = ThemeWindowRequest(style=style)
                            val pixels = capture(base,"baseline"); baselines[style]=pixels
                            fun equal(actual: IntArray, label: String) = check(pixels.contentEquals(actual)) { "Reconstruction mismatch: $style/$label" }
                            capture(base.copy(millis=4400),"forward-seek")
                            equal(capture(base,"reverse-seek"),"reverse-seek")
                            capture(base.copy(width=380,height=740),"narrow")
                            equal(capture(base,"resize-restore"),"resize-restore")
                            capture(base.copy(fontScale=1.4f),"large-font")
                            equal(capture(base,"font-restore"),"font-restore")
                            capture(base.copy(options=base.options.copy(reducedMotion=true,quality=FoliaQuality.Minimal)),"reduced")
                            equal(capture(base,"settings-restore"),"settings-restore")
                            equal(capture(base.copy(millis=1725, options=base.options.copy(offsetMillis=125)),"offset"),"offset")
                            capture(base.copy(replacement=true),"replace-song")
                            equal(capture(base,"song-restore"),"song-restore")
                            val detached = capture(base.copy(attached=false),"detach")
                            check(detached.all {it==detached[0]}) { "Detached renderer left pixels: $style" }
                            equal(capture(base,"remount"),"remount")
                        }
                        for (style in styles.reversed())
                            check(baselines.getValue(style).contentEquals(capture(ThemeWindowRequest(style=style),"theme-revisit"))) { "Theme revisit: $style" }
                        val fontHash = MessageDigest.getInstance("SHA-256").digest(File("verification/assets/FoliaParityLatin-Bold.ttf").readBytes()).joinToString("") {"%02x".format(it)}
                        out.resolve("conditions.txt").writeText("Real visible undecorated ComposeWindow; seven public themes; actual published core model; synthetic input; fontSHA256=$fontHash; density1; fontScale1/1.4;800x600<->380x740.\nBackend=${window.renderApi};OS=${System.getProperty("os.name")};Java=${System.getProperty("java.version")};platformDensity=$platformDensity\nRecorded GraphicsLayer after window draw, NOT OS screenshot, file-picker/audio/UI-input or performance validation. Pause equality is native self-determinism, NOT upstream parity.\n")
                        out.resolve("SUCCESS").writeText("Seven public themes: native draw, pause, seeks, resize, font/settings/offset/song restore, detach/remount, theme revisit.\n")
                        completed=true
                    }
                } catch (t:Throwable) {
                    failure=t
                    out.resolve("failure.txt").writeText(t.stackTraceToString()+"\nrequest=$request\nstatus=$status\n")
                } finally { exitApplication() }
            }
        }
    }
    failure?.let { throw IllegalStateException("Seven-theme native-window verification failed",it) }
    check(completed) { "Verification window was closed before completion" }
}

/** Separate JVM entry point; not packaged into the sample application. */
object ThemesWindowVerification {
    @JvmStatic fun main(args: Array<String>) = themesWindowVerificationMain()
}
