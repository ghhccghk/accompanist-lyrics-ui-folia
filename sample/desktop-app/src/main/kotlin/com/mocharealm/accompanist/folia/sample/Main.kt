package com.mocharealm.accompanist.folia.sample

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalWindowInfo

fun main() = application {
    val session = remember { SampleSession() }
    val state = rememberWindowState(width = 1100.dp, height = 740.dp)
    Window(onCloseRequest = { session.clock.pause(); exitApplication() }, title = "Folia · 演示时钟 · 无音频",
        state = state) {
        FoliaSampleApp(visible = !state.isMinimized && LocalWindowInfo.current.isWindowFocused, session = session)
    }
}
