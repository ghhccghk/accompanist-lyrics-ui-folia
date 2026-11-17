package top.ghhccghk.accompanist.lyrics.folia

import top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama.*
import java.io.File
import java.util.Base64
import kotlin.math.*
import kotlin.test.*

/** Isolated capability gate, not a public-theme or screenshot acceptance. */
class DioramaProjectionTest {
    @Test fun pathPlacementAndPerspectiveAgainstOriginalFunctionsAndThree(){
        var count=0;var peak=0.0
        val frames=mutableMapOf<String,List<DioramaFrame>>()
        for(line in File("../verification/diorama-fixtures/projection.tsv").readLines().filterNot{it.startsWith('#')||it.isBlank()}){
            val f=line.split('\t');val kind=f[0];val seed=String(Base64.getDecoder().decode(f[1]),Charsets.UTF_8)
            val i=f[2].toInt();val weave=f[3].toDouble();val width=f[4].toDouble()
            val args=f[5].split(',').filter{it.isNotBlank()}.map(String::toDouble);val expected=f[6].split(',').map(String::toDouble)
            val frame=frames.getOrPut(seed){buildDioramaPath(12,dioramaHashSeed(seed))}[i]
            val p=dioramaTextPlacement(i,dioramaHashSeed(seed),weave)
            fun values(v:DioramaVec)=listOf(v.x,v.y,v.z)
            val actual=when(kind){
                "frame"->values(frame.position)+values(frame.forward)+values(frame.right)+values(frame.up)
                "placement"->listOf(p.offsetR,p.offsetU,p.scale,p.roll,p.yaw,p.lookR)
                "axis"->values(dioramaPlaneAxis(frame,p,args[0],args[1]))
                "project"->{
                    val view=DioramaPerspective(DioramaVec(args[0],args[1],args[2]),DioramaVec(args[3],args[4],args[5]),width,600.0)
                    val origin=dioramaComposeLocal(frame,p.offsetR,p.offsetU,0.0)
                    val world=origin+dioramaPlaneAxis(frame,p,args[6],args[7]);val q=view.project(world)
                    assertFalse(q.clipped)
                    val h=view.planeHomography(origin,dioramaPlaneAxis(frame,p,1.0,0.0),dioramaPlaneAxis(frame,p,0.0,1.0))
                    val w=h[6]*args[6]+h[7]*args[7]+h[8]
                    assertEquals(q.x,(h[0]*args[6]+h[1]*args[7]+h[2])/w,1e-9)
                    assertEquals(q.y,(h[3]*args[6]+h[4]*args[7]+h[5])/w,1e-9)
                    listOf(q.x,q.y,q.depth)
                }
                "fit"->listOf(dioramaFrameFit(args[0],args[1],args[2],args[3]))
                "life"->listOf(dioramaTextLife(args[0]))
                else->error(kind)
            }
            for((a,b)in expected.zip(actual)){peak=max(peak,abs(a-b));assertEquals(a,b,1e-8,"$kind seed=$seed index=$i weave=$weave width=$width")}
            count++
        }
        for(path in frames.values)for(frame in path){
            assertTrue(frame.forward.z<0);assertEquals(1.0,frame.forward.dot(frame.forward),1e-12)
            assertEquals(0.0,frame.up.dot(frame.right),1e-12)
            assertTrue(frame.right.cross(frame.up).dot(frame.forward)<0,"proper facing basis")
        }
        val view=DioramaPerspective(DioramaVec(0.0,0.0,5.0),DioramaVec(0.0,0.0,0.0),800.0,600.0)
        assertTrue(view.project(DioramaVec(0.0,0.0,5.1)).clipped,"behind camera")
        assertTrue(view.project(DioramaVec(0.0,0.0,4.99)).clipped,"before near plane")
        assertTrue(view.project(DioramaVec(0.0,0.0,-1000.0)).clipped,"beyond far plane")
        val out=File("../build/verification/diorama-projection").apply{mkdirs()}
        out.resolve("numeric.txt").writeText("rows=$count maximumAbsoluteDifference=$peak\nNot CameraRig/visual/depth-composition acceptance\n")
        println("Diorama source rows=$count maxDifference=$peak")
    }
}
