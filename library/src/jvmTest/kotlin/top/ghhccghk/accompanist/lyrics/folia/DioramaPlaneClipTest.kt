package top.ghhccghk.accompanist.lyrics.folia

import top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama.*
import kotlin.test.*

class DioramaPlaneClipTest {
    private val view=DioramaPerspective(DioramaVec(0.0,0.0,0.0),DioramaVec(0.0,0.0,-1.0),800.0,600.0,near=.5,far=2.0)
    @Test fun bothDepthPlanesAndFrontSideBeforeHomogeneousDivide() {
        val x=DioramaVec(.001,0.0,0.0);val y=DioramaVec(0.0,-.001,0.0)
        val front=dioramaClipPlane(view,DioramaVec(-.5,.1,-1.0),x,y,1000.0,200.0)
        assertFalse(front.needsClip);assertEquals(4,front.points.size)
        for(z in listOf(-.2,-2.2,.2))assertTrue(dioramaClipPlane(view,DioramaVec(-.5,.1,z),x,y,1000.0,200.0).points.isEmpty())
        assertTrue(dioramaClipPlane(view,DioramaVec(.5,.1,-1.0),x * -1.0,y,1000.0,200.0).points.isEmpty(),"back face")
        val tilted=DioramaVec(.001,0.0,-.003)
        val origin=DioramaVec(-.5,.1,-.1)
        val clip=dioramaClipPlane(view,origin,tilted,y,1000.0,200.0)
        assertTrue(clip.needsClip);assertEquals(4,clip.points.size)
        assertEquals((.5-.1)/.003,clip.points.minOf{it.x},1e-10)
        assertEquals((2.0-.1)/.003,clip.points.maxOf{it.x},1e-10)
        for(p in clip.points) {
            val world=origin+tilted*p.x+y*p.y;val depth=(world-view.eye).dot(view.forward)
            assertTrue(depth>=view.near-1e-12&&depth<=view.far+1e-12)
            val projected=view.project(world)
            assertTrue(projected.x.isFinite()&&projected.y.isFinite())
        }
    }
}
