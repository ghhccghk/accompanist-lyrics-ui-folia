package top.ghhccghk.accompanist.lyrics.folia.internal.rendering

import androidx.compose.ui.unit.IntRect

/** Exact nontransparent bounds. Once a row's edges are found, its interior
 * cannot affect the rectangle, so avoid scanning every visible pixel again. */
internal fun alphaBounds(pixels: IntArray, width: Int, height: Int): IntRect? {
    var left=width;var right=-1;var top=height;var bottom=-1
    for (y in 0 until height) {
        val base=y*width
        var first=0
        while (first<width && pixels[base+first] ushr 24==0) first++
        if (first==width) continue
        var last=width-1
        while (last>first && pixels[base+last] ushr 24==0) last--
        left=minOf(left,first);right=maxOf(right,last)
        if (top==height) top=y
        bottom=y
    }
    return if (right<left) null else IntRect(left,top,right+1,bottom+1)
}
