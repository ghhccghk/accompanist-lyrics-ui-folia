package top.ghhccghk.accompanist.folia.sample.importing

import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine

/**
 * Resolve the published parser's open LRC endpoint against the host timeline.
 *
 * This is a display copy, not a parser change or a claimed word timestamp.
 * Keep the parsed object in SongSession, explicit ends/words and original starts.
 * Call once per document/timeline, never from the media frame loop.
 */
internal fun resolveOpenLrcEnds(source: SyncedLyrics, durationMillis: Long): SyncedLyrics {
    require(durationMillis > 0) { "A finite positive host duration is required" }
    val end = durationMillis.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    if (source.lines.none { it is SyncedLine && it.end == Int.MAX_VALUE }) return source
    return source.copy(lines = source.lines.map { line ->
        if (line is SyncedLine && line.end == Int.MAX_VALUE)
            line.copy(end = maxOf(line.start, end)) // Future lines must not invert.
        else line
    })
}
