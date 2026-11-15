package org.example.foliaconsumer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import top.ghhccghk.accompanist.lyrics.folia.*

val originalLine = SyncedLine("Independent consumer", "Original synthetic text", 1_000, 5_000)

@Composable
fun Consumer(modifier: Modifier = Modifier, onClick: (Any) -> Unit = {}) {
    FoliaLyricsView(SyncedLyrics(listOf(originalLine)), { FoliaPlaybackSnapshot(2_000, false) },
        modifier = modifier, options = FoliaOptions(quality = FoliaQuality.Minimal), onLineClicked = onClick)
}
