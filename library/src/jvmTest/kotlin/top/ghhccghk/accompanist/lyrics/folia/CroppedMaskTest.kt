package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.ui.graphics.ImageBitmap
import top.ghhccghk.accompanist.lyrics.folia.internal.rendering.*
import java.io.File
import java.lang.management.ManagementFactory
import kotlin.random.Random
import kotlin.test.*

class CroppedMaskTest {
    @Test fun finiteSupportMatchesFullSurfaceAtEdgesAndForRepeatedRadii() {
        val random=Random(20261116)
        for ((w,h) in listOf(1 to 1,1 to 43,97 to 1,97 to 43,320 to 160,1200 to 500)) {
            for (mode in 0..4) {
                val pixels=IntArray(w*h) { i ->
                    val x=i%w;val y=i/w
                    when (mode) {
                        0 -> random.nextInt() and 0xffffff
                        1 -> random.nextInt()
                        2 -> if (x in w/3..w*2/3 && y in h/3..h*2/3)random.nextInt() else 0x000000ff
                        3 -> if (x==0 && y==h-1)0x0100ff00 else 0
                        else -> if (x==w-1 && y==0)-1 else 0
                    }
                }
                val input=bitmapFromArgb(w,h,pixels)
                val sigmas=if(w<1000)floatArrayOf(1f,1.5f,2.25f,7.5f,20f,4096f,1f)
                    else floatArrayOf(1f,20f,60f,1f)
                for(group in listOf(sigmas,floatArrayOf(1f,3f),floatArrayOf(20f,10f,20f))) {
                    val expected=fullSurfaceGlowMasks(input,group)
                    val actual=blurredMasks(input,group)
                    for(i in group.indices) {
                        assertEquals(w,actual[i].width);assertEquals(h,actual[i].height)
                        val old=IntArray(w*h);val now=IntArray(w*h)
                        expected[i].readPixels(old);actual[i].readPixels(now)
                        assertContentEquals(old,now,"${w}x$h mode=$mode sigma=${group[i]} group=${group.toList()}")
                    }
                }
            }
        }
    }

    @Test fun pairedPreparationTimeAndManagedAllocation() {
        val bean=ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean
        check(bean.isThreadAllocatedMemorySupported);bean.isThreadAllocatedMemoryEnabled=true
        val thread=Thread.currentThread().threadId()
        val report=StringBuilder("width,height,mode,legacyP50Ms,croppedP50Ms,legacyJvmBytes,croppedJvmBytes\n")
        var checksum=0
        for((w,h) in listOf(320 to 160,640 to 500)) for(mode in listOf("padded","dense","blank")) {
            val source=bitmapFromArgb(w,h,IntArray(w*h) { i ->
                if(mode=="dense" || mode=="padded" && i%w in w/2-12..w/2+12 && i/w in h/2-18..h/2+18)-1 else 0
            })
            val sigmas=floatArrayOf(10f,20f)
            fun run(old:Boolean):List<ImageBitmap> = if(old)fullSurfaceGlowMasks(source,sigmas) else blurredMasks(source,sigmas)
            fun consume(old:Boolean) {
                val images=run(old)
                val pixel=IntArray(1)
                images.first().readPixels(pixel,startX=w/2,startY=h/2,width=1,height=1)
                checksum=checksum xor pixel[0]
            }
            repeat(8) {consume(true);consume(false)}
            val before=DoubleArray(21);val after=DoubleArray(21)
            var oldBytes=0L;var newBytes=0L
            fun measure(old:Boolean):Pair<Double,Long> {
                val allocated=bean.getThreadAllocatedBytes(thread);val start=System.nanoTime()
                consume(old)
                return (System.nanoTime()-start)/1e6 to bean.getThreadAllocatedBytes(thread)-allocated
            }
            for(i in before.indices) for(old in if(i%2==0)listOf(true,false) else listOf(false,true)) {
                val measured=measure(old)
                if(old) {before[i]=measured.first;oldBytes+=measured.second}
                else {after[i]=measured.first;newBytes+=measured.second}
            }
            before.sort();after.sort()
            report.append("$w,$h,$mode,${before[10]},${after[10]},${oldBytes/21},${newBytes/21}\n")
            if(mode=="padded") assertTrue(newBytes<oldBytes,"Cropping should reduce scratch memory")
        }
        File("../build/verification/cropped-masks/preparation.csv").apply {parentFile.mkdirs();writeText(report.toString()+"# checksum=$checksum\n")}
    }
}
