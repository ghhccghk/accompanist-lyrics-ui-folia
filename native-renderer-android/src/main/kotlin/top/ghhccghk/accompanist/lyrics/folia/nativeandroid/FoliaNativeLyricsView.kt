@file:OptIn(top.ghhccghk.accompanist.lyrics.folia.ExperimentalFoliaRendererBridge::class)
package top.ghhccghk.accompanist.lyrics.folia.nativeandroid

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import top.ghhccghk.accompanist.lyrics.folia.*
import java.util.IdentityHashMap

enum class FoliaRendererBackend { Kotlin, NativePreferred, NativeRequired }
enum class FoliaRendererPath { Kotlin, Preparing, NativeGpu, Failed }

/** Submit counters are not physical presentation FPS or GPU elapsed time.
 * Texture bytes exclude driver allocations; use system tools for total memory. */
data class FoliaNativeRendererStatus(
    val requested:FoliaRendererBackend,val actual:FoliaRendererPath,val state:String,
    val reason:String?=null,val frameId:Long=0,val textureCount:Long=0,
    val residentTextureBytes:Long=0,val peakTextureBytes:Long=0,val uploadedBytes:Long=0,
    val backend:String="Vulkan",val sceneVersion:Int=FOLIA_RENDERER_SCENE_VERSION,
    val gpuElapsedNanos:Long?=null,val physicalPresentNanos:Long?=null,
    val sampledMediaTimeUs:Long?=null,
)

/** Limits extra owned CPU coverage copies per prepared line scene. Concurrent
 * lines, transition copies, GPU/driver and original Compose images are separate. */
data class FoliaNativeRendererConfig(val preparedTextureBudgetBytes:Long=64L*1024*1024) {
    init { require(preparedTextureBudgetBytes in 1..192L*1024*1024) }
}

/** Optional Android entry point. Host snapshots are evaluated on UI exactly as
 * in FoliaLyricsView. Workers receive frozen packets and own no media clock.
 * Subtitles, secondary lines and finite waiting UI reuse the base composables. */
@Composable
fun FoliaNativeLyricsView(
    lyrics:SyncedLyrics,playback:()->FoliaPlaybackSnapshot,modifier:Modifier=Modifier,
    contentKey:Any=lyrics.id,typography:FoliaTypography=FoliaTypography(),colors:FoliaColors=FoliaColors(),
    options:FoliaOptions=FoliaOptions(),style:FoliaStyle=FoliaStyle.Partita(),
    backend:FoliaRendererBackend=FoliaRendererBackend.Kotlin,active:Boolean=true,
    rendererConfig:FoliaNativeRendererConfig=FoliaNativeRendererConfig(),
    diagnosticsIntervalFrames:Int=0,
    onRendererStatus:(FoliaNativeRendererStatus)->Unit={},onStatus:(FoliaStatus)->Unit={},
    onLineClicked:(ISyncedLine)->Unit={},onLinePressed:(ISyncedLine)->Unit={},
) {
    val observer by rememberUpdatedState(onRendererStatus)
    val diorama = style is FoliaStyle.Diorama
    val supported=style is FoliaStyle.Partita || diorama
    val theme = style.rendererThemeName()
    val target=remember(backend,contentKey,theme,rendererConfig){NativeSceneTarget(rendererConfig)}
    var failure by remember(target){mutableStateOf<String?>(null)}
    var loaded by remember(target){mutableStateOf(false)}
    var ready by remember(target){mutableStateOf(false)}
    val kotlin=backend==FoliaRendererBackend.Kotlin
    SideEffect{target.onFailure={failure=it};target.overlaysVisible=ready||backend==FoliaRendererBackend.NativeRequired}
    LaunchedEffect(target,supported){
        if(kotlin)observer(FoliaNativeRendererStatus(backend,FoliaRendererPath.Kotlin,"Kotlin"))
        else if(!supported)failure="UnsupportedTheme:${style.rendererThemeName()}"
        else {
            observer(FoliaNativeRendererStatus(backend,FoliaRendererPath.Preparing,"Preparing"))
            FoliaNativeRuntime.checkAvailability().fold({loaded=true},{failure="RuntimeUnavailable:${it.message}"})
        }
    }
    LaunchedEffect(failure){failure?.let{observer(FoliaNativeRendererStatus(backend,
        if(backend==FoliaRendererBackend.NativePreferred)FoliaRendererPath.Kotlin else FoliaRendererPath.Failed,
        if(backend==FoliaRendererBackend.NativePreferred)"Fallback" else "Failed",it))}}
    val base:@Composable (Modifier)->Unit={m->FoliaLyricsView(lyrics,playback,m,contentKey,typography,colors,options,style,onStatus,onLineClicked,onLinePressed)}
    if(kotlin||(failure!=null&&backend==FoliaRendererBackend.NativePreferred)) {
        base(modifier);return
    }
    if(failure!=null){Box(modifier){BasicText("NativeRequired failed: $failure",Modifier.padding(12.dp),typography.secondary.copy(color=colors.secondary))};return}
    Box(modifier){
        if(loaded) {
            key(target) {
            AndroidView(factory={context->FoliaNativeSurfaceView(context, waitForScene = true).also{view->
                target.attach(view)
                view.report={state->if(state.startsWith("Failed:")){failure=state}}
                view.onPacketSubmitted={packet,stats->
                    if(packet.frames.isNotEmpty()) {
                        val first=!ready;ready=true
                        // Avoid a recomposition or user callback per frame unless
                        // explicitly sampled by a diagnostics consumer.
                        if(first||(diagnosticsIntervalFrames>0&&packet.id%diagnosticsIntervalFrames==0L))observer(FoliaNativeRendererStatus(backend,FoliaRendererPath.NativeGpu,if (diorama) "Ready:DioramaText" else "Ready:PartitaMainAndGlow",frameId=packet.id,
                            textureCount=stats?.getOrNull(1)?:0,residentTextureBytes=stats?.getOrNull(2)?:0,
                            peakTextureBytes=stats?.getOrNull(3)?:0,uploadedBytes=stats?.getOrNull(4)?:0,
                            sampledMediaTimeUs=packet.frames.lastOrNull()?.frame?.mediaTimeUs))
                    }
                    packet.frames.forEach{it.acknowledge()}
                }
            }},modifier=Modifier.fillMaxSize(),onRelease={target.detach(it);it.report={};it.onPacketSubmitted={_,_->};it.release()},
                update={it.setRenderingActive(active)})
            }
            CompositionLocalProvider(
                LocalFoliaPartitaRenderTarget provides target.takeUnless { diorama },
                LocalFoliaDioramaRenderTarget provides target.takeIf { diorama },
                LocalFoliaMaskProcessor provides null,LocalFoliaColourProcessor provides null){base(Modifier.fillMaxSize())}
        }
        // Preferred keeps the current Kotlin image until an actual nonempty
        // native scene has submitted. Removing it never recreates/seeks a player.
        if(!ready&&backend==FoliaRendererBackend.NativePreferred&&(!diorama||!loaded))base(Modifier.fillMaxSize())
    }
}

