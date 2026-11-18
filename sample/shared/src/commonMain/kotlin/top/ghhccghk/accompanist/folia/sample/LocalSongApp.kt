package top.ghhccghk.accompanist.folia.sample

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.key.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import top.ghhccghk.accompanist.folia.sample.importing.*
import top.ghhccghk.accompanist.folia.sample.playback.*
import top.ghhccghk.accompanist.lyrics.folia.*
import kotlinx.coroutines.*

@Composable
fun FoliaSampleApp(
    visible: Boolean = true, session: SampleSession = remember { SampleSession() },
    settingsStore: SettingsStore? = rememberSettingsStore()
) {
    val songs = session.songs
    val prepare = rememberAudioPreparation()
    val scope = rememberCoroutineScope()
    val latestVisible by rememberUpdatedState(visible)
    var work by remember { mutableStateOf<Job?>(null) }
    var selection by remember { mutableStateOf(false) }
    var editingLyrics by remember { mutableStateOf(false) }
    var selectionError by remember { mutableStateOf<String?>(null) }
    var audio by remember { mutableStateOf<LyricsSource?>(null) }
    var lyrics by remember { mutableStateOf<LyricsSource?>(null) }
    var translation by remember { mutableStateOf<LyricsSource?>(null) }
    var settings by remember { mutableStateOf(false) }
    var renderDebug by remember { mutableStateOf(false) }
    var renderStatus by remember { mutableStateOf<FoliaStatus?>(null) }
    LaunchedEffect(songs.current?.revision,session.lyricTheme){renderStatus=null}
    fun cancelSelection() {
        work?.cancel(); songs.cancelPending(); selection = false
    }

    fun openSelection() {
        editingLyrics = false; audio = null; lyrics = null; translation = null; selectionError =
            null; selection = true
    }

    val systemPick = rememberSongFilePicker({ role, file ->
        songs.finishFileChoice()
        if (file != null) when (role) {
            FileRole.Audio -> audio = file; FileRole.Lyrics -> lyrics =
                file; FileRole.Translation -> translation = file
        }
    }, { songs.finishFileChoice(); selectionError = it })
    val pick: (FileRole) -> Unit = { role -> songs.beginFileChoice(); systemPick(role) }
    val nativePlayer = songs.current?.player
    // Remote transport events also wake paused/buffering/error states. The
    // per-frame path below reads only the adapter's local snapshot mirror.
    LaunchedEffect(visible, session.demo, nativePlayer) {
        if (visible && !session.demo && nativePlayer != null) {
            snapshotFlow { nativePlayer.updateRevision }.collect { songs.poll() }
        }
    }
    val nativePlaying = nativePlayer?.snapshot?.isPlaying == true
    val nativeSeeking = nativePlayer?.isSeeking == true
    LaunchedEffect(
        visible,
        session.demo,
        nativePlayer,
        songs.filePickerActive,
        nativePlaying,
        nativeSeeking
    ) {
        if (!visible || session.demo) songs.control { it.pause() }
        else if (!songs.filePickerActive) {
            songs.resumeFileChoice(); songs.poll()
            while (isActive && (nativePlayer?.snapshot?.isPlaying == true || nativePlayer?.isSeeking == true)) {
                withFrameNanos { songs.poll() }
            }
        }
    }
    DisposableEffect(session) { onDispose { songs.cancelPending(); songs.control { it.pause() }; session.clock.pause() } }
    if (session.demo) {
        Column {
            MaterialTheme(colorScheme = darkColorScheme()) {
                TextButton({
                    session.clock.pause(); session.demo = false
                }) { Text("返回本地歌曲") }
            }
            Box(Modifier.weight(1f)) { DemoFoliaSampleApp(visible, session, settingsStore) }
        }
        return
    }
    LaunchedEffect(settingsStore) {
        if (settingsStore != null) try {
            if (!session.preferencesLoaded) {
                settingsStore.read()
                    ?.let { session.applySettings(SampleSettings.decode(it)) }; session.preferencesLoaded =
                    true
            }
            snapshotFlow { session.settings().encode() }.collect { settingsStore.write(it) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            selectionError = "设置存储失败：" + failure.message
        }
    }
    MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFFB6CFFF))) {
        Surface(Modifier.fillMaxSize().onKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown || selection || settings || songs.error != null) false
            else when (event.key) {
                Key.Spacebar -> {
                    if (visible) songs.control { if (songs.snapshot.isPlaying) it.pause() else it.play() }; true
                }

                Key.DirectionLeft -> {
                    songs.control { it.seek(songs.snapshot.positionMillis - 5000) }; true
                }

                Key.DirectionRight -> {
                    songs.control { it.seek(songs.snapshot.positionMillis + 5000) }; true
                }

                else -> false
            }
        }, color = Color(0xFF162435), contentColor = MaterialTheme.colorScheme.onSurface) {
            BoxWithConstraints(Modifier.fillMaxSize().safeContentPadding().padding(16.dp)) {
                val song = songs.current
                val largeCover = maxWidth >= 600.dp && maxHeight >= 540.dp
                if (song == null) Column(
                    Modifier.align(Alignment.Center).widthIn(max = 460.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Folia · 本地歌曲", style = MaterialTheme.typography.headlineMedium)
                    Text("选择音频开始播放；歌词和翻译可以稍后添加。")
                    Button(::openSelection) { Text("选择歌曲") }
                    TextButton({ settings = true }) { Text("设置") }
                    Text(localAudioFormats, style = MaterialTheme.typography.bodySmall)
                    TextButton({ session.demo = true }) { Text("开发／演示") }
                } else {
                    // The real parser's open final LRC endpoint is not a
                    // 24-day song. Keep raw parsed lyrics in SongSession and
                    // bound only the display copy by actual native duration.
                    val displayLyrics=remember(song.lyrics,song.player.durationMillis) {
                        song.lyrics?.let{resolveOpenLrcEnds(it,song.player.durationMillis)}
                    }
                    val metadata: @Composable () -> Unit = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SongCover(
                                song.player.metadata.artwork,
                                Modifier.size(if (largeCover) 220.dp else 64.dp)
                            )
                            Text(
                                song.player.metadata.title,
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 2
                            )
                            Text(song.player.metadata.artist)
                            song.content.sourceLabel?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                            song.content.notice?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                            FlowRow {
                                TextButton(::openSelection) { Text("重新选歌") }
                                TextButton({
                                    editingLyrics = true; audio = song.files.audio; lyrics =
                                    song.files.lyrics; translation =
                                    song.files.translation; selectionError = null; selection = true
                                }) { Text("补选歌词／翻译") }
                                TextButton({ settings = true }) { Text("设置") }
                                TextButton({
                                    songs.control { it.pause() }; session.demo = true
                                }) { Text("开发／演示") }
                            }
                        }
                    }
                    val panel: @Composable (Modifier) -> Unit = { modifier ->
                        if (song.content.plainText != null) PlainLyricsPanel(song.content.plainText, modifier)
                        else if (song.lyrics == null) Box(
                            modifier,
                            contentAlignment = Alignment.Center
                        ) { Text("未加载歌词") }
                        else if (!songs.snapshot.isPlaying && songs.snapshot.positionMillis >= song.player.durationMillis)
                            Box(modifier, contentAlignment = Alignment.Center) { Text("播放结束") }
                        else SampleRenderStage(session.renderResolution,modifier) { SampleMaskScope(session) {
                            SampleLyricsRegion(session,visible,
                                displayLyrics!!,
                                { songs.snapshot },
                                Modifier.fillMaxSize(),
                                contentKey = song.revision,
                                options = session.options,
                                typography = FoliaTypography(scale = session.fontScale),
                                style = session.lyricStyleForTitle(song.player.metadata.title),
                                onStatus = { renderStatus=it },
                                onLineClicked = { line -> songs.control { it.seek(line.start.toLong() + session.options.offsetMillis) } })
                        } }
                    }
                    if (maxWidth < 600.dp) Column {
                        metadata(); panel(
                        Modifier.weight(1f).fillMaxWidth()
                    ); LocalTransport(songs, visible)
                    }
                    else Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        Column(
                            Modifier.fillMaxWidth(.38f).fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.Center
                        ) { metadata(); LocalTransport(songs, visible) }
                        panel(Modifier.weight(1f).fillMaxHeight())
                    }
                }
                RenderDebugOverlay(renderDebug,visible,session,{songs.snapshot},renderStatus,{renderDebug=false},
                    Modifier.align(Alignment.TopEnd))
            }
        }
        if (selection) AlertDialog(
            onDismissRequest = ::cancelSelection,
            title = { Text("选择歌曲") },
            text = {
                Column(
                    Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        { pick(FileRole.Audio) },
                        enabled = !songs.loading && !editingLyrics
                    ) { Text(audio?.name ?: "音频文件（必选）") }
                    OutlinedButton({ pick(FileRole.Lyrics) }, enabled = !songs.loading) {
                        Text(
                            lyrics?.name ?: "歌词文件（可选）"
                        )
                    }
                    if (lyrics != null) TextButton({
                        lyrics = null; translation = null
                    }) { Text("清除歌词") }
                    OutlinedButton(
                        { pick(FileRole.Translation) },
                        enabled = !songs.loading && audio != null
                    ) { Text(translation?.name ?: "翻译文件（可选）") }
                    if (translation != null) TextButton({ translation = null }) { Text("清除翻译") }
                    Text(localAudioFormats, style = MaterialTheme.typography.bodySmall)
                    Text(
                        "不选歌词文件时自动读取音频内嵌歌词；外部歌词优先。外部文件使用 UTF-8，翻译按相同起始时间戳匹配。",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (songs.loading) Text("正在准备…原会话保留")
                    (selectionError ?: songs.error)?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    {
                        val files = SongFiles(audio!!, lyrics, translation); work?.cancel()
                        work = scope.launch {
                            val success = if (editingLyrics) songs.attachContent(
                                files,
                                ::loadSongContent
                            ) else songs.openContent(files, prepare, ::loadSongContent)
                            if (success) {
                                session.clock.pause(); selection =
                                    false; if (!latestVisible) songs.control { it.pause() }
                            }
                        }
                    },
                    enabled = audio != null && !songs.loading
                ) { Text(if (editingLyrics) "应用歌词" else "播放所选歌曲") }
            },
            dismissButton = { TextButton(::cancelSelection) { Text("取消") } })
        if (!selection) (songs.error ?: selectionError)?.let { message ->
            AlertDialog(
                onDismissRequest = { songs.dismissError(); selectionError = null },
                title = { Text("操作失败 · 原会话保留") },
                text = { Text(message) },
                confirmButton = {
                    TextButton({
                        songs.dismissError(); selectionError = null
                    }) { Text("关闭") }
                })
        }
        if (settings) AlertDialog(
            onDismissRequest = { settings = false },
            title = { Text("显示设置") },
            text = {
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                    ExperimentalMaskControls(session)
                    LyricThemeControls(session)
                    RenderResolutionControls(session)
                    RenderDebugToggle(renderDebug) { renderDebug=it }
                    AudioToggle("翻译", session.options.showTranslation) {
                        session.options = session.options.copy(showTranslation = it)
                    }
                    AudioToggle("注音", session.options.showPhonetic) {
                        session.options = session.options.copy(showPhonetic = it)
                    }
                    AudioToggle("下一句", session.options.showNextLine) {
                        session.options = session.options.copy(showNextLine = it)
                    }
                    AudioToggle("减少动态效果", session.options.reducedMotion) {
                        session.options = session.options.copy(reducedMotion = it)
                    }
                    Text("歌词偏移 ms：" + session.options.offsetMillis)
                    Slider(
                        session.options.offsetMillis.toFloat(),
                        { session.options = session.options.copy(offsetMillis = it.toInt()) },
                        valueRange = -2000f..2000f
                    )
                    TextButton({
                        session.options =
                            session.options.copy(quality = FoliaQuality.entries[(session.options.quality.ordinal + 1) % FoliaQuality.entries.size])
                    }) { Text("画质：" + session.options.quality) }
                }
            },
            confirmButton = { TextButton({ settings = false }) { Text("完成") } })
    }
}

