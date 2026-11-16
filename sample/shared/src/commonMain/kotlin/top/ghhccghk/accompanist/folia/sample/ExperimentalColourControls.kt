package top.ghhccghk.accompanist.folia.sample

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import top.ghhccghk.accompanist.lyrics.folia.ExperimentalFoliaColourProcessor
import kotlinx.coroutines.delay

internal interface SampleNativeColourProcessor : ExperimentalFoliaColourProcessor {
    fun stats(): NativeMaskStats
    fun release()
}
internal data class NativeColourLoad(val processor: SampleNativeColourProcessor? = null, val error: String? = null)
internal expect fun loadSampleNativeColour(): NativeColourLoad

/** Independent from the alpha-mask experiment; never persisted as default. */
class SampleColourBackend {
    internal var processor by mutableStateOf<SampleNativeColourProcessor?>(null); private set
    var requested by mutableStateOf(false); private set
    internal var loadError by mutableStateOf<String?>(null); private set
    fun selectRust(enabled: Boolean) {
        processor?.release(); processor = null
        requested = enabled; loadError = null
        if (enabled) {
            val loaded = loadSampleNativeColour()
            processor = loaded.processor; loadError = loaded.error
        }
    }
}

@Composable
internal fun ExperimentalColourControls(session: SampleSession) {
    val backend = session.colourBackend
    var stats by remember(backend.processor) { mutableStateOf(backend.processor?.stats() ?: NativeMaskStats()) }
    LaunchedEffect(backend.processor) {
        while (true) { stats = backend.processor?.stats() ?: NativeMaskStats(); delay(500) }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("实验 Rust 颜色卷积")
        Switch(checked = backend.requested,onCheckedChange = backend::selectRust,
            modifier = Modifier.semantics { contentDescription = "实验 Rust 颜色卷积" })
    }
    val error = backend.loadError ?: stats.error
    Text(when {
        !backend.requested -> "颜色卷积：Kotlin"
        error != null -> "颜色卷积：Kotlin（Rust 不可用／已回退）"
        stats.calls == 0L -> "颜色卷积：Rust 已加载，尚无调用"
        else -> "颜色卷积：Rust CPU · ${stats.calls} 次"
    })
    error?.let { SelectionContainer { Text(it,color = MaterialTheme.colorScheme.error) } }
    if (stats.calls > 0) Text("JNI 颜色卷积累计 ${stats.millis.toLong()} ms（含复制；不是帧耗时）")
    Text("独立于辉光遮罩。仅替换小半径软件模糊数学计算，原生图层和较大半径仍走原路径。" +
        "默认关闭；32 MiB 独立工作区上限，可能增加内存。切换保持播放位置。",
        style = MaterialTheme.typography.bodySmall)
}
