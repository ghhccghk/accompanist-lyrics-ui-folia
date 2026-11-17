// SPDX-License-Identifier: AGPL-3.0-only
// Media-time adaptation of Folia cc6226e4. Source/limitations: docs/upstream.md.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.fume

import kotlin.math.*

internal data class FumeFocusEpoch(val time: Double, val lineIndex: Int)
internal data class FumeCameraTrackConfig(val viewport: FumeViewport, val overviewStart: Double,
    val speed: Double = 1.0, val stepped: Boolean = false, val static: Boolean = false,
    val intensity: String = "normal", val floatOrigin: Double = 0.0)
private data class FumeMediaCameraNode(val time: Double, val state: FumeCameraState)

/** Internal calibration track, not yet a publicly accepted Fume theme.
 *
 * Preparation follows media/printing/flight events with error-controlled RK4;
 * it is independent of display FPS and of sample order. Each query starts from
 * an immutable prepared node, never the previously displayed state. Idle float
 * uses media seconds with a fixed origin, so pause and reverse seek reconstruct.
 *
 * The original Euler response depends on browser frame intervals. This adapter
 * calibrates continuous coefficients against the original 60 Hz response; it
 * does not claim equivalence at all rates. Dynamic target and flight residuals
 * must be compared in full-scene captures before public integration.
 */
