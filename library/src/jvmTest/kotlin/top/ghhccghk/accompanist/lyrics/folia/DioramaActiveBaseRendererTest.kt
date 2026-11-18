// SPDX-License-Identifier: AGPL-3.0-only
package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.*
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama.*
import java.io.File
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class DioramaActiveBaseRendererTest {
    @Test fun boundedOwnershipUnavailableAndExplicitFailure()=runDesktopComposeUiTest(width=380,height=200) {
        var texts=emptyList<DioramaActiveText>()
        setContent {
            val resolver=LocalFontFamilyResolver.current
            val ready=remember {
                val style=TextStyle(fontFamily=FontFamily(Font(File("../verification/assets/FoliaParityLatin-Bold.ttf"),FontWeight.Bold)))
                val measurer=TextMeasurer(resolver,Density(1f),LayoutDirection.Ltr)
                listOf("Synthetic A","Synthetic B").map{DioramaActiveText.prepare(DioramaLyric(it,.7,2.0),style,measurer,resolver)}
            }
            SideEffect{texts=ready}
        }
        waitForIdle();val a=texts[0];val b=texts[1]
        val events=mutableListOf<ImageBitmap>();val closed=mutableListOf<ImageBitmap>()
        fun factory(image:ImageBitmap):DioramaPlainSampler {
            events+=image
            return object:DioramaPlainSampler {
                override fun draw(scope:DrawScope,view:DioramaPerspective,origin:DioramaVec,x:DioramaVec,y:DioramaVec,
                    color:DioramaLinearColor,alpha:Float,fog:DioramaFog?,toneMapping:DioramaToneMapping)=error("Not a drawing fake")
                override fun close(){closed+=image}
            }
        }
        val first=a.slices.first().image
        val bytes=dioramaPlainMipBytes(first.width,first.height)
        val owner=DioramaActiveBaseRenderer(maxEntries=1,byteBudget=bytes,factory=::factory)
        owner.prepare(a);owner.prepare(a)
        assertEquals(listOf(first),events);assertEquals(bytes,owner.residentBytes);assertEquals(1,owner.residentCount)
        owner.prepare(null);assertEquals(listOf(first),closed);assertEquals(0L,owner.residentBytes)
        owner.prepare(b);assertEquals(1,owner.residentCount)
        owner.close();owner.close();assertEquals(events,closed)
        assertFailsWith<IllegalStateException>{owner.prepare(a)}
        val zero=DioramaActiveBaseRenderer(byteBudget=0,factory={error("Must not allocate beyond budget")})
        zero.prepare(a);assertEquals(0,zero.preparations);zero.close()
        var unavailable=0
        val none=DioramaActiveBaseRenderer(factory={unavailable++;null})
        none.prepare(a);none.prepare(a);assertEquals(a.slices.size,unavailable);none.close()
        val failure=IllegalArgumentException("Sampler preparation failed")
        var calls=0
        val partial=DioramaActiveBaseRenderer(factory={if(calls++==1)throw failure else factory(it)})
        assertSame(failure,assertFailsWith<IllegalArgumentException>{partial.prepare(a)})
        assertEquals(1,partial.residentCount)
        partial.prepare(a);assertEquals(a.slices.size,partial.residentCount,"Explicit retry completes missing images")
        partial.close();assertEquals(events,closed,"Every successfully owned sampler closes exactly once")
        calls=0
        val failedThenUnmounted=DioramaActiveBaseRenderer(factory={if(calls++==1)throw failure else factory(it)})
        assertFailsWith<IllegalArgumentException>{failedThenUnmounted.prepare(a)}
        failedThenUnmounted.prepare(null)
        assertEquals(0,failedThenUnmounted.residentCount,"Unmount after partial error releases successfully prepared images")
        assertEquals(0L,failedThenUnmounted.residentBytes)
        failedThenUnmounted.close();assertEquals(events,closed)
    }

    @Test fun coreMediaOffsetResizeFallbackAndRemount()=runDesktopComposeUiTest(width=800,height=600) {
        val core=SyncedLyrics(listOf(SyncedLine("Synthetic incoming line",null,700,1500),SyncedLine("Second shaped line",null,2100,3000)))
        val renderer=DioramaActiveBaseRenderer()
        val unavailable=DioramaActiveBaseRenderer(factory={null})
        val budget=DioramaActiveBaseRenderer(byteBudget=0)
        var time by mutableDoubleStateOf(.9);var offset by mutableIntStateOf(0)
        var viewport by mutableIntStateOf(800);var mounted by mutableStateOf(true)
        var mode by mutableStateOf("ordinary");var reduced by mutableStateOf(false)
        var ready:DioramaTextPlayback?=null
        try {
            setContent {
                val resolver=LocalFontFamilyResolver.current
                val playback=if(mounted)remember(viewport) {
                    val style=TextStyle(fontFamily=FontFamily(Font(File("../verification/assets/FoliaParityLatin-Bold.ttf"),FontWeight.Bold)))
                    DioramaTextPlayback.prepare(core,style,TextMeasurer(resolver,Density(1f),LayoutDirection.Ltr),resolver,
                        viewport.toDouble(),600.0,seedName="Active sample",endTime=4.0)
                }else null
                val t=playback?.lyricTime(time,offset)?:0.0
                SideEffect {
                    ready=playback
                    for(owner in listOf(renderer,unavailable,budget)) {
                        if(playback==null)owner.prepare(null)else playback.prepareActiveBaseRenderer(owner,t)
                    }
                }
                Canvas(Modifier.width(viewport.dp).fillMaxHeight().background(Color(0xff162435))) {
                    playback?.drawStage(this,t,playback.camera.perspectiveAt(t,reduced),DioramaLinearColor(.7,.8,.9),
                        DioramaLinearColor(.8,.2,.1),reducedMotion=reduced,
                        activeBaseRenderer=when(mode){"sample"->renderer;"none"->unavailable;"budget"->budget;else->null})
                }
            }
            fun pixels():IntArray{waitForIdle();val im=captureToImage();return IntArray(im.width*im.height).also{im.readPixels(it)}}
            val ordinary=pixels()
            for(m in listOf("none","budget")){runOnIdle{mode=m};assertContentEquals(ordinary,pixels(),"Real ordinary fallback $m")}
            runOnIdle{mode="sample"};val original=pixels();val count=renderer.preparations
            assertTrue(renderer.residentCount>0);assertTrue(renderer.residentBytes<=32L*1024*1024)
            mainClock.advanceTimeBy(1000);assertContentEquals(original,pixels());assertEquals(count,renderer.preparations)
            runOnIdle{time=1.1;offset=200};assertContentEquals(original,pixels(),"Offset exactly once")
            runOnIdle{time=.9;offset=0}
            for(t in listOf(2.7,0.0,3.9,.7,2.099,2.1,.9)){runOnIdle{time=t};pixels()}
            assertContentEquals(original,pixels(),"Random seek reconstructs materials/camera")
            assertSame(core.lines.first(),ready!!.sourceAt(.9).source)
            runOnIdle{reduced=true};val reducedPixels=pixels()
            mainClock.advanceTimeBy(400);assertContentEquals(reducedPixels,pixels())
            runOnIdle{reduced=false};assertContentEquals(original,pixels())
            runOnIdle{viewport=400};pixels()
            runOnIdle{viewport=800};assertContentEquals(original,pixels(),"Actual Canvas resize restore")
            runOnIdle{mounted=false};pixels();assertNull(ready)
            assertEquals(0,renderer.residentCount);assertEquals(0L,renderer.residentBytes)
            runOnIdle{mounted=true};assertContentEquals(original,pixels(),"Remount rebuilds from media")
        }finally{renderer.close();unavailable.close();budget.close()}
    }
}
