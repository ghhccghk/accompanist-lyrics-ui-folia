package com.mocharealm.accompanist.lyrics.folia

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.sp
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.ceil
import com.mocharealm.accompanist.lyrics.folia.internal.text.cssBaseline
import com.mocharealm.accompanist.lyrics.folia.internal.text.paintedLineBaseline
import kotlin.test.*
import com.mocharealm.accompanist.lyrics.folia.internal.rendering.rasterizeParagraph
import androidx.compose.ui.graphics.ColorFilter
import com.mocharealm.accompanist.lyrics.folia.internal.rendering.bitmapFromArgb
import com.mocharealm.accompanist.lyrics.folia.internal.rendering.blurredMasks
import com.mocharealm.accompanist.lyrics.folia.internal.rendering.compositeGlowMasks
import com.mocharealm.accompanist.lyrics.folia.internal.rendering.drawParagraphShadowSource

/** Isolate font/body paint from timing, opacity, glow and line motion. */
@OptIn(ExperimentalTestApi::class,ExperimentalTextApi::class)
class BodyRasterTest {
    @Test fun shadowMaskDoesNotLeakIntoReusedBodyParagraph() = runDesktopComposeUiTest(width=320,height=320) {
        lateinit var layout:TextLayoutResult
        setContent {
            layout=rememberTextMeasurer().measure("Amber",TextStyle(
                fontFamily=FontFamily(Font(File("../verification/assets/FoliaParityLatin-Bold.ttf"),FontWeight.Bold)),
                fontSize=48.sp),softWrap=false)
        }
        runOnIdle {
            val density=androidx.compose.ui.unit.Density(1f)
            fun pixels()=rasterizeParagraph(layout,density).image.let {bitmap->
                IntArray(bitmap.width*bitmap.height).also {bitmap.readPixels(it)}
            }
            val before=pixels()
            val mask=androidx.compose.ui.graphics.ImageBitmap(320,320)
            androidx.compose.ui.graphics.drawscope.CanvasDrawScope().draw(density,
                androidx.compose.ui.unit.LayoutDirection.Ltr,androidx.compose.ui.graphics.Canvas(mask),
                androidx.compose.ui.geometry.Size(320f,320f)) {
                drawParagraphShadowSource(layout)
            }
            assertContentEquals(before,pixels(),"A reused shaped paragraph must rebuild identical body coverage after painting shadow input")
        }
    }

    @Test fun isolatedBodyPaint() = runDesktopComposeUiTest(width=600,height=400) {
        val output=File("../build/verification/raster-kotlin").apply {mkdirs()}
        val native=org.jetbrains.skia.FontMgr.default.makeFromFile("../verification/assets/FoliaParityLatin-Bold.ttf")!!
        native.use {assertEquals(700,it.fontStyle.weight);assertEquals(5,it.fontStyle.width)}
        val rows=File("../verification/classic-fixtures/body-layout.tsv").readLines().filterNot {it.startsWith("#") || it.isBlank()}.map {it.split('\t')} +
            // Original halo.tsx GlowWord: 64px M, parent y128, painted baseline191.
            listOf(listOf("halo64","FoliaParityLatin-Bold.ttf","64","M","60.359375","78.078125","63"))
        var row by mutableStateOf(rows.first())
        var transformed by mutableStateOf(false)
        var original by mutableStateOf(false)
        var cachedPaint by mutableStateOf(false)
        var shadowPaint by mutableStateOf(false)
        var hinting by mutableStateOf(FontRasterizationSettings.PlatformDefault.hinting)
        var smoothing by mutableStateOf(FontRasterizationSettings.PlatformDefault.smoothing)
        val metrics=StringBuilder("scene\tnaturalBaseline\tnaturalHeight\tcssBaseline\n")
        setContent {
            val measurer=rememberTextMeasurer()
            val file=if(original) "NotoSans-Bold.ttf" else row[1]
            val family=FontFamily(Font(File("../verification/assets/$file"),FontWeight.Bold))
            val style=TextStyle(fontFamily=family,fontWeight=FontWeight.Bold,fontSize=row[2].toFloat().sp,
                platformStyle=PlatformTextStyle(null,PlatformParagraphStyle(FontRasterizationSettings(smoothing,hinting,true,false))))
            val layout=measurer.measure(row[3],style,softWrap=false)
            val width=(ceil(layout.getPathForRange(0,row[3].length).getBounds().width*64)/64)
            val height=row[5].toFloat()
            val baseline=cssBaseline(layout.firstBaseline,layout.multiParagraph.height,height)
            val paintOffset=baseline-paintedLineBaseline(layout,0)
            assertEquals(row[6].toFloat(),baseline,.0001f,"CSS baseline for ${row[0]}")
            val raster=remember(layout) {rasterizeParagraph(layout,androidx.compose.ui.unit.Density(1f))}
            Canvas(androidx.compose.ui.Modifier) {
                withTransform({
                    translate(128f,128f)
                    if(transformed) {rotate(-4f,Offset(width/2,height/2));scale(1.6f,1.6f,Offset(width/2,height/2))}
                }) {
                    if(cachedPaint) drawImage(raster.image,raster.origin+Offset(0f,paintOffset),colorFilter=ColorFilter.tint(Color.White))
                    else if(shadowPaint) withTransform({translate(0f,paintOffset)}) {
                        clipPath(layout.getPathForRange(0,row[3].length)) {
                            drawParagraphShadowSource(layout)
                        }
                    }
                    else drawText(layout,Color.White,Offset(0f,paintOffset),shadow=androidx.compose.ui.graphics.Shadow.None)
                }
            }
            SideEffect {metrics.append("${row[0]}\t${layout.firstBaseline}\t${layout.multiParagraph.height}\t$baseline\n")}
        }
        for(case in rows) for(pose in if(case[0]=="baseline" || case[0]=="halo64") listOf("identity","transform","original","identity-raster","transform-raster","identity-no-hint","transform-no-hint","identity-lcd","transform-lcd","identity-shadow") else listOf("identity","transform","identity-no-hint","transform-no-hint")) {
            runOnIdle {row=case;transformed=pose.startsWith("transform");original=pose=="original";cachedPaint=pose.endsWith("raster")
                shadowPaint=pose.endsWith("shadow")
                hinting=if(pose.endsWith("no-hint")) FontHinting.None else FontRasterizationSettings.PlatformDefault.hinting
                smoothing=if(pose.endsWith("lcd")) FontSmoothing.SubpixelAntiAlias else FontRasterizationSettings.PlatformDefault.smoothing
            };waitForIdle()
            val image=captureToImage().toAwtImage()
            assertEquals(600,image.width)
            assertTrue(image.getRGB(0,0,600,400,null,0,600).any{it and 0xffffff != 0})
            val name=if(case[0]=="baseline") pose else "${case[0]}-$pose"
            ImageIO.write(image,"png",output.resolve("$name.png"))
            if(case[0]=="halo64" && pose in listOf("identity","identity-shadow","identity-no-hint","identity-lcd")) {
                val pixels=image.getRGB(0,0,320,320,null,0,320)
                val source=bitmapFromArgb(320,320,pixels)
                val halo=compositeGlowMasks(blurredMasks(source,intArrayOf(10,20)))
                ImageIO.write(halo.toAwtImage(),"png",output.resolve("$name-halo.png"))
            }
        }
        output.resolve("metrics.tsv").writeText(metrics.toString())
    }
}
