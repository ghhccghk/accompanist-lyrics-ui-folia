package com.mocharealm.accompanist.folia.sample

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Folia · 演示时钟 · 无音频",
        state = rememberWindowState(width = 1100.dp, height = 740.dp)) { FoliaSampleApp() }
}
