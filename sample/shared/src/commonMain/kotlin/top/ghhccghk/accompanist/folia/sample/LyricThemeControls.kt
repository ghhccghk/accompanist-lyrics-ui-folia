package top.ghhccghk.accompanist.folia.sample

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/** Only actual Kotlin renderers are selectable. No placeholders for planned themes. */
@Composable
internal fun LyricThemeControls(session: SampleSession) {
    Text("歌词主题")
    Row {
        TextButton({session.lyricTheme=SampleLyricTheme.Classic}) {
            Text("流光${if(session.lyricTheme==SampleLyricTheme.Classic)" ✓" else ""}")
        }
        TextButton({session.lyricTheme=SampleLyricTheme.Partita}) {
            Text("云阶${if(session.lyricTheme==SampleLyricTheme.Partita)" ✓" else ""}")
        }
        TextButton({session.lyricTheme=SampleLyricTheme.Tilt}) {
            Text("倾诉${if(session.lyricTheme==SampleLyricTheme.Tilt)" ✓" else ""}")
        }
    }
    TextButton({session.lyricTheme=SampleLyricTheme.Cadenza}) {
        Text("心象${if(session.lyricTheme==SampleLyricTheme.Cadenza)" ✓" else ""}")
    }
    TextButton({session.lyricTheme=SampleLyricTheme.Pendolo}) {
        Text("时计${if(session.lyricTheme==SampleLyricTheme.Pendolo)" ✓" else ""}")
    }
    TextButton({session.lyricTheme=SampleLyricTheme.Cappella}) {
        Text("群唱${if(session.lyricTheme==SampleLyricTheme.Cappella)" ✓" else ""}")
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
