package top.ghhccghk.accompanist.lyrics.folia

import top.ghhccghk.accompanist.lyrics.folia.internal.rendering.*
import kotlin.test.*

class ColourProcessorBoundaryTest {
    @Test fun nullFallbackPreservesProductionPixelsAndGeometry() {
        val source=bitmapFromArgb(19,13,IntArray(19*13) { i -> if(i%19 in 5..13 && i/19 in 4..8) 0x7f21af70 else 0 })
        var calls=0
        val processor=ExperimentalFoliaColourProcessor { pixels,w,h,kernel ->
            calls++
            assertEquals(w*h,pixels.size)
            assertTrue(kernel.size in 3..13 && kernel.size%2==1)
            assertEquals(1.0,kernel.sum(),1e-12)
            null
        }
        for(sigma in listOf(.02f,.25f,.5f,1.99f,2f,6f)) {
            val default=blurPremultiplied(source,sigma)
            val fallback=blurPremultiplied(source,sigma,processor)
            assertEquals(default.origin,fallback.origin)
            assertEquals(default.image.width,fallback.image.width)
            assertEquals(default.image.height,fallback.image.height)
            val a=IntArray(default.image.width*default.image.height);val b=IntArray(a.size)
            default.image.readPixels(a);fallback.image.readPixels(b)
            assertContentEquals(a,b)
        }
        assertEquals(4,calls,"Large sigma retains box coverage path")
        blurPremultiplied(bitmapFromArgb(3,3,IntArray(9)),.5f,processor)
        assertEquals(4,calls,"Empty bitmap never invokes native")
    }
    @Test fun malformedNativeResultIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            blurPremultiplied(bitmapFromArgb(3,3,IntArray(9) { -1 }),.5f,
                ExperimentalFoliaColourProcessor { _,_,_,_ -> IntArray(1) })
        }
    }
}
