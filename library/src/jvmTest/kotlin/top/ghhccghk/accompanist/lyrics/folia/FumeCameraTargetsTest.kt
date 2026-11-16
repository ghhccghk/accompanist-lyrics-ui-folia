package top.ghhccghk.accompanist.lyrics.folia

import top.ghhccghk.accompanist.lyrics.folia.internal.styles.fume.*
import java.io.File
import java.util.Base64
import kotlin.test.*

class FumeCameraTargetsTest {
    private fun rows(name: String) = File("../verification/fume-fixtures/$name.tsv").readLines()
        .filterNot { it.startsWith('#') || it.isBlank() }.map { it.split('\t') }
    private val blocks = rows("camera-blocks").map { f -> FumeArticleBlock(f[0].toInt(),
        FumeLyric(String(Base64.getDecoder().decode(f[1]),Charsets.UTF_8),f[2].toDouble(),f[3].toDouble()),false,
        f[4].toDouble(),f[5].toDouble(),f[6].toDouble(),f[7].toDouble(),f[6].toDouble(),32.0,f[8].toDouble(),1) }
    private val geometry = blocks.associate { block -> block.sourceIndex to FumeFocusBlock(block,
        rows("camera-rows").filter{it[0].toInt()==block.sourceIndex}.map { f ->
            FumeFocusRow(f[1].toInt(),f[2].toInt(),f[3].toDouble(),f[4].toDouble(),f[5].toDouble(),f[6].split(',').map(String::toDouble))
        }) }
    private fun article(index: Int): FumeArticle {
        val selected = when(index){0->emptyList();1->blocks.take(2);else->blocks}
        return FumeArticle(2500.0,4600.0,600.0,2,24.0,1.0,FumeBounds(0.0,0.0,2500.0,4600.0),selected)
    }
    private fun FumePoint.values() = listOf(x,y)
    private fun FumeCameraView.values() = listOf(x,y,scale)
    @Test fun sourceTargetsBridgesBoundsAndFocusMatchAfterRandomTraversal() {
        val source = rows("camera-targets")
        for(f in source + source.shuffled(kotlin.random.Random(727))) {
            val id=f[1].toInt();val viewport=FumeViewport(f[2].toDouble(),f[3].toDouble())
            val args=if(f[4].isEmpty())emptyList()else f[4].split(',').map(String::toDouble)
            val result: List<Double>? = when(f[0]) {
                "entry"->geometry.getValue(id).entry().values()
                "scale"->listOf(fumeCameraScale(geometry.getValue(id).block,viewport))
                "smooth"->geometry.getValue(id).smooth(args[0]).values()
                "stepped"->geometry.getValue(id).stepped(args[0].toInt()).values()
                "screen"->listOf(if(fumeBlockOnScreen(geometry.getValue(id).block,FumeCameraView(args[0],args[1],args[2]),viewport,args[3]))1.0 else 0.0)
                "overviewDuration"->listOf(fumeOverviewRetarget(viewport))
                "overview"->fumeOverview(article(id),viewport).values()
                "focus"->listOf((fumeFocusBlock(article(id),args[0].toInt(),args[1])?.sourceIndex?:-1).toDouble())
                "retarget"->listOf(fumeCameraRetarget(FumeLyric("test",.7,.7+args[0])))
                "bridge"->fumeOverviewBridge(FumeCameraView(args[0],args[1],args[2]),FumeCameraView(args[3],args[4],args[5]),
                    if(args[8]<0)null else FumeCameraView(args[6],args[7],args[8]),viewport)?.let { listOf(it.x,it.y,it.scale,it.phase,it.duration) }
                else->error("unknown fixture ${f[0]}")
            }
            if(f[5]=="-")assertNull(result,f.toString())else {
                val expected=f[5].split(',').map(String::toDouble)
                assertEquals(expected.size,assertNotNull(result).size,f.toString())
                expected.zip(result).forEachIndexed { i,(e,a)->assertEquals(e,a,1e-8,"$f field $i") }
            }
        }
    }
    @Test fun preserveSourceCrossRowReturnInsteadOfAssumingContinuity() {
        val focus=geometry.getValue(0)
        assertNotEquals(focus.stepped(4),focus.smooth(4.0))
        val before=focus.smooth(4-1e-8);val after=focus.smooth(4+1e-8)
        // Original fumeCamera.ts blends towards the NEXT row just before the boundary,
        // then towards the PREVIOUS row just after it. The subsequent spring absorbs
        // this target jump. Do not silently replace it by a different focus curve.
        assertEquals(focus.block.x + focus.rows[1].left,before.x,1e-5)
        assertEquals(focus.entry().y + focus.block.lineHeight,before.y,1e-5)
        assertEquals(focus.block.x + focus.rows[0].left + focus.rows[0].width,after.x,1e-5)
        assertEquals(focus.entry().y,after.y,1e-5)
        assertEquals(FumePoint(15.0,-8.0),geometry.getValue(4).entry())
    }
}
