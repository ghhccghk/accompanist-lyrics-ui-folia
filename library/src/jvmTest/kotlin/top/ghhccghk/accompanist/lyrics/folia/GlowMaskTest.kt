package top.ghhccghk.accompanist.lyrics.folia

import top.ghhccghk.accompanist.lyrics.folia.internal.rendering.*
import kotlin.test.*

class GlowMaskTest {
    @Test fun transparentOutsideAndIndependentResults() {
        val width=128; val height=96
        val source=bitmapFromArgb(width,height,IntArray(width*height) { i ->
            if(i%width in 40..87 && i/width in 24..71) -1 else 0
        })
        val masks=blurredMasks(source,floatArrayOf(3f,7.5f,3f))
        fun alpha(index:Int)=IntArray(width*height).also{masks[index].readPixels(it)}.map{it ushr 24}
        val a=alpha(0);val b=alpha(1)
        assertEquals(a,alpha(2))
        assertNotSame(masks[0],masks[2])
        assertEquals(255,a[48*width+64])
        assertEquals(0,a[0]);assertEquals(0,b[0])
        assertTrue(b[48*width+32]>a[48*width+32],"Wider sigma spreads coverage")
        for(y in 0 until height) for(x in 0 until width) {
            assertEquals(a[y*width+x],a[y*width+width-1-x],"centred horizontal support")
            assertEquals(b[y*width+x],b[(height-1-y)*width+x],"centred vertical support")
        }
        assertTrue(blurredMasks(source,intArrayOf()).isEmpty())
        assertFailsWith<IllegalArgumentException>{blurredMasks(source,intArrayOf(0))}
        assertFailsWith<IllegalArgumentException>{blurredMasks(source,floatArrayOf(Float.NaN))}
    }
}
