// Copyright (c) 2026 chthollyphile and contributors
// Folia cc6226e4 cadenzaFragments.ts, cadenzaPreparedState.ts and graphemeTiming.ts.
// SPDX-License-Identifier: AGPL-3.0-only. Provenance/adaptations: docs/upstream.md.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.cadenza

import top.ghhccghk.accompanist.lyrics.folia.internal.model.NormalizedLine
import top.ghhccghk.accompanist.lyrics.folia.internal.text.graphemeBoundaries
import kotlin.math.*

internal data class CadenzaTextSegment(val text: String, val width: Double,
    val advances: List<Double>? = null)
internal data class CadenzaWrappedLine(val start: Int, val end: Int, val width: Double)
internal data class CadenzaWordRange(val wordIndex: Int, val word: CadenzaWord, val start: Int, val end: Int,
    val color: String, val timings: List<CadenzaGlyphTime>)
internal data class CadenzaPreparationSize(val fontPx: Double, val lineHeight: Double, val maxWidth: Double)

internal fun cadenzaPreparationSize(line: CadenzaLyric, viewportWidth: Double,
    preparedText: String = line.text, fontScale: Double = 1.12, widthRatio: Double = .72): CadenzaPreparationSize {
    val count = max(cadenzaGraphemes(line.text).size,1)
    val words = max(line.words.size,1)
    val base = (viewportWidth * .086).coerceIn(34.0,94.0)
    val lengthPenalty = if(count>12)min((count-12)*1.8,34.0) else 0.0
    val densityPenalty = if(words>7)min((words-7)*1.5,18.0) else 0.0
    val font = ((base-lengthPenalty-densityPenalty).coerceIn(28.0,104.0)*fontScale).coerceIn(24.0,132.0)
    val height = floor(font*(if(cadenzaIsCjk(preparedText))1.22 else 1.1)+.5)
    val available = max(viewportWidth-48,120.0)
    val preparedCount = cadenzaGraphemes(preparedText).size
    val compression = if(preparedCount>12)(.92-(preparedCount-12)*.018).coerceIn(.62,.92) else .92
    val width = min(viewportWidth*widthRatio*compression,820.0).coerceIn(min(220.0,available),available)
    return CadenzaPreparationSize(font,height,width)
}

/** The source flattens syllables; only parser fragments cutting an EGC need a platform adaptation. */
internal fun cadenzaWordTimings(word: CadenzaWord): List<CadenzaGlyphTime> {
    fun even(text: String,start: Double,end: Double): List<CadenzaGlyphTime> {
        val gs=cadenzaGraphemes(text);val unit=max(end-start,0.0)/max(gs.size,1)
        return gs.mapIndexed{i,g->CadenzaGlyphTime(g,start+unit*i,if(i==gs.lastIndex)end else start+unit*(i+1))}
    }
    if(word.syllables.isEmpty())return even(word.text,word.start,word.end)
    val pieces=word.syllables.flatMap{even(it.text,it.start,it.end)}
    val edges=graphemeBoundaries(word.text)
    var offset=0
    if(pieces.all { offset+=it.text.length;offset in edges })return pieces
    // Rebuild spans from zero after the safety scan (which can short-circuit).
    var cursor=0
    val raw=pieces.map{g->val start=cursor;cursor+=g.text.length;Triple(start,cursor,g)}
    return edges.sorted().zipWithNext{a,b->
        val cover=raw.filter{it.second>a&&it.first<b}.map{it.third}
        CadenzaGlyphTime(word.text.substring(a,b),cover.minOfOrNull{it.start}?:word.start,cover.maxOfOrNull{it.end}?:word.end)
    }
}

/** Match source words forward against the *prepared* grapheme stream, including empty/fallback words. */
internal fun findCadenzaWordRanges(line: CadenzaLyric, graphemes: List<String>,
    color: (String)->String = {"#f17b4d"}): List<CadenzaWordRange> {
    var cursor=0
    return line.words.mapIndexed { wi,word ->
        val target=cadenzaGraphemes(word.text)
        val matched=(cursor..graphemes.size-target.size).firstOrNull { at->target.indices.all{graphemes[at+it]==target[it]} }
        val start=matched?:cursor.coerceIn(0,graphemes.size)
        val end=(start+target.size).coerceIn(start,graphemes.size);cursor=end
        CadenzaWordRange(wi,word,start,end,color(word.text),cadenzaWordTimings(word))
    }
}

