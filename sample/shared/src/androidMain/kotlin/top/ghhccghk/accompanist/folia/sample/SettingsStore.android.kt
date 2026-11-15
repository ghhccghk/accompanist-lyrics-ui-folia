package top.ghhccghk.accompanist.folia.sample

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.*
import java.io.IOException

@Composable
actual fun rememberSettingsStore(): SettingsStore {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        val preferences = context.getSharedPreferences("LyricsUiFoliaSample", Context.MODE_PRIVATE)
        object : SettingsStore {
            override suspend fun read(): String? = withContext(Dispatchers.IO) { preferences.getString("display-v1", null) }
            override suspend fun write(value: String) = withContext(Dispatchers.IO) {
                if (!preferences.edit().putString("display-v1", value).commit()) throw IOException("显示设置写入失败")
            }
        }
    }
}
