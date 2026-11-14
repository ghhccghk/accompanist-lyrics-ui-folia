package com.mocharealm.accompanist.folia.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Standalone application shell; the lyrics renderer is added in the next milestone. */
@Composable
fun FoliaSampleApp() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        BoxWithConstraints(Modifier.fillMaxSize().background(Brush.linearGradient(
            listOf(Color(0xFF182A38), Color(0xFF292337)))).safeContentPadding().padding(24.dp)) {
            val metadata: @Composable () -> Unit = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("FOLIA", style = MaterialTheme.typography.headlineLarge)
                    Text("独立歌词演示", style = MaterialTheme.typography.titleLarge)
                    Text("演示时钟 · 无音频")
                }
            }
            val stage: @Composable (Modifier) -> Unit = { modifier ->
                Box(modifier, contentAlignment = Alignment.Center) {
                    Text("歌词舞台 · 工程骨架", style = MaterialTheme.typography.headlineSmall)
                }
            }
            if (maxWidth < 600.dp) Column { metadata(); stage(Modifier.weight(1f).fillMaxWidth()) }
            else Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.fillMaxWidth(.4f)) { metadata() }
                stage(Modifier.weight(1f).fillMaxHeight())
            }
        }
    }
}
