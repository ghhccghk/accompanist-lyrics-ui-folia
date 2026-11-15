// Copyright (c) 2026 chthollyphile
// VisualizerTilt.tsx findSegmentWordRange/buildCharTimings, fixed cc6226e4.
// SPDX-License-Identifier: AGPL-3.0-only
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.tilt

import top.ghhccghk.accompanist.lyrics.folia.internal.text.*
import kotlin.math.max

internal data class TiltWord(val text:String,val start:Double,val end:Double,
    val syllables:List<TiltWord> = emptyList())
internal data class TiltCharTime(val index:Int,val start:Double,val end:Double)

/** Original search semantics, including a segment that starts/ends inside a word. */
internal fun tiltWordRange(offset:Int,text:String,fullText:String,words:List<TiltWord>):IntRange {
    if(offset<0 || offset>=fullText.length)return words.indices
    val end=offset+text.length
    var at=0;var first=0;var last=words.size
    words.forEachIndexed { i,word ->
        val start=fullText.indexOf(word.text,at)
        if(start<0){at+=word.text.length;return@forEachIndexed}
        val finish=start+word.text.length
        if(start<=offset && finish>offset)first=i
        if(start<end && finish>=end){last=i+1;return first until last}
        at=finish
    }
    return first until last
}

/** UTF-16 ranges are retained by the caller; this is the source's nonspace timing index. */
internal fun buildTiltCharTimings(offset:Int,text:String,start:Double,end:Double,
    fullText:String,words:List<TiltWord>):List<TiltCharTime> {
    fun nonspaces(value:String)=graphemeBoundaries(value).sorted().zipWithNext{a,b->value.substring(a,b)}
        .filterNot{it.all{c->c.isWhitespace()}}
    val count=nonspaces(text).size
    if(count==0)return emptyList()
    val out=mutableListOf<TiltCharTime>()
    for(i in tiltWordRange(offset,text,fullText,words)) {
        val word=words[i]
        if(word.syllables.isNotEmpty()) {
            // The shared timing helper also coalesces source fragments crossing an EGC.
            var begin=0
            val fragments=word.syllables.map {
                val from=begin;begin+=it.text.length
                top.ghhccghk.accompanist.lyrics.folia.internal.model.TimedFragment(from,begin,
                    (it.start*1e6).toLong(),(it.end*1e6).toLong())
            }
            sourceGraphemeTimings(word.text,fragments,(word.start*1e6).toLong(),(word.end*1e6).toLong())
                .filterNot{word.text.substring(it.begin,it.end).all{c->c.isWhitespace()}}
                .forEach { if(out.size<count)out+=TiltCharTime(out.size,it.startSeconds,it.endSeconds) }
        } else {
            val size=nonspaces(word.text).size
            val duration=max(word.end-word.start,.05)
            for(c in 0 until size)if(out.size<count)
                out+=TiltCharTime(out.size,word.start+c*duration/size,word.start+(c+1)*duration/size)
        }
    }
    if(out.size==count)return out
    val duration=max(end-start,.3)
    return (0 until count).map{TiltCharTime(it,start+it*duration/count,start+(it+1)*duration/count)}
}
