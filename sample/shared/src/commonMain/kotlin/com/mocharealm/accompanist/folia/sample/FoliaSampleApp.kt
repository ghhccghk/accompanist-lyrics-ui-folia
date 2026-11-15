package com.mocharealm.accompanist.folia.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.key.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.mocharealm.accompanist.folia.sample.fixtures.scenes
import com.mocharealm.accompanist.folia.sample.playback.PreviewPlaybackController
import com.mocharealm.accompanist.folia.sample.importing.*
import kotlinx.coroutines.*
import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.folia.*

@Composable
internal fun DemoFoliaSampleApp(visible: Boolean = true, session: SampleSession = remember { SampleSession() },
    settingsStore: SettingsStore? = rememberSettingsStore()) {
    var scene by session::scene
    val clock = session.clock
    val playing by remember { derivedStateOf { clock.snapshot.isPlaying } }
    var options by session::options
    var tuning by session::tuning
    var fontScale by session::fontScale
    var settings by remember { mutableStateOf(false) }
    var picker by remember { mutableStateOf(false) }
    var debug by remember { mutableStateOf(false) }
    var pressed by remember { mutableStateOf<ISyncedLine?>(null) }
    var status by remember { mutableStateOf<FoliaStatus?>(null) }
    var preferenceError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(session, settingsStore) {
        if (settingsStore != null) {
            try {
                if (!session.preferencesLoaded) {
                    val initial = session.settings()
                    val stored = settingsStore.read()?.let(SampleSettings::decode)
                    if (stored != null && session.settings() == initial) session.applySettings(stored)
                    session.preferencesLoaded = true
                }
                snapshotFlow { session.settings().encode() }.collect { settingsStore.write(it) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { preferenceError = "设置存储失败：${error.message ?: error}" }
        }
    }
    val requests = remember { ImportRequests() }
    val scope = rememberCoroutineScope()
    var importJob by remember { mutableStateOf<Job?>(null) }
    var importedSource by session::importedSource
    val load: (LyricsSource) -> Unit = { source ->
        importJob?.cancel()
        val token = requests.begin()
        importJob = scope.launch {
            try {
                val bytes = source.read()
                val imported = withContext(Dispatchers.Default) { parseLrc(source.name, bytes, token) }
                if (requests.accept(token)) {
                    scene = imported; importedSource = source; clock.replaceDuration(imported.durationMillis)
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { requests.fail(token, error.message ?: error.toString()) }
        }
    }
    val open = rememberLyricsPicker(load) { message -> requests.fail(requests.begin(), message) }
    val keyboardFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { keyboardFocus.requestFocus() }
    LaunchedEffect(playing, visible) {
        if (!visible) clock.pause()
        else if (playing) while (clock.snapshot.isPlaying) withFrameNanos { clock.refresh() }
    }
    DisposableEffect(clock) { onDispose { clock.pause() } }
    MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFFB6CFFF))) {
        BoxWithConstraints(Modifier.fillMaxSize().onKeyEvent { event ->
            // Bubble after focused controls: slider arrows/buttons retain their own behavior.
            if (event.type != KeyEventType.KeyDown || settings || picker || pressed != null || requests.error != null) false
            else when {
                event.isCtrlPressed && event.key == Key.O -> { open(); true }
                event.isCtrlPressed && event.key == Key.R -> { importedSource?.let(load); true }
                event.isCtrlPressed || event.isAltPressed || event.isMetaPressed -> false
                event.key == Key.Spacebar -> { if (visible) clock.toggle(); true }
                event.key == Key.DirectionLeft -> { clock.seek(clock.snapshot.positionMillis - 5_000); true }
                event.key == Key.DirectionRight -> { clock.seek(clock.snapshot.positionMillis + 5_000); true }
                else -> false
            }
        }.focusRequester(keyboardFocus).focusable().background(Brush.linearGradient(
            listOf(Color(0xFF152839), Color(0xFF272033)))).safeContentPadding().padding(16.dp)) {
            val metadata: @Composable (Boolean) -> Unit = { wide ->
                val largeCover = wide && maxHeight >= 540.dp
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (largeCover) Cover(Modifier.size(260.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        if (!largeCover) Cover(Modifier.size(64.dp))
                        Column(Modifier.weight(1f)) {
                            Text(scene.title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text("演示时钟 · 无音频", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton({ picker = true }) { Text("场景") }
                        TextButton(open, enabled = !requests.loading) { Text("导入 LRC") }
                        if (importedSource != null) TextButton({ load(importedSource!!) }, enabled = !requests.loading) { Text("重新载入") }
                        TextButton({ options = options.copy(showTranslation = !options.showTranslation) }) { Text(if (options.showTranslation) "译文 ✓" else "译文") }
                        TextButton({ options = options.copy(showPhonetic = !options.showPhonetic) }) { Text(if (options.showPhonetic) "注音 ✓" else "注音") }
                        TextButton({ settings = true }) { Text("设置") }
                    }
                    if (requests.loading) Text("正在读取歌词…", color = MaterialTheme.colorScheme.onSurface)
                    preferenceError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
            val lyrics: @Composable (Modifier) -> Unit = { modifier ->
                FoliaLyricsView(scene.lyrics, { clock.snapshot }, modifier, contentKey = scene.id, options = options,
                    typography = FoliaTypography(scale = fontScale), style = FoliaStyle.Classic(tuning),
                    onStatus = { status = it },
                    onLineClicked = { clock.seek(it.start.toLong() + options.offsetMillis) }, onLinePressed = { pressed = it })
            }
            if (maxWidth < 600.dp) Column {
                metadata(false); lyrics(Modifier.weight(1f).fillMaxWidth()); Transport(clock, visible)
            } else Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column(Modifier.fillMaxWidth(.38f).fillMaxHeight().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center) { metadata(true); Transport(clock, visible) }
                lyrics(Modifier.weight(1f).fillMaxHeight())
            }
        }
        if (picker) AlertDialog(onDismissRequest = { picker = false }, title = { Text("合成场景") }, text = {
            Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState())) {
                scenes.forEach { item -> TextButton({
                    importJob?.cancel(); requests.invalidate(); importedSource = null
                    scene = item; clock.replaceDuration(item.durationMillis); picker = false
                }) { Text(item.title) } }
            }
        }, confirmButton = { TextButton({ picker = false }) { Text("关闭") } })
        if (settings) AlertDialog(onDismissRequest = { settings = false }, title = { Text("显示设置") }, text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                Toggle("显示下一句", options.showNextLine) { options = options.copy(showNextLine = it) }
                Toggle("减少动态效果", options.reducedMotion) { options = options.copy(reducedMotion = it) }
                Toggle("文字旋转", tuning.enableWordRotation) { tuning = tuning.copy(enableWordRotation = it) }
                Toggle("保守间距", tuning.avoidOverlap) { tuning = tuning.copy(avoidOverlap = it) }
                Text("字号 ${fontScale}"); Slider(fontScale, { fontScale = it }, valueRange = .5f..2f)
                Text("词间距"); Slider(tuning.wordSpacing, { tuning = tuning.copy(wordSpacing = it) }, valueRange = 0f..2f)
                Text("呼吸幅度"); Slider(tuning.breathingFloatMultiplier, { tuning = tuning.copy(breathingFloatMultiplier = it) }, valueRange = 0f..2f)
                Text("歌词偏移 ${options.offsetMillis} ms"); Slider(options.offsetMillis.toFloat(), { options = options.copy(offsetMillis = it.toInt()) }, valueRange = -2_000f..2_000f)
                TextButton({ options = options.copy(quality = FoliaQuality.entries[(options.quality.ordinal + 1) % FoliaQuality.entries.size]) }) { Text("画质 ${options.quality}") }
                Toggle("开发面板", debug) { debug = it }
            }
        }, confirmButton = { TextButton({ settings = false }) { Text("完成") } })
        if (debug && !settings) AlertDialog(onDismissRequest = { debug = false }, title = { Text("检查点 · Kotlin") }, text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                DebugTime(clock)
                Text("场景 ${scene.id} · Kotlin")
                status?.let { value ->
                    Text("画质 ${value.requestedQuality} → ${value.actualQuality} · ${value.mainUnitCount} 单元")
                    value.diagnostics.forEach { Text(it) }
                }
                Row {
                    TextButton({ clock.pause(); clock.seek(clock.snapshot.positionMillis - 16) }) { Text("−16ms") }
                    TextButton({ clock.pause(); clock.seek(clock.snapshot.positionMillis + 16) }) { Text("+16ms") }
                }
                scene.checkpoints.forEach { point -> TextButton({ clock.pause(); clock.seek(point) }) { Text("$point ms") } }
            }
        }, confirmButton = { TextButton({ debug = false }) { Text("关闭") } })
        pressed?.let { line -> AlertDialog(onDismissRequest = { pressed = null }, title = { Text("来源行") },
            text = { Text("${line.start}–${line.end} ms") }, confirmButton = { TextButton({ pressed = null }) { Text("关闭") } }) }
        requests.error?.let { message -> AlertDialog(onDismissRequest = requests::dismissError,
            title = { Text("导入失败 · 已保留当前歌词") }, text = { Text(message) },
            confirmButton = { TextButton(requests::dismissError) { Text("关闭") } }) }
    }
}

