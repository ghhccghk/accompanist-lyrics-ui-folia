package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.runtime.Immutable

/** A single coherent media-time sample. Position already includes playback speed. */
@Immutable
data class FoliaPlaybackSnapshot(
    val positionMillis: Long,
    val isPlaying: Boolean,
    val discontinuity: Long = 0,
)
