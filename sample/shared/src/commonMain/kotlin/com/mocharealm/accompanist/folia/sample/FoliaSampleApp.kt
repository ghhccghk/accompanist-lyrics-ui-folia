package com.mocharealm.accompanist.folia.sample

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import com.mocharealm.accompanist.folia.sample.fixtures.scenes
import com.mocharealm.accompanist.folia.sample.playback.PreviewPlaybackController
import com.mocharealm.accompanist.folia.sample.importing.*
import kotlinx.coroutines.*
import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.folia.*

@Composable
fun FoliaSampleApp(visible: Boolean = true) {
    var scene by remember { mutableStateOf(scenes.first()) }
    val clock = remember { PreviewPlaybackController(scene.durationMillis) }
    val playing by remember { derivedStateOf { clock.snapshot.isPlaying } }
    var options by remember { mutableStateOf(FoliaOptions()) }
    var tuning by remember { mutableStateOf(ClassicTuning()) }
    var fontScale by remember { mutableFloatStateOf(1f) }
    var settings by remember { mutableStateOf(false) }
    var picker by remember { mutableStateOf(false) }
    var debug by remember { mutableStateOf(false) }
    var pressed by remember { mutableStateOf<ISyncedLine?>(null) }
    val requests = remember { ImportRequests() }
    val scope = rememberCoroutineScope()
    var importJob by remember { mutableStateOf<Job?>(null) }
    var importedSource by remember { mutableStateOf<LyricsSource?>(null) }
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
    LaunchedEffect(playing, visible) {
        if (!visible) clock.pause()
        else if (playing) while (clock.snapshot.isPlaying) withFrameNanos { clock.refresh() }
    }
    DisposableEffect(clock) { onDispose { clock.pause() } }
    MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFFB6CFFF))) {
        BoxWithConstraints(Modifier.fillMaxSize().background(Brush.linearGradient(
            listOf(Color(0xFF152839), Color(0xFF272033)))).safeContentPadding().padding(16.dp)) {
            val metadata: @Composable (Boolean) -> Unit = { wide ->
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (wide) Cover(Modifier.size(260.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        if (!wide) Cover(Modifier.size(64.dp))
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
                }
            }
            val lyrics: @Composable (Modifier) -> Unit = { modifier ->
                FoliaLyricsView(scene.lyrics, { clock.snapshot }, modifier, contentKey = scene.id, options = options,
                    typography = FoliaTypography(scale = fontScale), style = FoliaStyle.Classic(tuning),
                    onLineClicked = { clock.seek(it.start.toLong() + options.offsetMillis) }, onLinePressed = { pressed = it })
            }
            if (maxWidth < 600.dp) Column {
                metadata(false); lyrics(Modifier.weight(1f).fillMaxWidth()); Transport(clock)
            } else Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column(Modifier.fillMaxWidth(.38f).fillMaxHeight().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center) { metadata(true); Transport(clock) }
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
                TextButton({ options = options.copy(quality = FoliaQuality.entries[(options.quality.ordinal + 1) % FoliaQuality.entries.size]) }) { Text("画质 ${options.quality}") }
                Toggle("开发面板", debug) { debug = it }
            }
        }, confirmButton = { TextButton({ settings = false }) { Text("完成") } })
        if (debug && !settings) AlertDialog(onDismissRequest = { debug = false }, title = { Text("检查点 · Kotlin") }, text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                DebugTime(clock)
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
private fun Transport(clock: PreviewPlaybackController) {
    Column(Modifier.fillMaxWidth()) {
        Progress(clock)
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
private fun Progress(clock: PreviewPlaybackController) {
    var preview by remember(clock.durationMillis) { mutableStateOf<Float?>(null) }
    Slider(preview ?: clock.snapshot.positionMillis.toFloat(), { preview = it },
        valueRange = 0f..clock.durationMillis.toFloat(),
        onValueChangeFinished = { preview?.let { clock.seek(it.toLong()) }; preview = null })
    val seconds by remember(clock) { derivedStateOf { clock.snapshot.positionMillis / 1000 } }
    Text("${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')} / ${clock.durationMillis / 1000}s",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
