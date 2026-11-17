// Copyright (c) 2026 chthollyphile and contributors
// Folia cc6226e4 pendoloTextLayout.ts / monetLyricsModel.ts:measureMonetGraphemeOffsets.
// SPDX-License-Identifier: AGPL-3.0-only. See docs/upstream.md for platform differences.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.pendolo

import androidx.compose.ui.text.*
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.cappella.cappellaCanvasAdvance
import top.ghhccghk.accompanist.lyrics.folia.internal.text.*
import kotlin.math.*

internal data class PendoloTextRun(val path: Path, val shift: Float)
internal data class PendoloTextRow(val text: String, val width: Double, val graphemeStart: Int,
    val graphemeEnd: Int, val offsets: List<Double>, val paragraph: TextLayoutResult,
    val trackingRuns: List<PendoloTextRun> = emptyList())
internal data class PendoloPreparedText(val rows: List<PendoloTextRow>, val lineHeight: Double,
    val height: Double, val metrics: NaturalFontMetrics)

/** Independent platform wrapping and complete-run shaping. The shared native
 * Canvas-advance adapter was first calibrated for Cappella; no source geometry
 * or browser-generated expected row is read here. */
internal fun preparePendoloText(text: String, fontPx: Double, width: Double, measurer: TextMeasurer,
    base: TextStyle, density: Density, resolver: FontFamily.Resolver,
    lineHeight: Double = floor(fontPx * 1.2 + .5)): PendoloPreparedText {
    val style = classicBodyRasterStyle(base.copy(fontSize = fontPx.sp, lineHeight = TextUnit.Unspecified,
        textAlign = TextAlign.Left, textMotion = TextMotion.Animated,
        textDirection = base.textDirection.takeUnless { it == TextDirection.Unspecified } ?: TextDirection.Ltr))
    val safe = text.ifEmpty { " " }
    fun missingSpaceTracking(s: String): Double = if (style.letterSpacing.isSp &&
        s.any { it in '\u0600'..'\u06ff' || it in '\u0750'..'\u077f' || it in '\u08a0'..'\u08ff' } &&
        s.none { it.isLetter() && it.code < 0x600 }) style.letterSpacing.value.toDouble() else 0.0
    fun advance(s: String): Double {
        if (s.isEmpty()) return 0.0
        if (style.letterSpacing.isSp && style.letterSpacing.value != 0f) {
            return measurer.measure(s, style, softWrap = false).getPathForRange(0, s.length).getBounds().width / density.density.toDouble() +
                s.count { it == ' ' } * missingSpaceTracking(s)
        }
        // pretext measures complete word segments and retains pre-wrap spaces.
        val segments = mutableListOf<String>()
        for (part in segmentWords(s)) {
            if (part.text == "-" && segments.isNotEmpty()) segments[segments.lastIndex] += part.text else segments += part.text
        }
        return segments.sumOf { value -> cappellaCanvasAdvance(value, style, resolver, density) ?:
            (measurer.measure(value, style, softWrap = false).getPathForRange(0, value.length).getBounds().width / density.density.toDouble()) }
    }
    // Active source uses pretext, whereas idle nodes use CSS wrapping. In
    // particular, after splitting an oversized word pretext tests the NEXT
    // WHOLE word against the residual space, instead of packing its prefix.
    // Restrict this independent segment walk to ASCII: complex scripts retain
    // platform line breaking and complete shaping until separately calibrated.
    val asciiActive = style.letterSpacing == TextUnit.Unspecified && safe.all { it in ' '..'~' || it == '\n' }
    val strings = if(asciiActive) pendoloAsciiWrap(safe,width,::advance) else {
        val fits = safe.split('\n').all { advance(it) <= width }
        val layout = if (fits) measurer.measure(safe, style, softWrap = false) else
            measurer.measure(safe, style, constraints = Constraints(maxWidth =
                floor(max(1.0, width) * density.density).toInt().coerceAtLeast(1)))
        (0 until layout.lineCount).map { i ->
            val a=layout.getLineStart(i)
            val b=layout.getLineEnd(i).let { e -> if(e>a && safe[e-1]=='\n')e-1 else e }
            check(a in graphemeBoundaries(safe) && b in graphemeBoundaries(safe)) { "Pendolo platform wrap cuts a grapheme" }
            safe.substring(a,b)
        }
    }
    var cursor = 0
    val rows = strings.map { row ->
        val chars = pendoloGraphemes(row)
        val start = cursor
        cursor += chars.size
        val edges = graphemeBoundaries(row).sorted()
        // Original prefix measurement uses normal whitespace, so a trailing
        // ASCII space has zero *additional* width; row layout uses pre-wrap.
        val offsets = edges.map { end -> advance(row.substring(0, end).trimEnd(' ')) }
        val paragraph = measurer.measure(row.ifEmpty { " " }, style, softWrap = false)
        val spacing = missingSpaceTracking(row)
        val runs = if (spacing == 0.0 || ' ' !in row) emptyList() else {
            val spaces = row.indices.filter { row[it] == ' ' }.map { paragraph.getPathForRange(it, it + 1).getBounds().left }
            Regex("[^ ]+").findAll(row).map { word ->
                val path = paragraph.getPathForRange(word.range.first, word.range.last + 1)
                PendoloTextRun(path, (spaces.count { it < path.getBounds().left } * spacing * density.density).toFloat())
            }.toList()
        }
        PendoloTextRow(row, advance(row), start, cursor, offsets, paragraph, runs)
    }
    return PendoloPreparedText(rows, lineHeight, max(rows.size, 1) * lineHeight, naturalFontMetrics(style, resolver, density))
}

/** Independent ASCII segment walker for the source pre-wrap active layout.
 * Whole words use shaped advance; emergency cuts use individual grapheme fit
 * advances like pretext0.0.9. This determines line boundaries only: drawing
 * and prefix masks still shape complete rows, including ligatures. */
internal fun pendoloAsciiWrap(text:String,width:Double,advance:(String)->Double):List<String> {
    val limit=max(1.0,width)
    return text.split('\n').flatMap { paragraph ->
        val result=mutableListOf<String>()
        var line=""; var used=0.0
        fun flush(){ result+=line;line="";used=0.0 }
        for(token in Regex(" +|[^ ]+").findAll(paragraph).map{it.value}) {
            if(token.all{it==' '}) { line+=token;used+=advance(token);continue }
            val whole=advance(token)
            if(line.isNotEmpty() && used+whole>limit)flush()
            if(whole<=limit){line+=token;used+=whole;continue}
            val chars=pendoloGraphemes(token)
            var cursor=0
            while(cursor<chars.size){
                var end=cursor;var fit=0.0;var preferred=-1
                while(end<chars.size){
                    val part=advance(chars[end])
                    if(end>cursor && fit+part>limit)break
                    fit+=part;end++
                    if(chars[end-1]=="-")preferred=end
                }
                if(end<chars.size && preferred>cursor)end=preferred
                line+=chars.subList(cursor,end).joinToString("")
                used=chars.subList(cursor,end).sumOf(advance)
                cursor=end
                if(cursor<chars.size)flush()
            }
        }
        if(line.isNotEmpty()||result.isEmpty())flush()
        result
    }
}
