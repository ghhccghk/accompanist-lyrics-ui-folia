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
import com.mocharealm.accompanist.lyrics.folia.internal.timeline.VisualTimeline
import com.mocharealm.accompanist.lyrics.folia.internal.styles.classic.ClassicLine
import com.mocharealm.accompanist.lyrics.folia.internal.rendering.GlowBudget

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
    style: FoliaStyle = FoliaStyle.Classic(),
    onStatus: (FoliaStatus) -> Unit = {},
    onLineClicked: (ISyncedLine) -> Unit = {},
    onLinePressed: (ISyncedLine) -> Unit = {},
) {
    val document = remember(IdentityKey(lyrics), contentKey) { normalize(lyrics) }
    val index = remember(document) { VisualTimeline(document.lines) }
    val currentPlayback by rememberUpdatedState(playback)
    val selection by remember(index, options.offsetMillis) { derivedStateOf {
        index.at((currentPlayback().positionMillis - options.offsetMillis) * 1000)
    } }
    // Preparation callbacks belong to this document/epoch, including a null primary in gaps.
    // Never carry a previous line's count or budget fallback into the next selection.
    var quality by remember(document, selection.primary, options.quality) { mutableStateOf(FoliaQuality.Minimal) }
    var unitCount by remember(document, selection.primary, options.quality) { mutableIntStateOf(0) }
    val statusObserver by rememberUpdatedState(onStatus)
    LaunchedEffect(document, selection.primary, quality, options.quality, unitCount) {
        statusObserver(FoliaStatus(options.quality, quality, unitCount,
            document.diagnostics.map { "${it.ref.index}:${it.reason}" } +
                if (unitCount > 0 && quality != options.quality) listOf("GlowBudgetExceeded") else emptyList()))
    }
    BoxWithConstraints(modifier.padding(24.dp)) {
        val heightBudget = maxHeight * .6f
        val mainStyle = typography.main ?: typography.secondary.copy(
            fontSize = ((maxWidth.value * .06f).coerceIn(36f, 72f) * typography.scale).sp, fontWeight = FontWeight.Bold)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            val main = selection.primary?.line?.let(document.lines::get)
            val tuning = (style as FoliaStyle.Classic).tuning
            Box(Modifier.fillMaxWidth()) {
                selection.exiting.forEach { epoch -> key(epoch) {
                    ClassicLine(document.lines[epoch.line], playback, mainStyle, colors, options, tuning,
                        heightBudget = heightBudget, enterUs = epoch.enterUs, leaveUs = epoch.leaveUs,
                        maskBudgetBytes = GlowBudget.perExitBytes)
                } }
                if (main == null) BasicText(if (document.lines.isEmpty()) "暂无可显示歌词" else "· · ·",
                    modifier = Modifier.fillMaxWidth(),
                    style = mainStyle.copy(color = colors.secondary, textAlign = TextAlign.Center))
                else key(selection.primary) {
                    ClassicLine(main, playback, mainStyle, colors, options, tuning, heightBudget,
                        Modifier.fillMaxWidth(),
                        enterUs = selection.primary!!.enterUs,
                        onPrepared = { actual, count -> quality = actual; unitCount = count },
                        onClick = { onLineClicked(main.source) }, onLongPress = { onLinePressed(main.source) })
                }
            }
            if (main != null) {
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
