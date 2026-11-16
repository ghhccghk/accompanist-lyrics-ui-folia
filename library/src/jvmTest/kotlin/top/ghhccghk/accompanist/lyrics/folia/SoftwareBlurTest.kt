package top.ghhccghk.accompanist.lyrics.folia

import top.ghhccghk.accompanist.lyrics.folia.internal.rendering.*
import kotlin.math.*
import kotlin.test.*
import kotlin.random.Random

class SoftwareBlurTest {
    @Test fun paddedAndClippedColourBlurMatchesFrozenImplementation() {
        val random = Random(20261116)
        for ((w,h) in listOf(1 to 1, 3 to 7, 37 to 13, 97 to 43, 320 to 160)) {
            for (mode in 0..2) {
                val pixels = IntArray(w*h) { i ->
                    val x = i%w; val y = i/w
                    if (mode==0 || (mode==1 && (x<w/4 || x>=w*3/4 || y<h/3 || y>=h*2/3)))
                        random.nextInt() and 0xFFFFFF else random.nextInt()
                }
                val input = bitmapFromArgb(w,h,pixels)
                for (sigma in listOf(.02f,.25f,.5f,.6f,1f,1.99f,2f,6f,20f)) {
                    val old = legacyBlurPremultiplied(input,sigma)
                    val now = blurPremultiplied(input,sigma)
                    val message = "$w x $h mode=$mode sigma=$sigma"
                    assertEquals(old.origin,now.origin,message)
                    assertEquals(old.image.width,now.image.width,message)
                    assertEquals(old.image.height,now.image.height,message)
                    val expected = IntArray(old.image.width*old.image.height)
                    val actual = IntArray(expected.size)
                    old.image.readPixels(expected); now.image.readPixels(actual)
                    assertContentEquals(expected,actual,message)
                }
            }
        }
    }

    @Test fun temporaryHeapAllocationDropsWithoutChangingPixels() {
        val bean = java.lang.management.ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean
        check(bean.isThreadAllocatedMemorySupported)
        bean.isThreadAllocatedMemoryEnabled = true
        val width=640; val height=320
        val source=bitmapFromArgb(width,height,IntArray(width*height) { 0x80FF8040.toInt() })
        val thread=Thread.currentThread().threadId()
        val rows=mutableListOf("sigma,legacyJvmBytes,optimizedJvmBytes,savedJvmBytes")
        for (sigma in listOf(.5f,6f)) {
            repeat(3) { legacyBlurPremultiplied(source,sigma); blurPremultiplied(source,sigma) }
            fun measure(blur: () -> BlurredPixels): Long {
                val before=bean.getThreadAllocatedBytes(thread)
                repeat(4) { check(blur().image.width==width) }
                return (bean.getThreadAllocatedBytes(thread)-before)/4
            }
            val old=measure { legacyBlurPremultiplied(source,sigma) }
            val now=measure { blurPremultiplied(source,sigma) }
            // Removed one cropped IntArray and one DoubleArray (12 bytes/pixel).
            // Leave allowance for VM wrappers; this is heap allocation, not RSS.
            assertTrue(old-now >= width.toLong()*height*11, "$sigma old=$old new=$now")
            rows += "$sigma,$old,$now,${old-now}"
        }
        java.io.File("build/verification/software-blur-memory.csv").apply {
            parentFile.mkdirs(); writeText(rows.joinToString("\n")+"\n")
        }
    }

    @Test fun transparentColourCannotBleedAndOpacityIsPreserved() {
        val size=21
        // Deliberately coloured zero-alpha surroundings must contribute no blue.
        val pixels=IntArray(size*size) {0x000000FF}
        pixels[10*size+10]=0x80FF0000.toInt()
        val result=blurPremultiplied(bitmapFromArgb(size,size,pixels),1f)
        val actual=IntArray(result.image.width*result.image.height);result.image.readPixels(actual)
        for(pixel in actual) if(pixel ushr 24>0) {
            assertEquals(0,pixel and 0xFFFF,"transparent blue must not enter red halo")
            assertEquals(255,(pixel ushr 16) and 255)
        }
        val centre=actual[(10-result.origin.y.toInt())*result.image.width+10-result.origin.x.toInt()] ushr 24
        // Analytic separable unit Gaussian impulse, not another blur implementation.
        val norm=(-3..3).sumOf {exp(-.5*it*it)}
        assertEquals(floor(128/(norm*norm)+.5).toInt(),centre)
        val blank=blurPremultiplied(bitmapFromArgb(4,4,IntArray(16){0x0000FF00}),3f)
        assertEquals(1,blank.image.width)
        val blankPixels=IntArray(1);blank.image.readPixels(blankPixels);assertEquals(0,blankPixels[0])
    }
}
