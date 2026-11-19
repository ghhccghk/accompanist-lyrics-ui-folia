// Copyright (c) 2026 chthollyphile
// PartitaChunk.tsx animate/transition, fixed Folia cc6226e4. SPDX-License-Identifier: AGPL-3.0-only
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.partita

import top.ghhccghk.accompanist.lyrics.folia.internal.animation.*

internal fun partitaLineNeedsOffscreen(timeUs: Long, enterUs: Long, leaveUs: Long?,
    profile: RenderProfile, reducedMotion: Boolean): Boolean {
    if (reducedMotion) return false
    val enterDuration = when (profile.mode) {
        Reveal.Normal -> 300_000L
        Reveal.Fast -> 160_000L
        Reveal.Instant -> 0L
    }
    return (profile.mode != Reveal.Instant && motionElapsedUs(timeUs - enterUs) <= enterDuration) ||
        (leaveUs != null && timeUs >= leaveUs)
}

internal data class PartitaChunkFrame(val x: Float, val y: Float, val scale: Float,
    val rotation: Float, val alpha: Float, val guide: Float, val active: Boolean, val waiting: Boolean)

/** Chunk x/scale targets stay unchanged after singing: the original spring continues. */
internal fun samplePartitaChunk(timeUs: Long, enterUs: Long, row: PartitaRow,
    profile: RenderProfile, reducedMotion: Boolean): PartitaChunkFrame {
    val start = row.units.first().words.first().startUs
    val end = row.units.last().words.last().let { activeWordEnd(it.startUs, it.endUs, profile) }
    val begin = maxOf(enterUs, start - profile.lookaheadUs)
    val elapsed = motionElapsedUs(timeUs - begin)
    val waiting = timeUs < begin
    val active = !waiting && timeUs <= end
    val fromX = row.pose.x + if (row.index % 2 == 0) -40 else 40
    fun waitingValue(from: Float, target: Float, at: Long) = from + (target - from) *
        easeOut(motionElapsedUs(at - enterUs) / 400_000.0)
    fun initial(from: Float, target: Float) = waitingValue(from, target, begin)
    // y/rotate targets are identical in waiting/active/passed, even on the
    // first chunk: Motion leaves their original 400ms easeOut running.
    fun continued(target: Float): Float = waitingValue(0f, target, timeUs)
    return PartitaChunkFrame(
        if (reducedMotion) row.pose.x else if (waiting) waitingValue(0f, fromX, timeUs)
            else physicsValue(initial(0f, fromX), row.pose.x, elapsed / 1e6),
        if (reducedMotion) 0f else continued(row.pose.y),
        if (reducedMotion) 1f else if (waiting) waitingValue(1f, .85f, timeUs)
            else physicsValue(initial(1f, .85f), 1f, elapsed / 1e6),
        if (reducedMotion) 0f else continued(row.pose.rotation),
        if (reducedMotion) if (waiting) 0f else 1f else if (waiting) waitingValue(1f, 0f, timeUs)
            else initial(1f, 0f) + (1 - initial(1f, 0f)) * easeOut(elapsed / 100_000.0),
        if (reducedMotion) if (waiting) 0f else 1f else if (waiting) waitingValue(1f, 0f, timeUs)
            else initial(1f, 0f) + (1 - initial(1f, 0f)) * easeOut(elapsed / 400_000.0), active, waiting)
}