/** Same pretext-segment width arithmetic; full shaped rows may also provide measured selection advances. */
internal fun buildCadenzaFragmentLines(segments: List<CadenzaTextSegment>, lines: List<CadenzaWrappedLine>,
    ranges: List<CadenzaWordRange>): List<CadenzaFragmentLine> {
    val graphemes=segments.flatMap{cadenzaGraphemes(it.text)}
    var cursor=0
    val offsets=segments.map{val start=cursor;cursor+=cadenzaGraphemes(it.text).size;start until cursor}
    fun widthBetween(start: Int,end: Int): Double {
        if(end<=start)return 0.0
        var width=0.0
        for((i,range)in offsets.withIndex()) {
            val segment=segments[i];val s=range.first;val e=range.last+1
            if(end<=s)break
            if(start>=e)continue
            val a=max(start,s)-s;val b=min(end,e)-s
            if(b<=a)continue
            width+=when {
                a==0&&b==e-s ->segment.width
                !segment.advances.isNullOrEmpty() -> (a until b).sumOf{segment.advances.getOrNull(it)?:0.0}
                e==s ->segment.width
                else ->segment.width*(b-a)/(e-s)
            }
        }
        return width
    }
    val raw=lines.map { line ->
        CadenzaFragmentLine(line.width,line.start,line.end,ranges.mapNotNull { r ->
            if(r.end<=line.start||r.start>=line.end)null else {
                val a=max(r.start,line.start);val b=min(r.end,line.end)
                CadenzaFragment(r.wordIndex,r.word,graphemes.subList(a,b).joinToString(""),r.color,
                    widthBetween(line.start,a),widthBetween(line.start,b),a-r.start,b-r.start,
                    max(r.end-r.start,1),r.timings)
            }
        })
    }
    val counts=raw.flatMap{it.fragments}.groupingBy{it.wordIndex}.eachCount()
    val seen=mutableMapOf<Int,Int>()
    return raw.map{line->line.copy(fragments=line.fragments.map{f->
        val count=counts.getValue(f.wordIndex);val index=seen[f.wordIndex]?:0;seen[f.wordIndex]=index+1
        f.copy(indexInWord=index,countInWord=count,primary=index==0,split=count>1)
    })}
}

/** Source ingestion normally annotates lines before Cadenza. Keep that step explicit. */
internal fun annotateCadenzaLine(line: CadenzaLyric): CadenzaLyric {
    if(line.hints!=null)return line
    val raw=max(line.end-line.start,0.0)
    val transition=when{raw<.10->CadenzaTransition.NONE;raw<.18->CadenzaTransition.FAST;else->CadenzaTransition.NORMAL}
    val reveal=when{raw<.10->CadenzaReveal.INSTANT;raw<.18->CadenzaReveal.FAST;else->CadenzaReveal.NORMAL}
    val timing=cadenzaTransitionTiming(raw,transition,reveal)
    val end=if(transition==CadenzaTransition.NONE)max(line.end,line.start+.067) else {
        val pass=max(line.words.lastOrNull()?.end?:line.end,line.start)+timing.hold
        val exitStart=if(transition==CadenzaTransition.FAST)maxOf(line.start+timing.enter+.01,pass,line.end-timing.exit)
            else max(pass,line.end-timing.exit)
        max(line.end,exitStart+timing.exit)
    }
    return line.copy(hints=CadenzaHints(raw,end,transition,reveal))
}

/** Published lyrics-core normalization only; no dependency on classic grouping or lyrics-ui. */
internal fun cadenzaLyric(line: NormalizedLine): CadenzaLyric {
    val edges=graphemeBoundaries(line.text);val words=mutableListOf<CadenzaWord>();val pending=mutableListOf<CadenzaWord>()
    line.fragments.forEachIndexed{i,f->
        pending+=CadenzaWord(line.text.substring(f.begin,f.end),f.startUs/1e6,f.endUs/1e6)
        if(f.end in edges||i==line.fragments.lastIndex) {
            words+=if(pending.size==1)pending.single() else CadenzaWord(pending.joinToString(""){it.text},
                pending.minOf{it.start},pending.maxOf{it.end},pending.toList())
            pending.clear()
        }
    }
    return annotateCadenzaLine(CadenzaLyric(line.text,line.startUs/1e6,line.endUs/1e6,words))
}
