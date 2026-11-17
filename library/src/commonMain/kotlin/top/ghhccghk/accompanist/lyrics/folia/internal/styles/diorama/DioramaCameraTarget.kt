// Copyright (c) 2026 chthollyphile and contributors
// SPDX-License-Identifier: AGPL-3.0-only
// Folia cc6226e4 cameraPath.ts shot/hold/drift/truck + CameraRig.tsx target block.
// Target only, NOT the original damped/orientation/transition follower. docs/upstream.md.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama

import top.ghhccghk.accompanist.lyrics.folia.internal.text.isRtlLyric
import kotlin.math.*

internal enum class DioramaShot { pushIn, pullBack, orbit, track, crane, hold, swell, spiral, pendulum, flyby, arc, float, glide }
internal data class DioramaShotOffset(val right: Double, val up: Double, val back: Double)
internal data class DioramaDrift(val swayX: Double, val swayY: Double, val lift: Double, val dist: Double)
internal data class DioramaCameraTarget(val eye: DioramaVec, val look: DioramaVec,
    val progress: Double, val wordProgress: Double, val settle: Double, val truck: Double)
private fun clamp01(v: Double) = v.coerceIn(0.0, 1.0)
private fun smooth01(v: Double): Double { val t = clamp01(v); return t*t*(3-2*t) }

internal fun dioramaShotOffset(kind: DioramaShot, progress: Double, wordProgress: Double,
    seed: Double, lineIndex: Int, moveScale: Double, hero: Double = 5.2, lift: Double = .5): DioramaShotOffset {
    val e = smooth01(progress)
    val dir = if (dioramaSeededUnit(seed + lineIndex*7 + 3) < .5) -1 else 1
    var right = 0.0; var up = lift; var back = hero
    when (kind) {
        DioramaShot.pushIn -> { back = hero*(1.5-.8*e); up = lift*(1.2-.5*e)+.15 }
        DioramaShot.pullBack -> { back = hero*(.85+1.15*e); up = lift+1.7*e }
        DioramaShot.orbit -> { val t = dir*.55*(2*e-1); right = hero*sin(t); back = hero*cos(t); up = lift+.4 }
        DioramaShot.track -> { right = dir*hero*.34*sin(PI*clamp01(wordProgress)); back = hero*1.06; up = lift+.12 }
        DioramaShot.crane -> { val from = if (dir>0) -1.5 else 2.4; val to = if (dir>0) 2.4 else -1.4; up = from+(to-from)*e; back = hero*1.05 }
        DioramaShot.swell -> { val t = dir*.4*e; right = hero*sin(t)*1.1; back = hero*(1.0+.35*e); up = lift+1.2*e+.3 }
        DioramaShot.spiral -> { val t = dir*.7*(2*e-1); right = hero*sin(t)*.85; back = hero*(1.05-.1*e); up = lift-.8+2.4*e }
        DioramaShot.pendulum -> { right = dir*hero*.45*(2*e-1); up = lift+.9-.9*sin(PI*e); back = hero*1.02 }
        DioramaShot.flyby -> { right = dir*hero*(.7-1.4*e); back = hero*(.78+.12*sin(PI*e)); up = lift+.05 }
        DioramaShot.arc -> { val t = dir*.5*sin(PI*e); right = hero*sin(t)*1.3; back = hero*(1.05+.12*(1-cos(t))); up = lift+.5*sin(PI*e) }
        DioramaShot.float -> { right = dir*hero*.16*sin(e*PI*2); up = lift+.6+.5*sin(e*PI*2+1.0); back = hero*(1.05+.06*sin(e*PI)) }
        DioramaShot.glide -> { right = dir*hero*.22*e; up = lift+1.4*e; back = hero*(1.15-.25*e) }
        DioramaShot.hold -> { up = lift+.3*sin(progress*PI); back = hero }
    }
    return DioramaShotOffset(right*moveScale, lift+(up-lift)*moveScale, hero+(back-hero)*moveScale)
}
internal fun dioramaHoldSettle(secondsHeld: Double) = 1-smooth01((secondsHeld-3)/5)
internal fun dioramaReadHeadTruck(wordProgress: Double, trackWidth: Double, visibleHalfWidth: Double): Double {
    if (trackWidth <= 0) return 0.0
    val wordOffset = (clamp01(wordProgress)-.5)*trackWidth
    val allowed = max(abs(visibleHalfWidth-trackWidth/2), .001)
    return allowed*tanh(wordOffset/allowed)
}
internal fun dioramaCameraDrift(time: Double, seed: Double, scale: Double): DioramaDrift {
    val ph = seed*.017
    return DioramaDrift((sin(time*.11+ph)*.6+sin(time*.047+ph*1.7)*.4)*.4*scale,
        sin(time*.09+ph*.7)*.4*.5*scale,
        (sin(time*.13+ph*1.3)*.6+sin(time*.061+ph*2.1)*.4)*.4*scale,
        sin(time*.05+ph*1.1)*.5*scale)
}

