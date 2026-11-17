// Compose measurement adapter for pinned Folia Cadenza preparation.
// SPDX-License-Identifier: AGPL-3.0-only. Source/adaptations: docs/upstream.md.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.cadenza

import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.*
import top.ghhccghk.accompanist.lyrics.folia.internal.text.cssWordText
import top.ghhccghk.accompanist.lyrics.folia.internal.text.graphemeBoundaries
import kotlin.math.*

internal data class CadenzaPreparedText(val text: String, val size: CadenzaPreparationSize, val style: TextStyle,
    val lines: List<CadenzaWrappedLine>, val fragments: List<CadenzaFragmentLine>, val placements: List<CadenzaPlacement>)

/** Independent platform layout. No source fixtures, pretext runtime or classic list state. */
internal fun prepareCadenzaText(line: CadenzaLyric, viewportWidth: Double, measurer: TextMeasurer,
    baseStyle: TextStyle, density: Density, fontScale: Double = 1.12, widthRatio: Double = .72,
    intensity: CadenzaIntensity = CadenzaIntensity.NORMAL, color: (String)->String = {"#f17b4d"}): CadenzaPreparedText {
    val text=cssWordText(line.text).text
    val size=cadenzaPreparationSize(line,viewportWidth,text,fontScale,widthRatio)
    val style=baseStyle.copy(fontSize=(floor(size.fontPx*100+1e-9)/100).sp,
        fontWeight=baseStyle.fontWeight?:FontWeight.Bold,textAlign=TextAlign.Left,
        textDirection=baseStyle.textDirection.takeUnless{it==TextDirection.Unspecified}?:TextDirection.Ltr,
        textMotion=baseStyle.textMotion?:TextMotion.Animated,lineHeight=TextUnit.Unspecified)
    fun natural(value: String)=measurer.measure(value,style,softWrap=false)
    fun width(value: String): Double {
        if(value.isEmpty())return 0.0
        val layout=natural(value)
        // Canvas measureText includes a trailing space. getLineRight hangs it,
        // which would transfer its width from the preceding token to the next.
        return layout.getPathForRange(0,value.length).getBounds().width/density.density.toDouble()
    }
    val edges=graphemeBoundaries(text).sorted()
    val positions=edges.map{width(text.substring(0,it))}
    val segment=CadenzaTextSegment(text,width(text),positions.zipWithNext{a,b->b-a})
    val wrapped=if(width(text)<=size.maxWidth)natural(text) else measurer.measure(text,style,
        constraints=Constraints(maxWidth=floor(size.maxWidth*density.density).toInt().coerceAtLeast(1)))
    val lines=if(text.isEmpty())emptyList()else (0 until wrapped.lineCount).map{row->
        val a=wrapped.getLineStart(row);val b=wrapped.getLineEnd(row)
        val start=edges.binarySearch(a);val end=edges.binarySearch(b)
        check(start>=0&&end>=0){"Cadenza platform line break cuts a grapheme"}
        CadenzaWrappedLine(start,end,width(text.substring(a,b).trimEnd(' ')))
    }
    val fragments=buildCadenzaFragmentLines(listOf(segment),lines,findCadenzaWordRanges(line,cadenzaGraphemes(text),color))
    val placements=buildCadenzaPlacements(fragments,size.fontPx,size.lineHeight,size.maxWidth,intensity,
        line.start*1000,line.text=="......")
    return CadenzaPreparedText(text,size,style,lines,fragments,placements)
}
