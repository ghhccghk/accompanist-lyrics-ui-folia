package top.ghhccghk.accompanist.lyrics.folia.internal.timeline

import top.ghhccghk.accompanist.lyrics.folia.internal.animation.*
import top.ghhccghk.accompanist.lyrics.folia.internal.model.NormalizedLine

internal data class PrimaryEpoch(val line: Int, val enterUs: Long, val leaveUs: Long)
private const val MAX_EXIT_DURATION_US = 300_000L
internal data class VisualSelection(val primary: PrimaryEpoch?, val exiting: List<PrimaryEpoch>,
    val secondary: List<Int>, val next: Int?)

/** Canonical epochs are prepared once, including overlap resumption and gap tails. */
internal class VisualTimeline(private val lines: List<NormalizedLine>, private val fixedExitDurationUs: Long? = null) {
    private val maxExitDurationUs = fixedExitDurationUs ?: MAX_EXIT_DURATION_US
    private val source = TimelineIndex(lines)
    private val profiles = lines.map { renderProfile(it.startUs, it.endUs, it.fragments.lastOrNull()?.endUs ?: it.endUs) }
    private val epochs: List<PrimaryEpoch>
    private val changeTimes: List<Long>
    private val selections: List<VisualSelection>
    val maxExitingCount: Int
    private val empty = VisualSelection(null, emptyList(), emptyList(), lines.indices.firstOrNull())
    init {
        require(fixedExitDurationUs == null || fixedExitDurationUs in 0..1_000_000)
        val boundaries = lines.indices.flatMap { listOf(lines[it].startUs, lines[it].endUs, profiles[it].renderEndUs) }.distinct().sorted()
        val collected = mutableListOf<PrimaryEpoch>()
        var previous: Int? = null
        var entered = 0L
        var cursor = 0
        var latest: Int? = null
        boundaries.forEach { time ->
            val active = source.select(time)
            // Only the latest begun primary is eligible for a tail; never resurrect an older completed line.
            while (cursor < lines.size && lines[cursor].startUs <= time) {
                if (!lines[cursor].accompaniment && (latest == null || lines[cursor].startUs > lines[latest!!].startUs)) latest = cursor
                cursor++
            }
            val chosen = active.primary ?: latest?.takeIf { time < profiles[it].renderEndUs }
            if (chosen != previous) {
                previous?.let { collected += PrimaryEpoch(it, entered, time) }
                previous = chosen; entered = time
            }
        }
        epochs = collected
        changeTimes = (boundaries + epochs.flatMap { listOf(it.leaveUs + exitDuration(it.line)) }).distinct().sorted()
        selections = changeTimes.map(::prepareSelection)
        maxExitingCount = selections.maxOfOrNull { it.exiting.size } ?: 0
    }
    fun at(time: Long): VisualSelection {
        var low = 0; var high = changeTimes.size
        while (low < high) {
            val middle = (low + high) ushr 1
            if (changeTimes[middle] <= time) low = middle + 1 else high = middle
        }
        return if (low == 0) empty else selections[low - 1]
    }
    private fun exitDuration(line: Int): Long = fixedExitDurationUs ?: when(profiles[line].mode) {
        Reveal.Normal -> MAX_EXIT_DURATION_US; Reveal.Fast -> 160_000; Reveal.Instant -> 120_000
    }
    private fun prepareSelection(time: Long): VisualSelection {
        var low = 0; var high = epochs.size
        while (low < high) {
            val middle = (low + high) ushr 1
            if (epochs[middle].enterUs <= time) low = middle + 1 else high = middle
        }
        val current = (low - 1).takeIf { it >= 0 }?.let(epochs::get)?.takeIf { time < it.leaveUs }
        val end = if (current == null) low else low - 1
        // AnimatePresence retains every unfinished exit. Bound the search by
        // elapsed transition time, never by an arbitrary number of lines.
        var first = 0; var last = end
        while (first < last) {
            val middle = (first + last) ushr 1
            if (epochs[middle].leaveUs <= time - maxExitDurationUs) first = middle + 1 else last = middle
        }
        val exiting = (first until end).map { epochs[it] }.filter {
            time >= it.leaveUs && time < it.leaveUs + exitDuration(it.line)
        }
        val selection = source.select(time)
        return VisualSelection(current, exiting, (selection.secondary + listOfNotNull(selection.primary))
            .filter { it != current?.line }, selection.next)
    }
}
