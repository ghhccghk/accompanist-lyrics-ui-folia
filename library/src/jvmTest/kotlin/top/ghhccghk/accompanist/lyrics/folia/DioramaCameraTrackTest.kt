package top.ghhccghk.accompanist.lyrics.folia

import kotlinx.coroutines.CancellationException
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama.*
import java.io.File
import kotlin.math.abs
import kotlin.test.*

class DioramaCameraTrackTest {
    private val seed=dioramaHashSeed("DioramaProbe")
    private fun subjects(): List<DioramaCameraSubject> {
        val path=buildDioramaPath(4,seed)
        return listOf(.7 to "Synthetic mirror plane",1.27 to "Second camera line").mapIndexed { i,(start,text) ->
            DioramaCameraSubject(DioramaCameraLine(DioramaLyric(text,start,start+.8)),path[i],
                dioramaTextPlacement(i,seed),if(i==0)DioramaShot.orbit else DioramaShot.glide,4.0,i)
        }
    }
    @Test fun immutableMediaQueriesPauseSeekRateIndependenceAndToleranceConvergence() {
        val subjects=subjects()
        val track=DioramaCameraTrack(subjects,380.0,600.0,4.0,seed)
        val times=listOf(0.0,.7,.999,1.269999,1.27,1.270001,1.9,2.8,3.999,4.0)
        val expected=times.associateWith { track.at(it) }
        for(fps in listOf(24,60,144)) {
            repeat(fps*4) { track.at(it.toDouble()/fps) }
            for(t in times.reversed())assertEquals(expected[t],track.at(t),"display schedule $fps t$t")
        }
        val restored=DioramaCameraTrack(subjects,380.0,600.0,4.0,seed)
        for(t in times)assertEquals(expected[t],restored.at(t),"remount t$t")
        repeat(10){assertEquals(expected[1.9],track.at(1.9),"paused")}
        assertEquals(track.at(0.0),track.at(-1.0));assertEquals(track.at(4.0),track.at(400.0))
        val finer=DioramaCameraTrack(subjects,380.0,600.0,4.0,seed,tolerance=.000005)
        var positionPeak=0.0;var quaternionPeak=0.0
        for(t in times) {
            val a=track.at(t);val b=finer.at(t);val d=a.position-b.position
            positionPeak=maxOf(positionPeak,abs(d.x),abs(d.y),abs(d.z))
            val q=a.orientation!!;val r=b.orientation!!;val sign=if(q.dot(r)<0)-1 else 1
            quaternionPeak=maxOf(quaternionPeak,abs(q.x-r.x*sign),abs(q.y-r.y*sign),abs(q.z-r.z*sign),abs(q.w-r.w*sign))
        }
        assertTrue(positionPeak<.002,"integration convergence $positionPeak")
        assertTrue(quaternionPeak<.0002,"orientation convergence $quaternionPeak")
        assertEquals(track.at(1.4,true),track.at(2.0,true),"reduced camera stays fixed within subject")
        assertFailsWith<IllegalArgumentException>{track.at(Double.NaN)}
        File("../build/verification/diorama-follow").apply{mkdirs()}.resolve("track.txt")
            .writeText("nodes=${track.preparedNodeCount} finerNodes=${finer.preparedNodeCount} convergencePosition=$positionPeak convergenceQuaternion=$quaternionPeak\nSelf determinism/convergence, not raw RAF equivalence\n")
    }
    @Test fun preparationCanBeCancelledWithoutLeavingAnAnimationTask() {
        var checks=0
        assertFailsWith<CancellationException> {
            DioramaCameraTrack(subjects(),800.0,600.0,20.0,seed,checkpoint={
                if(++checks==7)throw CancellationException("test cancellation")
            })
        }
        assertEquals(7,checks)
    }
}
