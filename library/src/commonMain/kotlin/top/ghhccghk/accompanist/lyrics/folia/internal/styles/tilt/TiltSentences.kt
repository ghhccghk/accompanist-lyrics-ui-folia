// Copyright (c) 2026 chthollyphile
// Kotlin adaptation of utils/lyrics/sentenceLayout.ts at Folia cc6226e4.
// SPDX-License-Identifier: AGPL-3.0-only
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.tilt

import top.ghhccghk.accompanist.lyrics.folia.internal.text.graphemeBoundaries
import top.ghhccghk.accompanist.lyrics.folia.internal.text.segmentWords
import kotlin.math.*

/** Source punctuation hierarchy and seeded fallback; all offsets remain UTF-16. */
internal object TiltSentences {
    private val cjk=Regex("[\\u4e00-\\u9fa5\\u3040-\\u30ff\\uac00-\\ud7af]")
    private val punct=Regex("[，。；！？、…·.,;!?]+")
    private val western=Regex("[a-zA-Z0-9]+(?:[a-zA-Z0-9'\\-]*[a-zA-Z0-9]+)?(?:\\s+[a-zA-Z0-9]+(?:[a-zA-Z0-9'\\-]*[a-zA-Z0-9]+)?)+[.,;:!?。，；：！？]?\\s*")
    private fun MutableList<String>.append(value:String) {
        if(isEmpty()) add(value) else this[lastIndex]=last()+value
    }
    fun split(text:String,target:Int,seed:Double=0.0):List<String> {
        if(target in 0..1)return listOf(text)
        val levels=if(target in -5..-1)-target else 5
        var result=listOf(text)
        for(level in 1..levels) {
            if(target>0 && result.size>=target)break
            result=result.flatMap { when(level) {
                1->punctuation(it);2->pairs(it);3->westernBlocks(it);4->cjkSpaces(it);else->special(it)
            } }
        }
        val sentences=result.toMutableList()
        while(target>0 && sentences.size<target) {
            val candidates=sentences.indices.filter{sentences[it].length>2}
            if(candidates.isEmpty())break
            data class Candidate(val index:Int,val position:Int,val score:Double)
            val semantic=candidates.mapNotNull { index ->
                val value=sentences[index]
                var at=0
                val words=segmentWords(value).mapNotNull { word ->
                    val start=at;at+=word.text.length
                    if(word.wordLike)start else null
                }
                if(words.size<2)null else {
                    val midpoint=value.length/2.0
                    val position=words.drop(1).minBy{abs(it-midpoint)}
                    Candidate(index,position,(1-abs(position-midpoint)/midpoint)*10+words.size)
                }
            }
            val best=semantic.maxByOrNull{it.score}
            val index:Int
            val position:Int
            if(best!=null) {index=best.index;position=best.position}
            else {
                val hash=sentences.sumOf{it.sumOf{c->c.code}}+sentences.size+seed
                val random=(sin(hash)*10000).let{it-floor(it)}
                index=candidates[floor(random*candidates.size).toInt()]
                val value=sentences[index]
                // The original fallback cuts UTF-16 in half. Keep that result
                // only at an EGC boundary; an indivisible cluster cannot split.
                position=graphemeBoundaries(value).filter{it>0 && it<value.length}
                    .minByOrNull{abs(it-floor(value.length/2.0))}?:break
            }
            val value=sentences.removeAt(index)
            sentences.addAll(index,listOf(value.substring(0,position),value.substring(position)))
        }
        while(target>0 && sentences.size>target) {
            val index=(0 until sentences.lastIndex).minBy{sentences[it].length+sentences[it+1].length}
            sentences[index]+=sentences.removeAt(index+1)
        }
        // Punctuation/space boundaries can also meet a combining sequence.
        val boundaries=graphemeBoundaries(text)
        val safe=mutableListOf<String>();var offset=0;var pending=""
        sentences.forEach { pending+=it;offset+=it.length
            if(offset in boundaries){safe+=pending;pending=""}
        }
        if(pending.isNotEmpty())safe.append(pending)
        return safe.ifEmpty{listOf(text)}
    }
    private fun punctuation(text:String):List<String> {
        val matches=punct.findAll(text).toList()
        if(matches.isEmpty())return listOf(text)
        val out=mutableListOf<String>();var at=0
        matches.forEach { m ->
            val end=m.range.last+1
            val trailing=text.substring(end).takeWhile{it.isWhitespace()}
            val before=text.substring(at,m.range.first)
            if(before.isNotEmpty())out+=before+m.value+trailing else out.append(m.value+trailing)
            at=end+trailing.length
        }
        if(at<text.length)out+=text.substring(at)
        return out.filter{it.isNotEmpty()}
    }
    private fun pairs(text:String):List<String> {
        val definitions=listOf('「' to '」','『' to '』','《' to '》','【' to '】','｛' to '｝',
            '［' to '］','[' to ']','（' to '）','(' to ')','"' to '"','\'' to '\'')
        val best=definitions.mapNotNull{(open,close)->
            val start=text.indexOf(open)
            val end=if(start<0)-1 else text.indexOf(close,start+1)
            if(end<0)null else start to end
        }.sortedWith(compareBy<Pair<Int,Int>>{it.first}.thenByDescending{it.second}).firstOrNull()
            ?:return listOf(text)
        return buildList {
            if(best.first>0)addAll(pairs(text.substring(0,best.first)))
            add(text.substring(best.first,best.second+1))
            if(best.second+1<text.length)addAll(pairs(text.substring(best.second+1)))
        }
    }
    private fun westernBlocks(text:String):List<String> {
        if(!cjk.containsMatchIn(text))return listOf(text)
        val matches=western.findAll(text).toList()
        if(matches.none{Regex("[a-zA-Z0-9]+").findAll(it.value).count()>1})return listOf(text)
        val out=mutableListOf<String>();var at=0
        matches.forEach { m ->
            val before=text.substring(at,m.range.first)
            val separator=before.lastOrNull()
            val skip=separator=='-' || (separator!=null && separator in ":：/／|｜" && text.count{it==separator}>=2)
            if(skip)out+=before+m.value else {
                if(before.isNotEmpty())out+=before
                out+=m.value
            }
            at=m.range.last+1
        }
        if(at<text.length)out+=text.substring(at)
        return if(out.size>1)out else listOf(text)
    }
    private fun cjkSpaces(text:String):List<String> {
        if(!cjk.containsMatchIn(text))return listOf(text)
        val out=mutableListOf<String>();var current="";var inCjk=false
        text.forEach { char ->
            when {
                cjk.matches(char.toString())->{current+=char;inCjk=true}
                char in 'a'..'z'||char in 'A'..'Z'||char in '0'..'9'->current+=char
                char.isWhitespace()->{
                    if(current.isNotEmpty() && (char=='\u3000' || inCjk && cjk.matches(current.last().toString()))) {
                        out+=current+char;current="";inCjk=false
                    } else if(current.isEmpty() && out.isNotEmpty())out.append(char.toString()) else current+=char
                }
                current.isEmpty() && out.isNotEmpty()->out.append(char.toString())
                else->current+=char
            }
        }
        if(current.isNotEmpty()) {
            if(current.all{it.isWhitespace()} && out.isNotEmpty())out.append(current) else out+=current
        }
        return if(out.size>1)out else listOf(text)
    }
    private fun special(text:String):List<String> {
        val parts=text.split(Regex("[：:/／\\\\|｜~～]+"))
        if(parts.count{it.isNotEmpty()}<=1)return listOf(text)
        val out=mutableListOf<String>();var at=0
        parts.forEach { part ->
            val index=text.indexOf(part,at)
            if(index>at)out.append(text.substring(at,index))
            if(part.isNotEmpty()){out+=part;at=index+part.length} else if(index>=0)at=index
        }
        if(at<text.length)out.append(text.substring(at))
        return out.filter{it.isNotEmpty()}
    }
}
