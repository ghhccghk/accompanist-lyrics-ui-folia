// SPDX-License-Identifier: AGPL-3.0-only
package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.*
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama.*
import java.io.File
import kotlin.math.abs
import kotlin.math.PI
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class DioramaPlainRendererTest {
    @Test fun budgetsUnavailableAndDisposal()=runDesktopComposeUiTest(width=380,height=200) {
        var texts=emptyList<DioramaPlainText>()
        setContent {
            val resolver=LocalFontFamilyResolver.current
            val ready=remember {
                val style=TextStyle(fontFamily=FontFamily(Font(File("../verification/assets/FoliaParityLatin-Bold.ttf"),FontWeight.Bold)))
                val measurer=TextMeasurer(resolver,Density(1f),LayoutDirection.Ltr)
                listOf("Synthetic A","Synthetic B","Synthetic C").map{DioramaPlainText.prepare(it,style,measurer,resolver)}
            }
            SideEffect{texts=ready}
        }
        waitForIdle();val a=texts[0];val b=texts[1];val c=texts[2]
        val events=mutableListOf<DioramaPlainText>();val closed=mutableListOf<DioramaPlainText>()
        val factory:(DioramaPlainText)->DioramaPlainSampler? = {text->
            events+=text
            object:DioramaPlainSampler {
                override fun draw(scope:androidx.compose.ui.graphics.drawscope.DrawScope,view:DioramaPerspective,
                    origin:DioramaVec,x:DioramaVec,y:DioramaVec,color:DioramaLinearColor,alpha:Float,fog:DioramaFog?,toneMapping:DioramaToneMapping)=error("Not a rendering fake")
                override fun close(){closed+=text}
            }
        }
        val bytes=dioramaPlainMipBytes(a.image.width,a.image.height)
        val renderer=DioramaPlainRenderer(maxEntries=1,byteBudget=bytes,factory=factory)
        renderer.prepare(listOf(a,a,b));renderer.prepare(listOf(a,a,b))
        assertEquals(listOf(a),events);assertEquals(1,renderer.residentCount);assertEquals(bytes,renderer.residentBytes)
        renderer.prepare(emptyList());assertEquals(listOf(a),closed);assertEquals(0L,renderer.residentBytes)
        renderer.prepare(listOf(a));renderer.close();renderer.close()
        assertEquals(listOf(a,a),closed);assertEquals(0,renderer.residentCount)
        assertFailsWith<IllegalStateException>{renderer.prepare(listOf(a))}
        val none=DioramaPlainRenderer(byteBudget=0,factory={error("Budget must be checked before allocation")})
        none.prepare(listOf(a,b,c));assertEquals(0,none.preparations);none.close()
        var unsupported=0
        val unavailable=DioramaPlainRenderer(factory={unsupported++;null})
        unavailable.prepare(listOf(a));unavailable.prepare(listOf(a));assertEquals(1,unsupported);unavailable.close()
        val failure=IllegalArgumentException("Explicit preparation failure")
        val broken=DioramaPlainRenderer(factory={throw failure})
        assertSame(failure,assertFailsWith<IllegalArgumentException>{broken.prepare(listOf(a))});broken.close()
        val partialClosed=mutableListOf<DioramaPlainText>()
        val partial=DioramaPlainRenderer(factory={text->
            if(text===b)throw failure
            object:DioramaPlainSampler {
                override fun draw(scope:androidx.compose.ui.graphics.drawscope.DrawScope,view:DioramaPerspective,
                    origin:DioramaVec,x:DioramaVec,y:DioramaVec,color:DioramaLinearColor,alpha:Float,fog:DioramaFog?,toneMapping:DioramaToneMapping)=error("Not a drawing fake")
                override fun close(){partialClosed+=text}
            }
        })
        try {
            assertSame(failure,assertFailsWith<IllegalArgumentException>{partial.prepare(listOf(a,b))})
            assertEquals(1,partial.residentCount)
            partial.prepare(emptyList())
            assertEquals(listOf(a),partialClosed,"Empty/unmount after partial failure must release the successfully prepared sampler")
            assertEquals(0,partial.residentCount);assertEquals(0L,partial.residentBytes)
        }finally{partial.close()}
        assertEquals(8L,dioramaPlainMipBytes(1,1));assertEquals(44L,dioramaPlainMipBytes(2,2))
    }

    @Test fun canvasAlphaClipAndFallback()=runDesktopComposeUiTest(width=380,height=300) {
        val renderer=DioramaPlainRenderer()
        val fallback=DioramaPlainRenderer(byteBudget=0)
        val unavailable=DioramaPlainRenderer(factory={null})
        var mode by mutableStateOf("ordinary")
        val background=Color(0xff162435)
        val frame=DioramaFrame(DioramaVec(0.0,0.0,0.0),DioramaVec(0.0,0.0,-1.0),DioramaVec(1.0,0.0,0.0),DioramaVec(0.0,1.0,0.0))
        try {
            setContent {
                val resolver=LocalFontFamilyResolver.current
                val text=remember {
                    val style=TextStyle(fontFamily=FontFamily(Font(File("../verification/assets/FoliaParityLatin-Bold.ttf"),FontWeight.Bold)))
                    DioramaPlainText.prepare("Synthetic plane",style,TextMeasurer(resolver,Density(1f),LayoutDirection.Ltr),resolver)
                }
                SideEffect{renderer.prepare(listOf(text));fallback.prepare(listOf(text));unavailable.prepare(listOf(text))}
                Canvas(Modifier.fillMaxSize().background(background)) {
                    val view=DioramaPerspective(DioramaVec(0.0,0.0,5.0),DioramaVec(0.0,0.0,0.0),380.0,300.0,
                        near=if(mode=="near")5.5 else .1,far=if(mode=="far")4.0 else 1000.0)
                    val placement=DioramaPlacement(0.0,0.0,1.0,0.0,if(mode=="back")PI else 0.0,0.0)
                    fun draw(){
                        when(mode) {
                            "ordinary"->text.draw(this,view,frame,placement)
                            "fallback"->fallback.draw(text,this,view,frame,placement,DioramaLinearColor(1.0,1.0,1.0),1f,null,DioramaToneMapping.None)
                            "unavailable"->unavailable.draw(text,this,view,frame,placement,DioramaLinearColor(1.0,1.0,1.0),1f,null,DioramaToneMapping.None)
                            else->renderer.draw(text,this,view,frame,placement,DioramaLinearColor(1.0,1.0,1.0),if(mode=="alpha") .4f else 1f,null,DioramaToneMapping.None)
                        }
                    }
                    when(mode) {
                        "clip"->clipRect(right=190f){draw()}
                        "parent"->{drawContext.canvas.saveLayer(Rect(0f,0f,380f,300f),Paint().apply{alpha=.4f});try{draw()}finally{drawContext.canvas.restore()}}
                        else->draw()
                    }
                }
            }
            fun pixels():IntArray{waitForIdle();val im=captureToImage();return IntArray(im.width*im.height).also{im.readPixels(it)}}
            val ordinary=pixels()
            for(m in listOf("fallback","unavailable")){runOnIdle{mode=m};assertContentEquals(ordinary,pixels(),"Capability/budget fallback is the real ordinary path")}
            runOnIdle{mode="sampler"};val full=pixels();val count=renderer.preparations
            assertTrue(full.any{it!=background.toArgb()})
            for(m in listOf("near","far","back")){runOnIdle{mode=m};assertTrue(pixels().all{it==background.toArgb()},"$m plane must not leak")}
            runOnIdle{mode="clip"};val clipped=pixels()
            assertTrue(clipped.any{it!=background.toArgb()});assertTrue(clipped.withIndex().filter{it.index%380>=190}.all{it.value==background.toArgb()},"Parent Canvas clip retained")
            runOnIdle{mode="parent"};val parent=pixels()
            runOnIdle{mode="alpha"};val alpha=pixels()
            val peak=parent.indices.maxOf{i->listOf(0,8,16,24).maxOf{shift->abs((parent[i] ushr shift and 255)-(alpha[i] ushr shift and 255))}}
            assertTrue(peak<=1,"Parent compositing vs uniform alpha differs only by8bit rounding: $peak")
            runOnIdle{mode="sampler"};assertContentEquals(full,pixels());assertEquals(count,renderer.preparations)
        }finally{renderer.close();fallback.close();unavailable.close()}
    }

    @Test fun coloredInputAndPartialNearFar()=runDesktopComposeUiTest(width=380,height=300) {
        val image=ImageBitmap(64,32)
        CanvasDrawScope().draw(Density(1f),LayoutDirection.Ltr,Canvas(image),Size(64f,32f)) {
            drawRect(Color.Red,size=Size(32f,32f));drawRect(Color.Blue,topLeft=Offset(32f,0f),size=Size(32f,32f))
        }
        val sampler=SkiaDioramaPlainSampler(image)
        var mode by mutableStateOf("full")
        val background=Color(0xff162435)
        try {
            setContent {
                Canvas(Modifier.fillMaxSize().background(background)) {
                    val partial=mode!="full"
                    val view=DioramaPerspective(DioramaVec(0.0,0.0,5.0),DioramaVec(0.0,0.0,0.0),380.0,300.0,
                        near=if(mode=="near")5.0 else .1,far=if(mode=="far")5.0 else 1000.0)
                    sampler.draw(this,view,DioramaVec(-1.0,.5,if(partial)-.3 else 0.0),
                        DioramaVec(2.0/64,0.0,if(partial).6/64 else 0.0),DioramaVec(0.0,-1.0/32,0.0),
                        DioramaLinearColor(1.0,1.0,1.0),1f,null,DioramaToneMapping.None)
                }
            }
            fun pixels():IntArray{waitForIdle();val im=captureToImage();return IntArray(im.width*im.height).also{im.readPixels(it)}}
            val full=pixels()
            assertEquals(Color.Red.toArgb(),full[150*380+160],"Straight RGB source red, not native byte-order blue")
            assertEquals(Color.Blue.toArgb(),full[150*380+220])
            runOnIdle{mode="near"};val near=pixels()
            assertEquals(Color.Red.toArgb(),near[150*380+160]);assertEquals(background.toArgb(),near[150*380+220],"Near-plane crossing clips the blue half")
            runOnIdle{mode="far"};val far=pixels()
            assertEquals(background.toArgb(),far[150*380+160]);assertEquals(Color.Blue.toArgb(),far[150*380+220],"Far-plane crossing clips the red half")
            runOnIdle{mode="full"};assertContentEquals(full,pixels())
        }finally{sampler.close();sampler.close()}
        val original=IntArray(64*32).also{image.readPixels(it)}
        assertEquals(Color.Red.toArgb(),original[0],"Closing sampler must not close shared paragraph image")
    }
}