@Composable
private fun PlainLyricsPanel(text: String, modifier: Modifier) {
    // Plain tags carry no trustworthy clock. Keep the complete text readable and selectable.
    androidx.compose.foundation.text.selection.SelectionContainer(modifier) {
        Text(text, Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun SongCover(bitmap: ImageBitmap?, modifier: Modifier) {
    val shape = modifier.clip(RoundedCornerShape(12.dp))
    if (bitmap != null) Image(bitmap, "歌曲封面", shape)
    else Box(shape.background(Color(0xFF485970)), contentAlignment = Alignment.Center) {
        Text(
            "♫",
            style = MaterialTheme.typography.headlineLarge
        )
    }
}

@Composable
private fun AudioToggle(label: String, value: Boolean, change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            Modifier.weight(1f)
        ); Switch(value, change)
    }
}

@Composable
private fun LocalTransport(songs: SongSession, enabled: Boolean) {
    val song = songs.current ?: return
    var preview by remember(song.revision) { mutableStateOf<Float?>(null) }
    val interactions = remember { MutableInteractionSource() }
    var dragging by remember { mutableStateOf(false) }
    val latestEnabled by rememberUpdatedState(enabled)
    LaunchedEffect(interactions) {
        interactions.interactions.collect { event ->
            when (event) {
                is DragInteraction.Start -> dragging = true
                is DragInteraction.Stop -> {
                    if (latestEnabled) preview?.let { target -> songs.control { it.seek(target.toLong()) } }; preview =
                        null; dragging = false
                }

                is DragInteraction.Cancel, is PressInteraction.Cancel -> {
                    preview = null; dragging = false
                }
            }
        }
    }
    LaunchedEffect(enabled) { if (!enabled) preview = null }
    Column {
        Slider(
            preview ?: songs.snapshot.positionMillis.toFloat(),
            { preview = it },
            enabled = enabled,
            interactionSource = interactions,
            modifier = Modifier.semantics { contentDescription = "播放进度" },
            valueRange = 0f..song.player.durationMillis.toFloat(),
            onValueChangeFinished = {
                if (!dragging) {
                    if (enabled) preview?.let { target -> songs.control { it.seek(target.toLong()) } }; preview =
                        null
                }
            })
        Text(formatAudioTime(songs.snapshot.positionMillis) + " / " + formatAudioTime(song.player.durationMillis))
        Row {
            Button(
                { songs.control { if (songs.snapshot.isPlaying) it.pause() else it.play() } },
                enabled = enabled
            ) { Text(if (songs.snapshot.isPlaying) "暂停" else "播放") }
            TextButton({ songs.control { it.seek(0) } }, enabled = enabled) { Text("重播") }
        }
    }
}

private fun formatAudioTime(ms: Long): String {
    val s = ms / 1000; return (s / 60).toString() + ":" + (s % 60).toString().padStart(2, '0')
}