internal class FumeCameraTrack(private val article: FumeArticle,
    private val geometry: Map<Int, FumeFocusBlock>, private val epochs: List<FumeFocusEpoch>,
    private val config: FumeCameraTrackConfig, private val endTime: Double) {
    private val calibration = 1.0 / 60
    private val pixelError = .0000001
    private val overview = fumeOverview(article, config.viewport)
    // A camera derivative used to rebuild every candidate line's grapheme and
    // timing plan, including scanning the entire article in gaps. These plans
    // depend only on content; keep one per block for this track's lifetime.
    private val reveals=article.blocks.associate{it.sourceIndex to FumeReveal(it.line)}
    private val nodes: List<FumeMediaCameraNode>
    val preparedNodeCount: Int get() = nodes.size

    init {
        require(epochs.isNotEmpty() && epochs.all { it.time.isFinite() && (it.lineIndex == -1 || it.lineIndex in geometry) })
        require(epochs.zipWithNext().all { (a, b) -> a.time < b.time })
        require(endTime.isFinite() && endTime >= epochs.last().time)
        require(config.speed.isFinite() && config.speed > 0 && config.floatOrigin.isFinite())
        require(!config.overviewStart.isNaN())
        require(config.viewport.width.isFinite() && config.viewport.width > 0 &&
            config.viewport.height.isFinite() && config.viewport.height > 0)
        val controls = fumeFocusControlTimes(geometry.values)
        val events = (epochs.map { it.time } + controls + controls.map { it - calibration * .5 } +
            controls.map { it - calibration } + listOf(config.overviewStart, endTime))
            .filter { it.isFinite() && it >= epochs.first().time && it <= endTime }.distinct().sorted()
        val prepared = mutableListOf<FumeMediaCameraNode>()
        var time = epochs.first().time
        var state = fumeInitialCamera(article)
        for (end in events) {
            if (end > time) {
                state = integrate(state, time, end, epochAt(time), time >= config.overviewStart) { t, s ->
                    prepared += FumeMediaCameraNode(t, s)
                }
                time = end
            }
            val input = input(time, epochAt(time), time >= config.overviewStart)
            if (fumeCameraIntent(state, article, geometry, input,reveals).retarget != state.retarget)
                state = fumeCameraStep(state, article, geometry, input).state
            if (prepared.lastOrNull()?.time == time) prepared[prepared.lastIndex] = FumeMediaCameraNode(time, state)
            else prepared += FumeMediaCameraNode(time, state)
        }
        nodes = prepared.toList()
    }

    private fun epochAt(time: Double): Int {
        var low = 0; var high = epochs.size
        while (low < high) { val m = (low + high) ushr 1; if (epochs[m].time <= time) low = m + 1 else high = m }
        return epochs[max(low - 1, 0)].lineIndex
    }
    private fun input(time: Double, line: Int, show: Boolean) = FumeCameraInput(time,
        time + config.floatOrigin, 0.0, line, config.viewport, overview,
        if (show) config.overviewStart else Double.POSITIVE_INFINITY, config.speed, config.stepped, config.static, config.intensity)
    private fun intent(state: FumeCameraState, time: Double, line: Int, show: Boolean) =
        fumeCameraIntent(state, article, geometry, input(time, line, show),reveals)

    private fun anticipated(state: FumeCameraState, time: Double, line: Int, show: Boolean): FumeCameraIntent {
        val middle = intent(state, time + calibration * .5, line, show)
        val end = intent(state, time + calibration, line, show)
        return if (middle.bridgeTarget != null && end.bridgeTarget == null) end else middle
    }
    private fun limit(intent: FumeCameraIntent, pose: FumeCameraPose): Double {
        val distance = hypot(intent.target.x - pose.x, intent.target.y - pose.y)
        return mix(1320.0, (distance / max(intent.retarget.duration * .28, .028)).coerceIn(2600.0, 8800.0), intent.boost)
    }
    private fun derivative(state: FumeCameraState, time: Double, line: Int, show: Boolean): FumeCameraPose {
        val i = anticipated(state, time, line, show); val p = state.pose
        if (i.bridgeTarget != null) {
            val rate = i.bridgeRate
            // Continuous exponential catch-up with endpoint forcing: the linear-target
            // lead is 1/r-h/(exp(r*h)-1), not simply h/2. This is a calibration
            // adaptation; rapidly changing target/coefficients retain measured residual.
            val lead = 1 / rate - calibration / expm1(rate * calibration)
            val q = intent(state, time + lead, line, show).bridgeTarget ?: i.bridgeTarget
            return FumeCameraPose((q.x - p.x) * rate, (q.y - p.y) * rate,
                ln(.72) / calibration * p.vx, ln(.72) / calibration * p.vy, 0.0, 0.0,
                (q.scale - p.scale) * rate, ln(.68) / calibration * p.vs, 0.0)
        }
        val boost = i.boost; val duration = i.retarget.duration
        val strength = mix(208.0, (15.8 / max(duration * duration, .0064)).coerceIn(260.0, 780.0), boost)
        val damping = mix(24.0, (sqrt(strength) * 1.36).coerceIn(24.0, 40.0), boost)
        val response = FumeSpringResponse(strength, damping,
            mix(11.2, (4.8 / max(duration, .05)).coerceIn(20.0, 54.0), boost), calibration)
        val scaleResponse = FumeSpringResponse(mix(54.0, 108.0, boost), mix(13.5, 21.0, boost),
            mix(5.4, 12.8, boost), calibration)
        val cap = limit(i, p)
        fun axis(model: FumeSpringResponse, x: Double, v: Double, f: Double, target: Double, bound: Double): FumeSpringState {
            val d = model.derivative(x, v, f, target)
            return when {
                v >= bound && d.velocity > 0 -> d.copy(position = bound, velocity = 0.0)
                v <= -bound && d.velocity < 0 -> d.copy(position = -bound, velocity = 0.0)
                else -> d
            }
        }
        val x = axis(response, p.x, p.vx, p.fx, i.target.x, cap)
        val y = axis(response, p.y, p.vy, p.fy, i.target.y, cap)
        val s = axis(scaleResponse, p.scale, p.vs, p.fs, i.target.scale, 1.6)
        return FumeCameraPose(x.position, y.position, x.velocity, y.velocity, x.focus, y.focus, s.position, s.velocity, s.focus)
    }

    private fun rk(state: FumeCameraState, time: Double, h: Double, line: Int, show: Boolean): FumeCameraState {
        val p = state.pose
        val k1 = derivative(state, time, line, show)
        val k2 = derivative(state.copy(pose = plus(p, k1, h * .5)), time + h * .5, line, show)
        val k3 = derivative(state.copy(pose = plus(p, k2, h * .5)), time + h * .5, line, show)
        val k4 = derivative(state.copy(pose = plus(p, k3, h)), time + h, line, show)
        fun weighted(a: Double, b: Double, c: Double, d: Double) = (a + 2 * b + 2 * c + d) * h / 6
        val weighted = FumeCameraPose(weighted(k1.x,k2.x,k3.x,k4.x), weighted(k1.y,k2.y,k3.y,k4.y),
            weighted(k1.vx,k2.vx,k3.vx,k4.vx), weighted(k1.vy,k2.vy,k3.vy,k4.vy),
            weighted(k1.fx,k2.fx,k3.fx,k4.fx), weighted(k1.fy,k2.fy,k3.fy,k4.fy),
            weighted(k1.scale,k2.scale,k3.scale,k4.scale), weighted(k1.vs,k2.vs,k3.vs,k4.vs), weighted(k1.fs,k2.fs,k3.fs,k4.fs))
        var result = state.copy(pose = plus(p, weighted, 1.0))
        var pose = result.pose.copy(scale = result.pose.scale.coerceIn(.22, 2.24), vs = result.pose.vs.coerceIn(-1.6, 1.6))
        result = result.copy(pose = pose)
        val i = anticipated(result, time + h, line, show)
        if (i.bridgeTarget == null) {
            val bound = limit(i, pose)
            pose = pose.copy(vx = pose.vx.coerceIn(-bound, bound), vy = pose.vy.coerceIn(-bound, bound))
        } else pose = pose.copy(fx = i.bridgeTarget.x, fy = i.bridgeTarget.y, fs = i.bridgeTarget.scale)
        return result.copy(pose = pose)
    }
    private fun error(a: FumeCameraState, b: FumeCameraState): Double {
        val scale = maxOf(a.pose.scale, b.pose.scale, .001)
        return maxOf(abs(a.pose.x - b.pose.x) * scale, abs(a.pose.y - b.pose.y) * scale,
            abs(a.pose.scale - b.pose.scale) * max(config.viewport.width, config.viewport.height))
    }
    private fun integrate(initial: FumeCameraState, from: Double, to: Double, line: Int, show: Boolean,
        accepted: (Double, FumeCameraState) -> Unit = { _, _ -> }): FumeCameraState {
        var state = initial; var time = from; var h = min(.05, to - from); var attempts = 0
        while (time < to) {
            check(++attempts <= 100_000) { "Fume continuous camera failed to converge" }
            h = min(h, to - time)
            if (state.retarget.bridge != "none") for (delta in listOf(calibration, calibration * .5)) {
                val edge = state.retarget.started + state.retarget.duration - delta
                if (edge > time && edge < time + h) h = edge - time
            }
            check(h > 0 && time + h > time) { "Fume camera lost media-time precision" }
            val full = rk(state, time, h, line, show)
            val half = rk(rk(state, time, h * .5, line, show), time + h * .5, h * .5, line, show)
            val e = error(full, half)
            check(e.isFinite()) { "Nonfinite Fume camera response" }
            if (e <= pixelError) {
                time = if (h == to - time) to else time + h; state = half
                accepted(time, state); h *= if (e < pixelError * .05) 1.8 else 1.0
            } else h *= max(.15, .8 * (pixelError / e).pow(.2))
        }
        return state
    }
    fun sample(time: Double): FumeCameraFrame {
        require(time.isFinite())
        val t = time.coerceIn(nodes.first().time, endTime)
        var low = 0; var high = nodes.size
        while (low < high) { val m = (low + high) ushr 1; if (nodes[m].time <= t) low = m + 1 else high = m }
        val n = nodes[max(low - 1, 0)]
        val state = if (t <= n.time) n.state else integrate(n.state, n.time, t, epochAt(n.time), n.time >= config.overviewStart)
        val show = t >= config.overviewStart
        val restore = if (show && state.retarget.source == -2)
            fumeCameraEase((t - state.retarget.started) / max(state.retarget.duration, .001)) else 0.0
        return FumeCameraFrame(state, show, restore)
    }
    private fun mix(a: Double, b: Double, t: Double) = a + (b - a) * t
    private fun plus(p: FumeCameraPose, d: FumeCameraPose, h: Double) = FumeCameraPose(
        p.x + h * d.x, p.y + h * d.y, p.vx + h * d.vx, p.vy + h * d.vy,
        p.fx + h * d.fx, p.fy + h * d.fy, p.scale + h * d.scale, p.vs + h * d.vs, p.fs + h * d.fs)
}