private fun FoliaStyle.rendererThemeName(): String = when (this) {
    is FoliaStyle.Partita -> "Partita"
    is FoliaStyle.Classic -> "Classic"
    is FoliaStyle.Diorama -> "Diorama"
    is FoliaStyle.Fume -> "Fume"
    is FoliaStyle.Cappella -> "Cappella"
    is FoliaStyle.Pendolo -> "Pendolo"
    is FoliaStyle.Tilt -> "Tilt"
    is FoliaStyle.Cadenza -> "Cadenza"
}

@OptIn(ExperimentalFoliaRendererBridge::class)
private class NativeSceneTarget(private val config:FoliaNativeRendererConfig):FoliaPartitaRenderTarget, FoliaDioramaRenderTarget {
    override var overlaysVisible by mutableStateOf(false)
    var onFailure:(String)->Unit={}
    private val ui=android.os.Handler(android.os.Looper.getMainLooper())
    private fun fail(error:Throwable){ui.post{onFailure("SceneRejected:${error.message}")}}
    private class Entry(val upload:SceneUpload){var frame:FoliaRendererFrame?=null;var notify:(FoliaRendererFrame)->Unit={}}
    private val entries=IdentityHashMap<FoliaRendererScene,Entry>()
    private val ordered=mutableListOf<Entry>()
    private var view:FoliaNativeSurfaceView?=null
    private var nextScene=0L
    private var nextFrame=0L
    private var width=0
    private var height=0
    fun attach(surface:FoliaNativeSurfaceView){view=surface;publish()}
    fun detach(surface:FoliaNativeSurfaceView){if(view===surface)view=null}
    private fun publish(){
        val v=view?:return
        if(width<=0||height<=0)return
        val frames=ordered.mapNotNull{entry->entry.frame?.takeIf{it.width==width&&it.height==height}?.let{frame->
            FrameEntry(entry.upload,frame){if(ordered.contains(entry))entry.notify(frame)}
        }}
        try{v.publish(ScenePacketEncoder.encode(++nextFrame,width,height,ordered.map{it.upload},frames))}catch(e:Exception){fail(e)}
    }
    @Composable override fun Line(scene:FoliaRendererScene,modifier:Modifier,frame:(Int,Int)->FoliaRendererFrame,onPresented:(FoliaRendererFrame)->Unit) = Scene(scene, modifier, false, frame, onPresented)

    @Composable override fun Stage(scene: FoliaRendererScene, modifier: Modifier,
        frame: (Int, Int) -> FoliaRendererFrame?, onPresented: (FoliaRendererFrame) -> Unit) =
        Scene(scene, modifier, true, frame, onPresented)

    @Composable private fun Scene(scene: FoliaRendererScene, modifier: Modifier, preserveColor: Boolean,
        frame: (Int, Int) -> FoliaRendererFrame?, onPresented: (FoliaRendererFrame) -> Unit) {
        val prepared=remember(scene){runCatching{
            Entry(SceneUpload(++nextScene,scene,config.preparedTextureBudgetBytes,preserveColor))
        }}
        val entry=prepared.getOrNull()
        if(entry==null){LaunchedEffect(prepared){fail(prepared.exceptionOrNull()!!)};return}
        val build by rememberUpdatedState(frame)
        val acknowledge by rememberUpdatedState(onPresented)
        DisposableEffect(scene){
            entries[scene]=entry;ordered+=entry
            onDispose{entries.remove(scene);ordered.remove(entry);publish()}
        }
        Canvas(modifier){
            width=size.width.toInt();height=size.height.toInt()
            if(width>0&&height>0){
                entry.notify=acknowledge
                try {
                    val next=build(width,height) ?: return@Canvas
                    if(entry.frame!=next){entry.frame=next;publish()}
                } catch (error: Exception) { fail(error) }
            }
        }
    }
}
