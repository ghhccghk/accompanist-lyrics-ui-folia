// Copyright (c) 2026 chthollyphile and contributors
// Adapted from fixed Folia cc6226e4 fumeArticleLayout.ts / fumeTextMeasure.ts.
// SPDX-License-Identifier: AGPL-3.0-only. Source/modifications: docs/upstream.md.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.fume

import kotlin.math.*

internal data class FumeViewport(val width: Double, val height: Double)
internal data class FumeBounds(val left: Double, val top: Double, val right: Double, val bottom: Double)
internal data class FumeArticleBlock(val sourceIndex: Int, val line: FumeLyric, val hero: Boolean,
    val x: Double, val y: Double, val width: Double, val height: Double, val innerWidth: Double,
    val fontPx: Double, val lineHeight: Double, val lineCount: Int)
internal data class FumeArticle(val width: Double, val height: Double, val viewportHeight: Double,
    val columns: Int, val gap: Double, val density: Double, val bounds: FumeBounds,
    val blocks: List<FumeArticleBlock>) {
    val chronological = blocks.sortedBy { it.sourceIndex }
    val firstStart = chronological.firstOrNull()?.line?.start ?: Double.POSITIVE_INFINITY
    val lastRenderEnd = chronological.lastOrNull()?.line?.renderEnd ?: Double.NEGATIVE_INFINITY
}
internal data class FumeFontFit(val fontPx: Double, val lines: Int)
/** Measurement must preserve the whole shaped string. It must not draw separate UTF-16 chars. */
internal typealias FumeLineCounter = (text: String, hero: Boolean, fontPx: Double, width: Double) -> Int

internal fun fumeSeed(input: String): Double {
    var hash = 2166136261u
    input.forEach { hash = (hash xor it.code.toUInt()) * 16777619u }
    return (hash % 10000u).toDouble() / 10000
}
private fun renderable(text: String) = fumeGraphemes(text).count { it.any { c -> !fumeWhitespace(c) } }
private fun naturalHero(line: FumeLyric, index: Int, total: Int): Boolean {
    val count = renderable(line.text)
    if (count == 0) return false
    if (line.chorus && count <= 22) return true
    val centered = abs(index - total / 2.0) / max(total, 1)
    return count in 4..28 && centered < .72 && ((index + 1) % 6 == 0 || fumeSeed("${line.text}:$index") > .965)
}
private fun fallbackHero(entries: List<IndexedValue<FumeLyric>>): Int {
    if (entries.isEmpty() || entries.withIndex().any { (i, e) -> naturalHero(e.value, i, entries.size) }) return -1
    var best = -1
    var bestScore = Double.NEGATIVE_INFINITY
    entries.forEachIndexed { i, e ->
        val count = renderable(e.value.text)
        if (count == 0 || count > 36) return@forEachIndexed
        val center = 1 - abs(i - entries.size / 2.0) / max(entries.size, 1)
        val length = if (count in 6..22) 1.0 else if (count <= 28) .72 else .36
        val score = center * .62 + length * .34 + (if (e.value.chorus) .28 else 0.0) +
            fumeSeed("${e.value.text}:$i:fallback-hero") * .04
        if (score > bestScore) { bestScore = score; best = i }
    }
    if (best >= 0) return best
    return entries.indices.filter { renderable(entries[it].value.text) > 0 }
        .minByOrNull { renderable(entries[it].value.text) } ?: -1
}

internal fun chooseFumeFont(text: String, hero: Boolean, width: Double, fontScale: Double,
    density: Double, heroScale: Double, countLines: FumeLineCounter): FumeFontFit {
    var low = if (hero) 18.0 else 10.0
    var high = if (hero) 58.0 else 30.0
    var best: FumeFontFit? = null
    repeat(8) {
        val middle = (low + high) / 2
        val size = middle * fontScale * density * if (hero) heroScale else 1.0
        val count = countLines(text, hero, size, width)
        require(count >= 0) { "Negative measured line count" }
        if (count <= 1) { best = FumeFontFit(size, count); low = middle } else high = middle
    }
    return best ?: ((if (hero) 18.0 else 10.0) * fontScale * density * if (hero) heroScale else 1.0).let {
        FumeFontFit(it, countLines(text, hero, it, width))
    }
}

/** Entire-song geometry only: no raster, background, camera state or playback invalidation.
 * Native callers can cache measured paragraphs for the lifetime of this build. */
