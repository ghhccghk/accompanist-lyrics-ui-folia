// Copyright (c) 2026 chthollyphile and contributors
// Folia cc6226e4 wordColoring.ts / pendoloColorRuns.ts (range-based entry).
// SPDX-License-Identifier: AGPL-3.0-only. Keep distinct from token-word matching.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.pendolo

internal data class PendoloKeyword<C>(val word:String,val color:C)
internal data class PendoloColorRange<C>(val start:Int,val end:Int,val color:C,val priority:Int)
internal data class PendoloColorRun<C>(val start:Int,val text:String,val color:C)
private fun colorSpace(c:Char)=c in "\t\n\u000b\u000c\r \u00a0\u1680\u2028\u2029\u202f\u205f\u3000\ufeff"||c in '\u2000'..'\u200a'
private fun asciiColorWord(s:String)=s.lowercase().filter{it in 'a'..'z'||it in '0'..'9'||it=='_'}
private fun colorCjk(s:String)=s.any{it in '\u4e00'..'\u9fa5'||it in '\u3040'..'\u30ff'||it in '\uac00'..'\ud7af'}

/** Preserve priority by original UTF-16 target length and stable source order.
 * English matching is deliberately ASCII \w, not Unicode word segmentation. */
internal fun <C> pendoloColorRanges(text:String,entries:List<PendoloKeyword<C>>):List<PendoloColorRange<C>> {
    val candidates=mutableListOf<PendoloColorRange<C>>()
    val english=linkedMapOf<String,Pair<C,Int>>()
    for(entry in entries){
        val target=entry.word.trim(::colorSpace)
        if(target.isEmpty())continue
        if(colorCjk(target)){
            var cursor=0
            while(cursor<text.length){val start=text.indexOf(target,cursor);if(start<0)break
                candidates+=PendoloColorRange(start,start+target.length,entry.color,target.length);cursor=start+target.length}
        }else{
            val words=target.split(Regex("[\\s\\u00a0\\u1680\\u2000-\\u200a\\u2028\\u2029\\u202f\\u205f\\u3000\\ufeff]+"))
                .map(::asciiColorWord).filter{it.isNotEmpty()}
            val priority=words.maxOfOrNull{it.length}?:continue
            for(word in words){val old=english[word];if(old==null||priority>old.second)english[word]=entry.color to priority}
        }
    }
    for(match in Regex("[A-Za-z0-9_]+").findAll(text)){
        val color=english[asciiColorWord(match.value)]?:continue
        candidates+=PendoloColorRange(match.range.first,match.range.last+1,color.first,color.second)
    }
    val selected=mutableListOf<PendoloColorRange<C>>()
    for(range in candidates.sortedWith(compareByDescending<PendoloColorRange<C>>{it.priority}.thenBy{it.start}.thenBy{it.end}))
        if(selected.none{it.start<range.end&&range.start<it.end})selected+=range
    return selected.sortedWith(compareBy<PendoloColorRange<C>>{it.start}.thenBy{it.end})
}

internal fun <C> pendoloGraphemeColors(text:String,ranges:List<PendoloColorRange<C>>):Map<Int,C> {
    var cursor=0;var rangeIndex=0
    return buildMap {
        for((index,g)in pendoloGraphemes(text).withIndex()){
            val start=cursor;cursor+=g.length
            while(rangeIndex<ranges.size&&ranges[rangeIndex].end<=start)rangeIndex++
            ranges.getOrNull(rangeIndex)?.takeIf{it.start<cursor&&start<it.end}?.let{put(index,it.color)}
        }
    }
}

/** Adjacent equal colors coalesce; starts are global grapheme indices. */
internal fun <C> pendoloColorRuns(text:String,start:Int,colors:Map<Int,C>,fallback:C):List<PendoloColorRun<C>> {
    val runs=mutableListOf<PendoloColorRun<C>>()
    for((i,g)in pendoloGraphemes(text).withIndex()){
        val color=colors[start+i]?:fallback;val old=runs.lastOrNull()
        if(old?.color==color)runs[runs.lastIndex]=old.copy(text=old.text+g)
        else runs+=PendoloColorRun(start+i,g,color)
    }
    return runs
}
