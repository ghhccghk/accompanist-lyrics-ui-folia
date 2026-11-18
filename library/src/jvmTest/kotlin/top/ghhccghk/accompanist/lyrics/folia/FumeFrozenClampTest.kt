package top.ghhccghk.accompanist.lyrics.folia

import top.ghhccghk.accompanist.lyrics.folia.internal.styles.fume.*
import java.io.File
import kotlin.math.*
import kotlin.test.*

/** Controlled frozen source operator, not a production camera replacement. */
class FumeFrozenClampTest {
    private fun rows(name:String)=File("../verification/fume-fixtures/frozen-clamp-v1/$name.tsv").readLines()
        .filterNot{it.startsWith('#')||it.isBlank()}.map{it.split('\t')}
    private fun pose(v:List<Double>)=FumeCameraPose(v[0],v[1],v[2],v[3],v[4],v[5],v[6],v[7],v[8])
    private fun values(p:FumeCameraPose)=listOf(p.x,p.y,p.vx,p.vy,p.fx,p.fy,p.scale,p.vs,p.fs)

    @Test fun originalFullStateSaturationAndFinitePartitionDefect() {
        val rows=rows("sweep");assertEquals(756,rows.size)
        val out=File("../build/verification/fume-frozen-clamp").apply{mkdirs()}
        val diagnostics=mutableListOf("# case axis sign delta dt admissible original9 log3 projected3 refined3")
        var finitePeak=0.0;var projectedPositionPeak=0.0;var convergencePeak=0.0;var admitted=0
        var uncappedCalibrationPeak=0.0;var uncappedCalibrationCount=0
        var cappedCalibrationPositionPeak=0.0;var cappedCalibrationCount=0
        var cappedCalibrationRefinement=0.0
        val xy=FumeSpringResponse(208.0,24.0,11.2)
        val scale=FumeSpringResponse(54.0,13.5,5.4)
        // Same frozen projected-log vector field as current CameraTrack axis(),
        // including accepted velocity and scale bounds. Diagnostic only. 512/
        // 1024 steps are quadrature refinement, not a playback frame scheduler.
        fun projected(model:FumeSpringResponse,initial:FumeSpringState,target:Double,dt:Double,
            cap:Double,isScale:Boolean,steps:Int):FumeSpringState {
            fun derivative(s:FumeSpringState):FumeSpringState {
                val d=model.derivative(s.position,s.velocity,s.focus,target)
                return when {
                    s.velocity>=cap&&d.velocity>0->d.copy(position=cap,velocity=0.0)
                    s.velocity<=-cap&&d.velocity<0->d.copy(position=-cap,velocity=0.0)
                    else->d
                }
            }
            fun plus(s:FumeSpringState,d:FumeSpringState,h:Double)=FumeSpringState(s.position+d.position*h,s.velocity+d.velocity*h,s.focus+d.focus*h)
            val h=dt/steps;var s=initial
            repeat(steps) {
                val a=derivative(s);val b=derivative(plus(s,a,h/2));val c=derivative(plus(s,b,h/2));val d=derivative(plus(s,c,h))
                fun avg(a:Double,b:Double,c:Double,d:Double)=(a+2*b+2*c+d)*h/6
                s=FumeSpringState(s.position+avg(a.position,b.position,c.position,d.position),
                    (s.velocity+avg(a.velocity,b.velocity,c.velocity,d.velocity)).coerceIn(-cap,cap),
                    s.focus+avg(a.focus,b.focus,c.focus,d.focus))
                if(isScale)s=s.copy(position=s.position.coerceIn(.22,2.24))
            }
            return s
        }
        for((index,r)in rows.withIndex()) {
            val dt=r[4].toDouble();val before=pose(r.subList(12,21).map(String::toDouble));val target=r.subList(9,12).map(String::toDouble)
            assertEquals(0.0,r[6].toDouble(),"first original spring phase=1")
            fun axis(x:Double,v:Double,f:Double,t:Double,k:Double,d:Double,rate:Double,cap:Double):FumeSpringState {
                val focus=f+(t-f)*(1-exp(-dt*rate))
                val velocity=(v+((focus-x)*k-v*d)*dt).coerceIn(-cap,cap)
                return FumeSpringState(x+velocity*dt,velocity,focus)
            }
            val x=axis(before.x,before.vx,before.fx,target[0],208.0,24.0,11.2,1320.0)
            val y=axis(before.y,before.vy,before.fy,target[1],208.0,24.0,11.2,1320.0)
            val s=axis(before.scale,before.vs,before.fs,target[2],54.0,13.5,5.4,1.6)
            val finite=FumeCameraPose(x.position,y.position,x.velocity,y.velocity,x.focus,y.focus,s.position.coerceIn(.22,2.24),s.velocity,s.focus)
            for((i,v)in values(finite).withIndex()) {
                val expected=r[21+i].toDouble();finitePeak=max(finitePeak,abs(v-expected))
                assertEquals(expected,v,1e-9,"frozen original$index field$i")
            }
            val selected=when(r[1]){"x"->FumeSpringState(before.x,before.vx,before.fx);"y"->FumeSpringState(before.y,before.vy,before.fy);else->FumeSpringState(before.scale,before.vs,before.fs)}
            val expected=when(r[1]){"x"->x;"y"->y;else->s}
            val isScale=r[1]=="scale";val ti=when(r[1]){"x"->0;"y"->1;else->2};val cap=r[7].toDouble();val model=if(isScale)scale else xy
            val log=model.advance(selected,target[ti],dt)
            if(abs(dt-1.0/60)<1e-12 && abs(expected.velocity)<cap-1e-6 &&
                (!isScale || expected.position in .220001..2.239999)) {
                for((actual,want)in listOf(log.position to expected.position,
                    log.velocity to expected.velocity,log.focus to expected.focus)) {
                    uncappedCalibrationPeak=max(uncappedCalibrationPeak,abs(actual-want))
                    assertEquals(want,actual,1e-9,"unclamped calibrated source map $index")
                }
                uncappedCalibrationCount++
            }
            val admissible=abs(selected.velocity)<=cap
            var details="-\t-"
            if(admissible) {
                admitted++
                val p=projected(model,selected,target[ti],dt,cap,isScale,512)
                val q=projected(model,selected,target[ti],dt,cap,isScale,1024)
                projectedPositionPeak=max(projectedPositionPeak,abs(q.position-expected.position))
                convergencePeak=max(convergencePeak,abs(q.position-p.position))
                if(abs(dt-1.0/60)<1e-12 && abs(expected.velocity)==cap) {
                    cappedCalibrationPositionPeak=max(cappedCalibrationPositionPeak,abs(q.position-expected.position))
                    cappedCalibrationRefinement=max(cappedCalibrationRefinement,abs(q.position-p.position))
                    cappedCalibrationCount++
                }
                details="${p.position},${p.velocity},${p.focus}\t${q.position},${q.velocity},${q.focus}"
            }
            diagnostics+=listOf(r[0],r[1],r[2],r[3],dt,admissible,r.subList(21,30).joinToString(","),
                "${log.position},${log.velocity},${log.focus}",details).joinToString("\t")
        }
        // Independent source rows show velocity intervals mapped to exactly
        // the SAME FULL nine-state result, not just a terminal capped velocity.
        val groups=rows.groupBy{listOf(it[0],it[1],it[2],it[4])}
        var plateaus=0
        for(group in groups.values) {
            val sign=group.first()[2].toInt()
            val a=group.single{it[3].toDouble()==sign*.1}
            val b=group.single{it[3].toDouble()==sign*.001}
            assertNotEquals(a.subList(12,21),b.subList(12,21))
            assertEquals(a.subList(21,30).map(String::toDouble),b.subList(21,30).map(String::toDouble),"source full-state plateau")
            plateaus++
        }
        assertEquals(108,plateaus)
        assertTrue(uncappedCalibrationCount>0,"exercise the independent unsaturated control")
        assertTrue(cappedCalibrationCount>0,"exercise admissible source clamp at calibrated duration")
        val partitions=rows("partitions").groupBy{it[0]}
        var partitionPeak=0.0
        for(group in partitions.values) {
            val one=group.single{it[1]=="1"}.drop(3).map(String::toDouble)
            val two=group.single{it[1]=="2"}.drop(3).map(String::toDouble)
            partitionPeak=max(partitionPeak,abs(one[0]-two[0]))
        }
        assertTrue(partitionPeak>0,"raw finite source is NOT a continuous semigroup")
        out.resolve("diagnostic.tsv").writeText(diagnostics.joinToString("\n")+"\n")
        out.resolve("summary.txt").writeText("sourceCases=${rows.size} finitePeak=$finitePeak fullStatePlateaus=$plateaus admissible=$admitted projectedPositionResidual=$projectedPositionPeak refinementPositionDifference=$convergencePeak rawOneVsTwoPosition=$partitionPeak uncappedCalibrationCount=$uncappedCalibrationCount uncappedCalibrationPeak=$uncappedCalibrationPeak cappedCalibrationCount=$cappedCalibrationCount cappedCalibrationPositionResidual=$cappedCalibrationPositionPeak cappedCalibrationRefinement=$cappedCalibrationRefinement\nDiagnostic only; no production repair or visual equivalence\n")
    }
}
