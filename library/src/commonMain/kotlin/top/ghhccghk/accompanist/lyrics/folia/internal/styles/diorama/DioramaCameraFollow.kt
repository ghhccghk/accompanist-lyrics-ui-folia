// Copyright (c) 2026 chthollyphile and contributors
// SPDX-License-Identifier: AGPL-3.0-only
// Folia cc6226e4 CameraRig.tsx normal follow/alignment/frame-keep; cameraPath smoothDamp.
// Quaternion algebra adapted from Three.js0.185.1 math/Quaternion.js, Matrix4.js,
// Vector3.js, Copyright (c) 2010-2026 three.js authors, MIT (LICENSES/MIT-three-0.185.1.txt).
// Optional text-flight offsets; no background/audio or Three runtime dependency.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama

import kotlin.math.*

internal data class DioramaQuaternion(val x: Double, val y: Double, val z: Double, val w: Double) {
    operator fun times(b: DioramaQuaternion) = DioramaQuaternion(
        x*b.w+w*b.x+y*b.z-z*b.y, y*b.w+w*b.y+z*b.x-x*b.z,
        z*b.w+w*b.z+x*b.y-y*b.x, w*b.w-x*b.x-y*b.y-z*b.z)
    fun dot(b: DioramaQuaternion) = x*b.x+y*b.y+z*b.z+w*b.w
    fun normalized(): DioramaQuaternion {
        val n = sqrt(dot(this))
        return if (n == 0.0) Identity else DioramaQuaternion(x/n,y/n,z/n,w/n)
    }
    fun rotate(v: DioramaVec): DioramaVec {
        val q = DioramaVec(x,y,z); val t = q.cross(v)*2.0
        return v+t*w+q.cross(t)
    }
    fun slerp(other: DioramaQuaternion, t: Double): DioramaQuaternion {
        var b = other; var c = dot(b)
        if (c < 0) { b = DioramaQuaternion(-b.x,-b.y,-b.z,-b.w); c = -c }
        fun mix(a: Double, b: Double) = a*(1-t)+b*t
        // Use the PINNED Three version's small-angle branch, not an older
        // implementation's machine-epsilon branch (finite fixture catches it).
        if (c >= .9995)
            return DioramaQuaternion(mix(x,b.x),mix(y,b.y),mix(z,b.z),mix(w,b.w)).normalized()
        val a = acos(c); val s = sin(a)
        val l = sin((1-t)*a)/s; val r = sin(t*a)/s
        return DioramaQuaternion(x*l+b.x*r,y*l+b.y*r,z*l+b.z*r,w*l+b.w*r)
    }
    companion object {
        val Identity = DioramaQuaternion(0.0,0.0,0.0,1.0)
        fun axis(axis: DioramaVec, angle: Double): DioramaQuaternion {
            val s = sin(angle/2)
            return DioramaQuaternion(axis.x*s,axis.y*s,axis.z*s,cos(angle/2))
        }
        /** Columns of an orthonormal right-handed rotation matrix. */
        fun basis(x: DioramaVec, y: DioramaVec, z: DioramaVec): DioramaQuaternion {
            val trace = x.x+y.y+z.z
            return if (trace > 0) {
                val s = .5/sqrt(trace+1)
                DioramaQuaternion((y.z-z.y)*s,(z.x-x.z)*s,(x.y-y.x)*s,.25/s)
            } else if (x.x > y.y && x.x > z.z) {
                val s = 2*sqrt(1+x.x-y.y-z.z)
                DioramaQuaternion(.25*s,(y.x+x.y)/s,(z.x+x.z)/s,(y.z-z.y)/s)
            } else if (y.y > z.z) {
                val s = 2*sqrt(1+y.y-x.x-z.z)
                DioramaQuaternion((y.x+x.y)/s,.25*s,(z.y+y.z)/s,(z.x-x.z)/s)
            } else {
                val s = 2*sqrt(1+z.z-x.x-y.y)
                DioramaQuaternion((z.x+x.z)/s,(z.y+y.z)/s,.25*s,(x.y-y.x)/s)
            }
        }
        fun look(eye: DioramaVec, aim: DioramaVec): DioramaQuaternion {
            var z = eye-aim
            if (z.dot(z) == 0.0) z = DioramaVec(0.0,0.0,1.0)
            z = z.normalized()
            val worldUp = DioramaVec(0.0,1.0,0.0)
            var x = worldUp.cross(z)
            if (x.dot(x) == 0.0) {
                z = DioramaVec(z.x,z.y,z.z+.0001).normalized()
                x = worldUp.cross(z)
            }
            x = x.normalized()
            return basis(x,z.cross(x),z)
        }
    }
}

internal data class DioramaFollowState(
    val position: DioramaVec = DioramaVec(0.0,.6,9.0),
    val velocity: DioramaVec = DioramaVec(0.0,0.0,0.0),
    val look: DioramaVec = DioramaVec(0.0,0.0,-10.0),
    val lookVelocity: DioramaVec = DioramaVec(0.0,0.0,0.0),
    val alignment: Double = 0.0,
    val alignmentOrientation: DioramaQuaternion? = null,
    val orientation: DioramaQuaternion? = null,
)
internal data class DioramaFollowResult(val state: DioramaFollowState, val snapped: Boolean,
    val readTarget: Double, val offAxis: Double, val maxOffAxis: Double,
    val targetOrientation: DioramaQuaternion,
    val renderedPosition: DioramaVec = state.position,
    val renderedOrientation: DioramaQuaternion? = state.orientation)

