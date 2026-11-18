// SPDX-License-Identifier: AGPL-3.0-only
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama

import androidx.compose.ui.graphics.drawscope.DrawScope

/** Platform capability, not a promise that every graphics API supports the
 * calibrated fragment path. Unsupported platforms retain the ordinary plane. */
internal interface DioramaPlainSampler {
    fun draw(scope:DrawScope,view:DioramaPerspective,origin:DioramaVec,x:DioramaVec,y:DioramaVec,
        color:DioramaLinearColor,alpha:Float,fog:DioramaFog?,toneMapping:DioramaToneMapping)
    fun close()
}
internal expect fun createDioramaPlainSampler(text:DioramaPlainText):DioramaPlainSampler?

internal data class DioramaMipPass(val first:Int,val last:Int)

/** Six LOD intervals plus their shared endpoint = at most fourteen child
 * samplers. Leave two D3D ps_5_1 slots for the host's clip/composition inputs.
 * Passes own disjoint LOD intervals; no mip or anisotropic tap is discarded. */
internal fun dioramaMipPasses(last:Int):List<DioramaMipPass> {
    require(last>=0)
    val result=mutableListOf<DioramaMipPass>();var first=0
    while(true) {
        val end=minOf(last,first+6);result+=DioramaMipPass(first,end)
        if(end==last)return result
        first=end
    }
}

/** Exact resident image payload estimate: F16 straight color base plus both
 * F16 color/ARGB alpha lower mip chains. Excludes the already owned paragraph,
 * shader/driver overhead and transient preparation buffers; not total PSS. */
internal fun dioramaPlainMipBytes(width:Int,height:Int):Long {
    require(width>0&&height>0)
    var w=width;var h=height;var bytes=w.toLong()*h*8
    while(w>1||h>1){w=maxOf(1,w/2);h=maxOf(1,h/2);bytes+=w.toLong()*h*12}
    return bytes
}

/** Explicit stage owner. Call prepare outside the draw callback when the
 * visible paragraph set changes; draw NEVER prepares images or reshapes text.
 * Eviction uses identity of immutable plain rasters, independent of clocks.
 * Owns only a bounded visible set, never the whole playback/core/camera.
 * Use prepare/draw/close on one owning drawing thread. */
internal class DioramaPlainRenderer(private val maxEntries:Int=8,
    private val byteBudget:Long=32L*1024*1024,
    private val factory:(DioramaPlainText)->DioramaPlainSampler? = ::createDioramaPlainSampler) {
    private val entries=linkedMapOf<DioramaPlainText,DioramaPlainSampler>()
    private var requested:List<DioramaPlainText>?=null
    private var closed=false
    var residentBytes:Long=0;private set
    var preparations:Int=0;private set
    val residentCount:Int get()=entries.size
    init{require(maxEntries>0&&byteBudget>=0)}

    fun prepare(texts:List<DioramaPlainText>) {
        check(!closed){"Plain renderer is closed"}
        val unique=texts.distinct()
        if(unique==requested)return
        // No stale corridor/paragraph ownership after a change or empty set.
        for(text in entries.keys.toList())if(text !in unique) {
            entries.remove(text)!!.close();residentBytes-=dioramaPlainMipBytes(text.raster.width,text.raster.height)
        }
        // Failed preparation is not a successful empty request: an unmount
        // must still evict any images prepared before that exception.
        requested=null
        for(text in unique)if(text !in entries && entries.size<maxEntries) {
            val bytes=dioramaPlainMipBytes(text.image.width,text.image.height)
            if(bytes<=byteBudget-residentBytes) {
                val sampler=factory(text)
                if(sampler!=null){entries[text]=sampler;residentBytes+=bytes;preparations++}
            }
        }
        // Remember unsupported/over-budget choices too, avoiding an allocation
        // retry on every media sample. No errors are swallowed here.
        requested=unique
    }

    fun draw(text:DioramaPlainText,scope:DrawScope,view:DioramaPerspective,frame:DioramaFrame,
        placement:DioramaPlacement,color:DioramaLinearColor,alpha:Float,
        fog:DioramaFog?,toneMapping:DioramaToneMapping) {
        check(!closed){"Plain renderer is closed"}
        val sampler=entries[text]
        if(sampler==null){text.draw(scope,view,frame,placement,color.srgb(toneMapping),alpha,fog);return}
        if(alpha<=0)return
        val scale=dioramaFrameFit(text.worldWidth,5.2,view.fov,view.width/view.height)*placement.scale
        val x=dioramaPlaneAxis(frame,placement,1.0,0.0)*(scale*.62/text.fontPx)
        val y=dioramaPlaneAxis(frame,placement,0.0,-1.0)*(scale*.62/text.fontPx)
        val center=dioramaComposeLocal(frame,placement.offsetR,placement.offsetU,0.0)
        sampler.draw(scope,view,center-x*(text.image.width/2.0)-y*(text.image.height/2.0),x,y,color,alpha,fog,toneMapping)
    }

    fun close() {
        if(closed)return
        closed=true
        entries.values.forEach{it.close()};entries.clear();requested=emptyList();residentBytes=0
    }
}
