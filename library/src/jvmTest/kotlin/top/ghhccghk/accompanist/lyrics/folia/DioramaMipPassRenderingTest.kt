// SPDX-License-Identifier: AGPL-3.0-only
package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.*
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama.*
import kotlin.math.*
import kotlin.test.*

class DioramaMipPassRenderingTest {
    @Test fun coloredAlphaRasterAcrossSixLodBoundaryKeepsOldExactPixels() {
        val base=ImageBitmap(1025,513)
        CanvasDrawScope().draw(Density(1f),LayoutDirection.Ltr,Canvas(base),Size(1025f,513f)) {
            for(i in 0..7)drawRect(Color((i%3)/2f,(i%5)/4f,1f-i/8f,(i+1)/8f),
                topLeft=Offset(i*129f,0f),size=Size(129f,513f))
        }
        val reference=DioramaSinglePassControl(base)
        val production=SkiaDioramaPlainSampler(base)
        val view=DioramaPerspective(DioramaVec(0.0,0.0,10.0),DioramaVec(0.0,0.0,0.0),256.0,256.0)
        try {
            for(lod in listOf(0.0,3.25,5.999,6.0,6.001,6.5,7.25,9.9,10.0)) {
                val step=2*10*tan(view.fov*PI/360)/view.height*2.0.pow(-lod)
                val origin=DioramaVec(-base.width*step/2,base.height*step/2,0.0)
                fun pixels(sampler:DioramaPlainSampler):IntArray {
                    val target=ImageBitmap(256,256)
                    CanvasDrawScope().draw(Density(1f),LayoutDirection.Ltr,Canvas(target),Size(256f,256f)) {
                        drawRect(Color(0xff162435))
                        sampler.draw(this,view,origin,DioramaVec(step,0.0,0.0),DioramaVec(0.0,-step,0.0),
                            DioramaLinearColor(.8,.5,.3),.7f,null,DioramaToneMapping.None)
                    }
                    return IntArray(256*256).also{target.readPixels(it);target.asSkiaBitmap().close()}
                }
                val old=pixels(reference);val current=pixels(production)
                assertTrue(old.contentEquals(current),"Original single-pass exact pixels at LOD $lod")
                if(lod<=7.25)assertTrue(current.any{it!=Color(0xff162435).toArgb()},"Boundary case renders real foreground at LOD $lod")
            }
        }finally{reference.close();production.close();base.asSkiaBitmap().close()}
    }
}
