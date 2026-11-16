// SPDX-License-Identifier: AGPL-3.0-only
// Independently expressed continuous response of the pinned Folia spring operator.
// Calibration and source attribution: docs/upstream.md.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.fume

import kotlin.math.*

/** A three-state linear response: position, velocity, filtered focus.
 *
 * log(A)/h embeds the original finite-step operator A in continuous media time.
 * [calibrationInterval] identifies the source response being calibrated; it is NOT
 * a rendering/integration interval. Queries may use any duration. The source itself
 * changes with browser refresh rate, so this cannot match every original rate.
 *
 * Only valid while coefficients and target are constant and no velocity clamp is
 * active. A media trajectory must resolve those changes and constraints explicitly.
 * This class alone is neither a clock nor a completed Fume camera implementation.
 */
internal class FumeSpringResponse(strength: Double, damping: Double, focusRate: Double,
    calibrationInterval: Double = 1.0 / 60.0) {
    private val xx: Double
    private val xv: Double
    private val xf: Double
    private val vx: Double
    private val vv: Double
    private val vf: Double
    private val ff = -focusRate

    init {
        val h = calibrationInterval
        require(strength.isFinite() && strength > 0 && damping.isFinite() && damping > 0)
        require(focusRate.isFinite() && focusRate > 0 && h.isFinite() && h > 0 && h * damping < 1)
        val a = exp(-focusRate * h)
        val b00 = 1 - h * h * strength
        val b01 = h * (1 - h * damping)
        val b10 = -h * strength
        val b11 = 1 - h * damping
        val det = b00 * b11 - b01 * b10
        val tr = b00 + b11
        val discriminant = tr * tr - 4 * det
        val m00: Double; val m01: Double; val m10: Double; val m11: Double
        if (discriminant < 0) {
            val radius = sqrt(det)
            val cosine = (tr / (2 * radius)).coerceIn(-1.0, 1.0)
            val theta = acos(cosine)
            val factor = theta / sin(theta)
            val diagonal = ln(radius)
            m00 = diagonal + factor * (b00 / radius - cosine)
            m01 = factor * b01 / radius
            m10 = factor * b10 / radius
            m11 = diagonal + factor * (b11 / radius - cosine)
        } else {
            val root = sqrt(discriminant)
            val p = (tr + root) / 2
            val q = (tr - root) / 2
            require(q > 0) { "Fume spring has no supported real principal logarithm" }
            val factor = if (root < 1e-8) 1 / p else (ln(p) - ln(q)) / root
            m00 = ln(q) + factor * (b00 - q)
            m01 = factor * b01
            m10 = factor * b10
            m11 = ln(q) + factor * (b11 - q)
        }
        val u0 = h * h * strength * a
        val u1 = h * strength * a
        val z0 = (m00 - ln(a)) * u0 + m01 * u1
        val z1 = m10 * u0 + (m11 - ln(a)) * u1
        xx = m00 / h; xv = m01 / h
        vx = m10 / h; vv = m11 / h
        if (discriminant >= 0) {
            val root = sqrt(discriminant); val p = (tr + root) * .5; val q = (tr - root) * .5
            // Divided differences avoid (B-aI)^-1 at a coincident focus eigenvalue.
            fun g(x: Double): Double {
                val u = (x - a) / a
                return if (abs(u) < 1e-5) (1 - u / 2 + u * u / 3 - u.pow(3) / 4) / a
                    else ln1p(u) / (x - a)
            }
            val u = (p - a) / a
            val derivative = if (abs(u) < 1e-5) (-.5 + 2 * u / 3 - 3 * u * u / 4) / (a * a)
                else (u / (1 + u) - ln1p(u)) / (p - a).pow(2)
            val factor = if (root < 1e-8) derivative else (g(p) - g(q)) / root
            xf = ((g(q) + factor * (b00 - q)) * u0 + factor * b01 * u1) / h
            vf = (factor * b10 * u0 + (g(q) + factor * (b11 - q)) * u1) / h
        } else {
            val divisor = (b00 - a) * (b11 - a) - b01 * b10
            xf = ((b11 - a) * z0 - b01 * z1) / divisor / h
            vf = (-b10 * z0 + (b00 - a) * z1) / divisor / h
        }
    }

    fun derivative(position: Double, velocity: Double, focus: Double, target: Double): FumeSpringState {
        val x = position - target; val f = focus - target
        return FumeSpringState(xx * x + xv * velocity + xf * f,
            vx * x + vv * velocity + vf * f, ff * f)
    }

    /** Analytic constant-target response, including fractional calibration intervals. */
    fun advance(state: FumeSpringState, target: Double, seconds: Double): FumeSpringState {
        require(seconds.isFinite() && seconds >= 0)
        if (seconds == 0.0) return state
        val halfTrace = (xx + vv) * .5
        val deltaSquared = (xx - vv).pow(2) * .25 + xv * vx
        val diagonal: Double; val factor: Double
        if (deltaSquared < 0) {
            val omega = sqrt(-deltaSquared)
            val decay = exp(halfTrace * seconds)
            diagonal = decay * cos(omega * seconds); factor = decay * sin(omega * seconds) / omega
        } else {
            val omega = sqrt(deltaSquared)
            // Evaluate the two decaying eigenvalues separately: exp(trace*t)*cosh(...)
            // would yield 0*Infinity for a long seek despite a bounded response.
            val p = exp((halfTrace + omega) * seconds); val q = exp((halfTrace - omega) * seconds)
            diagonal = (p + q) * .5
            factor = if (omega < 1e-12) seconds * exp(halfTrace * seconds) else (p - q) / (2 * omega)
        }
        val e00 = diagonal + factor * (xx - halfTrace)
        val e01 = factor * xv
        val e10 = factor * vx
        val e11 = diagonal + factor * (vv - halfTrace)
        val ef = exp(ff * seconds)
        val c0: Double; val c1: Double
        if (deltaSquared >= 0) {
            val omega = sqrt(deltaSquared); val p = halfTrace + omega; val q = halfTrace - omega
            fun g(z: Double): Double {
                val u = (z - ff) * seconds
                return when {
                    abs(u) < 1e-5 -> ef * seconds * (1 + u / 2 + u * u / 6 + u.pow(3) / 24)
                    z > ff -> -exp(z * seconds) * expm1(-u) / (z - ff)
                    else -> ef * expm1(u) / (z - ff)
                }
            }
            val u = (p - ff) * seconds
            val derivative = if (abs(u) < 1e-5) ef * seconds * seconds * (.5 + u / 3 + u * u / 8)
                else (seconds * exp(p * seconds) - g(p)) / (p - ff)
            val f = if (omega < 1e-8) derivative else (g(p) - g(q)) / (2 * omega)
            c0 = (g(q) + f * (xx - q)) * xf + f * xv * vf
            c1 = f * vx * xf + (g(q) + f * (vv - q)) * vf
        } else {
            val divisor = (xx - ff) * (vv - ff) - xv * vx
            val w0 = ((vv - ff) * xf - xv * vf) / divisor
            val w1 = (-vx * xf + (xx - ff) * vf) / divisor
            c0 = (e00 - ef) * w0 + e01 * w1
            c1 = e10 * w0 + (e11 - ef) * w1
        }
        val x = state.position - target; val f = state.focus - target
        return FumeSpringState(target + e00 * x + e01 * state.velocity + c0 * f,
            e10 * x + e11 * state.velocity + c1 * f,
            target + ef * f)
    }
}

internal data class FumeSpringState(val position: Double, val velocity: Double, val focus: Double)
