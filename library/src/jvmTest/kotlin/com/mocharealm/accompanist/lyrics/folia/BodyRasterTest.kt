package com.mocharealm.accompanist.lyrics.folia

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.drawscope.withTransform
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

/** Isolate font/body paint from timing, opacity, glow and line motion. */
@OptIn(ExperimentalTestApi::class)
class BodyRasterTest {
    @Test fun isolatedBodyPaint() = runDesktopComposeUiTest(width=600,height=400) {
        val output=File("../build/verification/raster-kotlin").apply {mkdirs()}
        val native=org.jetbrains.skia.FontMgr.default.makeFromFile("../verification/assets/FoliaParityLatin-Bold.ttf")!!
        native.use {assertEquals(700,it.fontStyle.weight);assertEquals(5,it.fontStyle.width)}
        val rows=File("../verification/classic-fixtures/body-layout.tsv").readLines().filterNot {it.startsWith("#") || it.isBlank()}.map {it.split('\t')}
        var row by mutableStateOf(rows.first())
        var transformed by mutableStateOf(false)
        var original by mutableStateOf(false)
        var cachedPaint by mutableStateOf(false)
        val metrics=StringBuilder("scene\tnaturalBaseline\tnaturalHeight\tcssBaseline\n")
        setContent {
            val measurer=rememberTextMeasurer()
            val file=if(original) "NotoSans-Bold.ttf" else row[1]
            val family=FontFamily(Font(File("../verification/assets/$file"),FontWeight.Bold))
            val style=TextStyle(fontFamily=family,fontWeight=FontWeight.Bold,fontSize=row[2].toFloat().sp)
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
                    else drawText(layout,Color.White,Offset(0f,paintOffset))
                }
            }
            SideEffect {metrics.append("${row[0]}\t${layout.firstBaseline}\t${layout.multiParagraph.height}\t$baseline\n")}
        }
        for(case in rows) for(pose in if(case[0]=="baseline") listOf("identity","transform","original","identity-raster","transform-raster") else listOf("identity","transform")) {
            runOnIdle {row=case;transformed=pose.startsWith("transform");original=pose=="original";cachedPaint=pose.endsWith("raster")};waitForIdle()
            val image=captureToImage().toAwtImage()
            assertEquals(600,image.width)
            assertTrue(image.getRGB(0,0,600,400,null,0,600).any{it and 0xffffff != 0})
            val name=if(case[0]=="baseline") pose else "${case[0]}-$pose"
            ImageIO.write(image,"png",output.resolve("$name.png"))
        }
        output.resolve("metrics.tsv").writeText(metrics.toString())
    }
}
