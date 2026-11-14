package com.mocharealm.accompanist.lyrics.folia.internal.timeline

import com.mocharealm.accompanist.lyrics.folia.internal.animation.*
import com.mocharealm.accompanist.lyrics.folia.internal.model.NormalizedLine

internal data class PrimaryEpoch(val line: Int, val enterUs: Long, val leaveUs: Long)
internal data class VisualSelection(val primary: PrimaryEpoch?, val exiting: List<PrimaryEpoch>,
    val secondary: List<Int>, val next: Int?)

/** Canonical epochs are prepared once, including overlap resumption and gap tails. */
internal class VisualTimeline(private val lines: List<NormalizedLine>) {
    private val source = TimelineIndex(lines)
    private val profiles = lines.map { renderProfile(it.startUs, it.endUs, it.fragments.lastOrNull()?.endUs ?: it.endUs) }
    private val epochs: List<PrimaryEpoch>
    init {
        val boundaries = lines.indices.flatMap { listOf(lines[it].startUs, lines[it].endUs, profiles[it].renderEndUs) }.distinct().sorted()
        val collected = mutableListOf<PrimaryEpoch>()
        var previous: Int? = null
        var entered = 0L
        boundaries.forEach { time ->
            val active = source.select(time)
            // Only the latest begun primary is eligible for a tail; never resurrect an older completed line.
            val latest = lines.indices.lastOrNull { !lines[it].accompaniment && lines[it].startUs <= time }
            val chosen = active.primary ?: latest?.takeIf { time < profiles[it].renderEndUs }
            if (chosen != previous) {
                previous?.let { collected += PrimaryEpoch(it, entered, time) }
                previous = chosen; entered = time
            }
        }
        epochs = collected
    }
    fun at(time: Long): VisualSelection {
        var low = 0; var high = epochs.size
        while (low < high) {
            val middle = (low + high) ushr 1
            if (epochs[middle].enterUs <= time) low = middle + 1 else high = middle
        }
        val current = (low - 1).takeIf { it >= 0 }?.let(epochs::get)?.takeIf { time < it.leaveUs }
        val end = if (current == null) low else low - 1
        val exiting = (maxOf(0, end - 4) until end).map { epochs[it] }.filter {
            val exit = when(profiles[it.line].mode) { Reveal.Normal -> 300_000; Reveal.Fast -> 160_000; Reveal.Instant -> 120_000 }
            time >= it.leaveUs && time < it.leaveUs + exit
        }
        val selection = source.select(time)
        return VisualSelection(current, exiting, (selection.secondary + listOfNotNull(selection.primary))
            .filter { it != current?.line }, selection.next)
    }
}
