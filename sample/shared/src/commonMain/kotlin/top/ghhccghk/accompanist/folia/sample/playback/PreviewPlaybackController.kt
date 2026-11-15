package top.ghhccghk.accompanist.folia.sample.playback

import androidx.compose.runtime.*
import top.ghhccghk.accompanist.lyrics.folia.FoliaPlaybackSnapshot
import kotlin.time.TimeSource

/** Anchor-based media clock. The owner requests refreshes; no private timer/job. */
class PreviewPlaybackController(
    durationMillis: Long,
    private val nowNanos: () -> Long = monotonicClock(),
) {
    var durationMillis by mutableLongStateOf(durationMillis)
        private set
    var snapshot by mutableStateOf(FoliaPlaybackSnapshot(0, false))
        private set
    var speed by mutableDoubleStateOf(1.0)
        private set
    var looping by mutableStateOf(false)
    private var anchorPosition = 0.0
    private var anchorTime = nowNanos()

    init { require(durationMillis > 0) }

    fun refresh(): FoliaPlaybackSnapshot {
        if (!snapshot.isPlaying) return snapshot
        val now = nowNanos()
        val position = anchorPosition + (now - anchorTime).coerceAtLeast(0) / 1_000_000.0 * speed
        if (position >= durationMillis) {
            if (looping) {
                val loops = (position / durationMillis).toLong()
                anchorPosition = position % durationMillis
                anchorTime = now
                snapshot = FoliaPlaybackSnapshot(anchorPosition.toLong(), true, snapshot.discontinuity + loops)
            } else {
                anchorPosition = durationMillis.toDouble()
                anchorTime = now
                snapshot = snapshot.copy(positionMillis = durationMillis, isPlaying = false)
            }
        } else snapshot = snapshot.copy(positionMillis = position.toLong().coerceAtLeast(0))
        return snapshot
    }

    fun play() {
        if (snapshot.isPlaying) return
        if (snapshot.positionMillis == durationMillis) seek(0)
        anchorTime = nowNanos()
        snapshot = snapshot.copy(isPlaying = true)
    }
    fun pause() {
        refresh()
        anchorPosition = snapshot.positionMillis.toDouble()
        anchorTime = nowNanos()
        snapshot = snapshot.copy(isPlaying = false)
    }
    fun toggle() { if (snapshot.isPlaying) pause() else play() }
    fun seek(positionMillis: Long) {
        anchorPosition = positionMillis.coerceIn(0, durationMillis).toDouble()
        anchorTime = nowNanos()
        snapshot = snapshot.copy(positionMillis = anchorPosition.toLong(), discontinuity = snapshot.discontinuity + 1)
    }
    fun changeSpeed(value: Double) {
        require(value.isFinite() && value in .25..4.0)
        refresh()
        anchorPosition = snapshot.positionMillis.toDouble()
        anchorTime = nowNanos()
        speed = value
    }
    fun replaceDuration(value: Long) {
        require(value > 0)
        pause()
        durationMillis = value
        seek(0)
    }
}

private fun monotonicClock(): () -> Long {
    val origin = TimeSource.Monotonic.markNow()
    return { origin.elapsedNow().inWholeNanoseconds }
}
