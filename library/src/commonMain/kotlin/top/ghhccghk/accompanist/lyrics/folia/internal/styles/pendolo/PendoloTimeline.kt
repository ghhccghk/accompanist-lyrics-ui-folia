// Copyright (c) 2026 chthollyphile and contributors
// Folia cc6226e4 pendoloTimeline.ts / VisualizerPendolo.tsx.
// SPDX-License-Identifier: AGPL-3.0-only. Deterministic history adaptation: docs/upstream.md.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.pendolo

import kotlin.math.*

internal data class PendoloLineTime(val start: Double, val end: Double, val renderEnd: Double = end)

internal fun pendoloFallbackAnchor(lines: List<PendoloLineTime>, current: Int,
    lastValid: Int, observed: Boolean, time: Double): Double = when {
    current in lines.indices -> current.toDouble()
    !observed -> -1.0
    lines.lastOrNull()?.let { time > it.renderEnd } == true -> lines.size.toDouble()
    else -> lastValid + .5
}

/** No angle modulo: a row a full turn away must remain hidden. */
internal fun pendoloRotatingOpacity(baseAngle: Double, rotation: Double, baseOpacity: Double): Double {
    val angle = abs(baseAngle + rotation)
    return if (angle >= 110) 0.0 else baseOpacity * min(1.0, (110 - angle) / 28)
}

/** Rebuild the source's last-observed valid-line ref from media-boundary events,
 * including long lines that resume after a short overlap. No per-frame history
 * or most-recent-start shortcut. Exact render ends are inclusive, their right
 * limits are separate; source source-array priority is preserved. */
internal class PendoloTimeline(private val lines: List<PendoloLineTime>) {
    val boundaries: List<Double> = lines.flatMap { listOf(it.start, it.renderEnd) }.distinct().sorted()
    private data class Event(val time: Double, val after: Boolean, val lastValid: Int)
    private val history: List<Event>
    init {
        require(lines.all { it.start.isFinite() && it.end.isFinite() && it.renderEnd.isFinite() })
        var last = -1
        history = buildList {
            for (t in boundaries) {
                for (after in listOf(false, true)) {
                    val current = current(t, after)
                    if (current >= 0) last = current
                    add(Event(t, after, last))
                }
            }
        }
    }
    fun current(time: Double, after: Boolean = false): Int =
        lines.indexOfLast { time >= it.start && (time < it.renderEnd || (!after && time == it.renderEnd)) }

    fun anchor(time: Double, after: Boolean = false): Double {
        require(time.isFinite())
        var lo = 0
        var hi = history.size
        while (lo < hi) {
            val m = (lo + hi) / 2
            val e = history[m]
            if (e.time < time || e.time == time && (!e.after || after)) lo = m + 1 else hi = m
        }
        val last = history.getOrNull(lo - 1)?.lastValid ?: -1
        // The exact final end is inclusive; its right limit is already past
        // that end. Preserve this distinction without nudging media time.
        if (after && last >= 0 && current(time, true) < 0 && time == lines.lastOrNull()?.renderEnd)
            return lines.size.toDouble()
        return pendoloFallbackAnchor(lines, current(time, after), max(last, 0), last >= 0, time)
    }
}