/** Source renderHints uses the LAST source word end, not max timeline end. */
internal fun dioramaRenderEnd(line: DioramaLyric): Double {
    val duration = max(line.end-line.start, 0.0)
    if (duration < .10) return max(line.end, line.start+.067)
    val fast = duration < .18
    val enter = if (fast) (duration*.45).coerceIn(.045,.06) else min(.42,max(.22,max(duration,.12)*.34))
    val exit = if (fast) (duration*.22).coerceIn(.03,.04) else min(.32,max(.18,max(duration,.12)*.18))
    val passed = max(line.words.lastOrNull()?.end ?: line.end, line.start) + if (fast) .03 else .06
    val exitStart = if (fast) maxOf(line.start+enter+.01,passed,line.end-exit) else max(passed,line.end-exit)
    return max(line.end,exitStart+exit)
}

/** Prepare once per source line. Internal clocks, render polish and direction are
 * shared by every target query. No glyph parsing or whole-song work per frame. */
internal class DioramaCameraLine(val line: DioramaLyric, val renderEnd: Double = dioramaRenderEnd(line)) {
    private val timeline = dioramaLineTimeline(line)
    private val rtl = isRtlLyric(line.text)
    fun wordProgress(now: Double): Double {
        if (timeline.isEmpty()) return 0.0
        if (now >= renderEnd) return 1.0
        if (now <= line.start) return 0.0
        var revealed = 0.0
        for ((i,t) in timeline.withIndex()) {
            if (now >= t.end) revealed = i+1.0
            else if (now >= t.start) { revealed = i+clamp01((now-t.start)/max(t.end-t.start,.001)); break }
            else break
        }
        return revealed/timeline.size
    }
    fun target(now: Double, frame: DioramaFrame, placement: DioramaPlacement, seed: Double,
        index: Int, shot: DioramaShot, trackWidth: Double, aspect: Double, fov: Double = 55.0,
        moveScale: Double = 1.0, driftScale: Double = .62): DioramaCameraTarget {
        val word = wordProgress(now)
        val span = renderEnd-line.start
        val progress = if (span>0) clamp01((now-line.start)/span) else if (now>=line.start) 1.0 else 0.0
        val settle = dioramaHoldSettle(now-renderEnd)
        val visibleHalf = 5.2*tan((fov*(PI/180))/2)*aspect*.92
        val truck = dioramaReadHeadTruck(if (rtl) 1-word else word,trackWidth,visibleHalf)*settle
        val base = dioramaComposeLocal(frame,placement.offsetR+truck,placement.offsetU,0.0)
        val offset = dioramaShotOffset(shot,progress,word,seed,index,moveScale*settle)
        val drift = dioramaCameraDrift(now,seed,driftScale)
        val eye = base+frame.right*(offset.right+drift.swayX)+frame.up*(offset.up+drift.swayY+drift.lift)-frame.forward*(offset.back+drift.dist)
        val look = base+frame.right*(placement.lookR*settle)
        return DioramaCameraTarget(eye,look,progress,word,settle,truck)
    }
}
