// Copyright (c) 2026 chthollyphile
// VisualizerTilt.tsx layout helpers, fixed Folia cc6226e4. SPDX-License-Identifier: AGPL-3.0-only
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.tilt

import kotlin.math.*

internal data class TiltConfig(val splitProbability:Double=.75,val tiltProbability:Double=.35) {
    init {require(splitProbability.isFinite() && splitProbability in 0.0..1.0)
        require(tiltProbability.isFinite() && tiltProbability in 0.0..1.0)}
}
internal data class TiltSegment(val text:String,val isTilt:Boolean,val shortLast:Boolean,val charOffset:Int)
internal data class TiltLayout(val segments:List<TiltSegment>,val scale:Double)
internal fun tiltRandom(seed:Double,offset:Int)=(sin(seed*1000+offset)*10000).let{it-floor(it)}
internal fun tiltLineCount(charCount:Int,seed:Double,splitProbability:Double):Int {
    val score=ln(charCount+4.0)/ln(24.0)*(tiltRandom(seed,1)*.6+.7)*splitProbability
    return when {score<.45->1;score<1.05->2;score<1.7->3;else->4}
}

/** Measures intact runs at max(viewport*6.875%,90*fontScale), as the original does.
 * The callback returns CSS pixels; actual shaping stays in the platform renderer. */
internal fun buildTiltLayout(text:String,seed:Double,config:TiltConfig,viewportWidth:Double,
    fontScale:Double=1.0,measure:(String,Boolean,Double)->Double):TiltLayout {
    val measureSize=max(viewportWidth*.06875,90*fontScale)
    val available=max(320.0,viewportWidth)*.85
    val count=tiltLineCount(text.trim().length,seed,config.splitProbability)
    val split=if(Regex("[\\s.…·。]+").matches(text.trim()))listOf(text) else TiltSentences.split(text,count,seed)
    val candidates=split.indices.filter{tiltRandom(seed,100+it)<config.tiltProbability}
    val tilt=if(candidates.isEmpty())-1 else candidates[floor(tiltRandom(seed,200)*candidates.size).toInt()]
    fun segments(values:List<String>,tiltIndex:Int):List<TiltSegment> {
        var offset=0
        return values.mapIndexed { i,value ->
            TiltSegment(value.trim(),i==tiltIndex,false,offset+value.length-value.trimStart().length)
                .also{offset+=value.length}
        }
    }
    fun shortLast(values:List<TiltSegment>):List<TiltSegment> {
        if(values.size<2)return values
        val last=values.last()
        return if(!last.isTilt && last.text.trim().length<=2 && last.text.trim().length*2<=values[values.lastIndex-1].text.length)
            values.dropLast(1)+last.copy(shortLast=true) else values
    }
    val parts=segments(split,tilt)
    fun width(s:TiltSegment)=measure(s.text,s.isTilt,measureSize)
    val widestTilt=parts.filter{it.isTilt}.maxByOrNull(::width)
    val widestNormal=parts.filterNot{it.isTilt}.maxByOrNull(::width)
    val tiltWidth=widestTilt?.let(::width)?:0.0
    val normalWidth=widestNormal?.let(::width)?:0.0
    val overflow=max(tiltWidth,normalWidth)/available
    if(overflow>=1.6) {
        val total=measure(text,false,measureSize)
        val extra=min(4,max(parts.size+1,ceil(total/(available*1.45)).toInt()))
        if(extra>parts.size) {
            val values=TiltSentences.split(text,extra,seed)
            val picked=if(values.isNotEmpty() && tiltRandom(seed,300)<config.tiltProbability)
                floor(tiltRandom(seed,301)*values.size).toInt() else -1
            val revised=segments(values,picked)
            val tilted=revised.firstOrNull{it.isTilt}
            val normal=revised.filterNot{it.isTilt}.maxByOrNull(::width)
            // Keep the original post-resplit branch: if a tilt segment exists,
            // its size determines the fit even when a normal row is wider.
            val selected=tilted?:normal
            val scale=selected?.let{if(width(it)>available)max(if(it.isTilt).5 else .55,available/width(it))else 1.0}?:1.0
            return TiltLayout(shortLast(revised),scale)
        }
    }
    val scale=if(overflow<=1)1.0 else if(tiltWidth>=normalWidth && widestTilt!=null)
        max(.5,available/tiltWidth) else if(widestNormal!=null)max(.55,available/normalWidth) else 1.0
    return TiltLayout(shortLast(parts),scale)
}

/** The pulse is not a loop: a 0.2–0.9s sine, then a ramp to the retained 25% tail. */
internal fun tiltPulse(time:Double,start:Double,end:Double):Double {
    val duration=max(end-start,.05).coerceIn(.2,.9)
    val elapsed=time-start
    if(elapsed<0)return 0.0
    if(elapsed<=duration)return sin(elapsed/duration*PI)
    return .25*((elapsed-duration)/(duration*1.2)).coerceAtMost(1.0)
}