/** Explicit printing and 0.7-EGC focus knots. No uniformly spaced frame events. */
internal fun fumeFocusControlTimes(blocks: Collection<FumeFocusBlock>): List<Double> = buildList {
    for (block in blocks) {
        val reveal = FumeReveal(block.block.line); val line = reveal.line; val size = reveal.graphemes.size
        add(line.start); add(line.end)
        val offsets = ((0..size).map(Int::toDouble) + block.rows.flatMap {
            listOf(it.start - .7, it.start + .7, it.end - .7, it.end + .7)
        }).filter { it >= 0 && it <= size }
        if (reveal.ranges.isEmpty()) {
            val duration = max(line.end - line.start, .12)
            if (size > 0) for (offset in offsets) add(line.start + offset / size * duration)
        } else for (range in reveal.ranges) {
            val duration = max(range.word.end - range.word.start, .08); val count = max(range.end - range.start, 1)
            add(range.word.start); add(range.word.end); add(range.word.start + duration)
            if (range.word.syllables.isNotEmpty()) addAll(range.timings.map { it.start })
            else for (i in 1..count) add(range.word.start + (i - .2) / count * duration)
            for (offset in offsets) if (offset >= range.start && offset <= range.end)
                add(range.word.start + (offset - range.start) / count * duration)
        }
    }
}.filter { it.isFinite() }.distinct().sorted()
