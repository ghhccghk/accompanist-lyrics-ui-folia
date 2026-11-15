package com.mocharealm.accompanist.lyrics.folia

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.folia.internal.model.normalize
import com.mocharealm.accompanist.lyrics.folia.internal.timeline.VisualTimeline
import com.mocharealm.accompanist.lyrics.folia.internal.timeline.SubtitleHistory
import com.mocharealm.accompanist.lyrics.folia.internal.timeline.SubtitleSource
import com.mocharealm.accompanist.lyrics.folia.internal.timeline.resolveSubtitleContent
import com.mocharealm.accompanist.lyrics.folia.internal.styles.classic.ClassicLine
import com.mocharealm.accompanist.lyrics.folia.internal.styles.classic.ClassicSubtitles
import com.mocharealm.accompanist.lyrics.folia.internal.styles.classic.ClassicStage
import com.mocharealm.accompanist.lyrics.folia.internal.styles.classic.classicHeightBudget
import com.mocharealm.accompanist.lyrics.folia.internal.rendering.GlowBudget
import com.mocharealm.accompanist.lyrics.folia.internal.waiting.*
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

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
    // Unknown/invalid vocal intervals cannot safely establish silence.
    val waitingTimeline = remember(document) {
        WaitingTimeline(if(document.diagnostics.isEmpty()) document.lines else emptyList())
    }
    val subtitleHistory = remember(document) { SubtitleHistory(document.lines) }
    val currentPlayback by rememberUpdatedState(playback)
    val selection by remember(index, options.offsetMillis) { derivedStateOf {
        index.at((currentPlayback().positionMillis - options.offsetMillis) * 1000)
    } }
    val waiting by remember(waitingTimeline,index,options.offsetMillis) { derivedStateOf {
        val time=(currentPlayback().positionMillis-options.offsetMillis)*1000
        waitingTimeline.visibleAt(time,index.at(time))
    } }
    val subtitles by remember(index,subtitleHistory,options) {derivedStateOf {
        val time=(currentPlayback().positionMillis-options.offsetMillis)*1000
        val active=index.at(time).primary?.line
        resolveSubtitleContent(document.lines,active,subtitleHistory.recent(time,active),buildList {
            if(options.showTranslation) add(SubtitleSource.Translation)
            if(options.showPhonetic) add(SubtitleSource.Phonetic)
            if(options.showNextLine) add(SubtitleSource.Next)
        },visible=options.showTranslation||options.showPhonetic||options.showNextLine,allowFallback=options.showNextLine)
    }}
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
    BoxWithConstraints(modifier) {
        // Upstream classic uses the full viewport for 6vw, then a centred 70vh
        // stage with p-8. Subtitles are an overlay and must not move the main line.
        val heightBudget = with(LocalDensity.current) { classicHeightBudget(maxHeight.toPx(), density).toDp() }
        val mainStyle = typography.main ?: typography.secondary.copy(
            fontSize = ((maxWidth.value * .06f).coerceIn(36f, 72f) * typography.scale).sp, fontWeight = FontWeight.Bold)
        val main = selection.primary?.line?.let(document.lines::get)
        val tuning = (style as FoliaStyle.Classic).tuning
        ClassicStage(Modifier.fillMaxSize()) {
                selection.exiting.forEach { epoch -> key(epoch) {
                    ClassicLine(document.lines[epoch.line], playback, mainStyle, colors, options, tuning,
                        heightBudget = heightBudget, enterUs = epoch.enterUs, leaveUs = epoch.leaveUs,
                        maskBudgetBytes = GlowBudget.perExitBytes(index.maxExitingCount))
                } }
                if (document.lines.isEmpty()) BasicText("暂无可显示歌词",
                    modifier = Modifier.fillMaxWidth(),
                    style = mainStyle.copy(color = colors.secondary, textAlign = TextAlign.Center))
                else if(main != null) key(selection.primary) {
                    ClassicLine(main, playback, mainStyle, colors, options, tuning, heightBudget,
                        Modifier.fillMaxWidth(),
                        enterUs = selection.primary!!.enterUs,
                        onPrepared = { actual, count -> quality = actual; unitCount = count },
                        onClick = { onLineClicked(main.source) }, onLongPress = { onLinePressed(main.source) })
                }
                waiting?.let { gap ->
                    WaitingBreathingDots((gap.startUs/1000).toInt(),(gap.endUs/1000).toInt(),
                        { (currentPlayback().positionMillis-options.offsetMillis)
                            .coerceIn(Int.MIN_VALUE.toLong(),Int.MAX_VALUE.toLong()).toInt() },
                        Modifier.semantics { contentDescription="等待演唱" },
                        reducedMotion=options.reducedMotion,
                        defaults=WaitingDotsDefaults(breathingDotsColor=colors.active))
                }
        }
        ClassicSubtitles(subtitles,document.lines,playback,typography,colors,options,maxWidth,
            selection.primary?.enterUs?:subtitles.line?.let {document.lines[it].startUs},
            Modifier.align(Alignment.BottomCenter).padding(bottom=112.dp)) {
            selection.secondary.forEach { secondary ->
                val line = document.lines[secondary]
                BasicText(line.text, Modifier.padding(top = 12.dp).combinedClickable(
                    onClick = { onLineClicked(line.source) }, onLongClick = { onLinePressed(line.source) }),
                    typography.secondary.copy(color = colors.secondary, textAlign = TextAlign.Center))
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
