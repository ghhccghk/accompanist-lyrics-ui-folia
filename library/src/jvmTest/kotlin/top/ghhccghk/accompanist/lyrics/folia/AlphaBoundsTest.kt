package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.ui.unit.IntRect
import top.ghhccghk.accompanist.lyrics.folia.internal.rendering.alphaBounds
import kotlin.random.Random
import kotlin.test.*

class AlphaBoundsTest {
    private fun fullScan(pixels:IntArray,width:Int,height:Int):IntRect? {
        var left=width;var right=-1;var top=height;var bottom=-1
        for(y in 0 until height) for(x in 0 until width) if(pixels[y*width+x] ushr 24!=0) {
            left=minOf(left,x);right=maxOf(right,x);top=minOf(top,y);bottom=maxOf(bottom,y)
        }
        return if(right<left)null else IntRect(left,top,right+1,bottom+1)
    }
    @Test fun rowEdgesMatchFullScanForSparseAndColouredTransparentPixels() {
        val random=Random(20261116)
        for((w,h) in listOf(1 to 1,1 to 47,73 to 1,3 to 7,97 to 43,1200 to 500)) {
            for(mode in 0..5) {
                val pixels=IntArray(w*h) {i ->
                    val x=i%w;val y=i/w
                    when(mode) {
                        0 -> random.nextInt() and 0xffffff
                        1 -> random.nextInt() or (0xff shl 24)
                        2 -> if (x in w/4..w*3/4 && y in h/3..h*2/3)random.nextInt() else 0
                        3 -> if(random.nextInt(1000)==0)0x0100ff00 else 0x000000ff
                        4 -> if(x==w-1 && y==h-1)0x01000000 else 0
                        else -> if(x==0 && y==0)0x01000000 else 0
                    }
                }
                assertEquals(fullScan(pixels,w,h),alphaBounds(pixels,w,h),"$w/$h mode=$mode")
            }
        }
    }
}
