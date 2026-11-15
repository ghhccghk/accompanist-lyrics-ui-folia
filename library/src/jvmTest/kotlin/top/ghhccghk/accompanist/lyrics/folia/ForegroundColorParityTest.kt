package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import top.ghhccghk.accompanist.lyrics.folia.internal.text.*
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.*

/** Same intact paragraphs/ink/surfaces as original GlowWord's coverage-colors grid. */
@OptIn(ExperimentalTestApi::class)
class ForegroundColorParityTest {
    @Test fun isolateNativeColorFromWhiteMaskTint() = runDesktopComposeUiTest(width=512,height=320) {
        data class Probe(val name:String,val asset:String,val text:String,val size:Float)
        val fonts=listOf(Triple("latin","FoliaParityLatin-Bold.ttf","Synthetic"),
            Triple("arabic","NotoSansArabic-Bold.ttf","مرحبا"),Triple("cjk","FoliaParityCJK-Bold.otf","合成歌词"))
        val probes=fonts.flatMap { (name,asset,text)->listOf(19.74f,48f).map {Probe(name,asset,text,it)} }
        var requested by mutableStateOf(probes.first())
        lateinit var layout:TextLayoutResult
        var baseline=0f
        setContent {
            val family=remember(requested.asset) {FontFamily(Font(File("../verification/assets/${requested.asset}"),FontWeight.Bold))}
            val style=classicBodyRasterStyle(TextStyle(fontFamily=family,fontWeight=FontWeight.Bold,
                fontSize=requested.size.sp,textMotion=TextMotion.Animated,textDirection=TextDirection.Ltr,textAlign=TextAlign.Left))
            val measured=rememberTextMeasurer().measure(requested.text,style,softWrap=false)
            val metrics=naturalFontMetrics(style,androidx.compose.ui.platform.LocalFontFamilyResolver.current,Density(1f))
            SideEffect {layout=measured;baseline=paintedNaturalBaseline(measured,metrics)}
        }
        val output=File("../build/verification/foreground-coverage-colors-jvm").apply {mkdirs()}
        val rows=mutableListOf("font,size,ink,surface,path,file,against_mode,against_max_delta")
        val density=Density(1f)
        fun raster(ink:Color,surface:Color?=null):ImageBitmap=ImageBitmap(512,320).also {bitmap->
            CanvasDrawScope().draw(density,LayoutDirection.Ltr,Canvas(bitmap),Size(512f,320f)) {
                if(surface!=null)drawRect(surface)
                drawText(layout,ink,Offset(128f,191f-baseline),shadow=Shadow.None)
            }
        }
        for(probe in probes) {
            runOnIdle {requested=probe};waitForIdle()
            runOnIdle {
                for(ink in listOf(0xffffff,0x808080,0xf2b45d)) for(surface in listOf(0x000000,0xe8edf2)) {
                    fun color(rgb:Int)=Color(0xff000000L or rgb.toLong())
                    val foreground=color(ink);val background=color(surface)
                    val prefix="${probe.name}-${probe.size.toString().removeSuffix(".0")}-${ink.toString(16).padStart(6,'0')}-${surface.toString(16).padStart(6,'0')}"
                    val direct=raster(foreground,background)
                    val pixels=IntArray(512*320).also {direct.readPixels(it)}
                    for(mode in listOf("direct","cached","tinted")) {
                        val image=if(mode=="direct")direct else {
                            val source=raster(if(mode=="tinted")Color.White else foreground)
                            ImageBitmap(512,320).also {bitmap->
                                CanvasDrawScope().draw(density,LayoutDirection.Ltr,Canvas(bitmap),Size(512f,320f)) {
                                    drawRect(background)
                                    drawImage(source,colorFilter=if(mode=="tinted")ColorFilter.tint(foreground) else null)
                                }
                            }
                        }
                        val actual=IntArray(pixels.size).also {image.readPixels(it)}
                        val max=pixels.indices.maxOf {i->listOf(0,8,16).maxOf {shift->
                            kotlin.math.abs(((pixels[i] ushr shift) and 255)-((actual[i] ushr shift) and 255))} }
                        val file="$prefix-$mode.png"
                        ImageIO.write(image.toAwtImage(),"png",output.resolve(file))
                        rows+="${probe.name},${probe.size},${ink.toString(16)},${surface.toString(16)},$mode,$file,direct,$max"
                        assertTrue(actual.any {it!=color(surface).toArgb()},"Empty control $file")
                    }
                    val repeated=IntArray(pixels.size).also {raster(foreground,background).readPixels(it)}
                    assertContentEquals(pixels,repeated,"Repeat must be deterministic: $prefix")
                }
            }
        }
        output.resolve("paths.csv").writeText(rows.joinToString("\n")+"\n")
        val digester=java.security.MessageDigest.getInstance("SHA-256")
        output.resolve("conditions.txt").writeText(buildString {
            appendLine("Static offscreen CanvasDrawScope; 512x320 density/fontScale=1; x128 baseline191; LTR host; intact shaping")
            appendLine("Style=classicBodyRasterStyle with TextMotion.Animated; no motion, opacity, glow, or blur")
            appendLine("OS=${System.getProperty("os.name")} ${System.getProperty("os.version")}; JVM=${System.getProperty("java.version")}")
            for(asset in fonts.map {it.second}) {
                appendLine("$asset SHA256="+digester.digest(File("../verification/assets/$asset").readBytes()).joinToString("") {"%02x".format(it)})
            }
        })
    }
}
