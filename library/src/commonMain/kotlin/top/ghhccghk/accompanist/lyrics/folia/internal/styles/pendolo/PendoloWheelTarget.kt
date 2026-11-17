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
import top.ghhccghk.accompanist.lyrics.folia.FoliaWordColor
import kotlin.math.*

internal data class PendoloStageLine(val lyric: PendoloLyric, val subtitles: PendoloSubtitleSource = PendoloSubtitleSource(),
    val renderEnd: Double = lyric.end)
internal data class PendoloPreparedSubtitle(val track: PendoloSubtitleTrack, val text: PendoloPreparedText)
internal data class PendoloStageRow(val item: PendoloWheelItem, val main: PendoloPreparedText,
    val subtitles: List<PendoloPreparedSubtitle>, val blockHeight: Double, val reservedHeight: Double,
    val width: Double, val contentRect: List<Double>)

/** The original wheel hub rotates first, then each left-center text block
 * counter-rotates by -.65 of that angle. No modulo/culling of the rotation. */
internal fun pendoloRotatedContentRect(row: PendoloStageRow, rotation: Double, hubX: Double, hubY: Double): List<Double> {
    val item = row.item
    val x = floor(item.x * 64) / 64; val y = floor(item.y * 64) / 64 + row.blockHeight / 2
    val a = (item.angleDeg * .35 - rotation * .65) * PI / 180
    val w = rotation * PI / 180
    val points = listOf(0.0 to 0.0,row.width to 0.0,0.0 to row.blockHeight,row.width to row.blockHeight).map { (xx,yy) ->
        val tx = x + (xx * cos(a) - (yy - row.blockHeight) * sin(a)) * item.scale - hubX
        val ty = y + (xx * sin(a) + (yy - row.blockHeight) * cos(a)) * item.scale - hubY
        (hubX + tx * cos(w) - ty * sin(w)) to (hubY + tx * sin(w) + ty * cos(w))
    }
    val left = points.minOf { it.first }; val top = points.minOf { it.second }
    return listOf(left,top,points.maxOf { it.first } - left,points.maxOf { it.second } - top)
}

/** Media-driven mounted text wheel; still an internal calibration entry.
 * Layout responds to targets/constraints and quantized in-flight CSS widths.
 * Optional rotation override is an internal raster-isolation seam only. */
@Composable
internal fun PendoloPlaybackWheel(lines: List<PendoloStageLine>, time: () -> Double,
    style: TextStyle, modifier: Modifier = Modifier,
    intensity: PendoloIntensity = PendoloIntensity.NORMAL, reducedMotion: Boolean = false,
    mode: PendoloSubtitleMode = PendoloSubtitleMode.BOTH,
    wordColors: List<FoliaWordColor> = emptyList(),
    wheelRotationOverride: (() -> Double)? = null, onRows: (List<PendoloStageRow>) -> Unit = {}) {
    val clock by rememberUpdatedState(time)
    val track = remember(lines,intensity) { PendoloTickTrack(lines.map { PendoloLineTime(it.lyric.start,it.lyric.end,it.renderEnd) },intensity) }
    val target by remember(track) { derivedStateOf { track.at(clock()).target } }
    val density = LocalDensity.current; val resolver = LocalFontFamilyResolver.current
    val measurer = rememberTextMeasurer(cacheSize = 128)
    BoxWithConstraints(modifier) {
        val heights = remember(lines,style,mode,maxWidth,maxHeight,density,resolver,measurer) {
            val available=pendoloAvailableTextWidth(maxWidth.value.toDouble(),maxHeight.value.toDouble())/1.25
            lines.map { line ->
                val main=preparePendoloText(line.lyric.text,28.0,available,measurer,style.copy(letterSpacing=TextUnit.Unspecified),density,resolver,34.0)
                (main.height+pendoloSubtitleHeight(pendoloSubtitleTracks(line.subtitles,mode)) { sub ->
                    preparePendoloText(sub.text,sub.fontPx,available,measurer,style.copy(letterSpacing=TextUnit.Unspecified),density,resolver,sub.lineHeight).height
                })*1.25
            }
        }
        val widthTrack=remember(lines,heights,maxWidth,maxHeight) { PendoloWidthTrack(lines.map { PendoloLineTime(it.lyric.start,it.lyric.end,it.renderEnd) },maxWidth.value.toDouble(),maxHeight.value.toDouble(),heights) }
        val limits by remember(widthTrack,reducedMotion) { derivedStateOf { widthTrack.at(clock(),reducedMotion).mapValues { (_,v)->floor(v*64)/64 } } }
        PendoloWheelTarget(lines, { clock() }, target, style, Modifier.fillMaxSize(), mode,
            wheelRotation = wheelRotationOverride ?: { track.at(clock(),reducedMotion).wheelRotation }, idleMainLimits=limits,
            reservedHeights=heights,wordColors=wordColors,onRows = onRows)
    }
}

