package top.ghhccghk.accompanist.lyrics.folia

import top.ghhccghk.accompanist.lyrics.folia.internal.rendering.*
import kotlin.math.*
import kotlin.test.*

class SoftwareBlurTest {
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
