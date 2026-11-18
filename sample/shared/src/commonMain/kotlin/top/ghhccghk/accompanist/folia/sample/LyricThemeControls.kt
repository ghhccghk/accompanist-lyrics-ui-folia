package top.ghhccghk.accompanist.folia.sample

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/** Only actual Kotlin renderers are selectable. No placeholders for planned themes. */
@Composable
internal fun LyricThemeControls(session: SampleSession) {
    Text("歌词主题")
    Text("7 个校准预览主题 + 镜台实验调试预览，尚未完成正式视觉验收。", style=MaterialTheme.typography.bodySmall)
    FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        for(theme in SampleLyricTheme.entries) {
            FilterChip(
                selected=session.lyricTheme==theme,
                onClick={session.lyricTheme=theme},
                modifier=Modifier.testTag("lyric-theme-${theme.name}"),
                label={
                    Column {
                        Text(theme.displayName)
                        Text(theme.name,style=MaterialTheme.typography.labelSmall)
                    }
                }
            )
        }
    }
    if(session.lyricTheme==SampleLyricTheme.Diorama){
        Text("镜台 Diorama · 实验调试预览",style=MaterialTheme.typography.bodySmall)
        Text("真实文字透视与运镜已接入；镜头、遮挡与 Android 辉光仍待校准。",style=MaterialTheme.typography.bodySmall)
        ThemeToggle("纹理预算检查",session.dioramaTuning.enforceRasterBudget){session.dioramaTuning=session.dioramaTuning.copy(enforceRasterBudget=it)}
        Text(if(session.dioramaTuning.enforceRasterBudget)"单张纹理或驻留窗口超出预算会显示错误。" else "纹理预算检查已关闭，超大歌词可能导致内存不足。",style=MaterialTheme.typography.bodySmall)
        ThemeToggle("纹理磁盘缓存",session.dioramaTuning.cacheTexturesOnDisk){session.dioramaTuning=session.dioramaTuning.copy(cacheTexturesOnDisk=it)}
        Text(if(session.dioramaTuning.cacheTexturesOnDisk)"超过纹理内存目标后转临时缓存，后台加载当前句与邻行。" else "整曲纹理保存在内存中。",style=MaterialTheme.typography.bodySmall)
        Text("镜台字号倍率 ${session.dioramaTuning.fontScale}")
        Slider(session.dioramaTuning.fontScale,{session.dioramaTuning=session.dioramaTuning.copy(fontScale=it)},valueRange=.5f..2f)
    }
    if(session.lyricTheme==SampleLyricTheme.Fume){
        Text("浮名：文章镜头校准预览。短桥接、总览与文字光栅仍有差异。",style=MaterialTheme.typography.bodySmall)
        Text("文章字号倍率 ${session.fumeTuning.fontScale}")
        Slider(session.fumeTuning.fontScale,{session.fumeTuning=session.fumeTuning.copy(fontScale=it)},valueRange=.5f..2f)
        Text("读后保留 ${session.fumeTuning.textHoldRatio}")
        Slider(session.fumeTuning.textHoldRatio,{session.fumeTuning=session.fumeTuning.copy(textHoldRatio=it)},valueRange=0f..1f)
        ThemeToggle("步进镜头",session.fumeTuning.trackingMode==top.ghhccghk.accompanist.lyrics.folia.FumeTrackingMode.Stepped){
            session.fumeTuning=session.fumeTuning.copy(trackingMode=if(it)top.ghhccghk.accompanist.lyrics.folia.FumeTrackingMode.Stepped else top.ghhccghk.accompanist.lyrics.folia.FumeTrackingMode.Smooth)
        }
        Text("镜头速度 ${session.fumeTuning.cameraSpeed}")
        Slider(session.fumeTuning.cameraSpeed,{session.fumeTuning=session.fumeTuning.copy(cameraSpeed=it)},valueRange=.55f..1.85f)
        Text("浮名辉光 ${session.fumeTuning.glowIntensity}")
        Slider(session.fumeTuning.glowIntensity,{session.fumeTuning=session.fumeTuning.copy(glowIntensity=it)},valueRange=0f..1.8f)
        ThemeToggle("打印符号",session.fumeTuning.showPrintSymbols){session.fumeTuning=session.fumeTuning.copy(showPrintSymbols=it)}
    }
    if(session.lyricTheme==SampleLyricTheme.Cappella) {
        Text("群唱：文字对话校准预览。密集消息投影、字形与辉光仍有差异。",style=MaterialTheme.typography.bodySmall)
        Text("群唱字号倍率 ${session.cappellaTuning.fontScale}")
        Slider(session.cappellaTuning.fontScale,{session.cappellaTuning=session.cappellaTuning.copy(fontScale=it)},valueRange=.5f..2f)
    }
    if(session.lyricTheme==SampleLyricTheme.Pendolo) {
        Text("时计：移植校准预览。密集跳步、轮盘旋转与文字辉光仍有差异。",style=MaterialTheme.typography.bodySmall)
        Text("正文字号倍率 ${session.pendoloTuning.lyricsFontScale}")
        Slider(session.pendoloTuning.lyricsFontScale,{session.pendoloTuning=session.pendoloTuning.copy(lyricsFontScale=it)},valueRange=.5f..2f)
        Text("字幕字号倍率 ${session.pendoloTuning.subtitleFontScale}")
        Slider(session.pendoloTuning.subtitleFontScale,{session.pendoloTuning=session.pendoloTuning.copy(subtitleFontScale=it)},valueRange=.5f..2f)
    }
    if(session.lyricTheme==SampleLyricTheme.Cadenza) {
        Text("心象：移植校准预览。原版刷新率响应、CJK 字形及辉光栅格仍有差异。",style=MaterialTheme.typography.bodySmall)
        Text("主题字号倍率 ${session.cadenzaTuning.fontScale}")
        Slider(session.cadenzaTuning.fontScale,{session.cadenzaTuning=session.cadenzaTuning.copy(fontScale=it)},valueRange=.5f..2f)
        Text("排版宽度 ${(session.cadenzaTuning.widthRatio*100).toInt()}%")
        Slider(session.cadenzaTuning.widthRatio,{session.cadenzaTuning=session.cadenzaTuning.copy(widthRatio=it)},valueRange=.35f..1f)
        Text("辉光强度 ${session.cadenzaTuning.glowIntensity}")
        Slider(session.cadenzaTuning.glowIntensity,{session.cadenzaTuning=session.cadenzaTuning.copy(glowIntensity=it)},valueRange=0f..2f)
        Text("脉动幅度 ${session.cadenzaTuning.motionAmount}")
        Slider(session.cadenzaTuning.motionAmount,{session.cadenzaTuning=session.cadenzaTuning.copy(motionAmount=it)},valueRange=0f..2f)
    }
    if(session.lyricTheme==SampleLyricTheme.Partita) {
        Text("云阶移植校准中。",style=MaterialTheme.typography.bodySmall)
        fun tune(block: (top.ghhccghk.accompanist.lyrics.folia.PartitaTuning)->top.ghhccghk.accompanist.lyrics.folia.PartitaTuning) {
            session.partitaTuning=block(session.partitaTuning)
        }
        ThemeToggle("引导线",session.partitaTuning.showGuideLines){v->tune{it.copy(showGuideLines=v)}}
        ThemeToggle("语义分组",session.partitaTuning.useSemanticLayout){v->tune{it.copy(useSemanticLayout=v)}}
        ThemeToggle("避让与自动拟合",session.partitaTuning.avoidOverlap){v->tune{it.copy(avoidOverlap=v)}}
        Text("最小错位 ${session.partitaTuning.staggerMin.toInt()} px")
        Slider(session.partitaTuning.staggerMin,{v->tune{it.copy(staggerMin=v,staggerMax=maxOf(v,it.staggerMax))}},valueRange=0f..180f)
        Text("最大错位 ${session.partitaTuning.staggerMax.toInt()} px")
        Slider(session.partitaTuning.staggerMax,{v->tune{it.copy(staggerMax=v,staggerMin=minOf(v,it.staggerMin))}},valueRange=0f..180f)
    }
    if(session.lyricTheme==SampleLyricTheme.Tilt) {
        Text("倾诉：移植校准预览。字距、变换延迟及 Arabic 与原版仍有差异。",style=MaterialTheme.typography.bodySmall)
        Text("分段概率 ${(session.tiltTuning.splitProbability*100).toInt()}%")
        Slider(session.tiltTuning.splitProbability,{session.tiltTuning=session.tiltTuning.copy(splitProbability=it)},valueRange=0f..1f)
        Text("斜体段概率 ${(session.tiltTuning.tiltStyleProbability*100).toInt()}%")
        Slider(session.tiltTuning.tiltStyleProbability,{session.tiltTuning=session.tiltTuning.copy(tiltStyleProbability=it)},valueRange=0f..1f)
        TextButton({
            val entries=top.ghhccghk.accompanist.lyrics.folia.TiltColorScheme.entries
            session.tiltTuning=session.tiltTuning.copy(colorScheme=entries[(session.tiltTuning.colorScheme.ordinal+1)%entries.size])
        }) {Text("配色："+when(session.tiltTuning.colorScheme) {
            top.ghhccghk.accompanist.lyrics.folia.TiltColorScheme.Default->"默认"
            top.ghhccghk.accompanist.lyrics.folia.TiltColorScheme.Swap->"互换"
            top.ghhccghk.accompanist.lyrics.folia.TiltColorScheme.AccentAll->"全部强调色"
            top.ghhccghk.accompanist.lyrics.folia.TiltColorScheme.PrimaryAll->"全部正文色"
        })}
    }
}

@Composable
private fun ThemeToggle(label: String, value: Boolean, onChange: (Boolean)->Unit) {
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
        Text(label,Modifier.weight(1f));Switch(value,onChange)
    }
}
