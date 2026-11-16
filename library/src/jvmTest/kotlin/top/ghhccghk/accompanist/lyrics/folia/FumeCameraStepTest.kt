package top.ghhccghk.accompanist.lyrics.folia

import top.ghhccghk.accompanist.lyrics.folia.internal.styles.fume.*
import java.io.File
import java.util.Base64
import kotlin.test.*

class FumeCameraStepTest {
    private fun rows(name:String)=File("../verification/fume-fixtures/$name.tsv").readLines()
        .filterNot{it.startsWith('#')||it.isBlank()}.map{it.split('\t')}
    @Test fun originalSpringAndBridgeTrajectoriesAtMultipleStepRates() {
        val blocks=rows("camera-blocks").map { f -> FumeArticleBlock(f[0].toInt(),
            FumeLyric(String(Base64.getDecoder().decode(f[1]),Charsets.UTF_8),f[2].toDouble(),f[3].toDouble()),false,
            f[4].toDouble(),f[5].toDouble(),f[6].toDouble(),f[7].toDouble(),f[6].toDouble(),32.0,f[8].toDouble(),1) }
        val geometry=blocks.associate { block -> block.sourceIndex to FumeFocusBlock(block,
            rows("camera-rows").filter{it[0].toInt()==block.sourceIndex}.map { f ->
                FumeFocusRow(f[1].toInt(),f[2].toInt(),f[3].toDouble(),f[4].toDouble(),f[5].toDouble(),f[6].split(',').map(String::toDouble))
            }) }
        val article=FumeArticle(2500.0,4600.0,600.0,2,24.0,1.0,FumeBounds(0.0,0.0,2500.0,4600.0),blocks)
        val expected=rows("camera-step").groupBy{it[0]}
        for((scenario,frames)in expected) {
            var state=fumeInitialCamera(article)
            for(f in frames) {
                val input=FumeCameraInput(f[1].toDouble(),f[2].toDouble(),f[3].toDouble(),f[4].toInt(),
                    FumeViewport(f[5].toDouble(),f[6].toDouble()),fumeOverview(article,FumeViewport(f[5].toDouble(),f[6].toDouble())),
                    f[11].toDouble(),f[7].toDouble(),f[8].toBoolean(),f[9].toBoolean(),f[10])
                val snapshot=state.copy()
                val result=fumeCameraStep(state,article,geometry,input)
                assertEquals(snapshot,state,"step must not mutate its input")
                assertEquals(result,fumeCameraStep(state,article,geometry,input),"same step same result")
                state=result.state
                val p=state.pose;val t=state.retarget
                fun numbers(source:List<String>,actual:List<Double>,label:String) {
                    source.map(String::toDouble).zip(actual).forEachIndexed { i,(e,a)->
                        assertEquals(e,a,1e-8,"scenario=$scenario time=${input.time} $label field=$i")
                    }
                }
                numbers(f[12].split(','),listOf(p.x,p.y,p.vx,p.vy,p.fx,p.fy,p.scale,p.vs,p.fs),"pose")
                val r=f[13].split(',')
                assertEquals(r[6],t.bridge)
                numbers(r.take(6)+r.drop(7),listOf(t.source.toDouble(),t.started,t.duration,t.from.x,t.from.y,t.from.scale,
                    t.waypoint.x,t.waypoint.y,t.waypoint.scale,t.phase),"retarget")
                assertEquals(f[14].toBoolean(),result.overview)
                assertEquals(f[15].toDouble(),result.restore,1e-10)
            }
        }
        assertEquals(16,expected.size)
    }
}
