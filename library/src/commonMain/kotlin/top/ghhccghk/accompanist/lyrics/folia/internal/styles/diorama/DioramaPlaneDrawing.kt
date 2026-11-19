// SPDX-License-Identifier: AGPL-3.0-only
// Independent planar near/far clipping adapter for Folia's FrontSide text planes.
// This is not a depth buffer or general 3D renderer. docs/upstream.md.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama

import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*

internal data class DioramaTextPlane(
    val image: ImageBitmap,
    val origin: DioramaVec,
    val x: DioramaVec,
    val y: DioramaVec,
    val alpha: Float,
    val color: Color,
    val additive: Boolean,
    val fog: DioramaFog?,
    val mipMaterial: DioramaLinearColor? = null,
)

internal data class DioramaPlanePoint(val x: Double, val y: Double)
internal data class DioramaPlaneClip(val points: List<DioramaPlanePoint>, val needsClip: Boolean)

/** Texture coordinates have downward Y; their front normal is -cross(x,y).
 * Clip in DOUBLE plane coordinates before any projective float matrix. */
internal fun dioramaClipPlane(view: DioramaPerspective, origin: DioramaVec,
    x: DioramaVec, y: DioramaVec, width: Double, height: Double): DioramaPlaneClip {
    require(width.isFinite() && height.isFinite() && width>0 && height>0)
    val hidden=DioramaPlaneClip(emptyList(),true)
    if(x.cross(y).dot(origin-view.eye)<=0)return hidden
    val z0=(origin-view.eye).dot(view.forward)
    val zx=x.dot(view.forward);val zy=y.dot(view.forward)
    var points=listOf(DioramaPlanePoint(0.0,0.0),DioramaPlanePoint(width,0.0),
        DioramaPlanePoint(width,height),DioramaPlanePoint(0.0,height))
    fun depth(p:DioramaPlanePoint)=z0+zx*p.x+zy*p.y
    if(points.all{depth(it)>=view.near&&depth(it)<=view.far})return DioramaPlaneClip(points,false)
    for(near in listOf(true,false)) {
        if(points.isEmpty())return hidden
        fun distance(p:DioramaPlanePoint)=if(near)depth(p)-view.near else view.far-depth(p)
        val result=ArrayList<DioramaPlanePoint>(6)
        var a=points.last();var da=distance(a)
        for(b in points) {
            val db=distance(b)
            if((da>=0)!=(db>=0)) {
                val t=da/(da-db)
                result+=DioramaPlanePoint(a.x+(b.x-a.x)*t,a.y+(b.y-a.y)*t)
            }
            if(db>=0)result+=b
            a=b;da=db
        }
        points=result
    }
    return DioramaPlaneClip(points,true)
}

/** Shared by full-line and active base/halo/soul planes. Fully visible planes
 * retain the old draw path exactly, avoiding a gratuitous extra raster clip. */
internal fun DrawScope.drawDioramaPlane(image: ImageBitmap, view: DioramaPerspective,
    origin: DioramaVec, x: DioramaVec, y: DioramaVec, alpha: Float = 1f,
    color: Color = Color.White, additive: Boolean = false, fog: DioramaFog? = null) {
    if(alpha<=0)return
    withDioramaPlane(view,origin,x,y,image.width.toDouble(),image.height.toDouble()) {
        if(fog==null)drawImage(image,alpha=alpha,colorFilter=ColorFilter.tint(color),
            blendMode=if(additive)BlendMode.Plus else BlendMode.SrcOver)
        else drawDioramaFogMask(image,view,origin,x,y,alpha,color,additive,fog)
    }
}

/** Same FrontSide/double near-far clip and Canvas transform for mask and
 * fragment bodies. Shader sampling does not bypass parent Canvas state. */
internal fun DrawScope.withDioramaPlane(view:DioramaPerspective,origin:DioramaVec,
    x:DioramaVec,y:DioramaVec,width:Double,height:Double,body:DrawScope.()->Unit) {
    val clip=dioramaClipPlane(view,origin,x,y,width,height)
    if(clip.points.size<3)return
    val h=view.planeHomography(origin,x,y)
    val matrix=Matrix().apply {
        this[0,0]=h[0].toFloat();this[1,0]=h[1].toFloat();this[3,0]=h[2].toFloat()
        this[0,1]=h[3].toFloat();this[1,1]=h[4].toFloat();this[3,1]=h[5].toFloat()
        this[0,3]=h[6].toFloat();this[1,3]=h[7].toFloat();this[3,3]=h[8].toFloat()
    }
    withTransform({transform(matrix)}) {
        if(!clip.needsClip)body()else {
            val path=Path().apply {
                moveTo(clip.points.first().x.toFloat(),clip.points.first().y.toFloat())
                clip.points.drop(1).forEach{lineTo(it.x.toFloat(),it.y.toFloat())}
                close()
            }
            clipPath(path){body()}
        }
    }
}
