// Copyright (c) 2026 chthollyphile
// Kotlin adaptation of pinned Folia partita/partitaLayout.ts and partitaOverlap.ts.
// SPDX-License-Identifier: AGPL-3.0-only
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.partita

import top.ghhccghk.accompanist.lyrics.folia.FoliaAnimationIntensity
import top.ghhccghk.accompanist.lyrics.folia.internal.animation.Pose
import top.ghhccghk.accompanist.lyrics.folia.internal.model.NormalizedLine
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.classic.pairGap
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.classic.wordReach
import top.ghhccghk.accompanist.lyrics.folia.internal.text.*
import kotlin.math.*

/** Internal until the renderer and public-entry comparison are in place. Values are CSS px. */
internal data class PartitaConfig(
    val semantic: Boolean = true,
    val staggerMin: Double = 20.0,
    val staggerMax: Double = 100.0,
    val avoidOverlap: Boolean = false,
    val showGuideLines: Boolean = true,
) {
    init {
        require(staggerMin.isFinite() && staggerMax.isFinite())
        require(staggerMin in 0.0..180.0 && staggerMax in staggerMin..180.0)
    }
}

internal data class PartitaRow(
    val units: List<LayoutUnit>,
    val words: List<DisplayUnit>,
    val pose: Pose,
    val marginBottom: Double,
    val index: Int,
)
internal data class PartitaLayout(val rows: List<PartitaRow>, val graphemes: Int, val perspective: Int)

/** This seeded stream uses seconds, not truncated milliseconds or a Kotlin random generator. */
private class PartitaRandom(private var seed: Double) {
    fun next(): Double = sin(seed++) .let { it * 10000 }.let { it - floor(it) }
}
private fun countGraphemes(text: String) = graphemeBoundaries(text.filterNot { it.isWhitespace() }).size - 1

internal fun buildPartitaLayout(line: NormalizedLine, windowHeight: Double,
    intensity: FoliaAnimationIntensity, config: PartitaConfig = PartitaConfig()): PartitaLayout {
    require(windowHeight.isFinite() && windowHeight >= 0)
    val calm = intensity == FoliaAnimationIntensity.Calm
    val chaotic = intensity == FoliaAnimationIntensity.Chaotic
    val units = stickyLayoutUnits(if (config.semantic) semanticLayoutUnits(line) else line.fragments.map {
        val text = line.text.substring(it.begin, it.end)
        LayoutUnit(listOf(timedDisplayUnit(text, it.startUs, it.endUs,
            listOf(it.copy(begin = 0, end = text.length)))), false)
    }).ifEmpty {
        // lyrics-core line-only input has no parser words: one explicit line-timed run.
        if (line.text.isEmpty()) emptyList() else listOf(LayoutUnit(listOf(timedDisplayUnit(line.text,
            line.startUs, line.endUs, emptyList())), false))
    }
    val count = min(units.size, max(1, floor(windowHeight * .65 / 100).toInt()))
    val random = PartitaRandom(line.startUs / 1e6)
    var remainingUnits = units.size
    var remainingChunks = count
    var unitIndex = 0
    val chunks = (0 until count).map { c ->
        val average = remainingUnits.toDouble() / remainingChunks
        val proposed = if (c == count - 1) remainingUnits else {
            min(ceil(average * 1.5).toInt(), max(1, floor(average + (random.next() - .5) * average + .5).toInt()))
        }
        val size = proposed.coerceIn(1, remainingUnits - (remainingChunks - 1))
        units.subList(unitIndex, unitIndex + size).toList().also {
            unitIndex += size; remainingUnits -= size; remainingChunks--
        }
    }
    val rows = chunks.mapIndexed { index, chunk ->
        val words = displayWordsFromLayoutUnits(chunk)
        val rawWords = chunk.flatMap { it.words }
        val length = countGraphemes(rawWords.joinToString("") { it.text.trim() })
        val stagger = config.staggerMin + random.next() * (config.staggerMax - config.staggerMin)
        val scale = if (calm) 1.0 else .8 + random.next() * .9
        PartitaRow(chunk, words, Pose(
            (if (index % 2 == 0) -stagger else stagger).toFloat(),
            (if (calm) 0.0 else (index - (count - 1) / 2.0) * 2.5).toFloat(),
            ((if (chaotic) 1 + min(length * .01, .05) else 1.0) * scale).toFloat(),
            (if (chaotic) if (index % 2 == 0) -3f else 3f else 0f),
            (if (index % 2 == 0) 1 else -1) * (if (chaotic) 6f else 3f)),
            if (chaotic) 6.4 else 9.6, index)
    }
    return PartitaLayout(rows, maxOf(countGraphemes(line.text), line.fragments.size, 1), if (chaotic) 720 else 1000)
}

