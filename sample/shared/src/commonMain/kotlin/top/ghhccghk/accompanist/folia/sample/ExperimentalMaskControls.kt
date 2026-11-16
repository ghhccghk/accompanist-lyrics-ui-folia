package top.ghhccghk.accompanist.folia.sample

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import top.ghhccghk.accompanist.lyrics.folia.*
import kotlinx.coroutines.delay

internal data class NativeMaskStats(val calls: Long = 0, val millis: Double = 0.0, val error: String? = null)
internal interface SampleNativeMaskProcessor : ExperimentalFoliaMaskProcessor {
    fun stats(): NativeMaskStats
    fun release()
}
internal data class NativeMaskLoad(val processor: SampleNativeMaskProcessor? = null, val error: String? = null)
internal expect fun loadSampleNativeMask(): NativeMaskLoad

/** Session-only experiment: a new launch always starts with Kotlin. */
class SampleMaskBackend {
    internal var processor by mutableStateOf<SampleNativeMaskProcessor?>(null)
        private set
    var requested by mutableStateOf(false)
        private set
    internal var loadError by mutableStateOf<String?>(null)
        private set

    fun selectRust(enabled: Boolean) {
        processor?.release()
        processor = null
        requested = enabled
        loadError = null
        if (enabled) {
            val result = loadSampleNativeMask()
            processor = result.processor
            loadError = result.error
        }
    }
}

@Composable
internal fun SampleMaskScope(session: SampleSession, content: @Composable () -> Unit) {
    val processor = session.maskBackend.processor
    DisposableEffect(processor) { onDispose { processor?.release() } }
    CompositionLocalProvider(LocalFoliaMaskProcessor provides processor, content = content)
}

@Composable
internal fun ExperimentalMaskControls(session: SampleSession) {
    val backend = session.maskBackend
    var stats by remember(backend.processor) { mutableStateOf(backend.processor?.stats() ?: NativeMaskStats()) }
    LaunchedEffect(backend.processor) {
        while (true) {
            stats = backend.processor?.stats() ?: NativeMaskStats()
            delay(500)
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("实验 Rust 辉光遮罩")
        Switch(checked = backend.requested, onCheckedChange = backend::selectRust,
            modifier = Modifier.semantics { contentDescription = "实验 Rust 辉光遮罩" })
    }
    val error = backend.loadError ?: stats.error
    Text(when {
        !backend.requested -> "实际：Kotlin"
        error != null -> "实际：Kotlin（Rust 不可用／已回退）"
        stats.calls == 0L -> "Rust 已加载，尚未计算遮罩"
        else -> "实际：Rust CPU 遮罩 · ${stats.calls} 次"
    })
    error?.let { SelectionContainer { Text(it, color = MaterialTheme.colorScheme.error) } }
    if (stats.calls > 0) Text("JNI 遮罩累计 ${stats.millis.toLong()} ms（含复制；不是帧耗时）")
    Text("默认 Kotlin。仅切换流光／云阶辉光准备；文字排版、动画及绘制仍为 Kotlin/Compose。" +
        "切换会重建遮罩，保持播放位置。最简画质可能没有遮罩调用。", style = MaterialTheme.typography.bodySmall)
}
