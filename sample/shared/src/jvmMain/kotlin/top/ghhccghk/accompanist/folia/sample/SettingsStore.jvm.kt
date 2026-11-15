package top.ghhccghk.accompanist.folia.sample

import androidx.compose.runtime.*
import kotlinx.coroutines.*
import java.util.prefs.Preferences

@Composable
actual fun rememberSettingsStore(): SettingsStore = remember {
    object : SettingsStore {
        private fun preferences() = Preferences.userRoot().node("com/mocharealm/accompanist/LyricsUiFoliaSample")
        override suspend fun read(): String? = withContext(Dispatchers.IO) { preferences().get("display-v1", null) }
        override suspend fun write(value: String) = withContext(Dispatchers.IO) {
            preferences().put("display-v1", value)
            preferences().flush()
        }
    }
}
