package top.ghhccghk.accompanist.folia.sample

import androidx.compose.runtime.*
import top.ghhccghk.accompanist.folia.sample.fixtures.scenes
import top.ghhccghk.accompanist.folia.sample.importing.LyricsSource
import top.ghhccghk.accompanist.folia.sample.playback.PreviewPlaybackController
import top.ghhccghk.accompanist.lyrics.folia.*

/** Retained by the platform owner; jobs and file picker still belong to the composition. */
class SampleSession(demoInitially: Boolean = false) {
    val maskBackend = SampleMaskBackend()
    var demo by mutableStateOf(demoInitially)
    val songs = top.ghhccghk.accompanist.folia.sample.playback.SongSession()
    var scene by mutableStateOf(scenes.first())
    val clock = PreviewPlaybackController(scene.durationMillis)
    var options by mutableStateOf(FoliaOptions())
    var tuning by mutableStateOf(ClassicTuning())
    var lyricTheme by mutableStateOf(SampleLyricTheme.Classic)
    var partitaTuning by mutableStateOf(PartitaTuning())
    val lyricStyle: FoliaStyle get() = when(lyricTheme) {
        SampleLyricTheme.Classic -> FoliaStyle.Classic(tuning)
        SampleLyricTheme.Partita -> FoliaStyle.Partita(partitaTuning)
    }
    var fontScale by mutableFloatStateOf(1f)
    var renderResolution by mutableStateOf(RenderResolution.Full)
    var importedSource by mutableStateOf<LyricsSource?>(null)
    var preferencesLoaded = false
}

enum class SampleLyricTheme { Classic, Partita }
