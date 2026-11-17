package top.ghhccghk.accompanist.lyrics.folia

import java.io.File
import java.util.Base64
import kotlin.math.*
import kotlin.test.*
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.fume.*

/** Original incoming states and passive source trace, not camera-continuous's
 * adapted trajectory. Reflection is confined here: no test seam in playback. */
class FumeCameraHandoffTest {
    private fun rows(name: String) = File("../verification/fume-fixtures/handoff-v1/$name.tsv")
        .readLines().filterNot { it.isBlank() || it.startsWith('#') }.map { it.split('\t') }
    private fun values(s: String) = s.split(',').map(String::toDouble)
    private fun text(s: String) = String(Base64.getDecoder().decode(s), Charsets.UTF_8)
    private fun pose(s: String) = values(s).let { FumeCameraPose(it[0],it[1],it[2],it[3],it[4],it[5],it[6],it[7],it[8]) }
    private fun fields(p: FumeCameraPose) = listOf(p.x,p.y,p.vx,p.vy,p.fx,p.fy,p.scale,p.vs,p.fs)
    private fun view(s: String) = values(s).let { FumeCameraView(it[0],it[1],it[2]) }

    @Test fun sameOriginalIncomingStateSeparatesLocalOperatorAndReconstructionResiduals() {
        val out=File("../build/verification/fume-handoff").apply { mkdirs() }
        val report=mutableListOf("# case rate from time original9 continuous9 currentPhase currentBranch anticipatedPhase anticipatedBranch forcing3 derivative9 cap")
        var operatorPeak=0.0;var targetPeak=0.0
        for(seed in rows("seeds")) {
            val id=seed[0];val start=seed[3].toDouble();val width=seed[1].toDouble()
            val blocks=rows("blocks").filter { it[0]==id }.map { f ->
                val words=f[13].takeIf { it!="-" }?.split('|')?.map { w ->
                    w.split('~').let { FumeWord(text(it[0]),it[1].toDouble(),it[2].toDouble()) }
                }.orEmpty()
                FumeArticleBlock(f[1].toInt(),FumeLyric(text(f[2]),f[3].toDouble(),f[4].toDouble(),words),f[5].toBoolean(),
                    f[6].toDouble(),f[7].toDouble(),f[8].toDouble(),f[9].toDouble(),f[10].toDouble(),f[11].toDouble(),f[12].toDouble(),
                    rows("rows").count { it[0]==id && it[1]==f[1] })
            }
            val a=rows("articles").single { it[0]==id };val b=values(a[6])
            val article=FumeArticle(a[1].toDouble(),a[2].toDouble(),a[3].toDouble(),a[4].toInt(),a[5].toDouble(),1.0,FumeBounds(b[0],b[1],b[2],b[3]),blocks)
            val geometry=blocks.associate { block -> block.sourceIndex to FumeFocusBlock(block,rows("rows")
                .filter { it[0]==id && it[1].toInt()==block.sourceIndex }.map { f ->
                    FumeFocusRow(f[2].toInt(),f[3].toInt(),f[4].toDouble(),f[5].toDouble(),f[6].toDouble(),values(f[7]))
                }) }
            val initial=FumeCameraState(pose(seed[4]),FumeRetarget(seed[5].toInt(),seed[6].toDouble(),seed[7].toDouble(),view(seed[8]),seed[9],view(seed[10]),seed[11].toDouble()))
            val config=FumeCameraTrackConfig(FumeViewport(width,600.0),seed[12].toDouble(),stepped=seed[2].toBoolean())
            val track=FumeCameraTrack(article,geometry,listOf(FumeFocusEpoch(start,initial.retarget.source)),config,start+.25)
            val integrate=FumeCameraTrack::class.java.declaredMethods.single { it.name=="integrate" }.apply { isAccessible=true }
            val anticipate=FumeCameraTrack::class.java.declaredMethods.single { it.name=="anticipated" }.apply { isAccessible=true }
            val derivative=FumeCameraTrack::class.java.declaredMethods.single { it.name=="derivative" }.apply { isAccessible=true }
            fun reconstruct(t:Double)=integrate.invoke(track,initial,start,t,initial.retarget.source,false,{ _:Double,_:FumeCameraState -> Unit }) as FumeCameraState
            for(row in rows("steps").filter { it[0]==id }) {
                val t=row[3].toDouble();val before=initial.copy(pose=pose(row[5]))
                val input=FumeCameraInput(t,t,row[4].toDouble(),initial.retarget.source,config.viewport,fumeOverview(article,config.viewport),config.overviewStart,
                    stepped=config.stepped)
                val intent=fumeCameraIntent(before,article,geometry,input)
                val expectedTarget=values(row[9]);val target=listOf(intent.target.x,intent.target.y,intent.target.scale)
                for(i in target.indices) { targetPeak=max(targetPeak,abs(target[i]-expectedTarget[i]));assertEquals(expectedTarget[i],target[i],1e-9,"original target $id/$t/$i") }
                assertEquals(row[8],if(intent.bridgeTarget!=null)"bridge" else "spring","original current phase $id/$t")
                val result=fumeCameraStep(before,article,geometry,input);val original=values(row[6])
                for((i,v) in fields(result.state.pose).withIndex()) { operatorPeak=max(operatorPeak,abs(v-original[i]));assertEquals(original[i],v,1e-9,"original finite operator $id/$t/$i") }
                val continuous=reconstruct(t)
                assertEquals(continuous,reconstruct(t),"independent paused reconstruction")
                val current=fumeCameraIntent(continuous,article,geometry,input.copy(dt=0.0))
                val future=anticipate.invoke(track,continuous,t,initial.retarget.source,false) as FumeCameraIntent
                val d=derivative.invoke(track,continuous,t,initial.retarget.source,false) as FumeCameraPose
                val q=if(future.bridgeTarget==null)future.target else {
                    val lead=1/future.bridgeRate-(1.0/60)/expm1(future.bridgeRate/60)
                    fumeCameraIntent(continuous,article,geometry,input.copy(time=t+lead,floatTime=t+lead)).bridgeTarget?:future.bridgeTarget
                }
                val cap=1320+( (hypot(future.target.x-continuous.pose.x,future.target.y-continuous.pose.y)/max(future.retarget.duration*.28,.028)).coerceIn(2600.0,8800.0)-1320)*future.boost
                report+=listOf(id,row[1],row[2],t,row[6],fields(continuous.pose).joinToString(","),current.phase,
                    if(current.bridgeTarget!=null)"bridge" else "spring",future.phase,if(future.bridgeTarget!=null)"bridge" else "spring",
                    listOf(q.x,q.y,q.scale).joinToString(","),fields(d).joinToString(","),cap).joinToString("\t")
            }
        }
        out.resolve("continuous.tsv").writeText(report.joinToString("\n")+"\n")
        println("original finite operator peak=$operatorPeak target peak=$targetPeak; continuous residual is diagnostic, NOT accepted equivalence")
    }
}