internal fun partitaWordPoses(row: PartitaRow, intensity: FoliaAnimationIntensity): List<Pose> {
    val spread = when (intensity) { FoliaAnimationIntensity.Calm -> 0; FoliaAnimationIntensity.Normal -> 6; FoliaAnimationIntensity.Chaotic -> 15 }
    val rotation = when (intensity) { FoliaAnimationIntensity.Calm -> 0; FoliaAnimationIntensity.Normal -> 3; FoliaAnimationIntensity.Chaotic -> 8 }
    return row.words.indices.map { index ->
        val seed = row.words.first().startUs / 1e6 + index * 7.13
        fun random(offset: Int) = (sin(seed + offset) * 10000).let { it - floor(it) }
        Pose(((random(1) - .5) * spread * 2).toFloat(), ((random(2) - .5) * spread * 2).toFloat(),
            row.pose.scale, ((random(3) - .5) * rotation * 2).toFloat(), ((random(8) - .5) * 20).toFloat())
    }
}

/** Preserve CSS toFixed rounding BEFORE evaluating mixed em/px lengths. */
private fun cssFixed(value: Double, decimals: Int): Double {
    val factor = 10.0.pow(decimals)
    return sign(value) * floor(abs(value) * factor + .5) / factor
}
internal data class PartitaSpacing(val gaps: List<Double>, val top: Double, val bottom: Double)
internal data class PartitaMeasuredRow(val widthsEm: List<Double>, val poses: List<Pose>, val row: PartitaRow,
    val rtl: Boolean)
private data class RowReach(val em: Double, val topPx: Double, val bottomPx: Double)

/** font is the used CSS font size; root rem remains 16px regardless of lyric scale. */
internal fun partitaSpacing(rows: List<PartitaMeasuredRow>, font: Double): List<PartitaSpacing> {
    require(font.isFinite() && font > 0)
    val reaches = rows.map { row ->
        var height = Double.NEGATIVE_INFINITY
        var topPx = Double.NEGATIVE_INFINITY
        var bottomPx = Double.NEGATIVE_INFINITY
        var width = 0.0
        row.poses.forEachIndexed { i, pose ->
            val w = row.widthsEm[i]
            val reach = wordReach(w, 1.0, pose)
            height = max(height, reach.halfHeight - 1.22 / 2)
            topPx = max(topPx, -pose.y.toDouble()); bottomPx = max(bottomPx, pose.y.toDouble())
            width += max(w, 2 * reach.halfWidth) + .8
        }
        RowReach(height + width / 2 * abs(sin(row.row.pose.rotation * PI / 180)), topPx, bottomPx)
    }
    fun length(em: Double, px: Double) = cssFixed(em, 3) * font + cssFixed(px, 1)
    return rows.mapIndexed { index, row ->
        val reach = reaches[index]
        val below = reaches.getOrNull(index + 1)
        val gaps = row.poses.indices.map { i ->
            if (i == row.poses.lastIndex) 12.8 else {
                val pose = row.poses[i]; val next = row.poses[i + 1]
                val em = pairGap(row.widthsEm[i], row.widthsEm[i + 1], 1.0,
                    pose, next, false, 0.0) + .05
                val px = if (row.rtl) next.x - pose.x else pose.x - next.x
                max(12.8, length(em, px.toDouble()))
            }
        }
        PartitaSpacing(gaps, if (index == 0) max(0.0, length(reach.em, reach.topPx) - 3.2) else 0.0,
            max(row.row.marginBottom, if (below == null) length(reach.em, reach.bottomPx) - 3.2
                else length(reach.em + below.em + .05, reach.bottomPx + below.topPx) - 6.4))
    }
}
