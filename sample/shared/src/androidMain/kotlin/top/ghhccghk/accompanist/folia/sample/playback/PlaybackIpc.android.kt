package top.ghhccghk.accompanist.folia.sample.playback

import android.os.Bundle

/** Private sample protocol. Only primitives, source URI and a reply Messenger;
 * lyrics, covers and per-frame drawing never cross this boundary. */
internal object PlaybackIpc {
    const val PREPARE = 1
    const val PLAY = 2
    const val PAUSE = 3
    const val SEEK = 4
    const val CLOSE = 5
    const val ACK = 6
    const val STATE = 100
    const val ERROR = 101
    const val SAMPLE_MILLIS = 50L
    const val PREPARE_TIMEOUT_MILLIS = 15_000L

    fun encode(state: RemotePlaybackState) = Bundle().apply {
        putString("session",state.session); putLong("revision",state.revision); putLong("command",state.command)
        putLong("position",state.positionMillis); putLong("duration",state.durationMillis)
        putLong("anchor",state.anchorNanos); putBoolean("playing",state.playing)
        putBoolean("seeking",state.seeking); putBoolean("buffering",state.buffering)
        putFloat("speed",state.speed); putLong("discontinuity",state.discontinuity)
    }
    fun decode(data: Bundle) = RemotePlaybackState(
        requireNotNull(data.getString("session")),data.getLong("revision"),data.getLong("command"),
        data.getLong("position"),data.getLong("duration"),data.getLong("anchor"),data.getBoolean("playing"),
        data.getBoolean("seeking"),data.getBoolean("buffering"),data.getFloat("speed"),data.getLong("discontinuity"))
}
