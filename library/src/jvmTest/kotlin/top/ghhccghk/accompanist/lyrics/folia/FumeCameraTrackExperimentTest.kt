package top.ghhccghk.accompanist.lyrics.folia

import top.ghhccghk.accompanist.lyrics.folia.internal.styles.fume.*
import java.io.File
import java.util.Base64
import kotlin.math.*
import kotlin.test.*

class FumeCameraTrackExperimentTest {
    private fun rows(name:String)=File("../verification/fume-fixtures/$name.tsv").readLines()
        .filterNot{it.startsWith('#')||it.isBlank()}.map{it.split('\t')}
    @Test fun reconstructOriginalOperatorInMediaTimeWithoutDisplayHistory() {
        val blocks=rows("camera-blocks").map { f -> FumeArticleBlock(f[0].toInt(),
            FumeLyric(String(Base64.getDecoder().decode(f[1]),Charsets.UTF_8),f[2].toDouble(),f[3].toDouble()),false,
            f[4].toDouble(),f[5].toDouble(),f[6].toDouble(),f[7].toDouble(),f[6].toDouble(),32.0,f[8].toDouble(),1) }
        val geometry=blocks.associate { block -> block.sourceIndex to FumeFocusBlock(block,
            rows("camera-rows").filter{it[0].toInt()==block.sourceIndex}.map { f ->
                FumeFocusRow(f[1].toInt(),f[2].toInt(),f[3].toDouble(),f[4].toDouble(),f[5].toDouble(),f[6].split(',').map(String::toDouble))
            }) }
        val article=FumeArticle(2500.0,4600.0,600.0,2,24.0,1.0,FumeBounds(0.0,0.0,2500.0,4600.0),blocks)
        val report=mutableListOf("# scenario,time,adaptive_x,native_x,adaptive_y,native_y,adaptive_scale,native_scale,source60_x,source60_y,source60_scale,nodes")
        fun FumeCameraPose.values()=listOf(x,y,vx,vy,fx,fy,scale,vs,fs)
        for((scenario,frames)in rows("camera-track").groupBy{it[0]}) {
            val config=FumeCameraTrackExperimentConfig(FumeViewport(800.0,600.0),6.8,stepped=frames.first()[2].toBoolean(),
                static=frames.first()[3].toBoolean(),floatOrigin=9.25)
            val epochs=listOf(FumeCameraEpoch(0.0,0),FumeCameraEpoch(1.5,2),FumeCameraEpoch(3.0,0),FumeCameraEpoch(4.5,4))
            val track=FumeCameraTrackExperiment(article,geometry,epochs,config,8.0)
            assertEquals(frames.first()[4].toInt(),track.preparedNodeCount)
            val frozen=frames.associate { it[1].toDouble() to track.sample(it[1].toDouble()) }
            for(f in frames) {
                val time=f[1].toDouble();val actual=track.sample(time);val expected=f[5].split(',').map(String::toDouble)
                expected.zip(actual.state.pose.values()).forEachIndexed { i,(e,a)->assertEquals(e,a,1e-8,"scenario=$scenario t=$time pose field=$i") }
                val r=f[6].split(',');val t=actual.state.retarget
                assertEquals(r[6],t.bridge)
                val values=listOf(t.source.toDouble(),t.started,t.duration,t.from.x,t.from.y,t.from.scale,t.waypoint.x,t.waypoint.y,t.waypoint.scale,t.phase)
                (r.take(6)+r.drop(7)).map(String::toDouble).zip(values).forEachIndexed { i,(e,a)->assertEquals(e,a,1e-8,"scenario=$scenario t=$time retarget field=$i") }
                val source60=f[7].split(',').map(String::toDouble)
                report+=listOf(scenario,time,expected[0],actual.state.pose.x,expected[1],actual.state.pose.y,expected[6],actual.state.pose.scale,
                    source60[0],source60[1],source60[6],track.preparedNodeCount).joinToString(",")
            }
            for((time,expected)in frozen.entries.shuffled(kotlin.random.Random(816))) {
                track.sample(12.342);track.sample(.000001)
                assertEquals(expected,track.sample(time),"seek/reverse/pause must not alter trajectory")
            }
            assertEquals(frozen.getValue(0.0).state.pose.view(),track.sample(-10.0).state.pose.view())
            assertFailsWith<IllegalArgumentException>{track.sample(Double.NaN)}
        }
        val out=File("../build/verification/fume-camera-track").apply{mkdirs()}
        out.resolve("comparison.csv").writeText(report.joinToString("\n")+"\n")
    }
}
