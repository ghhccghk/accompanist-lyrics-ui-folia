// Adapted from Accompanist SelectedLyrics.kt, commit 6b1ab72319bf2143225acf7797031efa7491897c.
// SPDX-License-Identifier: Apache-2.0. Attribution and modifications: docs/upstream.md.
package top.ghhccghk.accompanist.folia.sample.importing

import top.ghhccghk.accompanist.folia.sample.playback.SongFiles
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import com.mocharealm.accompanist.lyrics.core.parser.AutoParser
import kotlinx.coroutines.*

suspend fun loadSongLyrics(files: SongFiles): SyncedLyrics? {
    return loadSongContent(files).lyrics
}

data class SongLyricsContent(
    val lyrics: SyncedLyrics? = null,
    val plainText: String? = null,
    val sourceLabel: String? = null,
    val notice: String? = null
)

/** Explicit files win, including their errors. Tag failures never discard playable audio. */
suspend fun loadSongContent(files: SongFiles): SongLyricsContent {
    files.lyrics?.let { source ->
        val bytes = source.read()
        val translation = files.translation?.read()
        return withContext(Dispatchers.Default) {
            SongLyricsContent(parseSongLyrics(bytes, translation), sourceLabel = "外部歌词")
        }
    }
    val embedded = files.audio.embeddedLyrics()
    val content = withContext(Dispatchers.Default) {
        var parseFailure: String? = null
        var plain: String? = null
        for (raw in embedded.texts) {
            ensureActive()
            val text = raw.removePrefix("\uFEFF").trim('\u0000', ' ', '\r', '\n')
            if (text.isBlank()) continue
            if (plain == null) plain = text
            try {
                if (!AutoParser().canParse(text)) continue
                val parsed = parseSongLyrics(text.encodeToByteArray())
                return@withContext SongLyricsContent(parsed, sourceLabel = "内嵌计时歌词", notice = embedded.notice)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (invalid: Exception) {
                parseFailure = "内嵌时间轴解析失败：${invalid.message ?: invalid.toString()}"
            }
        }
        if (plain != null) SongLyricsContent(plainText = plain, sourceLabel = "内嵌纯文本歌词",
            notice = "没有可用时间轴，按纯文本显示，不进行高亮或点击跳转。" +
                (listOfNotNull(embedded.notice, parseFailure).takeIf { it.isNotEmpty() }?.joinToString(" ", prefix = " ") ?: ""))
        else SongLyricsContent(notice = embedded.notice)
    }
    if (files.translation == null) return content
    require(content.lyrics != null) { "翻译需要外部或内嵌计时歌词；当前歌曲没有可匹配的时间轴" }
    val translation = files.translation.read()
    return withContext(Dispatchers.Default) {
        content.copy(lyrics = applySongTranslation(content.lyrics, parseSongLyrics(translation)))
    }
}
fun parseSongLyrics(bytes: ByteArray, translation: ByteArray? = null): SyncedLyrics {
    fun parse(value: ByteArray): SyncedLyrics {
        require(value.size <= MAX_IMPORT_BYTES) { "歌词文件超过 1 MiB" }
        val content = value.decodeToString(throwOnInvalidSequence=true).removePrefix("﻿")
        val parser = AutoParser()
        require(parser.canParse(content)) { "无法识别计时歌词；请使用 UTF-8 LRC 等解析器支持的文本格式" }
        return parser.parse(content).also { require(it.lines.isNotEmpty()) { "没有可用的计时歌词" } }
    }
    val lyrics = parse(bytes)
    if (translation == null) return lyrics
    return applySongTranslation(lyrics, parse(translation))
}

private fun applySongTranslation(lyrics: SyncedLyrics, translation: SyncedLyrics): SyncedLyrics {
    val translations = translation.lines.mapNotNull { line ->
        val text = when(line) { is KaraokeLine -> line.syllables.joinToString("") { it.content }; is SyncedLine -> line.content; else -> "" }.trim()
        text.takeIf { it.isNotBlank() }?.let { line.start to it }
    }.groupBy({ it.first }, { it.second }).mapValues { it.value.first() }
    var matches = 0
    fun translated(start: Int, fallback: String?): String? = translations[start]?.also { matches++ } ?: fallback
    val result = lyrics.copy(lines=lyrics.lines.map { line -> when(line) {
        is KaraokeLine.MainKaraokeLine -> line.copy(translation=translated(line.start,line.translation),
            accompanimentLines=line.accompanimentLines?.map { it.copy(translation=translated(it.start,it.translation)) })
        is KaraokeLine.AccompanimentKaraokeLine -> line.copy(translation=translated(line.start,line.translation))
        is SyncedLine -> line.copy(translation=translated(line.start,line.translation))
        else -> line
    } })
    require(matches > 0) { "翻译文件没有与歌词匹配的起始时间戳" }
    return result
}
