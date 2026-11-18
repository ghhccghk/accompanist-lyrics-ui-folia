package top.ghhccghk.accompanist.folia.sample

import androidx.compose.runtime.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Modifier
import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import top.ghhccghk.accompanist.lyrics.folia.*

enum class SampleRendererBackend { Kotlin, NativePreferred, NativeRequired }
data class SampleRendererReport(val requested:SampleRendererBackend=SampleRendererBackend.Kotlin,
    val actual:String="Kotlin",val state:String="Idle",val reason:String?=null,
    val frameId:Long=0,val textureBytes:Long=0,val uploadedBytes:Long=0)

data class SampleLyricsContent(val lyrics:SyncedLyrics,val playback:()->FoliaPlaybackSnapshot,val modifier:Modifier,
    val contentKey:Any,val typography:FoliaTypography,val colors:FoliaColors,val options:FoliaOptions,val style:FoliaStyle,
    val backend:SampleRendererBackend,val active:Boolean,val onRendererStatus:(SampleRendererReport)->Unit,
    val onStatus:(FoliaStatus)->Unit,val onLineClicked:(ISyncedLine)->Unit,val onLinePressed:(ISyncedLine)->Unit)

/** Only the lyrics region is injectable. Shared song selection, media service,
 * file import, transport, settings and navigation keep their existing owners. */
interface SampleLyricsRenderer {
    val supportsBackendSelection:Boolean
    @Composable fun Render(content:SampleLyricsContent)
}
private object KotlinSampleLyricsRenderer:SampleLyricsRenderer {
    override val supportsBackendSelection=false
    @Composable override fun Render(content:SampleLyricsContent) {
        val callback by rememberUpdatedState(content.onRendererStatus)
        val required=content.backend==SampleRendererBackend.NativeRequired
        LaunchedEffect(content.backend){callback(SampleRendererReport(content.backend,
            if(required)"Failed" else "Kotlin",if(required)"Failed" else "Ready",
            if(content.backend!=SampleRendererBackend.Kotlin)"NativeArtifactNotInstalled" else null))}
        if(required)BasicText("NativeRequired: NativeArtifactNotInstalled",content.modifier)
        else with(content){FoliaLyricsView(lyrics,playback,modifier,contentKey,typography,colors,options,style,onStatus,onLineClicked,onLinePressed)}
    }
}
val LocalSampleLyricsRenderer=staticCompositionLocalOf<SampleLyricsRenderer>{KotlinSampleLyricsRenderer}

@Composable
internal fun SampleLyricsRegion(session:SampleSession,active:Boolean,lyrics:SyncedLyrics,playback:()->FoliaPlaybackSnapshot,
    modifier:Modifier=Modifier,contentKey:Any=lyrics.id,typography:FoliaTypography=FoliaTypography(),colors:FoliaColors=FoliaColors(),
    options:FoliaOptions=FoliaOptions(),style:FoliaStyle=FoliaStyle.Classic(),onStatus:(FoliaStatus)->Unit={},
    onLineClicked:(ISyncedLine)->Unit={},onLinePressed:(ISyncedLine)->Unit={}) {
    LocalSampleLyricsRenderer.current.Render(SampleLyricsContent(lyrics,playback,modifier,contentKey,typography,colors,options,style,
        session.rendererBackend,active,{session.rendererReport=it},onStatus,onLineClicked,onLinePressed))
}
