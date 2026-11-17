// Copyright (c) 2026 chthollyphile and contributors
// Folia cc6226e4 VisualizerPendolo.tsx: retained scale and conditional halo.
// SPDX-License-Identifier: AGPL-3.0-only. Media-event reconstruction adaptation.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.pendolo

import top.ghhccghk.accompanist.lyrics.folia.internal.animation.easeOut
import kotlin.math.min

internal data class PendoloChorusFrame(val presentation: PendoloChorusPresentation,
    val scale: Double, val haloOpacity: Double, val haloScale: Double)

/** Original parent initial=false, retained-node easeOut retargeting, and
 * conditional halo mount (no exit presence). No elapsed frame accumulation. */
internal class PendoloChorusTrack(lines: List<PendoloLineTime>, chorus: List<Boolean>,
    width: Double, height: Double, heights: List<Double>,
    private val intensity: PendoloIntensity = PendoloIntensity.NORMAL) {
    private data class Segment(val time: Double, val after: Boolean,
        val presentation: PendoloChorusPresentation, val from: Double, val duration: Double,
        val haloStart: Double?) {
        fun scale(t: Double): Double = if (duration == 0.0) presentation.haloScale
            else from + (presentation.haloScale - from) * easeOut((t-time)/duration)
    }
    private val tracks: Map<Int,List<Segment>>
    init {
        require(lines.size == chorus.size)
        val timeline=PendoloTimeline(lines)
        val events=listOf(0.0 to false)+timeline.boundaries.filter{it>=0}.flatMap{listOf(it to false,it to true)}
        var previous=emptyMap<Int,Segment>()
        val prepared=mutableMapOf<Int,MutableList<Segment>>()
        for((time,after)in events){
            val target=timeline.anchor(time,after)
            val current=timeline.current(time,after)
            val next=mutableMapOf<Int,Segment>()
            for(item in pendoloWheelLayout(lines.size,target,0.0,width,height,
                radiusOffset=min(width,height)*.06,blockHeights=heights)){
                val p=pendoloChorus(chorus[item.index],item.active && item.index==current,intensity)
                val old=previous[item.index]
                val from=old?.scale(time)?:p.haloScale
                val span=if(old!=null && old.presentation==p)old else Segment(time,after,p,from,
                    if(old==null)0.0 else p.duration,if(p.active)time else null)
                next[item.index]=span
                if(span!==old)prepared.getOrPut(item.index){mutableListOf()}.add(span)
            }
            previous=next
        }
        tracks=prepared
    }
    fun at(index:Int,time:Double,reducedMotion:Boolean=false):PendoloChorusFrame {
        require(time.isFinite())
        val list=tracks[index].orEmpty()
        var lo=0;var hi=list.size
        while(lo<hi){val m=(lo+hi)/2;val s=list[m];if(s.time<time||s.time==time&&!s.after)lo=m+1 else hi=m}
        val s=list.getOrNull(lo-1)
        val p=s?.presentation?:pendoloChorus(false,false,intensity)
        val progress=if(reducedMotion)1.0 else s?.haloStart?.let{easeOut((time-it)/p.duration).toDouble()}?:0.0
        return PendoloChorusFrame(p,if(reducedMotion)p.haloScale else s?.scale(time)?:1.0,
            p.haloOpacity*progress,if(p.active).96+.04*progress else 1.0)
    }
}
