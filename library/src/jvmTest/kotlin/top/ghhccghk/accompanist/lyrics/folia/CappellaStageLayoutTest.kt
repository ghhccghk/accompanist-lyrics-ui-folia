package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.cappella.*
import java.io.File
import java.util.Base64
import kotlin.random.Random
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class CappellaStageLayoutTest {
    @Test fun sourceCompleteStageTargetsMatchIndependentNativeMeasurement() = runDesktopComposeUiTest(width=1200,height=900) {
        var measurer:TextMeasurer?=null;var density:androidx.compose.ui.unit.Density?=null;var resolver:FontFamily.Resolver?=null
        setContent { measurer=rememberTextMeasurer(cacheSize=32);density=LocalDensity.current;resolver=LocalFontFamilyResolver.current }
        waitForIdle()
        val source=File("../verification/cappella-fixtures/stage-targets.tsv").readLines().filterNot{it.startsWith('#')||it.isBlank()}.map{it.split('\t')}
        val fixtures=File("../verification/cadenza-fixtures/sceneInputs.tsv").readLines().filterNot{it.startsWith('#')||it.isBlank()}.map{it.split('\t')}.associateBy{it[0]}
        fun decode(s:String)=String(Base64.getDecoder().decode(s),Charsets.UTF_8)
        val residuals=mutableListOf("# scene width intensity time row field original native error")
        for((key,observations)in source.groupBy{Triple(it[0],it[1].toInt(),it[2])}) {
            val(scene,width,intensity)=key;val f=fixtures.getValue(scene)
            val lines=(0 until 8).map { i->CappellaLine(decode(f[1]),f[2].toDouble()+i*4,f[3].toDouble()+i*4,
                f[4].split('|').map{v->val w=v.split('~');CappellaWord(decode(w[0]),w[1].toDouble()+i*4,w[2].toDouble()+i*4)}) }
            val font=when(scene){"arabic"->"FoliaParityBidi-Bold.ttf";"cjk"->"FoliaParityChatCJK-Bold.otf";else->"FoliaParityLatin-Bold.ttf"}
            val style=TextStyle(fontFamily=FontFamily(Font(File("../verification/assets/$font"),FontWeight.Bold)),fontWeight=FontWeight.Bold,textDirection=TextDirection.Ltr)
            val prep=CappellaStagePreparation(lines,"Synthetic title",cappellaStageSize(width.toDouble()),CappellaIntensity.valueOf(intensity.uppercase()),assertNotNull(measurer),style,assertNotNull(density),assertNotNull(resolver))
            val times=observations.groupBy{it[3].toDouble()}
            val results=mutableMapOf<Double,List<CappellaRowLayout>>()
            for((time,rows)in times) {
                val native=prep.at(time,rows.first()[4].toInt(),900.0);results[time]=native
                assertEquals(rows.size,native.size,"$key/$time count")
                for((r,n)in rows.zip(native)){
                    val original=r[6].split(',').map(String::toDouble).let{listOf(it[1],it[3])}+
                        r[7].split(',').map(String::toDouble)+r[8].split(',').map(String::toDouble)+r[9].toDouble()
                    val actual=listOf(n.rowTop,n.rowHeight,n.avatarX,n.avatarY,40*n.scale,40*n.scale,
                        n.bubbleX,n.bubbleY,n.bubbleWidth,n.bubbleHeight,n.opacity)
                    original.zip(actual).forEachIndexed { i,(a,b)->
                        residuals+="$scene\t$width\t$intensity\t$time\t${r[5]}\t$i\t$a\t$b\t${kotlin.math.abs(a-b)}"
                    }
                }
            }
            for((time,expected)in results.entries.shuffled(Random(191)))assertEquals(expected,prep.at(time,times.getValue(time).first()[4].toInt(),900.0),"random seek/reprepare $key")
        }
        val out=File("../build/verification/cappella-stage-kotlin").apply{mkdirs()}
        out.resolve("source-residual.tsv").writeText(residuals.joinToString("\n")+"\n")
        // Float DOM transforms and 1/64 CSS geometry are compared independently
        // of screenshots. Never loosen this bound for a missing visual effect.
        for(r in residuals.drop(1).map{it.split('\t')})assertEquals(r[6].toDouble(),r[7].toDouble(),1e-3,"source target ${r.take(6)}")
    }
}
