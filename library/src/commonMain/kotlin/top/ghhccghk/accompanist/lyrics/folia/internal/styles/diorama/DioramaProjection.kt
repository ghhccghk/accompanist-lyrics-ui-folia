// Copyright (c) 2026 chthollyphile and contributors
// SPDX-License-Identifier: AGPL-3.0-only
// Path/placement port: Folia cc6226e4 cameraPath.ts. Independent perspective
// plane capability probe; NOT a public Diorama implementation. docs/upstream.md.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama

import kotlin.math.*

internal data class DioramaVec(val x:Double,val y:Double,val z:Double){
    operator fun plus(b:DioramaVec)=DioramaVec(x+b.x,y+b.y,z+b.z)
    operator fun minus(b:DioramaVec)=DioramaVec(x-b.x,y-b.y,z-b.z)
    operator fun times(s:Double)=DioramaVec(x*s,y*s,z*s)
    fun dot(b:DioramaVec)=x*b.x+y*b.y+z*b.z
    fun cross(b:DioramaVec)=DioramaVec(y*b.z-z*b.y,z*b.x-x*b.z,x*b.y-y*b.x)
    fun normalized():DioramaVec{val d=sqrt(dot(this)).takeIf{it!=0.0}?:1.0;return this*(1/d)}
}
internal data class DioramaFrame(val position:DioramaVec,val forward:DioramaVec,val right:DioramaVec,val up:DioramaVec)
internal data class DioramaPlacement(val offsetR:Double,val offsetU:Double,val scale:Double,val roll:Double,val yaw:Double,val lookR:Double)
internal fun dioramaHashSeed(seed:String):Double{
    var hash=0;for(c in seed)hash=(hash*31+c.code)%100000
    return (if(hash==0)1 else hash).toDouble()
}
internal fun dioramaSeededUnit(seed:Double):Double{val x=sin(seed*12.9898)*43758.5453;return x-floor(x)}
internal fun buildDioramaPath(count:Int,seed:Double):List<DioramaFrame>{
    val n=max(count,1);val positions=ArrayList<DioramaVec>(n+1);var pos=DioramaVec(0.0,0.0,0.0)
    repeat(n+1){i->
        positions+=pos
        val yaw=.5*(sin(i*.23+seed*.017)*.62+sin(i*.11+seed*.041+1.3)*.38)
        val pitch=.32*sin(i*.17+seed*.029+.7);val cp=cos(pitch)
        pos+=DioramaVec(sin(yaw)*cp,sin(pitch),-cos(yaw)*cp)*8.0
    }
    return (0 until n).map{i->
        val f=(positions[i+1]-positions[i]).normalized();val r=f.cross(DioramaVec(0.0,1.0,0.0)).normalized()
        DioramaFrame(positions[i],f,r,r.cross(f).normalized())
    }
}
internal fun dioramaTextPlacement(index:Int,seed:Double,weave:Double=1.0):DioramaPlacement{
    val i=max(index,0)
    fun r(k:Int)=dioramaSeededUnit(seed+i*11+k)
    return DioramaPlacement((sin(i*.47+seed*.019)*.7+(r(2)-.5)*.6)*1.8*weave,
        (sin(i*.29+seed*.027+.9)*.7+(r(4)-.5)*.6)*1.2*weave,.82+r(5)*.46,
        (r(6)-.5)*.4*weave,(r(7)-.5)*.32*weave,(r(8)-.5)*2.2*weave)
}
internal fun dioramaComposeLocal(f:DioramaFrame,right:Double,up:Double,depth:Double)=
    f.position+f.right*right+f.up*up+f.forward*depth

/** Same orientation as frameQuaternion: basis * yaw(Y) * roll(Z).
 * Input is a local plane coordinate, +Y up; no camera-facing billboarding. */
internal fun dioramaPlaneAxis(f:DioramaFrame,p:DioramaPlacement,x:Double,y:Double):DioramaVec{
    val rx=cos(p.roll)*x-sin(p.roll)*y;val ry=sin(p.roll)*x+cos(p.roll)*y
    return f.right*(cos(p.yaw)*rx)+f.up*ry+f.forward*(sin(p.yaw)*rx)
}
internal fun dioramaFrameFit(width:Double,distance:Double,fov:Double,aspect:Double):Double=
    if(width<=0||distance<=0)1.0 else (2*distance*tan(fov*PI/360)*aspect*.72/width).coerceIn(.28,1.0)
internal fun dioramaTextLife(distance:Double):Double{
    fun smooth(v:Double):Double{val t=v.coerceIn(0.0,1.0);return t*t*(3-2*t)}
    return smooth((40-distance)/8)*smooth((distance-.9)/1.1)
}

internal data class DioramaProjectedPoint(val x:Double,val y:Double,val depth:Double,val clipped:Boolean)

/** Explicit frozen view, deliberately separate from the unported CameraRig's
 * shot language, damping, quaternion followers and cross-song flight. */
internal class DioramaPerspective(val eye:DioramaVec,look:DioramaVec,val width:Double,val height:Double,
    val fov:Double=55.0,val near:Double=.1,val far:Double=1000.0){
    init{require(width>0&&height>0&&fov>0&&fov<180&&near>0&&far>near);require(eye!=look)}
    val forward=(look-eye).normalized()
    val right=forward.cross(DioramaVec(0.0,1.0,0.0)).normalized()
    val up=right.cross(forward).normalized()
    private val focal=height/(2*tan(fov*PI/360))
    fun project(world:DioramaVec):DioramaProjectedPoint{
        val d=world-eye;val z=d.dot(forward)
        return DioramaProjectedPoint(width/2+focal*d.dot(right)/z,height/2-focal*d.dot(up)/z,z,z<near||z>far)
    }
    /** Row-major 3x3 homography from an actual rotated world plane to pixels.
     * Caller must clip near/far crossings before drawing; a 2D painter sort
     * alone does not implement depth-testing against arbitrary scene meshes. */
    fun planeHomography(origin:DioramaVec,x:DioramaVec,y:DioramaVec):List<Double>{
        val o=origin-eye
        fun a(v:DioramaVec)=width/2*v.dot(forward)+focal*v.dot(right)
        fun b(v:DioramaVec)=height/2*v.dot(forward)-focal*v.dot(up)
        return listOf(a(x),a(y),a(o),b(x),b(y),b(o),x.dot(forward),y.dot(forward),o.dot(forward))
    }
}
