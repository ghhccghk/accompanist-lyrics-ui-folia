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
import kotlin.test.*

/** Isolate font/body paint from timing, opacity, glow and line motion. */
@OptIn(ExperimentalTestApi::class)
class BodyRasterTest {
    @Test fun isolatedBodyPaint() = runDesktopComposeUiTest(width=600,height=400) {
        val output=File("../build/verification/raster-kotlin").apply {mkdirs()}
        val native=org.jetbrains.skia.FontMgr.default.makeFromFile("../verification/assets/FoliaParityLatin-Bold.ttf")!!
        native.use {assertEquals(700,it.fontStyle.weight);assertEquals(5,it.fontStyle.width)}
        var transformed by mutableStateOf(false)
        var original by mutableStateOf(false)
        setContent {
            val measurer=rememberTextMeasurer()
            val file=if(original) "NotoSans-Bold.ttf" else "FoliaParityLatin-Bold.ttf"
            val family=FontFamily(Font(File("../verification/assets/$file"),FontWeight.Bold))
            val layout=measurer.measure("Amber",TextStyle(fontFamily=family,fontWeight=FontWeight.Bold,fontSize=48.sp,lineHeight=58.56.sp),softWrap=false)
            val natural=measurer.measure("Amber",TextStyle(fontFamily=family,fontWeight=FontWeight.Bold,fontSize=48.sp),softWrap=false)
            val width=(ceil(layout.getPathForRange(0,5).getBounds().width*64)/64)
            Canvas(androidx.compose.ui.Modifier) {
                withTransform({
                    translate(128f,128f)
                    if(transformed) {rotate(-4f,Offset(width/2,58.5625f/2));scale(1.6f,1.6f,Offset(width/2,58.5625f/2))}
                }) {drawText(layout,Color.White,Offset(0f,(58.5625f-layout.multiParagraph.height)/2))}
            }
            SideEffect {output.resolve("metrics.txt").writeText("width=$width paragraphHeight=${layout.multiParagraph.height} firstBaseline=${layout.firstBaseline} size=${layout.size} naturalHeight=${natural.multiParagraph.height} naturalBaseline=${natural.firstBaseline}\n")}
        }
        for(pose in listOf("identity","transform","original")) {
            runOnIdle {transformed=pose=="transform";original=pose=="original"};waitForIdle()
            val image=captureToImage().toAwtImage()
            assertEquals(600,image.width)
            assertTrue(image.getRGB(0,0,600,400,null,0,600).any{it and 0xffffff != 0})
            ImageIO.write(image,"png",output.resolve("$pose.png"))
        }
    }
}
