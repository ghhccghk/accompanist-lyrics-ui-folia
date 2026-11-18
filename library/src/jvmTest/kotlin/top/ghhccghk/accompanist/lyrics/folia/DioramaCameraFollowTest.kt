package top.ghhccghk.accompanist.lyrics.folia

import top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama.*
import java.io.File
import kotlin.math.*
import kotlin.test.*

class DioramaCameraFollowTest {
    @Test fun originalNormalFollowFromIdenticalOriginalIncomingState() {
        val source=File("../verification/diorama-fixtures/follow-v1/finite.tsv").readLines()
            .filterNot { it.startsWith('#')||it.isBlank() }.map { it.split('\t') }
        assertEquals(1482,source.size)
        val seed=dioramaHashSeed("DioramaProbe"); val path=buildDioramaPath(12,seed)
        fun values(s:DioramaFollowState)=listOf(s.position.x,s.position.y,s.position.z,s.velocity.x,s.velocity.y,s.velocity.z,
            s.look.x,s.look.y,s.look.z,s.lookVelocity.x,s.lookVelocity.y,s.lookVelocity.z,s.alignment,
            s.alignmentOrientation!!.x,s.alignmentOrientation.y,s.alignmentOrientation.z,s.alignmentOrientation.w,
            s.orientation!!.x,s.orientation.y,s.orientation.z,s.orientation.w)
        var statePeak=0.0; var anglePeak=0.0; var snaps=0; var limited=0
        for((row,r) in source.withIndex()) {
            fun d(i:Int)=r[i].toDouble()
            fun v(i:Int)=DioramaVec(d(i),d(i+1),d(i+2))
            fun q(i:Int)=DioramaQuaternion(d(i),d(i+1),d(i+2),d(i+3))
            val previous=DioramaFollowState(v(8),v(11),v(14),v(17),d(20),
                if(d(29)==1.0)q(21)else null,if(d(30)==1.0)q(25)else null)
            val result=dioramaFollowStep(previous,DioramaCameraTarget(v(31),v(34),0.0,0.0,0.0,0.0),
                path[r[6].toInt()],dioramaTextPlacement(r[6].toInt(),seed,1.35),DioramaShot.valueOf(r[0]),
                d(4),.7,3.48,d(5),d(7),d(1)/600)
            // The original line's last end3.1 +.06 pass +.32 exit =3.48.
            for((field,value)in values(result.state).withIndex()) {
                statePeak=max(statePeak,abs(value-d(37+field)))
                assertEquals(d(37+field),value,1e-9,"original$row field$field")
            }
            assertEquals(d(60)==1.0,result.snapped,"snap$row")
            assertEquals(d(61),result.readTarget,1e-12,"read target$row")
            anglePeak=max(anglePeak,abs(d(62)-result.offAxis))
            // acos is ill-conditioned near a zero angle. Compare source radian
            // diagnostic at1e-7; all resulting quaternion channels still1e-9.
            assertEquals(d(62),result.offAxis,1e-7,"angle diagnostic$row")
            assertEquals(d(63),result.maxOffAxis,1e-12)
            for((i,v)in listOf(result.targetOrientation.x,result.targetOrientation.y,result.targetOrientation.z,result.targetOrientation.w).withIndex())
                assertEquals(d(64+i),v,1e-9,"targetQuaternion$row/$i")
            if(result.snapped)snaps++
            if(result.offAxis>result.maxOffAxis)limited++
        }
        assertTrue(snaps>0);assertTrue(limited>0)
        File("../build/verification/diorama-follow").apply{mkdirs()}.resolve("finite.txt")
            .writeText("originalCases=${source.size} statePeak=$statePeak anglePeak=$anglePeak snaps=$snaps frameKeep=$limited\n")
    }
}
