// Copyright (c) 2026 chthollyphile and contributors
// Folia cc6226e4 VisualizerPendolo.tsx: idle transition-all duration-200.
// SPDX-License-Identifier: AGPL-3.0-only. Native CSS response adaptation.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.pendolo

import kotlin.math.*

/** Tailwind's original cubic-bezier(.4,0,.2,1), solved continuously. */
internal fun pendoloWidthEase(fraction: Double): Double {
    val p=fraction.coerceIn(0.0,1.0)
    if(p==0.0||p==1.0)return p
    var lo=0.0;var hi=1.0
    repeat(48){val u=(lo+hi)/2;val x=3*(1-u)*(1-u)*u*.4+3*(1-u)*u*u*.2+u*u*u;if(x<p)lo=u else hi=u}
    val u=(lo+hi)/2
    return 3*(1-u)*u*u+u*u*u
}

internal data class PendoloWidthResponse(val time:Double,val after:Boolean,val from:Double,val to:Double,
    val duration:Double,val reverseStart:Double,val factor:Double) {
    fun progress(t:Double)=if(duration==0.0)1.0 else pendoloWidthEase((t-time)/duration)
    fun value(t:Double)=from+(to-from)*progress(t)
}

/** CSS Transitions §3 reversing-shortening rule, checked against real Blink
 * transitions as well as the unchanged Pendolo subtree. */
internal fun pendoloRetargetWidth(previous:PendoloWidthResponse?,time:Double,after:Boolean,
    next:Double,retained:Boolean):PendoloWidthResponse {
    val from=if(retained&&previous!=null)previous.value(time) else next
    val reversing=retained&&previous!=null&&time<previous.time+previous.duration&&next==previous.reverseStart
    val factor=if(reversing)abs(previous.progress(time)*previous.factor+(1-previous.factor)).coerceIn(0.0,1.0)else 1.0
    val duration=if(retained&&from!=next).2*factor else 0.0
    return PendoloWidthResponse(time,after,from,next,duration,if(reversing)previous.to else from,factor)
}

/** Only retained idle main DIVs transition their max-width. Focal spans and
 * newly mounted/replaced nodes start at their target. Subtitles do not animate
 * max-width. This is media-event history, not observed display-frame history. */
internal class PendoloWidthTrack(lines: List<PendoloLineTime>, width: Double, height: Double,
    heights: List<Double>, tuning: PendoloGeometryTuning = PendoloGeometryTuning()) {
    private val timeline=PendoloTimeline(lines)
    private val states:Map<Double,List<PendoloWheelItem>>
    private val tracks:Map<Int,List<PendoloWidthResponse>>
    private val limit=pendoloAvailableTextWidth(width,height,tuning)
    init {
        val targets=(listOf(timeline.anchor(0.0))+timeline.boundaries.filter{it>=0}.flatMap{listOf(timeline.anchor(it),timeline.anchor(it,true))}).distinct()
        states=targets.associateWith{pendoloWheelLayout(lines.size,it,0.0,width,height,tuning,min(width,height)*.06,heights)}
        val prepared=mutableMapOf<Int,MutableList<PendoloWidthResponse>>()
        var previous=emptyMap<Int,PendoloWheelItem>()
        val events=listOf(Triple(0.0,false,timeline.anchor(0.0)))+timeline.boundaries.filter{it>=0}.flatMap{t->listOf(Triple(t,false,timeline.anchor(t)),Triple(t,true,timeline.anchor(t,true)))}
        for((time,after,target)in events){
            val items=states.getValue(target)
            for(item in items){
                val next=limit/item.scale
                val old=previous[item.index];val list=prepared.getOrPut(item.index){mutableListOf()};val span=list.lastOrNull()
                val retained=old!=null&&!old.active&&!item.active
                if(retained&&span!=null&&span.to==next)continue
                list+=pendoloRetargetWidth(span,time,after,next,retained)
            }
            previous=items.associateBy{it.index}
        }
        tracks=prepared
    }
    fun at(time:Double,reducedMotion:Boolean=false):Map<Int,Double> {
        require(time.isFinite())
        val t=max(0.0,time)
        val items=states.getValue(timeline.anchor(t))
        return items.filterNot{it.active}.associate{item->
            val list=tracks.getValue(item.index)
            var lo=0;var hi=list.size
            while(lo<hi){val m=(lo+hi)/2;val s=list[m];if(s.time<t||s.time==t&&!s.after)lo=m+1 else hi=m}
            item.index to if(reducedMotion)limit/item.scale else list[max(0,lo-1)].value(t)
        }
    }
}
