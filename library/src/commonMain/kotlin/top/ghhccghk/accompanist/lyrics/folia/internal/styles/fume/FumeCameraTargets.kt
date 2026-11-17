// Copyright (c) 2026 chthollyphile and contributors
// Adapted from Folia cc6226e4 fumeCamera.ts. No source wall-clock/frame accumulator.
// SPDX-License-Identifier: AGPL-3.0-only. Source/modifications: docs/upstream.md.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.fume

import kotlin.math.*

internal data class FumePoint(val x: Double, val y: Double)
internal data class FumeCameraView(val x: Double, val y: Double, val scale: Double)
/** Offsets are the layout's untransformed glyph advances, including the terminal advance.
 * They remain distinct from rendered glyph ink/selection bounds and from word timing. */
internal data class FumeFocusRow(val start: Int, val end: Int, val left: Double, val top: Double,
    val width: Double, val offsets: List<Double>)
internal data class FumeFocusBlock(val block: FumeArticleBlock, val rows: List<FumeFocusRow>) {
    private val count = fumeGraphemes(block.line.text).size
    private fun fallback() = FumePoint(block.x + block.width * .5, block.y + block.height * .5)
    fun entry(): FumePoint = rows.firstOrNull()?.let {
        FumePoint(block.x + it.left, block.y + it.top + block.lineHeight * .5)
    } ?: fallback()
    private fun onRow(index: Int, offset: Double): FumePoint {
        val row = rows[index]
        val local = offset.coerceIn(row.start.toDouble(), row.end.toDouble()) - row.start
        val base = floor(local).toInt()
        val baseWidth = row.offsets.getOrNull(base) ?: row.width
        val advance = if (base < row.end - row.start) max((row.offsets.getOrNull(base + 1) ?: row.width) - baseWidth, 0.0) else 0.0
        val min = block.x + row.left
        return FumePoint((min + baseWidth + advance * (local - base)).coerceIn(min, min + row.width),
            block.y + row.top + block.lineHeight * .5)
    }
    fun stepped(printed: Int): FumePoint {
        if (rows.isEmpty()) return fallback()
        val offset = printed.coerceIn(0, count)
        val row = rows.indexOfFirst { offset <= it.end }.takeIf { it >= 0 } ?: rows.lastIndex
        return onRow(row, offset.toDouble())
    }
    fun smooth(printed: Double): FumePoint {
        if (rows.isEmpty()) return fallback()
        val offset = printed.coerceIn(0.0, count.toDouble())
        val index = rows.indexOfFirst { offset <= it.end }.takeIf { it >= 0 } ?: rows.lastIndex
        val row = rows[index]
        var point = onRow(index, offset)
        if (index > 0 && offset < row.start + .7) {
            val previous = rows[index - 1]
            val blend = fumeCameraEase(1 - (offset - previous.end) / .7)
            point = fumePointMix(point, onRow(index - 1, previous.end.toDouble()), blend)
        } else if (index < rows.lastIndex && offset > row.end - .7) {
            val next = rows[index + 1]
            val blend = fumeCameraEase((offset - (row.end - .7)) / .7)
            point = fumePointMix(point, onRow(index + 1, next.start.toDouble()), blend)
        }
        return point
    }
}
internal fun fumeCameraEase(value: Double): Double {
    val q = value.coerceIn(0.0, 1.0)
    return if (q < .5) 4 * q.pow(3) else 1 - (-2 * q + 2).pow(3) / 2
}
private fun fumePointMix(a: FumePoint, b: FumePoint, q: Double) = FumePoint(a.x + (b.x - a.x) * q, a.y + (b.y - a.y) * q)
internal fun fumeCameraScale(block: FumeArticleBlock, viewport: FumeViewport): Double =
    ((max(min(viewport.width, viewport.height), 1.0) * .115).coerceIn(64.0, 124.0) / max(block.lineHeight, 1.0)).coerceIn(.88, 2.2)
