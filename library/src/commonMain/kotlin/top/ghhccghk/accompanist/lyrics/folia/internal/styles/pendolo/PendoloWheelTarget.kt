// Copyright (c) 2026 chthollyphile and contributors
// Folia cc6226e4 VisualizerPendolo.tsx text-wheel subtree; no gears/audio effects.
// SPDX-License-Identifier: AGPL-3.0-only. Scope/provenance: docs/upstream.md.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.pendolo

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.*
import top.ghhccghk.accompanist.lyrics.folia.internal.text.*
import kotlin.math.*

internal data class PendoloStageLine(val lyric: PendoloLyric, val subtitles: PendoloSubtitleSource = PendoloSubtitleSource())
internal data class PendoloPreparedSubtitle(val track: PendoloSubtitleTrack, val text: PendoloPreparedText)
internal data class PendoloStageRow(val item: PendoloWheelItem, val main: PendoloPreparedText,
    val subtitles: List<PendoloPreparedSubtitle>, val blockHeight: Double, val reservedHeight: Double,
    val width: Double, val contentRect: List<Double>)

/** Native target-state preparation. All layouts are independently measured;
 * browser snapshots/expected geometry are not input to production. */
internal fun preparePendoloWheel(lines: List<PendoloStageLine>, target: Double, width: Double, height: Double,
    style: TextStyle, measurer: TextMeasurer, density: Density, resolver: FontFamily.Resolver,
    mode: PendoloSubtitleMode, tuning: PendoloGeometryTuning = PendoloGeometryTuning()): List<PendoloStageRow> {
    val available = pendoloAvailableTextWidth(width, height, tuning)
    fun prepare(text: String, font: Double, limit: Double, lineHeight: Double, tracking: Boolean = false) =
        preparePendoloText(text, font, limit, measurer,
            if (tracking) style.copy(letterSpacing = (font * .01).sp) else style.copy(letterSpacing = TextUnit.Unspecified),
            density, resolver, lineHeight)
    val heights = lines.mapIndexed { i, line ->
        if (abs(i - target) > 10) 0.0 else {
            val tracks = pendoloSubtitleTracks(line.subtitles, mode)
            val main = prepare(line.lyric.text, 28.0, available / tuning.activeScale, 34.0)
            (main.height + pendoloSubtitleHeight(tracks) { t ->
                prepare(t.text, t.fontPx, available / tuning.activeScale, t.lineHeight).height
            }) * tuning.activeScale
        }
    }
    return pendoloWheelLayout(lines.size, target, 0.0, width, height, tuning,
        min(width, height) * .06, heights).map { item ->
        val line = lines[item.index]
        val limit = available / item.scale
        val font = if (item.active) 28.0 else 22.0
        val tracks = pendoloSubtitleTracks(line.subtitles, mode, focal = item.active)
        // CSS shrink-to-fit starts from max-content, not the longest already
        // wrapped row. Intrinsic widths round upward; explicit maxima truncate.
        val natural = if (item.active) limit else (prepare(line.lyric.text, font, 100000.0, 33.0, true).rows.map { it.width } +
            tracks.flatMap { prepare(it.text, it.fontPx, 100000.0, it.lineHeight, true).rows.map { row -> row.width } }).maxOrNull() ?: 0.0
        val rowWidth = min(ceil(natural * 64) / 64, floor(limit * 64) / 64)
        val main = prepare(line.lyric.text, font, if (item.active) limit else rowWidth, if (item.active) 34.0 else 33.0, !item.active)
        val subs = tracks.map { t ->
            PendoloPreparedSubtitle(t, prepare(t.text, t.fontPx, rowWidth, t.lineHeight, true))
        }
        val blockHeight = main.height + subs.sumOf { it.text.height + it.track.fontPx * it.track.gapEm }
        // CSS lays out left/top/max-width in 1/64px units before applying its
        // continuous transforms. TranslateY(-50%) is inside the rotated/scaled
        // left-center box; rotating directly around the lyric baseline differs.
        val x = floor(item.x * 64) / 64
        val y = floor(item.y * 64) / 64
        val angle = item.angleDeg * .35 * PI / 180
        val cos = cos(angle); val sin = sin(angle)
        val corners = listOf(0.0 to 0.0, rowWidth to 0.0, 0.0 to blockHeight, rowWidth to blockHeight).map { (a, b) ->
            val yy = b - blockHeight
            (x + (a * cos - yy * sin) * item.scale) to (y + blockHeight / 2 + (a * sin + yy * cos) * item.scale)
        }
        val left = corners.minOf { it.first }; val top = corners.minOf { it.second }
        PendoloStageRow(item, main, subs, blockHeight, heights[item.index], rowWidth,
            listOf(left, top, corners.maxOf { it.first } - left, corners.maxOf { it.second } - top))
    }
}

