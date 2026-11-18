package top.ghhccghk.accompanist.lyrics.folia

import top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama.*
import java.io.File
import java.util.Base64
import kotlin.math.*
import kotlin.test.*

class DioramaCameraFlightTest {
    @Test fun pinnedFlightAndFollowerFromSameOriginalIncomingState() {
        val records=File("../verification/diorama-fixtures/flight-v1/finite.tsv").readLines()
            .filterNot { it.startsWith('#')||it.isBlank() }.map { it.split('\t') }
        assertEquals(480,records.size)
        var peak=0.0;var anglePeak=0.0;var flyingCount=0;var snapCount=0
        val oldPath=buildDioramaPath(6,dioramaHashSeed("DioramaProbe"))
        for((case,r)in records.withIndex()) {
            fun d(i:Int)=r[i].toDouble()
            fun v(i:Int)=DioramaVec(d(i),d(i+1),d(i+2))
            fun q(i:Int)=DioramaQuaternion(d(i),d(i+1),d(i+2),d(i+3))
            fun check(expected:Double,actual:Double,label:String) {
                peak=max(peak,abs(expected-actual));assertEquals(expected,actual,1e-9,"$case $label")
            }
            val seed=dioramaHashSeed(String(Base64.getDecoder().decode(r[0]),Charsets.UTF_8))
            val epoch=r[2].toInt();val time=d(3);val index=r[5].toInt()
            val origin=oldPath[3].position+dioramaTransitionOffset(seed,epoch)
            for((i,a)in listOf(origin.x,origin.y,origin.z).withIndex())check(d(6+i),a,"origin$i")
            val frame=buildDioramaPath(3,seed)[index].let{it.copy(position=it.position+origin)}
            val previous=DioramaFollowState(v(9),v(12),v(15),v(18),d(21),
                if(d(30)==1.0)q(22)else null,if(d(31)==1.0)q(26)else null)
            val flight=DioramaCameraFlight(previous.position,v(32),seed,epoch).at(time,.42)
            val result=dioramaFollowStep(previous,DioramaCameraTarget(v(35),v(38),0.0,0.0,0.0,0.0),
                frame,dioramaTextPlacement(index,seed),DioramaShot.orbit,time,.7,3.48,d(4),.42,d(1)/600,flight=flight)
            val s=result.state
            val fields=listOf(s.position.x,s.position.y,s.position.z,s.velocity.x,s.velocity.y,s.velocity.z,
                s.look.x,s.look.y,s.look.z,s.lookVelocity.x,s.lookVelocity.y,s.lookVelocity.z,s.alignment,
                s.alignmentOrientation!!.x,s.alignmentOrientation.y,s.alignmentOrientation.z,s.alignmentOrientation.w,
                s.orientation!!.x,s.orientation.y,s.orientation.z,s.orientation.w)
            for((i,value)in fields.withIndex())check(d(41+i),value,"state$i")
            val rendered=result.renderedPosition;val orientation=result.renderedOrientation!!
            for((i,value)in listOf(rendered.x,rendered.y,rendered.z,orientation.x,orientation.y,orientation.z,orientation.w).withIndex())
                check(d(64+i),value,"rendered$i")
            assertEquals(d(71)==1.0,flight.flying,"flying$case")
            assertEquals(d(72)==1.0,result.snapped,"snap$case")
            check(d(73),flight.smoothTime,"smooth")
            check(d(76),flight.bank,"bank")
            anglePeak=max(anglePeak,abs(d(77)-result.offAxis))
            assertEquals(d(77),result.offAxis,1e-7,"ill-conditioned acos diagnostic$case")
            for((i,value)in listOf(result.targetOrientation.x,result.targetOrientation.y,result.targetOrientation.z,result.targetOrientation.w).withIndex())
                check(d(78+i),value,"targetQuaternion$i")
            if(flight.flying)flyingCount++
            if(result.snapped)snapCount++
        }
        assertTrue(flyingCount>0);assertTrue(snapCount>0)
        File("../build/verification/diorama-flight-kotlin").apply{mkdirs()}.resolve("finite.txt")
            .writeText("sourceCases=${records.size} stateAndRenderedPeak=$peak anglePeak=$anglePeak flying=$flyingCount snaps=$snapCount\n")
    }
    @Test fun visualOffsetsAreNotFeedbackAndEndpointsReleaseWithoutAHiddenFrameDelay() {
        val flight=DioramaCameraFlight(DioramaVec(0.0,.6,9.0),DioramaVec(30.0,10.0,-20.0),17.0,2)
        val first=flight.at(0.0,.42);val final=flight.at(3.2,.42)
        assertTrue(first.flying);assertFalse(final.flying)
        assertEquals(DioramaVec(0.0,0.0,0.0),first.positionOffset)
        assertEquals(DioramaVec(0.0,0.0,0.0),final.positionOffset)
        assertEquals(1.1,first.smoothTime);assertEquals(.42,final.smoothTime)
        // Source multiplication may retain IEEE -0 at a zero envelope. Test
        // exact numerical zero, not boxed Double bit identity (no tolerance).
        assertTrue(final.bank == 0.0)
        for(t in listOf(0.0,.9,1.6,3.199,3.2,4.0)) {
            val expected=flight.at(t,.42)
            flight.at(7.0,.42);flight.at(.2,.42)
            assertEquals(expected,flight.at(t,.42),"arbitrary media seek")
        }
        // Vertical and coincident flights use the ORIGINAL horizontal fallback.
        for(to in listOf(DioramaVec(0.0,10.0,0.0),DioramaVec(0.0,0.0,0.0))) {
            val value=DioramaCameraFlight(DioramaVec(0.0,0.0,0.0),to,17.0,1).at(1.6,.42)
            assertTrue(value.positionOffset.z == 0.0)
            assertTrue(value.positionOffset.x.isFinite())
        }
    }
}
