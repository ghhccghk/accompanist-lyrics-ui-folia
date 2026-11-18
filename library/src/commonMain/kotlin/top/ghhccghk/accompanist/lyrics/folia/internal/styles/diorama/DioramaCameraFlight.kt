// Copyright (c) 2026 chthollyphile and contributors
// SPDX-License-Identifier: AGPL-3.0-only
// Folia cc6226e4 dioramaTransition.ts and CameraRig.tsx:280-329,438-440.
// Media-domain flight; no wall-clock animation, background engine or audio input.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama

import kotlin.math.*

internal const val DioramaFlightDuration = 3.2

internal fun dioramaTransitionOffset(seed: Double, epoch: Int): DioramaVec {
    require(seed.isFinite() && epoch > 0)
    val base = seed + epoch * 131.0
    val azimuth = dioramaSeededUnit(base + 1) * PI * 2
    val elevation = (dioramaSeededUnit(base + 2) - .35) * 1.3
    val ce = cos(elevation)
    return DioramaVec(sin(azimuth) * ce, sin(elevation), -abs(cos(azimuth) * ce) - .35).normalized() * 46.0
}

internal data class DioramaFlightFrame(val flying: Boolean, val smoothTime: Double,
    val positionOffset: DioramaVec, val lookOffset: DioramaVec, val bank: Double)

/** The finite source captures direction/length ONCE, then keeps following the
 * live incoming lyric. Arc/bank are visual offsets, never fed back into refs. */
internal class DioramaCameraFlight(from: DioramaVec, to: DioramaVec, seed: Double, epoch: Int,
    val started: Double = 0.0) {
    private val perpendicular: DioramaVec
    private val length: Double
    private val side: Double
    init {
        require(started.isFinite() && epoch > 0 && seed.isFinite())
        val d = to - from
        length = sqrt(d.dot(d))
        require(length.isFinite())
        val horizontal = DioramaVec(d.z, 0.0, -d.x)
        perpendicular = if (sqrt(horizontal.dot(horizontal)) < .001) DioramaVec(1.0, 0.0, 0.0)
            else horizontal.normalized()
        side = if (dioramaSeededUnit(seed + epoch * 17.0) < .5) -1.0 else 1.0
    }
    fun at(time: Double, normalSmooth: Double): DioramaFlightFrame {
        require(time.isFinite() && normalSmooth.isFinite() && normalSmooth > 0)
        val te = (time - started) / DioramaFlightDuration
        val flying = te >= 0 && te < 1
        val c = te.coerceIn(0.0, 1.0)
        val ease = c*c*c*(c*(c*6-15)+10)
        val envelope = if (flying) sin(PI * te).pow(2) else 0.0
        val arc = envelope * length * .16
        return DioramaFlightFrame(flying, if (flying) 1.1 + (normalSmooth-1.1)*ease else normalSmooth,
            perpendicular * arc + DioramaVec(0.0, arc*.5, 0.0),
            perpendicular * (envelope*length*.14*side), envelope*.14*side)
    }
}

/** Value-only cut anchor. Does not retain the outgoing playback/textures. A
 * backward seek alone never creates a new song, epoch or corridor. */
internal data class DioramaTextTransition(val previous: DioramaFollowState,
    val outgoingOrigin: DioramaVec, val epoch: Int) {
    init { require(epoch > 0) }
}