/** Native target-state preparation. All layouts are independently measured;
 * browser snapshots/expected geometry are not input to production. */
internal fun preparePendoloWheel(lines: List<PendoloStageLine>, target: Double, width: Double, height: Double,
    style: TextStyle, measurer: TextMeasurer, density: Density, resolver: FontFamily.Resolver,
    mode: PendoloSubtitleMode, tuning: PendoloGeometryTuning = PendoloGeometryTuning(),
    idleMainLimits: Map<Int,Double> = emptyMap(), reservedHeights: List<Double>? = null): List<PendoloStageRow> {
    val available = pendoloAvailableTextWidth(width, height, tuning)
    fun prepare(text: String, font: Double, limit: Double, lineHeight: Double, tracking: Boolean = false) =
        preparePendoloText(text, font, limit, measurer,
            if (tracking) style.copy(letterSpacing = (font * .01).sp) else style.copy(letterSpacing = TextUnit.Unspecified),
            density, resolver, lineHeight)
    val heights = reservedHeights ?: lines.mapIndexed { i, line ->
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
        val mainLimit = if (item.active) limit else idleMainLimits[item.index] ?: limit
        val naturalMain = if (item.active) limit else prepare(line.lyric.text, font, 100000.0, 33.0, true).rows.maxOfOrNull { it.width } ?: 0.0
        val naturalSub = tracks.flatMap { prepare(it.text, it.fontPx, 100000.0, it.lineHeight, true).rows.map { row -> row.width } }.maxOrNull() ?: 0.0
        fun intrinsic(natural: Double, maximum: Double) = min(ceil(natural * 64) / 64,floor(maximum * 64) / 64)
        val rowWidth = if (item.active) floor(limit * 64) / 64 else max(intrinsic(naturalMain,mainLimit),intrinsic(naturalSub,limit))
        val main = prepare(line.lyric.text, font, if (item.active) limit else min(rowWidth,floor(mainLimit * 64) / 64), if (item.active) 34.0 else 33.0, !item.active)
        val subs = tracks.map { t ->
            PendoloPreparedSubtitle(t, prepare(t.text, t.fontPx, min(rowWidth,floor(limit * 64) / 64), t.lineHeight, true))
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
    wheelRotation: () -> Double = { 0.0 },
    idleMainLimits: Map<Int,Double> = emptyMap(), reservedHeights: List<Double>? = null,
    wordColors: List<FoliaWordColor> = emptyList(),
    onRows: (List<PendoloStageRow>) -> Unit = {}) {
    val density = LocalDensity.current; val resolver = LocalFontFamilyResolver.current
    val measurer = rememberTextMeasurer(cacheSize = 64); val clock by rememberUpdatedState(time)
    val rotation by rememberUpdatedState(wheelRotation)
    val timelines = remember(lines) { lines.map { pendoloLineTimeline(it.lyric) } }
    BoxWithConstraints(modifier.fillMaxSize()) {
        val rows = remember(lines, target, style, mode, maxWidth, maxHeight, density, resolver, measurer,idleMainLimits,reservedHeights) {
            preparePendoloWheel(lines, target, maxWidth.value.toDouble(), maxHeight.value.toDouble(), style, measurer, density, resolver, mode,
                idleMainLimits=idleMainLimits,reservedHeights=reservedHeights)
        }
        val inks=remember(rows,lines,wordColors) {
            if(wordColors.isEmpty())emptyMap() else rows.associate { row ->
                row.item.index to preparePendoloInk(lines[row.item.index].lyric.text,row.main,wordColors,
                    pendoloSweepColor(Color(0xffdfe5ec),Color(0xfff17b4d)))
            }
        }
        SideEffect { onRows(rows) }
        Canvas(Modifier.fillMaxSize()) {
            val d = density.density; val t = clock(); val wheel = rotation()
            rotate(wheel.toFloat(), Offset(0f, size.height * .5f)) {
                for (row in rows) {
                    val item = row.item
                    val alpha = pendoloRotatingOpacity(item.angleDeg, wheel, item.alpha).toFloat()
                    withTransform({
                        translate((floor(item.x * 64) / 64 * d).toFloat(), ((floor(item.y * 64) / 64 + row.blockHeight / 2) * d).toFloat())
                        rotate((item.angleDeg * .35 - wheel * .65).toFloat(), Offset.Zero)
                        scale(item.scale.toFloat(), item.scale.toFloat(), Offset.Zero)
                        translate(0f, (-row.blockHeight * d).toFloat())
                    }) {
                        drawContext.canvas.saveLayer(Rect(-64*d, -64*d, (row.width.toFloat()+64)*d, (row.blockHeight.toFloat()+64)*d), Paint().apply { this.alpha = alpha })
                        if (item.active) drawPendoloSweep(row.main, timelines[item.index],
                            lines[item.index].lyric.end, 28.0, t, Color(0xffdfe5ec), Color(0xfff17b4d),inks[item.index])
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