/** Target-state wheel only. Internal calibration seam, not public theme support
 * or a substitute for the source escapement/chorus transitions. */
@Composable
internal fun PendoloWheelTarget(lines: List<PendoloStageLine>, time: () -> Double, target: Double,
    style: TextStyle, modifier: Modifier = Modifier, mode: PendoloSubtitleMode = PendoloSubtitleMode.BOTH,
    onRows: (List<PendoloStageRow>) -> Unit = {}) {
    val density = LocalDensity.current; val resolver = LocalFontFamilyResolver.current
    val measurer = rememberTextMeasurer(cacheSize = 64); val clock by rememberUpdatedState(time)
    val timelines = remember(lines) { lines.map { pendoloLineTimeline(it.lyric) } }
    BoxWithConstraints(modifier.fillMaxSize()) {
        val rows = remember(lines, target, style, mode, maxWidth, maxHeight, density, resolver, measurer) {
            preparePendoloWheel(lines, target, maxWidth.value.toDouble(), maxHeight.value.toDouble(), style, measurer, density, resolver, mode)
        }
        SideEffect { onRows(rows) }
        Canvas(Modifier.fillMaxSize()) {
            val d = density.density; val t = clock()
            for (row in rows) {
                val item = row.item
                val alpha = pendoloRotatingOpacity(item.angleDeg, 0.0, item.alpha).toFloat()
                withTransform({
                    translate((floor(item.x * 64) / 64 * d).toFloat(), ((floor(item.y * 64) / 64 + row.blockHeight / 2) * d).toFloat())
                    rotate((item.angleDeg * .35).toFloat(), Offset.Zero)
                    scale(item.scale.toFloat(), item.scale.toFloat(), Offset.Zero)
                    translate(0f, (-row.blockHeight * d).toFloat())
                }) {
                    drawContext.canvas.saveLayer(Rect(-64*d, -64*d, (row.width.toFloat()+64)*d, (row.blockHeight.toFloat()+64)*d), Paint().apply { this.alpha = alpha })
                    if (item.active) drawPendoloSweep(row.main, timelines[item.index],
                        lines[item.index].lyric.end, 28.0, t, Color(0xffdfe5ec), Color(0xfff17b4d))
                    else drawPendoloRows(row.main, Color(0xffdfe5ec).copy(alpha = .75f))
                    var top = row.main.height
                    for (sub in row.subtitles) {
                        top += sub.track.gapEm * sub.track.fontPx
                        translate(0f, (top * d).toFloat()) {
                            drawPendoloRows(sub.text, Color(0xffadb9c8).copy(alpha =
                                ((if (item.active) 1.0 else .6) * sub.track.opacity).toFloat()))
                        }
                        top += sub.text.height
                    }
                    drawContext.canvas.restore()
                }
            }
        }
    }
}

private fun DrawScope.drawPendoloRows(text: PendoloPreparedText, color: Color) {
    for ((i, row) in text.rows.withIndex()) {
        val y = (i * text.lineHeight * density).toFloat() + cssFontBaseline(text.metrics.ascent,
            text.metrics.descent, (text.lineHeight * density).toFloat()) - paintedNaturalBaseline(row.paragraph, text.metrics)
        if (row.trackingRuns.isEmpty()) drawText(row.paragraph, color, Offset(0f, y), shadow = Shadow.None)
        else {
            // SkParagraph drops tracking on Arabic spaces as well as cursive
            // letters; Blink retains tracking on spaces. Preserve the original
            // complete shaped paragraph, moving only whitespace-separated runs
            // in its visual order. No joining boundary is cut or reshaped.
            for (run in row.trackingRuns) {
                translate(run.shift, y) { clipPath(run.path) { drawText(row.paragraph, color, shadow = Shadow.None) } }
            }
        }
    }
}
