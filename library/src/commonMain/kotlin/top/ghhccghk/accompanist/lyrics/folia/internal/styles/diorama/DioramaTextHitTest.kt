// SPDX-License-Identifier: AGPL-3.0-only
// Independent inverse of the actual text plane homography. Source geometry:
// Folia cc6226e4 dioramaSceneLayout.ts; adaptation: docs/upstream.md.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import kotlin.math.*

/** Base ink only: transparent raster padding, glow and distant fog do not
 * capture gestures. Uses FrontSide, near/far and the same actual view as draw. */
internal fun dioramaPlaneInkHit(image:ImageBitmap,view:DioramaPerspective,
    origin:DioramaVec,x:DioramaVec,y:DioramaVec,point:Offset,fog:DioramaFog?=null):Boolean {
    if(point.x<0||point.y<0||point.x>=view.width||point.y>=view.height)return false
    if(x.cross(y).dot(origin-view.eye)<=0)return false
    val h=view.planeHomography(origin,x,y)
    val a=h[0]-point.x*h[6];val b=h[1]-point.x*h[7];val c=point.x*h[8]-h[2]
    val d=h[3]-point.y*h[6];val e=h[4]-point.y*h[7];val f=point.y*h[8]-h[5]
    val determinant=a*e-b*d
    if(!determinant.isFinite()||determinant==0.0)return false
    val u=(c*e-b*f)/determinant;val v=(a*f-c*d)/determinant
    if(!u.isFinite()||!v.isFinite()||u<0||v<0||u>=image.width||v>=image.height)return false
    val depth=(origin+x*u+y*v-view.eye).dot(view.forward)
    if(depth<view.near||depth>view.far||fog?.factor(depth)==1.0)return false
    val pixel=IntArray(1)
    image.readPixels(pixel,startX=floor(u).toInt(),startY=floor(v).toInt(),width=1,height=1)
    return pixel[0].ushr(24)>0
}

/** Return the original playback-line index, including real accompaniment refs.
 * Mirrors renderOrder: active BASE is in front of neighbour painter order.
 * No synthetic current-line full-stage hit area is installed. */
internal fun DioramaTextPlayback.hitTest(time:Double,view:DioramaPerspective,point:Offset,
    reducedMotion:Boolean=false,fog:DioramaFog?=null):Int? {
    val i=camera.subjectIndex(time);val subject=camera.subjects[i];val active=lines[i].text
    val center=dioramaComposeLocal(subject.frame,subject.placement.offsetR,subject.placement.offsetU,0.0)
    val life=dioramaTextLife(sqrt((center-view.eye).let{it.dot(it)}))
    val white=DioramaLinearColor(1.0,1.0,1.0)
    val states=active.framesAt(time,white,white,life=life,breath=breathAt(time,reducedMotion=reducedMotion))
    val fit=dioramaFrameFit(active.lineAdvance*.62/128,5.2,view.fov,view.width/view.height)*subject.placement.scale
    val x=dioramaPlaneAxis(subject.frame,subject.placement,1.0,0.0)*(fit*.62/128)
    val up=dioramaPlaneAxis(subject.frame,subject.placement,0.0,1.0)*(fit*.62/128)
    for(n in active.slices.indices.reversed())if(states[n].baseOpacity>0) {
        val slice=active.slices[n];val image=slice.body
        val origin=center+x*(slice.centerPx+slice.imageOffsetPx)-x*(image.width/2.0)+up*(image.height/2.0)
        if(dioramaPlaneInkHit(image,view,origin,x,up*-1.0,point,fog))return i
    }
    for(neighbor in neighborFramesAt(time,view,white).asReversed())if(neighbor.opacity>0&&neighbor.index>=globalStart) {
        val j=neighbor.index-globalStart;val s=camera.subjects[j];val text=lines[j].neighbor
        val px=dioramaPlaneAxis(s.frame,s.placement,1.0,0.0)*(neighbor.fit*.62/text.fontPx)
        val py=dioramaPlaneAxis(s.frame,s.placement,0.0,1.0)*(neighbor.fit*.62/text.fontPx)
        val origin=neighbor.center-px*(text.image.width/2.0)+py*(text.image.height/2.0)
        if(dioramaPlaneInkHit(text.image,view,origin,px,py*-1.0,point,fog))return j
    }
    return null
}
