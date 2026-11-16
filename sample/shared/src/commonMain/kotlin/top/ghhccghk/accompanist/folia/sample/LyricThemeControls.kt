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
