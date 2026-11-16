package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.IntSize
import top.ghhccghk.accompanist.lyrics.folia.internal.rendering.*
import java.io.File
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class TransparentLayerTest {
    @Test fun invisibleSoftwareSubtreeAvoidsRasterWork() = runDesktopComposeUiTest(width=192,height=192) {
        var report: String? = null
        setContent {
            val oldNative=rememberGraphicsLayer()
            val newNative=rememberGraphicsLayer()
            val old = remember(oldNative) { LegacyTransparentLayer(oldNative,true) }
            val current = remember(newNative) { ClassicLayer(newNative,true) }
            Canvas(Modifier.fillMaxSize()) {
                if(report==null) {
                    val bean=java.lang.management.ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean
                    check(bean.isThreadAllocatedMemorySupported)
                    bean.isThreadAllocatedMemoryEnabled=true
                    val thread=Thread.currentThread().threadId()
                    val extent=IntSize(640,320)
                    var oldCalls=0;var newCalls=0
                    old.alpha=0f;old.blurSigma=.25f
                    current.alpha=0f;current.blurSigma=.25f
                    val legacy: () -> Unit = {
                        old.record(this,extent) {oldCalls++;drawRect(Color.White)}
                        old.draw(this)
                    }
                    val optimized: () -> Unit = {
                        current.record(this,extent) {newCalls++;drawRect(Color.White)}
                        current.draw(this)
                    }
                    repeat(4) {legacy();optimized()}
                    fun measure(action: () -> Unit): Pair<Long,Double> {
                        val before=bean.getThreadAllocatedBytes(thread)
                        val start=System.nanoTime()
                        repeat(8) {action()}
                        val elapsed=(System.nanoTime()-start)/8e6
                        return (bean.getThreadAllocatedBytes(thread)-before)/8 to elapsed
                    }
                    val before=measure(legacy);val after=measure(optimized)
                    assertEquals(12,oldCalls);assertEquals(0,newCalls)
                    assertTrue(after.first<before.first/100,"Invisible subtree still allocates raster buffers")
                    report="scope,width,height,sigma,oldCallbacks,newCallbacks,oldJvmBytesPerCall,newJvmBytesPerCall,oldMeanMs,newMeanMs\n"+
                        "isolated-software-zero-alpha,640,320,.25,$oldCalls,$newCalls,${before.first},${after.first},${before.second},${after.second}\n"
                }
            }
        }
        waitForIdle()
        File("../build/verification/transparent-layers/work.csv").apply {parentFile.mkdirs();writeText(checkNotNull(report))}
    }

    @Test fun nestedOpacitySeekAndBackendChangesMatchOldPixels() = runDesktopComposeUiTest(width=192,height=192) {
        var old by mutableStateOf(true)
        var software by mutableStateOf(false)
        var opacity by mutableFloatStateOf(1f)
        var outerOpacity by mutableFloatStateOf(1f)
        var sigma by mutableFloatStateOf(0f)
        var revision by mutableIntStateOf(0)
        var callbacks = 0
        setContent {
            val nativeOuter = rememberGraphicsLayer()
            val nativeInner = rememberGraphicsLayer()
            val legacyOuter = remember(software) { LegacyTransparentLayer(nativeOuter,software) }
            val legacyInner = remember(software) { LegacyTransparentLayer(nativeInner,software) }
            val newOuter = remember(software) { ClassicLayer(nativeOuter,software) }
            val newInner = remember(software) { ClassicLayer(nativeInner,software) }
            Canvas(Modifier.fillMaxSize()) {
                drawRect(Color(0xff162435))
                val extent = if (revision%2==0) IntSize(160,160) else IntSize(144,148)
                val paint: DrawScope.() -> Unit = {
                    callbacks++
                    drawRect(Color(0x90fa4080),Offset(32f,38f),Size(52f,42f))
                    drawCircle(if(revision%2==0)Color.Cyan else Color.Yellow,18f,Offset(78f,78f))
                }
                if (old) {
                    legacyOuter.alpha=outerOpacity;legacyOuter.blurSigma=sigma
                    legacyOuter.record(this,extent) {
                        legacyInner.alpha=opacity;legacyInner.blurSigma=.25f
                        legacyInner.record(this,extent,paint)
                        legacyInner.draw(this)
                    }
                    legacyOuter.draw(this)
                } else {
                    newOuter.alpha=outerOpacity;newOuter.blurSigma=sigma
                    newOuter.record(this,extent) {
                        newInner.alpha=opacity;newInner.blurSigma=.25f
                        newInner.record(this,extent,paint)
                        newInner.draw(this)
                    }
                    newOuter.draw(this)
                }
            }
        }
        val report=StringBuilder("software,sigma,outerAlpha,innerAlpha,revision,oldPaintCalls,newPaintCalls,equalPixels\n")
        for (soft in listOf(false,true)) for (blur in listOf(0f,.25f,.5f,.6f,2f)) {
            // Revisit in reverse, switch native/software branches, resize, and repaint
            // different ink after zero opacity. Tiny nonzero alpha is never culled.
            val states=listOf(1f to 1f, 1f to 0f, 0f to 1f, 1f to .0001f, .4f to .5f, 1f to 1f)
            for ((index,alphas) in (states+states.reversed()).withIndex()) {
                fun capture(legacy:Boolean): Pair<IntArray,Int> {
                    runOnIdle {
                        old=legacy;software=soft;sigma=blur
                        outerOpacity=alphas.first;opacity=alphas.second;revision=index
                    }
                    waitForIdle()
                    runOnIdle {callbacks=0}
                    // Repaint the same media state, then freeze it for capture.
                    runOnIdle {revision=index+2};waitForIdle()
                    val image=captureToImage()
                    return IntArray(image.width*image.height).also {image.readPixels(it)} to callbacks
                }
                val before=capture(true);val after=capture(false)
                assertContentEquals(before.first,after.first,"software=$soft blur=$blur alpha=$alphas index=$index")
                if(alphas.first==0f || alphas.second==0f) assertEquals(0,after.second,"Invisible group executed painting")
                else assertTrue(after.second>0,"Visible group must paint even at tiny opacity")
                report.append("$soft,$blur,${alphas.first},${alphas.second},$index,${before.second},${after.second},true\n")
            }
        }
        File("../build/verification/transparent-layers/pixels.csv").apply {parentFile.mkdirs();writeText(report.toString())}
    }
}
