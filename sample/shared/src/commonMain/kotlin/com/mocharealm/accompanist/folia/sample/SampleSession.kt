package com.mocharealm.accompanist.folia.sample

import androidx.compose.runtime.*
import com.mocharealm.accompanist.folia.sample.fixtures.scenes
import com.mocharealm.accompanist.folia.sample.importing.LyricsSource
import com.mocharealm.accompanist.folia.sample.playback.PreviewPlaybackController
import com.mocharealm.accompanist.lyrics.folia.*

/** Retained by the platform owner; jobs and file picker still belong to the composition. */
class SampleSession {
    var scene by mutableStateOf(scenes.first())
    val clock = PreviewPlaybackController(scene.durationMillis)
    var options by mutableStateOf(FoliaOptions())
    var tuning by mutableStateOf(ClassicTuning())
    var fontScale by mutableFloatStateOf(1f)
    var importedSource by mutableStateOf<LyricsSource?>(null)
}