private val alignmentCeiling = mapOf(DioramaShot.hold to .9, DioramaShot.pushIn to .85,
    DioramaShot.track to .85, DioramaShot.swell to .8, DioramaShot.float to .78,
    DioramaShot.glide to .72, DioramaShot.crane to .7, DioramaShot.pullBack to .7,
    DioramaShot.arc to .62, DioramaShot.pendulum to .6, DioramaShot.orbit to .55,
    DioramaShot.spiral to .5, DioramaShot.flyby to .5)

/** Exact source finite operator for a supplied original delta, NOT a UI ticker.
 * Position snap resets orientation, but (as upstream) does not reset read weight. */
internal fun dioramaFollowStep(previous: DioramaFollowState, target: DioramaCameraTarget,
    frame: DioramaFrame, placement: DioramaPlacement, shot: DioramaShot,
    now: Double, lineStart: Double, renderEnd: Double, delta: Double,
    smoothTime: Double, aspect: Double, fov: Double = 55.0,
    flight: DioramaFlightFrame? = null): DioramaFollowResult {
    require(delta.isFinite() && delta >= 0 && now.isFinite())
    require(smoothTime.isFinite() && smoothTime > 0 && aspect.isFinite() && aspect > 0 && fov > 0 && fov < 180)
    fun distance(a: DioramaVec, b: DioramaVec) = (a-b).let { sqrt(it.dot(it)) }
    fun damp(current: DioramaVec, goal: DioramaVec, velocity: DioramaVec): Pair<DioramaVec,DioramaVec> {
        val omega = 2/max(.0001,flight?.smoothTime ?: smoothTime); val x = omega*delta
        val decay = 1/(1+x+.48*x*x+.235*x*x*x)
        val change = current-goal; val temp = (velocity+change*omega)*delta
        return goal+(change+temp)*decay to (velocity-temp*omega)*decay
    }
    val snapped = flight?.flying != true && distance(target.eye,previous.position) > 24
    val (position,velocity) = if (snapped) target.eye to DioramaVec(0.0,0.0,0.0)
        else damp(previous.position,target.eye,previous.velocity)
    val goalLook = target.look + (flight?.lookOffset ?: DioramaVec(0.0,0.0,0.0))
    val (look,lookVelocity) = if (flight?.flying != true && distance(goalLook,previous.look) > 24)
        goalLook to DioramaVec(0.0,0.0,0.0) else damp(previous.look,goalLook,previous.lookVelocity)
    val renderedPosition = position + (flight?.positionOffset ?: DioramaVec(0.0,0.0,0.0))
    val desiredAlignment = DioramaQuaternion.basis(frame.right,frame.up,frame.forward * -1.0) *
        DioramaQuaternion.axis(DioramaVec(0.0,1.0,0.0),placement.yaw) *
        DioramaQuaternion.axis(DioramaVec(0.0,0.0,1.0),placement.roll)
    val alignmentOrientation = if (snapped || previous.alignmentOrientation == null) desiredAlignment
        else previous.alignmentOrientation.slerp(desiredAlignment,1-exp(-2.6*delta))
    fun smooth(t: Double): Double { val c=t.coerceIn(0.0,1.0); return c*c*(3-2*c) }
    val read = min(smooth((now-(lineStart-.6))/.95),1-smooth(now-renderEnd))*alignmentCeiling.getValue(shot)
    val alignment = previous.alignment+(read-previous.alignment)*(1-exp(-1.2*delta))
    val lookOrientation = DioramaQuaternion.look(renderedPosition,look)
    var orientationTarget = lookOrientation.slerp(alignmentOrientation,alignment)
    val view = (look-renderedPosition).normalized()
    val forward = orientationTarget.rotate(DioramaVec(0.0,0.0,-1.0))
    val denominator = sqrt(forward.dot(forward)*view.dot(view))
    val offAxis = if (denominator == 0.0) PI/2 else acos((forward.dot(view)/denominator).coerceIn(-1.0,1.0))
    val halfV = fov*PI/360; val halfH = atan(tan(halfV)*aspect)
    val maxOffAxis = min(halfV,halfH)*.7
    if (offAxis > maxOffAxis) orientationTarget = orientationTarget.slerp(lookOrientation,1-maxOffAxis/offAxis)
    val orientation = if (snapped || previous.orientation == null) orientationTarget
        else previous.orientation.slerp(orientationTarget,1-exp(-4.2*delta))
    val renderedOrientation = if (flight != null && flight.bank != 0.0)
        orientation * DioramaQuaternion.axis(DioramaVec(0.0,0.0,1.0),flight.bank) else orientation
    return DioramaFollowResult(DioramaFollowState(position,velocity,look,lookVelocity,alignment,
        alignmentOrientation,orientation),snapped,read,offAxis,maxOffAxis,orientationTarget,
        renderedPosition,renderedOrientation)
}
