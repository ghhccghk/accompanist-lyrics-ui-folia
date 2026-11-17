// Copyright (c) 2026 chthollyphile and contributors
// Folia cc6226e4 PendoloActiveLyricSweep.tsx, isolated non-chorus text scope.
// SPDX-License-Identifier: AGPL-3.0-only. See docs/upstream.md.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.pendolo

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.*
import top.ghhccghk.accompanist.lyrics.folia.internal.text.*
import top.ghhccghk.accompanist.lyrics.folia.FoliaWordColor
import kotlin.math.*

/** Isolated real sweep, not the full wheel. Parent supplies media time; no
 * coroutine or infinite animation survives pause, seek, or component disposal. */
@Composable
internal fun PendoloSweepTarget(line: PendoloLyric, time: () -> Double, maxWidth: Double,
    style: TextStyle, modifier: Modifier = Modifier, fontPx: Double = 28.0,
    primary: Color = Color(0xffdfe5ec), accent: Color = Color(0xfff17b4d),
    wordColors: List<FoliaWordColor> = emptyList(),
    onPrepared: (PendoloPreparedText) -> Unit = {}) {
    val density = LocalDensity.current
    val resolver = LocalFontFamilyResolver.current
    val measurer = rememberTextMeasurer(cacheSize = 32)
    val prepared = remember(line.text, fontPx, maxWidth, style, density, resolver, measurer) {
        preparePendoloText(line.text, fontPx, maxWidth, measurer, style, density, resolver)
    }
    val timings = remember(line) { pendoloLineTimeline(line) }
    val inks = remember(line,prepared,wordColors,primary,accent) {
        preparePendoloInk(line.text,prepared,wordColors,pendoloSweepColor(primary,accent))
    }
    val clock by rememberUpdatedState(time)
    SideEffect { onPrepared(prepared) }
    Canvas(modifier) { drawPendoloSweep(prepared, timings, line.end, fontPx, clock(), primary, accent,inks) }
}

internal data class PendoloInkRun(val path: Path,val color: Color)
internal fun pendoloSweepColor(primary:Color,accent:Color,amount:Double=.32):Color {
    fun channel(a:Float,b:Float)=floor(a*255+(b*255-a*255)*amount+.5).toInt()
    return Color(channel(primary.red,accent.red),channel(primary.green,accent.green),channel(primary.blue,accent.blue))
}
internal fun preparePendoloInk(text:String,prepared:PendoloPreparedText,words:List<FoliaWordColor>,fallback:Color):List<List<PendoloInkRun>>? {
    if(words.isEmpty())return null
    val colors=pendoloGraphemeColors(text,pendoloColorRanges(text,words.map{PendoloKeyword(it.word,it.color)}))
    if(colors.isEmpty())return null
    return prepared.rows.map{row->
        var cursor=0
        pendoloColorRuns(row.text,row.graphemeStart,colors,fallback).map{run->
            val start=cursor;cursor+=run.text.length
            PendoloInkRun(row.paragraph.getPathForRange(start,cursor),run.color)
        }
    }
}

internal fun DrawScope.drawPendoloSweep(prepared: PendoloPreparedText, timings: List<PendoloGlyphTime>,
    lineEnd: Double, fontPx: Double, t: Double, primary: Color, accent: Color,
    inks:List<List<PendoloInkRun>>?=null) {
    val fill = pendoloSweepColor(primary,accent)
    val d = density
    for ((index, row) in prepared.rows.withIndex()) {
        val p = row.paragraph
        val origin = Offset(0f, (index * prepared.lineHeight * d).toFloat() +
            cssFontBaseline(prepared.metrics.ascent, prepared.metrics.descent, (prepared.lineHeight * d).toFloat()) -
            paintedNaturalBaseline(p, prepared.metrics))
        // Both passes draw the entire shaped row. Only its opacity shader
        // changes; ligatures/Arabic joining are never rendered per glyph.
        drawText(p, primary.copy(alpha = .52f), origin, shadow = Shadow.None)
        val frame = pendoloSweepFrame(timings.subList(row.graphemeStart.coerceAtMost(timings.size),
            row.graphemeEnd.coerceAtMost(timings.size)), row.offsets, row.width, lineEnd, fontPx, t)
        if (frame.opacity == 0.0) continue
        val end = ((frame.width + frame.edge) * d).toFloat().coerceAtLeast(.001f)
        val brush = Brush.horizontalGradient(
            0f to fill,
            (max(frame.width - frame.edge, 0.0) * d / end).toFloat() to fill,
            (frame.width * d / end).toFloat() to fill.copy(alpha = .84f),
            1f to fill.copy(alpha = 0f), endX = end,
        )
        val runs=inks?.getOrNull(index)
        if(runs==null)drawText(p, brush, origin, alpha = frame.opacity.toFloat(), shadow = Shadow.None)
        else translate(origin.x,origin.y) {
            // Complete original shaped paragraph is painted through color-range
            // clips. Paint segmentation never reshapes an EGC/ligature/cursive run.
            val bounds=Rect(-64*d,-64*d,(row.width.toFloat()+64)*d,p.size.height+64*d)
            drawContext.canvas.saveLayer(bounds,Paint().apply{alpha=frame.opacity.toFloat()})
            for(run in runs)clipPath(run.path){drawText(p,run.color,shadow=Shadow.None)}
            val mask=Brush.horizontalGradient(0f to Color.White,
                (max(frame.width-frame.edge,0.0)*d/end).toFloat() to Color.White,
                (frame.width*d/end).toFloat() to Color.White.copy(alpha=.84f),1f to Color.Transparent,endX=end)
            drawRect(mask,bounds.topLeft,bounds.size,blendMode=BlendMode.DstIn)
            drawContext.canvas.restore()
        }
    }
}