@Composable
private fun Cover(modifier: Modifier) {
    Box(modifier.clip(RoundedCornerShape(20.dp)).background(Brush.linearGradient(
        listOf(Color(0xFF647C92), Color(0xFF6D547D)))), contentAlignment = Alignment.Center) {
        Text("F", style = MaterialTheme.typography.displayLarge, color = Color.White.copy(alpha = .7f))
    }
}

@Composable
private fun Transport(clock: PreviewPlaybackController, enabled: Boolean) {
    Column(Modifier.fillMaxWidth()) {
        Progress(clock, enabled)
        val playing by remember(clock) { derivedStateOf { clock.snapshot.isPlaying } }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            FilledTonalButton({ clock.toggle() }) { Text(if (playing) "暂停" else "播放") }
            TextButton({ clock.seek(0) }) { Text("重播") }
            TextButton({ val rates = listOf(.5, 1.0, 1.5, 2.0); clock.changeSpeed(rates[(rates.indexOf(clock.speed) + 1) % rates.size]) }) { Text("${clock.speed}×") }
            TextButton({ clock.looping = !clock.looping }) { Text(if (clock.looping) "循环 ✓" else "循环") }
        }
    }
}

@Composable
private fun Progress(clock: PreviewPlaybackController, enabled: Boolean) {
    var preview by remember(clock.durationMillis) { mutableStateOf<Float?>(null) }
    val interactions = remember { MutableInteractionSource() }
    var dragging by remember { mutableStateOf(false) }
    val inputEnabled by rememberUpdatedState(enabled)
    LaunchedEffect(interactions) {
        interactions.interactions.collect { event ->
            when (event) {
                is DragInteraction.Start -> dragging = true
                is DragInteraction.Stop -> {
                    if (inputEnabled) preview?.let { clock.seek(it.toLong()) }
                    preview = null; dragging = false
                }
                is DragInteraction.Cancel, is PressInteraction.Cancel -> { preview = null; dragging = false }
            }
        }
    }
    LaunchedEffect(enabled) { if (!enabled) preview = null }
    Slider(preview ?: clock.snapshot.positionMillis.toFloat(), { preview = it },
        modifier = Modifier.semantics { contentDescription = "演示进度" }, interactionSource = interactions,
        enabled = enabled,
        valueRange = 0f..clock.durationMillis.toFloat(),
        onValueChangeFinished = {
            // Drag Stop commits; Cancel discards. This callback also handles taps/keyboard.
            if (!dragging) { if (enabled) preview?.let { clock.seek(it.toLong()) }; preview = null }
        })
    val seconds by remember(clock) { derivedStateOf { clock.snapshot.positionMillis / 1000 } }
    Text("${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')} / ${clock.durationMillis / 1000}s",
        style = MaterialTheme.typography.bodySmall.copy(textDirection = TextDirection.Ltr), color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun Toggle(label: String, checked: Boolean, change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f)); Switch(checked, change)
    }
}

@Composable
private fun DebugTime(clock: PreviewPlaybackController) {
    Text("${clock.snapshot.positionMillis} ms · discontinuity ${clock.snapshot.discontinuity}")
}
