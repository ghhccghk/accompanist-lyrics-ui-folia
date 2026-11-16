package top.ghhccghk.accompanist.lyrics.folia

import top.ghhccghk.accompanist.lyrics.folia.internal.rendering.*
import java.io.File
import java.lang.management.ManagementFactory
import kotlin.random.Random
import kotlin.test.*

class NeutralBlurTest {
    @Test fun exactColourClassificationPreservesAllPixels() {
        val random = Random(20261116)
        for ((w,h) in listOf(1 to 1,3 to 7,97 to 43,320 to 160)) for (mode in 0..8) {
            val pixels = IntArray(w*h) { i ->
                val alpha = listOf(0,1,2,127,128,254,255)[i%7]
                val grey = random.nextInt(256)
                val rgb = when (mode) {
                    0 -> 0xFFFFFF
                    1 -> grey * 0x010101
                    2 -> 0x808080
                    3 -> 0
                    4 -> 0xFF0000
                    5 -> if (i == w*h-1) 0xFFFF00 else 0xFFFFFF
                    6 -> if (i == w*h-1) 0x808180 else 0x808080
                    7 -> random.nextInt(0x1000000)
                    else -> if (i%7 == 0) 0xFF0000 else 0xFFFFFF
                }
                // Last-pixel counterexamples must remain visible.
                ((if (mode in 5..6 && i == w*h-1) 255 else alpha) shl 24) or rgb
            }
            val source = bitmapFromArgb(w,h,pixels)
            for (sigma in listOf(.02f,.25f,.5f,.6f,1f,1.99f,2f,2.25f,6f,20f)) {
                val old = fullChannelBlurPremultiplied(source,sigma)
                val now = blurPremultiplied(source,sigma)
                val context = "${w}x$h mode=$mode sigma=$sigma"
                assertEquals(old.origin,now.origin,context)
                assertEquals(old.image.width,now.image.width,context)
                assertEquals(old.image.height,now.image.height,context)
                val expected = IntArray(old.image.width*old.image.height)
                val actual = IntArray(expected.size)
                old.image.readPixels(expected); now.image.readPixels(actual)
                assertContentEquals(expected,actual,context)
            }
        }
    }

    @Test fun pairedSoftwarePreparationMeasurement() {
        val bean = ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean
        check(bean.isThreadAllocatedMemorySupported)
        bean.isThreadAllocatedMemoryEnabled = true
        val thread = Thread.currentThread().threadId()
        val report = StringBuilder("width,height,mode,sigma,oldP50Ms,newP50Ms,oldJvmBytes,newJvmBytes\n")
        var checksum = 0
        for ((w,h) in listOf(640 to 320,1200 to 500)) for (mode in listOf("white","neutral","colour")) {
            val source = bitmapFromArgb(w,h,IntArray(w*h) { i ->
                val rgb = when (mode) {
                    "white" -> 0xFFFFFF
                    "neutral" -> ((i*17)%256)*0x010101
                    else -> (i*7937) and 0xFFFFFF
                }
                (((i*13)%256) shl 24) or rgb
            })
            for (sigma in listOf(.25f,1.99f,6f)) {
                fun run(old: Boolean) {
                    val output = if (old) fullChannelBlurPremultiplied(source,sigma) else blurPremultiplied(source,sigma)
                    val pixel = IntArray(1)
                    output.image.readPixels(pixel,startX=w/2,startY=h/2,width=1,height=1)
                    checksum = checksum xor pixel[0]
                }
                repeat(5) { run(true); run(false) }
                val before = DoubleArray(15); val after = DoubleArray(15)
                var oldBytes = 0L; var newBytes = 0L
                for (i in before.indices) for (old in if (i%2 == 0) listOf(true,false) else listOf(false,true)) {
                    val bytes = bean.getThreadAllocatedBytes(thread)
                    val start = System.nanoTime()
                    run(old)
                    val elapsed = (System.nanoTime()-start)/1e6
                    val allocated = bean.getThreadAllocatedBytes(thread)-bytes
                    if (old) { before[i] = elapsed; oldBytes += allocated }
                    else { after[i] = elapsed; newBytes += allocated }
                }
                before.sort(); after.sort()
                report.append("$w,$h,$mode,$sigma,${before[7]},${after[7]},${oldBytes/15},${newBytes/15}\n")
            }
        }
        File("../build/verification/neutral-blur/preparation.csv").apply {
            parentFile.mkdirs(); writeText(report.toString()+"# checksum=$checksum\n")
        }
    }
}
