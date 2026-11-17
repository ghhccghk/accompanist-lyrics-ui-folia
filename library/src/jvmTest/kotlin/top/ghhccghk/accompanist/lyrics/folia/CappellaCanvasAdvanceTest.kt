package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.ui.platform.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.sp
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.cappella.cappellaCanvasAdvance
import java.io.File
import java.util.Base64
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class CappellaCanvasAdvanceTest {
    @Test fun originalCanvasWidthsIncludeKerningLigaturesAndJoining() = runDesktopComposeUiTest(width=100,height=100) {
        var resolver:FontFamily.Resolver?=null;var density:androidx.compose.ui.unit.Density?=null
        setContent{resolver=LocalFontFamilyResolver.current;density=LocalDensity.current}
        waitForIdle()
        val results=mutableListOf("# scene fontPx text64 original native error")
        for(r in File("../verification/cappella-fixtures/canvas-advances.tsv").readLines().filterNot{it.startsWith('#')||it.isBlank()}.map{it.split('\t')}){
            val font=when(r[0]){"arabic"->"FoliaParityBidi-Bold.ttf";"cjk"->"FoliaParityChatCJK-Bold.otf";else->"FoliaParityLatin-Bold.ttf"}
            val style=TextStyle(fontFamily=FontFamily(Font(File("../verification/assets/$font"),FontWeight.Bold)),fontWeight=FontWeight.Bold,fontSize=r[1].toFloat().sp)
            val text=String(Base64.getDecoder().decode(r[2]),Charsets.UTF_8)
            val actual=assertNotNull(cappellaCanvasAdvance(text,style,assertNotNull(resolver),assertNotNull(density)))
            results+="${r.take(3).joinToString("\t")}\t${r[3]}\t$actual\t${kotlin.math.abs(r[3].toDouble()-actual)}"
        }
        File("../build/verification/cappella-stage-kotlin").apply{mkdirs()}.resolve("canvas-residual.tsv").writeText(results.joinToString("\n")+"\n")
        for(r in results.drop(1).map{it.split('\t')})assertEquals(r[3].toDouble(),r[4].toDouble(),1e-5,"source Canvas ${r.take(3)}")
    }
}
