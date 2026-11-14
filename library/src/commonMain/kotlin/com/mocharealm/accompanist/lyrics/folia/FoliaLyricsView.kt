package com.mocharealm.accompanist.lyrics.folia

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.folia.internal.model.normalize
import com.mocharealm.accompanist.lyrics.folia.internal.timeline.TimelineIndex

/** Host owns media time and seeking. Whole runs are shaped by Compose, never per UTF-16 char. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FoliaLyricsView(
    lyrics: SyncedLyrics,
    playback: () -> FoliaPlaybackSnapshot,
    modifier: Modifier = Modifier,
    contentKey: Any = lyrics.id,
    typography: FoliaTypography = FoliaTypography(),
    colors: FoliaColors = FoliaColors(),
    options: FoliaOptions = FoliaOptions(),
    onLineClicked: (ISyncedLine) -> Unit = {},
    onLinePressed: (ISyncedLine) -> Unit = {},
) {
    val document = remember(IdentityKey(lyrics), contentKey) { normalize(lyrics) }
    val index = remember(document) { TimelineIndex(document.lines) }
    val currentPlayback by rememberUpdatedState(playback)
    val selection by remember(index, options.offsetMillis) { derivedStateOf {
        index.select((currentPlayback().positionMillis - options.offsetMillis) * 1000)
    } }
    BoxWithConstraints(modifier.padding(24.dp)) {
        val mainStyle = typography.main ?: typography.secondary.copy(
            fontSize = (maxWidth.value * .06f).coerceIn(36f, 72f).sp, fontWeight = FontWeight.Bold)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            val main = selection.primary?.let(document.lines::get)
            if (main == null) BasicText(if (document.lines.isEmpty()) "暂无可显示歌词" else "· · ·",
                style = mainStyle.copy(color = colors.secondary, textAlign = TextAlign.Center))
            else {
                BasicText(main.text, Modifier.fillMaxWidth().combinedClickable(
                    onClick = { onLineClicked(main.source) }, onLongClick = { onLinePressed(main.source) }),
                    style = mainStyle.copy(color = colors.text, textAlign = TextAlign.Center))
                if (options.showTranslation) main.translation?.takeIf { it.isNotBlank() && it != main.text }?.let {
                    BasicText(it, Modifier.padding(top = 20.dp), typography.subtitle.copy(color = colors.secondary, textAlign = TextAlign.Center))
                }
                if (options.showPhonetic) main.phonetic?.takeIf { it.isNotBlank() && it != main.text && it != main.translation }?.let {
                    BasicText(it, Modifier.padding(top = 8.dp), typography.subtitle.copy(color = colors.secondary, textAlign = TextAlign.Center))
                }
            }
            selection.secondary.forEach { secondary ->
                val line = document.lines[secondary]
                BasicText(line.text, Modifier.padding(top = 12.dp).combinedClickable(
                    onClick = { onLineClicked(line.source) }, onLongClick = { onLinePressed(line.source) }),
                    typography.secondary.copy(color = colors.secondary, textAlign = TextAlign.Center))
            }
            if (options.showNextLine) selection.next?.let { next ->
                BasicText(document.lines[next].text, Modifier.padding(top = 32.dp),
                    typography.subtitle.copy(color = colors.secondary.copy(alpha = .55f), textAlign = TextAlign.Center))
            }
        }
    }
}

@Composable
fun FoliaLyricsView(
    lyrics: SyncedLyrics,
    currentPosition: () -> Int,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    discontinuity: Long = 0,
    onLineClicked: (ISyncedLine) -> Unit = {},
) = FoliaLyricsView(lyrics, { FoliaPlaybackSnapshot(currentPosition().toLong(), isPlaying, discontinuity) },
    modifier = modifier, onLineClicked = onLineClicked)

/** remember() normally uses structural equality, but callbacks must retain the new input identity. */
private class IdentityKey(private val value: Any) {
    override fun equals(other: Any?) = other is IdentityKey && value === other.value
    override fun hashCode() = 0
}
