// SPDX-License-Identifier: AGPL-3.0-only
package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
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
import java.io.File
import java.util.Base64
import javax.imageio.ImageIO
import kotlin.math.*
import kotlin.test.*

/** Exact existing24 original plane conditions; own complete-shaped input.
 * TEST ONLY: original isolated Three stage uses no ACES/fog, unlike mounted.
 * Matrices/targets are independently derived from source input seed/offsets. */
@OptIn(ExperimentalTestApi::class)
class DioramaPlainFragmentMatrixTest {
    @Test fun narrow()=capture(380)
    @Test fun wide()=capture(800)
    @Test fun fractional()=capture(801)

    private fun capture(width:Int)=runDesktopComposeUiTest(width=width,height=600) {
        val rows=File("../verification/diorama-fixtures/text-planes.tsv").readLines()
            .filterNot{it.startsWith('#')||it.isBlank()}.map{it.split('\t')}.filter{it[2].toInt()==width}
        val path=buildDioramaPath(12,dioramaHashSeed("DioramaProbe"))
        val placement=dioramaTextPlacement(3,dioramaHashSeed("DioramaProbe"))
        val controls=mutableListOf<DioramaFragmentControl>()
        var current by mutableIntStateOf(0)
        var mounted by mutableStateOf(true)
        var viewportWidth by mutableIntStateOf(width)
        var useSampler by mutableStateOf(false)
        val renderer=DioramaPlainRenderer()
        var prepared:DioramaPlainText?=null
        var builds=0
        val out=File("../build/verification/diorama-plain-fragment-kotlin").apply{mkdirs()}
        try {
            setContent {
                val row=rows[current]
                val resolver=LocalFontFamilyResolver.current
                val raster=if(mounted)remember(row[1]) {
                    val font=when(row[1]) {
                        "arabic"->"FoliaParityBidi-Bold.ttf"
                        "cjk"->"FoliaParityChatCJK-Bold.otf"
                        else->"FoliaParityLatin-Bold.ttf"
                    }
                    val style=TextStyle(fontFamily=FontFamily(Font(File("../verification/assets/$font"),FontWeight.Bold)))
                    builds++
                    DioramaPlainText.prepare(String(Base64.getDecoder().decode(row[5]),Charsets.UTF_8),style,
                        TextMeasurer(resolver,Density(1f),LayoutDirection.Ltr),resolver)
                }else null
                val control=if(raster!=null)remember(raster) {
                    // Full chain, including non-square/non-power-of-two extents;
                    // prepared once per paragraph, never at a camera query.
                    DioramaFragmentControl(raster.image,floor(log2(max(raster.image.width,raster.image.height).toDouble())).toInt())
                        .also{controls+=it}
                }else null
                DisposableEffect(control){onDispose{control?.close();controls.remove(control)}}
                SideEffect{prepared=raster;renderer.prepare(if(raster==null)emptyList()else listOf(raster))}
                Canvas(Modifier.fillMaxHeight().width(viewportWidth.dp).background(Color(0xff162435))) {
                    if(raster!=null&&control!=null) {
                        val eye=dioramaComposeLocal(path[3],placement.offsetR+row[3].toDouble(),placement.offsetU+row[4].toDouble(),-5.2)
                        val look=dioramaComposeLocal(path[3],placement.offsetR+placement.lookR,placement.offsetU,0.0)
                        val view=DioramaPerspective(eye,look,size.width.toDouble(),600.0)
                        if(useSampler)renderer.draw(raster,this,view,path[3],placement,DioramaLinearColor(1.0,1.0,1.0),1f,null,DioramaToneMapping.None)
                        else control.draw(this,view,DioramaDepartingPlane(3,raster,path[3],placement),
                            DioramaLinearColor(1.0,1.0,1.0),1f,maxAniso=4f,toneMapped=false,fogEnabled=false)
                    }
                }
            }
            fun pixels():IntArray{waitForIdle();val im=captureToImage();return IntArray(im.width*im.height).also{im.readPixels(it)}}
            val captured=mutableMapOf<Int,IntArray>()
            for(i in rows.indices) {
                runOnIdle{current=i};val image=pixels();val raster=assertNotNull(prepared)
                assertEquals(rows[i][6].toInt(),raster.advancePx);assertEquals(rows[i][7].toInt(),raster.fontPx)
                assertTrue(image.any{it!=Color(0xff162435).toArgb()})
                captured[i]=image
                ImageIO.write(captureToImage().toAwtImage(),"png",out.resolve(rows[i][0]))
                runOnIdle{useSampler=true};val actual=pixels()
                if(!image.contentEquals(actual)) {
                    val differing=image.indices.filter{image[it]!=actual[it]}
                    val peak=differing.maxOf{j->(0..2).maxOf{c->abs((image[j] ushr (c*8) and 255)-(actual[j] ushr (c*8) and 255))}}
                    ImageIO.write(captureToImage().toAwtImage(),"png",out.resolve("atlas-failure-${rows[i][0]}"))
                    assertTrue(false,"Exact independent control ${rows[i][0]}: ${differing.size} differing pixels, peak=$peak, first=${differing.first()}")
                }
                val samplerOut=File("../build/verification/diorama-plain-sampler-kotlin").apply{mkdirs()}
                ImageIO.write(captureToImage().toAwtImage(),"png",samplerOut.resolve(rows[i][0]))
                assertEquals(1,renderer.residentCount)
                assertEquals(dioramaPlainMipBytes(raster.image.width,raster.image.height),renderer.residentBytes)
                runOnIdle{useSampler=false};assertContentEquals(image,pixels())
                if(i%2==1)assertEquals((i+1)/2,builds,"Camera queries must not reprepare paragraphs/mips")
                assertEquals(1,controls.size,"Old paragraph samplers are disposed")
            }
            for(i in listOf(0,rows.lastIndex,3,1)) {
                runOnIdle{current=i};assertContentEquals(captured.getValue(i),pixels(),"Unordered view/content restoration")
            }
            val before=builds;val paused=pixels();mainClock.advanceTimeBy(1100)
            assertContentEquals(paused,pixels(),"No independent sampler/media clock");assertEquals(before,builds)
            runOnIdle{viewportWidth=width/2};assertFalse(paused.contentEquals(pixels()),"Actual canvas resize changes projection")
            assertEquals(before,builds,"Viewport resize must reuse complete paragraph/mips")
            runOnIdle{viewportWidth=width};assertContentEquals(paused,pixels(),"Canvas size restoration is exact")
            runOnIdle{useSampler=true};assertContentEquals(paused,pixels())
            val preparedBefore=renderer.preparations;mainClock.advanceTimeBy(700);assertContentEquals(paused,pixels())
            assertEquals(preparedBefore,renderer.preparations,"Stable sampler frames prepare no images")
            runOnIdle{mounted=false};pixels();assertNull(prepared);assertTrue(controls.isEmpty(),"Unmount disposes children/effect/builder")
            assertEquals(0,renderer.residentCount);assertEquals(0L,renderer.residentBytes)
            runOnIdle{mounted=true};assertContentEquals(paused,pixels(),"Remount reconstructs shaped plane")
            runOnIdle{mounted=false};pixels();assertTrue(controls.isEmpty())
        }finally{controls.toList().forEach{it.close()};controls.clear();renderer.close()}
    }
}
