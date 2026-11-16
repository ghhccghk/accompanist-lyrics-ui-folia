package top.ghhccghk.accompanist.folia.sample.playback

import top.ghhccghk.accompanist.lyrics.folia.FoliaPlaybackSnapshot

/** Sample transport data, not a library or external player integration API.
 * anchorNanos uses Android elapsedRealtimeNanos, shared by both processes. */
internal data class RemotePlaybackState(
    val session: String,
    val revision: Long,
    val command: Long,
    val positionMillis: Long,
    val durationMillis: Long,
    val anchorNanos: Long,
    val playing: Boolean,
    val seeking: Boolean = false,
    val buffering: Boolean = false,
    val speed: Float = 1f,
    val discontinuity: Long = 1,
)

internal enum class RemotePlaybackCommand { Play, Pause, Seek }

/** Reconstructs from the newest authoritative sample, never from frame deltas.
 * Commands freeze the local anchor until acknowledged. Old sessions, packets and
 * command acknowledgements cannot override a newer seek/pause. No independent clock.
 */
internal class RemotePlaybackMirror(private val session: String) {
    var durationMillis = 0L; private set
    var isSeeking = false; private set
    private var revision = -1L
    private var issued = 0L
    private var remoteDiscontinuity: Long? = null
    private var discontinuity = 0L
    private var jumpPending = false
    private var position = 0L
    private var anchor = 0L
    private var speed = 1f
    private var moving = false
    private var broken = false

    fun accept(state: RemotePlaybackState): Boolean {
        if (broken || state.session != session || state.revision <= revision || state.command < issued) return false
        require(state.command == issued && state.durationMillis > 0 && state.anchorNanos >= 0 &&
            state.positionMillis in 0..state.durationMillis && state.speed.isFinite() && state.speed in .25f..4f &&
            (durationMillis == 0L || durationMillis == state.durationMillis)) { "播放进程状态不合法" }
        if (remoteDiscontinuity != state.discontinuity && !jumpPending) discontinuity++
        remoteDiscontinuity = state.discontinuity
        jumpPending = false
        revision = state.revision
        durationMillis = state.durationMillis
        position = state.positionMillis
        anchor = state.anchorNanos
        speed = state.speed
        isSeeking = state.seeking
        moving = state.playing && !state.seeking && !state.buffering
        return true
    }

    fun snapshot(nowNanos: Long): FoliaPlaybackSnapshot {
        val elapsed = (nowNanos - anchor).coerceAtLeast(0)
        // A lost/hung sampler must not drive lyrics indefinitely. A fresh packet
        // can resume the view; this bound is not a frame-rate integration step.
        val delta = if (moving) elapsed.coerceAtMost(MAX_EXTRAPOLATION_NANOS) / 1_000_000.0 * speed else 0.0
        val sampled = (position + delta.toLong()).coerceIn(0,durationMillis)
        return FoliaPlaybackSnapshot(sampled,moving && elapsed <= MAX_EXTRAPOLATION_NANOS && sampled < durationMillis,discontinuity)
    }

    fun command(command: RemotePlaybackCommand, nowNanos: Long, target: Long = 0): Long {
        check(!broken && durationMillis > 0) { "播放会话尚未准备或已断开" }
        position = snapshot(nowNanos).positionMillis
        anchor = nowNanos
        moving = false
        issued++
        if (command == RemotePlaybackCommand.Seek || (command == RemotePlaybackCommand.Play && position >= durationMillis)) {
            position = if (command == RemotePlaybackCommand.Seek) target.coerceIn(0,durationMillis) else 0
            isSeeking = true
            discontinuity++
            jumpPending = true
        }
        return issued
    }

    fun freeze(nowNanos: Long) {
        if (broken) return
        position = snapshot(nowNanos).positionMillis
        anchor = nowNanos
        moving = false
        isSeeking = false
        broken = true
        discontinuity++
    }

    companion object { const val MAX_EXTRAPOLATION_NANOS = 250_000_000L }
}
