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
    require(files.lyrics != null || files.translation == null) { "请先选择歌词，再选择翻译文件" }
    val lyrics = files.lyrics?.read() ?: return null
    val translation = files.translation?.read()
    return withContext(Dispatchers.Default) { parseSongLyrics(lyrics, translation) }
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
    val translations = parse(translation).lines.mapNotNull { line ->
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
