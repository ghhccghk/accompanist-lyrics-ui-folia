package top.ghhccghk.accompanist.folia.sample

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalWindowInfo

fun main() = application {
    val session = remember { SampleSession() }
    val state = rememberWindowState(width = 1100.dp, height = 740.dp)
    Window(onCloseRequest = { session.clock.pause(); session.songs.close(); exitApplication() }, title = "Folia · 本地歌曲",
        state = state) {
        val debugHost=remember(window){DesktopRenderDebug(window)}
        DisposableEffect(debugHost){onDispose{debugHost.setEnabled(false)}}
        LaunchedEffect(window) {
            while (!window.isShowing) withFrameNanos { }
            withFrameNanos { }
            println("FOLIA_RENDER_API requested=${debugHost.requestedBackend} actual=${window.renderApi}")
        }
        CompositionLocalProvider(LocalSampleRenderDebugHost provides debugHost) {
            FoliaSampleApp(visible = !state.isMinimized && LocalWindowInfo.current.isWindowFocused, session = session)
        }
    }
}