internal fun buildFumeArticle(lines: List<FumeLyric>, viewport: FumeViewport, themeName: String,
    fontScale: Double = 1.0, heroScale: Double = 1.0, countLines: FumeLineCounter): FumeArticle? {
    require(fontScale.isFinite() && fontScale > 0 && heroScale.isFinite() && heroScale > 0)
    if (!viewport.width.isFinite() || !viewport.height.isFinite() || viewport.width <= 0 || viewport.height <= 0 || lines.isEmpty()) return null
    val paperWidth = max(viewport.width * 1.95, viewport.width + 520).coerceIn(920.0, 2400.0)
    val viewHeight = max(viewport.height, 240.0)
    val maxColumns = if (paperWidth >= 1120) 4 else if (paperWidth >= 760) 3 else if (paperWidth >= 500) 2 else 1
    val targetHeight = viewHeight * 2.45
    val marginX = max(viewport.width * .86, 280.0)
    val marginY = max(viewport.height * .82, 220.0)
    data class Recipe(val columns: Int, val gap: Double, val density: Double)
    // JS number string is integer-valued here for standard CSS viewports. Explicitly handle
    // the integral case so deterministic shuffle keys don't gain Kotlin's trailing '.0'.
    val paperKey = if (paperWidth == floor(paperWidth)) paperWidth.toLong().toString() else paperWidth.toString()
    fun attempt(recipe: Recipe): FumeArticle {
        val (columns, gap, density) = recipe
        val columnWidth = (paperWidth - gap * (columns - 1)) / columns
        val seed = "$themeName:$columns:$paperKey"
        val entries = lines.withIndex().filter { it.value.text.any { c -> !fumeWhitespace(c) } }
            .sortedBy { fumeSeed("$seed:${it.index}:${it.value.text}") }
        val forced = fallbackHero(entries)
        val heights = DoubleArray(columns) { marginY }
        val blocks = ArrayList<FumeArticleBlock>(entries.size)
        var bodyCursor = 0
        var heroCursor = 0
        entries.forEachIndexed { i, entry ->
            val hero = i == forced || naturalHero(entry.value, i, entries.size)
            val span = if (hero) min(columns, if (columns <= 1) 1 else 2) else 1
            val spanWidth = if (span > 1) columnWidth * span + gap * (span - 1) else paperWidth
            val width = if (!hero) columnWidth else if (span == 1) paperWidth
                else if (columns == 2) columnWidth * 1.5 + gap * .5 else spanWidth
            val innerWidth = max(width, 120.0)
            val fit = chooseFumeFont(entry.value.text, hero, innerWidth, fontScale, density, heroScale, countLines)
            val lineHeight = floor(fit.fontPx * (if (hero) 1.02 else 1.06) + .5)
            val blockGap = if (hero) max(floor(lineHeight * .2 + .5), 6.0) else max(floor(lineHeight * .08 + .5), 2.0)
            val height = fit.lines * lineHeight
            val x: Double
            val y: Double
            if (hero && span == 1) {
                y = heights.max(); x = marginX; heights[0] = y + height + blockGap
            } else if (hero) {
                val covered = (0..columns - span).map { start -> (start until start + span).maxOf { heights[it] } }
                val bestHeight = covered.min()
                val candidates = covered.indices.filter { covered[it] == bestHeight }
                val start = candidates[heroCursor % candidates.size]; heroCursor++
                y = bestHeight
                x = marginX + start * (columnWidth + gap) + max((spanWidth - width) * .5, 0.0)
                for (c in start until start + span) heights[c] = y + height + blockGap
            } else {
                val minHeight = heights.min()
                val candidates = heights.indices.filter { heights[it] == minHeight }
                val c = candidates[bodyCursor % candidates.size]; bodyCursor++
                x = marginX + c * (columnWidth + gap); y = heights[c]
                heights[c] = y + height + blockGap
            }
            blocks += FumeArticleBlock(entry.index, entry.value, hero, x, y, width, height, innerWidth, fit.fontPx, lineHeight, fit.lines)
        }
        val height = max(0.0, heights.max()) + marginY
        return FumeArticle(paperWidth + marginX * 2, height, viewHeight, columns, gap, density,
            FumeBounds(marginX, marginY, marginX + paperWidth, max(height - marginY, marginY)), blocks)
    }
    var bestRecipe: Recipe? = null
    var bestScore = Double.POSITIVE_INFINITY
    for (columns in maxColumns downTo 1) {
        var low = .82
        var high = 1.42
        val gap = floor(paperWidth * (if (columns >= 4) .0065 else if (columns == 3) .0085 else .0115) + .5).coerceIn(6.0, 14.0)
        repeat(8) {
            val recipe = Recipe(columns, gap, (low + high) / 2)
            val layout = attempt(recipe)
            val score = abs(layout.height - targetHeight) + if (layout.height < targetHeight) 0.0 else (layout.height - targetHeight) * .14
            if (score < bestScore) { bestScore = score; bestRecipe = recipe }
            if (layout.height < targetHeight) low = recipe.density else high = recipe.density
        }
    }
    return bestRecipe?.let(::attempt)
}
