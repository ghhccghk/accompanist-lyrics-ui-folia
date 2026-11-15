package top.ghhccghk.accompanist.folia.sample.importing

import androidx.compose.runtime.*
import kotlinx.coroutines.*
import java.awt.EventQueue
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
actual fun rememberLyricsPicker(onSelected: (LyricsSource) -> Unit, onError: (String) -> Unit): () -> Unit {
    val scope = rememberCoroutineScope()
    val selected by rememberUpdatedState(onSelected)
    val failed by rememberUpdatedState(onError)
    return {
        scope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    var choice: File? = null
                    EventQueue.invokeAndWait {
                        val dialog = FileDialog(null as Frame?, "打开 UTF-8 LRC", FileDialog.LOAD)
                        try {
                            dialog.isVisible = true
                            dialog.file?.let { choice = File(dialog.directory, it) }
                        } finally { dialog.dispose() }
                    }
                    choice
                }
                file?.let { selected(object : LyricsSource {
                    override val name = it.name
                    override suspend fun read(): ByteArray = withContext(Dispatchers.IO) {
                        it.inputStream().use { stream ->
                            val bytes = stream.readNBytes(MAX_IMPORT_BYTES + 1)
                            require(bytes.size <= MAX_IMPORT_BYTES) { "文件超过 1 MiB 限制" }
                            bytes
                        }
                    }
                }) }
            } catch (error: java.lang.reflect.InvocationTargetException) { failed("文档选择器失败：${error.cause?.message}") }
        }
    }
}