internal fun fumeCameraRetarget(line: FumeLyric): Double {
    val duration = max(line.end - line.start, 0.0)
    return when {
        duration < .1 -> (max(duration, .08) * .34).coerceIn(.04, .075)
        duration < .18 -> ((duration * .45).coerceIn(.045, .06) * .5 + (duration * .22).coerceIn(.03, .04) * .12).coerceIn(.055, .095)
        else -> ((max(duration, .12) * .34).coerceIn(.22, .42) * .44 + .06 * .22).coerceIn(.075, .13)
    }
}
internal fun fumeOverviewRetarget(viewport: FumeViewport) = (min(viewport.width, viewport.height) / 1500).coerceIn(.38, .58)
internal data class FumeFlightBridge(val x: Double, val y: Double, val scale: Double, val phase: Double, val duration: Double)
internal fun fumeOverviewBridge(from: FumeCameraView, target: FumeCameraView,
    overview: FumeCameraView?, viewport: FumeViewport): FumeFlightBridge? {
    overview ?: return null
    val safeScale = maxOf(from.scale, target.scale, overview.scale, .001)
    val side = max(min(viewport.width, viewport.height), 1.0)
    val distance = hypot(from.x - target.x, from.y - target.y)
    val screen = distance * safeScale
    if (distance <= 0 || screen < side * 2.75) return null
    fun mix(a: Double, b: Double, q: Double) = a + (b - a) * q
    val loft = ((screen / side - 2.75) / 3.4).coerceIn(0.0, 1.0)
    val bias = mix(.18, .42, loft)
    val x = mix(mix(from.x, target.x, .5), overview.x, bias)
    val y = mix(mix(from.y, target.y, .5), overview.y, bias)
    val endpointScale = maxOf(from.scale, target.scale, .001)
    val scale = max(endpointScale * mix(.62, .4, loft), overview.scale * mix(1.85, 1.55, loft))
        .coerceIn(.22, max(endpointScale * .92, .22))
    val first = hypot(x - from.x, y - from.y) * maxOf(from.scale, scale, .001)
    val second = hypot(target.x - x, target.y - y) * maxOf(target.scale, scale, .001)
    val total = first + second
    return FumeFlightBridge(x, y, scale, if (total <= 0) .36 else (first / total).coerceIn(.26, .44),
        (.26 + screen / (side * 5.5) * .28).coerceIn(.3, .68))
}
internal fun fumeOverview(article: FumeArticle, viewport: FumeViewport): FumeCameraView {
    if (article.blocks.isEmpty()) return FumeCameraView(article.width * .5, article.height * .5,
        (min(viewport.width / max(article.width, 1.0), viewport.height / max(article.height, 1.0)) * .92).coerceIn(.22, .72))
    val left = article.blocks.minOf { it.x }; val right = article.blocks.maxOf { it.x + it.width }
    val top = article.blocks.minOf { it.y }; val bottom = article.blocks.maxOf { it.y + it.height }
    val padX = (viewport.width * .2).coerceIn(120.0, 280.0)
    val padY = (viewport.height * .2).coerceIn(96.0, 220.0)
    val scale = min(viewport.width / max(right - left + padX * 2, 1.0), viewport.height / max(bottom - top + padY * 2, 1.0)).coerceIn(.22, .72)
    return FumeCameraView((left + right) * .5, (top + bottom) * .5, scale)
}
internal fun fumeFocusBlock(article: FumeArticle, lineIndex: Int, time: Double,
    reveals:Map<Int,FumeReveal> = emptyMap()): FumeArticleBlock? {
    if (lineIndex >= 0) article.blocks.firstOrNull { it.sourceIndex == lineIndex }?.let { return it }
    val last = article.chronological.lastOrNull()
    if (last != null && time >= article.lastRenderEnd) return last
    return article.chronological.asReversed().firstOrNull { (reveals[it.sourceIndex]?:FumeReveal(it.line)).count(time) > 0 }
        ?: article.chronological.firstOrNull()
}
internal fun fumeBlockOnScreen(block: FumeArticleBlock, camera: FumeCameraView, viewport: FumeViewport, overscan: Double = 180.0): Boolean {
    val left = viewport.width * .5 + (block.x - camera.x) * camera.scale
    val top = viewport.height * .5 + (block.y - camera.y) * camera.scale
    return !(left + block.width * camera.scale < -overscan || left > viewport.width + overscan ||
        top + block.height * camera.scale < -overscan || top > viewport.height + overscan)
}
