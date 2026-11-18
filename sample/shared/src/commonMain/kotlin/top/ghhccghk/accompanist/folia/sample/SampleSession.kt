package top.ghhccghk.accompanist.folia.sample

import androidx.compose.runtime.*
import top.ghhccghk.accompanist.folia.sample.fixtures.scenes
import top.ghhccghk.accompanist.folia.sample.importing.LyricsSource
import top.ghhccghk.accompanist.folia.sample.playback.PreviewPlaybackController
import top.ghhccghk.accompanist.lyrics.folia.*

/** Retained by the platform owner; jobs and file picker still belong to the composition. */
class SampleSession(demoInitially: Boolean = false) {
    val maskBackend = SampleMaskBackend()
    val colourBackend = SampleColourBackend()
    var demo by mutableStateOf(demoInitially)
    val songs = top.ghhccghk.accompanist.folia.sample.playback.SongSession()
    var scene by mutableStateOf(scenes.first())
    val clock = PreviewPlaybackController(scene.durationMillis)
    var options by mutableStateOf(FoliaOptions())
    var tuning by mutableStateOf(ClassicTuning())
    var lyricTheme by mutableStateOf(SampleLyricTheme.Classic)
    var partitaTuning by mutableStateOf(PartitaTuning())
    var tiltTuning by mutableStateOf(TiltTuning())
    var cadenzaTuning by mutableStateOf(CadenzaTuning())
    var pendoloTuning by mutableStateOf(PendoloTuning())
    var cappellaTuning by mutableStateOf(CappellaTuning())
    var fumeTuning by mutableStateOf(FumeTuning())
    var dioramaTuning by mutableStateOf(DioramaTuning())
    val lyricStyle: FoliaStyle get() = when(lyricTheme) {
        SampleLyricTheme.Classic -> FoliaStyle.Classic(tuning)
        SampleLyricTheme.Partita -> FoliaStyle.Partita(partitaTuning)
        SampleLyricTheme.Tilt -> FoliaStyle.Tilt(tiltTuning)
        SampleLyricTheme.Cadenza -> FoliaStyle.Cadenza(cadenzaTuning)
        SampleLyricTheme.Pendolo -> FoliaStyle.Pendolo(pendoloTuning)
        SampleLyricTheme.Cappella -> FoliaStyle.Cappella(cappellaTuning)
        SampleLyricTheme.Fume -> FoliaStyle.Fume(fumeTuning)
        SampleLyricTheme.Diorama -> FoliaStyle.Diorama(dioramaTuning)
    }
    fun lyricStyleForTitle(title:String):FoliaStyle = when(val style=lyricStyle){
        is FoliaStyle.Cappella->style.copy(tuning=style.tuning.copy(title=title))
        else->style
    }
    var fontScale by mutableFloatStateOf(1f)
    var renderResolution by mutableStateOf(RenderResolution.Full)
    var importedSource by mutableStateOf<LyricsSource?>(null)
    var preferencesLoaded = false
}

/** Names are presentation only; enum identifiers remain stable preference keys. */
enum class SampleLyricTheme(val displayName: String) {
    Classic("流光"), Partita("云阶"), Tilt("倾诉"), Cadenza("心象"),
    Pendolo("时计"), Cappella("群唱"), Fume("浮名"), Diorama("镜台")
}
