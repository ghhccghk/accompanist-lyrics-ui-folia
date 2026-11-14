package com.mocharealm.accompanist.folia.sample.importing

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

@Composable
actual fun rememberLyricsPicker(onSelected: (LyricsSource) -> Unit, onError: (String) -> Unit): () -> Unit {
    val resolver = LocalContext.current.applicationContext.contentResolver
    val selected by rememberUpdatedState(onSelected)
    val failed by rememberUpdatedState(onError)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) selected(object : LyricsSource {
            override val name = "导入的 LRC"
            override suspend fun read(): ByteArray = withContext(Dispatchers.IO) {
                val stream = resolver.openInputStream(uri) ?: throw IOException("无法打开所选文档")
                stream.use {
                    val output = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    while (true) {
                        val count = it.read(buffer)
                        if (count < 0) break
                        require(output.size() + count <= MAX_IMPORT_BYTES) { "文件超过 1 MiB 限制" }
                        output.write(buffer, 0, count)
                    }
                    output.toByteArray()
                }
            }
        })
    }
    return {
        try { launcher.launch(arrayOf("*/*")) }
        catch (error: android.content.ActivityNotFoundException) { failed("未安装文档选择器：${error.message}") }
    }
}
