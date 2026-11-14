package com.mocharealm.accompanist.lyrics.folia.internal.timeline

import com.mocharealm.accompanist.lyrics.folia.internal.model.NormalizedLine

internal data class TimelineSelection(val primary: Int?, val secondary: List<Int>, val next: Int?)

/** Boundary deltas + checkpoints: arbitrary seek replays at most 63 boundaries. */
internal class TimelineIndex(val lines: List<NormalizedLine>) {
    private data class Delta(val time: Long, val add: List<Int>, val remove: List<Int>)
    private val deltas: List<Delta>
    private val checkpoints: Map<Int, Set<Int>>

    init {
        val events = mutableMapOf<Long, Pair<MutableList<Int>, MutableList<Int>>>()
        lines.forEachIndexed { index, line ->
            if (line.endUs > line.startUs) {
                events.getOrPut(line.startUs) { mutableListOf<Int>() to mutableListOf() }.first += index
                events.getOrPut(line.endUs) { mutableListOf<Int>() to mutableListOf() }.second += index
            }
        }
        deltas = events.entries.sortedBy { it.key }.map { (time, event) -> Delta(time, event.first, event.second) }
        val active = mutableSetOf<Int>()
        checkpoints = buildMap {
            deltas.forEachIndexed { index, delta ->
                active.removeAll(delta.remove.toSet()); active.addAll(delta.add)
                if (index % 64 == 0) put(index, active.toSet())
            }
        }
    }

    fun select(timeUs: Long): TimelineSelection {
        var low = 0
        var high = deltas.size
        while (low < high) {
            val middle = (low + high) ushr 1
            if (deltas[middle].time <= timeUs) low = middle + 1 else high = middle
        }
        val boundary = low - 1
        val active = if (boundary < 0) mutableSetOf() else {
            val checkpoint = boundary / 64 * 64
            checkpoints.getValue(checkpoint).toMutableSet().also { set ->
                for (index in checkpoint + 1..boundary) {
                    set.removeAll(deltas[index].remove.toSet()); set.addAll(deltas[index].add)
                }
            }
        }
        val primary = active.filter { !lines[it].accompaniment }
            .maxWithOrNull(compareBy<Int>({ lines[it].startUs }, { -lines[it].ref.index }))
            ?: active.minByOrNull { lines[it].ref.index }
        // lines are sorted by start; upper-bound query also works in interludes.
        low = 0; high = lines.size
        while (low < high) {
            val middle = (low + high) ushr 1
            if (lines[middle].startUs <= timeUs) low = middle + 1 else high = middle
        }
        return TimelineSelection(primary, active.filter { it != primary }.sorted(),
            low.takeIf { it < lines.size })
    }
}
