package top.ghhccghk.accompanist.folia.sample.importing

import androidx.compose.runtime.*
import top.ghhccghk.accompanist.folia.sample.fixtures.Scene
import com.mocharealm.accompanist.lyrics.core.parser.EnhancedLrcParser

const val MAX_IMPORT_BYTES = 1024 * 1024

/** Ephemeral document/file capability; never stored as a raw filesystem path in UI state. */
interface LyricsSource {
    val name: String
    suspend fun read(): ByteArray
}

@Composable
expect fun rememberLyricsPicker(onSelected: (LyricsSource) -> Unit, onError: (String) -> Unit): () -> Unit

fun parseLrc(name: String, bytes: ByteArray, generation: Long): Scene {
    require(bytes.size <= MAX_IMPORT_BYTES) { "文件超过 1 MiB 限制" }
    val text = bytes.decodeToString(throwOnInvalidSequence = true).removePrefix("\uFEFF")
    require(EnhancedLrcParser.canParse(text)) { "只支持 UTF-8 标准／增强 LRC（[mm:ss.xx] 或 [mm:ss.xxx]）" }
    // The published parser infers an unbounded final line in standard LRC.
    // Keep its source model intact; only the no-audio demo clock has a finite tail.
    val lyrics = EnhancedLrcParser.parse(text)
    require(lyrics.lines.isNotEmpty()) { "没有可显示的计时歌词" }
    val latestStart = lyrics.lines.maxOf { it.start.toLong() }
    val finiteEnd = lyrics.lines.filter { it.end != Int.MAX_VALUE }.maxOfOrNull { it.end.toLong() } ?: 0
    val duration = maxOf(1_000, latestStart + 5_000, finiteEnd)
    return Scene("import-$generation", lyrics.title.ifBlank { name }, duration, lyrics, emptyList())
}

/** UI owner serializes requests; stale background completions cannot replace a newer choice. */
class ImportRequests {
    private var generation = 0L
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    fun begin(): Long { loading = true; error = null; return ++generation }
    fun invalidate() { ++generation; loading = false; error = null }
    fun accept(token: Long): Boolean {
        if (token != generation) return false
        loading = false
        return true
    }
    fun fail(token: Long, message: String) { if (accept(token)) error = message }
    fun dismissError() { error = null }
}
