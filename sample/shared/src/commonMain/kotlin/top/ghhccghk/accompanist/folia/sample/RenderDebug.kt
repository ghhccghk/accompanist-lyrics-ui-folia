package top.ghhccghk.accompanist.folia.sample

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import top.ghhccghk.accompanist.lyrics.folia.FoliaPlaybackSnapshot
import top.ghhccghk.accompanist.lyrics.folia.FoliaStatus
import kotlin.math.ceil
import kotlin.math.roundToInt

data class RenderFrameStats(val frames:Int=0,val fps:Double=0.0,val meanMillis:Double=0.0,
    val p95Millis:Double=0.0,val maxMillis:Double=0.0,val intervalP95Millis:Double=0.0,val lostReports:Int=0)

/** Bounded primitive ring. One owner or external synchronization; no media-time
 * dependency and no request for an extra frame. Durations are caller-defined. */
class RenderFrameWindow(private val capacity:Int=360,private val horizonNanos:Long=2_000_000_000) {
    private val times=LongArray(capacity);private val durations=LongArray(capacity);private val lost=IntArray(capacity)
    private var cursor=0;private var count=0;private var last=-1L
    init{require(capacity>=2&&horizonNanos>0)}
    fun reset(){cursor=0;count=0;last=-1}
    fun record(atNanos:Long,durationNanos:Long,lostReports:Int=0) {
        if(atNanos<0||atNanos<=last||durationNanos<0)return
        times[cursor]=atNanos;durations[cursor]=durationNanos;lost[cursor]=lostReports.coerceAtLeast(0)
        cursor=(cursor+1)%capacity;count=minOf(capacity,count+1);last=atNanos
    }
    fun read(nowNanos:Long):RenderFrameStats {
        val selected=(0 until count).map{(cursor-count+it+capacity)%capacity}
            .filter{times[it]<=nowNanos&&nowNanos-times[it]<horizonNanos}
        if(selected.isEmpty())return RenderFrameStats()
        val work=selected.map{durations[it]}.sorted()
        val intervals=selected.zipWithNext{a,b->times[b]-times[a]}.sorted()
        fun p95(values:List<Long>)=if(values.isEmpty())0.0 else values[(ceil(values.size*.95).toInt()-1).coerceAtLeast(0)]/1e6
        val span=times[selected.last()]-times[selected.first()]
        val fps=if(span>0&&(nowNanos-last)<1_000_000_000)(selected.size-1)*1e9/span else 0.0
        return RenderFrameStats(selected.size,fps,work.average()/1e6,p95(work),work.last()/1e6,p95(intervals),selected.sumOf{lost[it]})
    }
}

/** Sample instrumentation only. Implementations must restore hooks/listeners
 * on disable; these are callback rates/durations, never GPU-present timings. */
interface SampleRenderDebugHost {
    val backend:String
    val requestedBackend:String
    val metric:String
    fun setEnabled(enabled:Boolean)
    fun read():RenderFrameStats
}

val LocalSampleRenderDebugHost=staticCompositionLocalOf<SampleRenderDebugHost?>{null}
@Composable internal expect fun rememberPlatformRenderDebugHost():SampleRenderDebugHost?

@Composable internal fun RenderDebugToggle(checked:Boolean,onChange:(Boolean)->Unit) {
    Row(Modifier.fillMaxWidth().toggleable(checked,role=Role.Switch,onValueChange=onChange),
        verticalAlignment=Alignment.CenterVertically) {
        Text("调试信息（后端／FPS）",Modifier.weight(1f))
        Switch(checked,onCheckedChange=null)
    }
}

@Composable
internal fun RenderDebugOverlay(enabled:Boolean,visible:Boolean,session:SampleSession,
    playback:()->FoliaPlaybackSnapshot,status:FoliaStatus?,onClose:()->Unit,modifier:Modifier=Modifier) {
    val platform=rememberPlatformRenderDebugHost()
    val host=LocalSampleRenderDebugHost.current?:platform
    var frames by remember{mutableStateOf(RenderFrameStats())}
    var backend by remember{mutableStateOf("")}
    var metric by remember{mutableStateOf("")}
    var media by remember{mutableStateOf(playback())}
    val clock by rememberUpdatedState(playback)
    LaunchedEffect(host,enabled,visible) {
        host?.setEnabled(enabled&&visible)
        try {
            while(enabled&&visible) {
                frames=host?.read()?:RenderFrameStats();backend=host?.backend?:"未提供平台探针"
                metric=host?.metric?:"帧数据不可用";media=clock()
                delay(500)
            }
        }finally{host?.setEnabled(false)}
    }
    if(!enabled)return
    val density=LocalDensity.current
    val windowSize=LocalWindowInfo.current.containerSize
    fun number(value:Double)=(value*10).roundToInt()/10.0
    Surface(modifier.widthIn(max=420.dp),color=MaterialTheme.colorScheme.surface.copy(alpha=.93f),
        contentColor=MaterialTheme.colorScheme.onSurface,tonalElevation=3.dp) {
        Column(Modifier.heightIn(max=380.dp).verticalScroll(rememberScrollState()).padding(8.dp),verticalArrangement=Arrangement.spacedBy(2.dp)) {
            Row(horizontalArrangement=Arrangement.SpaceBetween,modifier=Modifier.fillMaxWidth()) {
                Text("调试 · ${session.lyricTheme} · ${session.renderResolution.label}",style=MaterialTheme.typography.labelMedium,modifier=Modifier.weight(1f))
                TextButton(onClose){Text("关闭调试信息")}
            }
            Text("后端 $backend（请求 ${host?.requestedBackend?:"AUTO"}）",style=MaterialTheme.typography.bodySmall)
            Text("窗口 ${windowSize.width}×${windowSize.height} px · density ${density.density} · fontScale ${density.fontScale}",style=MaterialTheme.typography.bodySmall)
            Text("窗口回调 FPS ${number(frames.fps)} · ${frames.frames} 样本 / 最近2秒",style=MaterialTheme.typography.bodySmall)
            Text("耗时均值 ${number(frames.meanMillis)} / P95 ${number(frames.p95Millis)} / 最大 ${number(frames.maxMillis)} ms",style=MaterialTheme.typography.bodySmall)
            Text("帧间隔P95 ${number(frames.intervalP95Millis)} ms · 丢失报告 ${frames.lostReports}",style=MaterialTheme.typography.bodySmall)
            Text(metric,style=MaterialTheme.typography.bodySmall)
            Text("媒体 ${media.positionMillis} ms · ${if(media.isPlaying)"播放" else "暂停"} · discontinuity ${media.discontinuity}",style=MaterialTheme.typography.bodySmall)
            status?.let { value->
                Text("画质 ${value.requestedQuality} → ${value.actualQuality} · ${value.mainUnitCount} 单元",style=MaterialTheme.typography.bodySmall)
                value.diagnostics.forEach{Text(it,style=MaterialTheme.typography.bodySmall)}
            }
            Text("含调试面板绘制；不是GPU呈现FPS／GPU耗时。面板每500ms刷新。",style=MaterialTheme.typography.labelSmall)
        }
    }
}
