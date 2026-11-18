// SPDX-License-Identifier: AGPL-3.0-only
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope

internal expect fun createDioramaActiveBaseSampler(image:ImageBitmap):DioramaPlainSampler?

/** Opt-in owner of only the current line's BASE image samplers. Halo/soul keep
 * their existing additive path. Prepare outside Canvas, on the drawing thread;
 * draw never allocates a pyramid or prepares text. Unavailable/over-budget
 * images return false so the caller draws the actual ordinary plane instead.
 * Budget is additional image payload, NOT process/GPU memory or a PSS claim. */
internal class DioramaActiveBaseRenderer(private val maxEntries:Int=32,
    private val byteBudget:Long=32L*1024*1024,
    private val factory:(ImageBitmap)->DioramaPlainSampler? = ::createDioramaActiveBaseSampler) {
    private val entries=linkedMapOf<ImageBitmap,DioramaPlainSampler>()
    private var requested:List<ImageBitmap>?=null
    private var closed=false
    var residentBytes:Long=0;private set
    var preparations:Int=0;private set
    val residentCount:Int get()=entries.size
    init{require(maxEntries>0&&byteBudget>=0)}

    fun prepare(text:DioramaActiveText?) {
        check(!closed){"Active base renderer is closed"}
        val images=text?.slices?.map{it.image}?.distinct().orEmpty()
        if(images==requested)return
        for(image in entries.keys.toList())if(image !in images) {
            entries.remove(image)!!.close();residentBytes-=dioramaPlainMipBytes(image.width,image.height)
        }
        requested=null // Failure is not a successful empty request/unmount.
        for(image in images)if(image !in entries&&entries.size<maxEntries) {
            val bytes=dioramaPlainMipBytes(image.width,image.height)
            if(bytes<=byteBudget-residentBytes) {
                val sampler=factory(image)
                if(sampler!=null){entries[image]=sampler;residentBytes+=bytes;preparations++}
            }
        }
        // Remember capability/budget misses for this unchanged line too.
        requested=images
    }

    fun draw(image:ImageBitmap,scope:DrawScope,view:DioramaPerspective,
        origin:DioramaVec,x:DioramaVec,y:DioramaVec,color:DioramaLinearColor,alpha:Float,
        fog:DioramaFog?,toneMapping:DioramaToneMapping):Boolean {
        check(!closed){"Active base renderer is closed"}
        val sampler=entries[image]?:return false
        if(alpha>0)sampler.draw(scope,view,origin,x,y,color,alpha,fog,toneMapping)
        return true
    }

    fun close() {
        if(closed)return
        closed=true;entries.values.forEach{it.close()};entries.clear()
        requested=emptyList();residentBytes=0
    }
}
