// SPDX-License-Identifier: AGPL-3.0-only
package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.*
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama.*
import top.ghhccghk.accompanist.lyrics.folia.internal.text.classicBodyRasterStyle
import java.io.File
import java.util.Base64
import javax.imageio.ImageIO
import org.jetbrains.skia.Image
import kotlin.test.*

/** Four-way source/native INPUT isolation only. Original rasters/placement
 * never enter production. Fixed original base800/1100ms, no new source run. */
@OptIn(ExperimentalTestApi::class)
class DioramaCjkInputIsolationTest {
    @Test fun fixedBaseTextureAndPlacementCross()=runDesktopComposeUiTest(width=800,height=600) {
        val name="cjk-800-1100.png"
        val r=File("../verification/diorama-fixtures/active-base.tsv").readLines()
            .filterNot{it.startsWith('#')}.map{it.split('\t')}.single{it[0]==name}
        val source=File("../verification/diorama-fixtures/active-base-sampler-input-v1/source.tsv").readLines()
            .filterNot{it.startsWith('#')}.map{it.split('\t')}.filter{it[0]==name}
        val images=source.map { u -> Image.makeFromEncoded(File("../build/verification/diorama-active-layers-browser/cjk-unit${u[2]}-base.png").readBytes()).toComposeImageBitmap() }
        val shared=images.map{SkiaDioramaPlainSampler(it)}
        val own=mutableListOf<SkiaDioramaPlainSampler>()
        val foreground=mutableListOf<SkiaDioramaPlainSampler>()
        var mode by mutableStateOf("own-own");var prepared:DioramaActiveText?=null
        val frame=buildDioramaPath(12,dioramaHashSeed("DioramaProbe"))[3]
        val placement=dioramaTextPlacement(3,dioramaHashSeed("DioramaProbe"))
        val out=File("../build/verification/diorama-cjk-input-kotlin").apply{mkdirs()}
        fun decode(s:String)=String(Base64.getDecoder().decode(s),Charsets.UTF_8)
        try {
            setContent {
                val resolver=LocalFontFamilyResolver.current
                val stage=remember {
                    val words=r[5].split(';').map{it.split(',').let{DioramaTimedText(decode(it[0]),it[1].toDouble(),it[2].toDouble())}}
                    val style=TextStyle(fontFamily=FontFamily(Font(File("../verification/assets/FoliaParityChatCJK-Bold.otf"),FontWeight.Bold)))
                    val measurer=TextMeasurer(resolver,Density(1f),LayoutDirection.Ltr)
                    val line=DioramaLyric(decode(r[4]),.7,words.last().end,words)
                    val text=DioramaActiveText.prepare(line,style,measurer,resolver)
                    val f=DioramaActiveText.prepare(line,classicBodyRasterStyle(style),measurer,resolver)
                    for((i,s)in text.slices.withIndex()) {
                        own+=SkiaDioramaPlainSampler(s.image);foreground+=SkiaDioramaPlainSampler(f.slices[i].image)
                        assertEquals(s.unit,f.slices[i].unit);assertEquals(s.centerPx,f.slices[i].centerPx)
                        assertEquals(s.imageOffsetPx,f.slices[i].imageOffsetPx)
                        ImageIO.write(s.image.toAwtImage(),"png",out.resolve("own-unit-$i.png"))
                        ImageIO.write(f.slices[i].image.toAwtImage(),"png",out.resolve("foreground-unit-$i.png"))
                    }
                    text
                }
                SideEffect{prepared=stage}
                Canvas(Modifier.fillMaxSize().background(Color(0xff162435))) {
                    fun vec(i:Int)=DioramaVec(r[i].toDouble(),r[i+1].toDouble(),r[i+2].toDouble())
                    val view=DioramaPerspective(vec(9),vec(12),800.0,600.0)
                    val scale=dioramaFrameFit(stage.lineAdvance*.62/128,5.2,view.fov,800.0/600)*placement.scale*.62/128
                    val x=dioramaPlaneAxis(frame,placement,1.0,0.0)*scale
                    val y=dioramaPlaneAxis(frame,placement,0.0,-1.0)*scale
                    val center=dioramaComposeLocal(frame,placement.offsetR,placement.offsetU,0.0)
                    for((i,slice)in stage.slices.withIndex()) {
                        val input=source[i];val textureSource=mode.startsWith("source-")
                        val image=if(textureSource)images[i]else slice.image
                        val sampler=when{textureSource->shared[i];mode.startsWith("foreground-")->foreground[i];else->own[i]}
                        val pivot=if(mode.endsWith("-source"))input[3].toDouble()*128/.62 else slice.centerPx+slice.imageOffsetPx
                        sampler.draw(this,view,center+x*(pivot-image.width/2.0)-y*(image.height/2.0),x,y,
                            DioramaLinearColor(input[6].toDouble(),input[7].toDouble(),input[8].toDouble()),input[9].toFloat(),null,DioramaToneMapping.None)
                    }
                }
            }
            fun pixels():IntArray{waitForIdle();val image=captureToImage();return IntArray(image.width*image.height).also{image.readPixels(it)}}
            val hashes=mutableMapOf<String,IntArray>()
            for(m in listOf("own-own","source-own","own-source","source-source","foreground-own")) {
                runOnIdle{mode=m};hashes[m]=pixels()
                if(m=="own-own") {
                    val existing=ImageIO.read(File("../build/verification/diorama-active-base-sampler-kotlin/$name"))
                    val expected=IntArray(existing.width*existing.height).also{existing.getRGB(0,0,existing.width,existing.height,it,0,existing.width)}
                    assertContentEquals(expected,hashes[m],"Unmodified own input reproduces actual base sampler exactly")
                }
                ImageIO.write(captureToImage().toAwtImage(),"png",out.resolve("$m.png"))
            }
            for(m in hashes.keys.reversed()){runOnIdle{mode=m};assertContentEquals(hashes[m],pixels(),"Cross-input round trip $m")}
            mainClock.advanceTimeBy(1000);assertContentEquals(hashes[mode],pixels())
            out.resolve("inputs.tsv").writeText("# unit ownPivot ownImageOffset ownWidth sourcePivot sourceWidth\n"+
                prepared!!.slices.mapIndexed{i,s->listOf(i,s.centerPx,s.imageOffsetPx,s.image.width,
                    source[i][3].toDouble()*128/.62,source[i][4].toDouble()*128/.62).joinToString("\t")}.joinToString("\n")+"\n")
        }finally{(shared+own+foreground).forEach{it.close()};images.forEach{it.asSkiaBitmap().close()}}
    }
}
