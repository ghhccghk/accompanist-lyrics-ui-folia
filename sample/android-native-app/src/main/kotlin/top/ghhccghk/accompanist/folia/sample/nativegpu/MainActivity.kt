package top.ghhccghk.accompanist.folia.sample.nativegpu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import top.ghhccghk.accompanist.folia.sample.*
import top.ghhccghk.accompanist.lyrics.folia.nativeandroid.*

class NativeSampleModel:ViewModel(){
    val session=SampleSession()
    override fun onCleared(){session.clock.pause();session.songs.close()}
}
private object NativeSampleRenderer:SampleLyricsRenderer {
    override val supportsBackendSelection=true
    @Composable override fun Render(content:SampleLyricsContent){with(content){
        FoliaNativeLyricsView(lyrics,playback,modifier,contentKey,typography,colors,options,style,
            backend=FoliaRendererBackend.valueOf(backend.name),active=active,diagnosticsIntervalFrames=60,
            onRendererStatus={s->onRendererStatus(SampleRendererReport(SampleRendererBackend.valueOf(s.requested.name),s.actual.name,s.state,s.reason,s.frameId,s.residentTextureBytes,s.uploadedBytes))},
            onStatus=onStatus,onLineClicked=onLineClicked,onLinePressed=onLinePressed)
    }}
}
class MainActivity:ComponentActivity(){
    private var hostVisible by mutableStateOf(false)
    private val model by lazy{ViewModelProvider(this)[NativeSampleModel::class.java]}
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        setContent{CompositionLocalProvider(LocalSampleLyricsRenderer provides NativeSampleRenderer){FoliaSampleApp(hostVisible,model.session)}}
    }
    override fun onStart(){super.onStart();hostVisible=true}
    override fun onStop(){hostVisible=false;model.session.clock.pause();model.session.songs.current?.player?.pause();super.onStop()}
}
