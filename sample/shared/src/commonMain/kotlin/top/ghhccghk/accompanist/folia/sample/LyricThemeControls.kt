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
    }
    if(session.lyricTheme==SampleLyricTheme.Partita) {
        Text("云阶移植校准中；其余主题尚未实现。",style=MaterialTheme.typography.bodySmall)
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
}

@Composable
private fun ThemeToggle(label: String, value: Boolean, onChange: (Boolean)->Unit) {
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
        Text(label,Modifier.weight(1f));Switch(value,onChange)
    }
}
