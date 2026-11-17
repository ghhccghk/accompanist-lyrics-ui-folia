// Copyright (c) 2026 chthollyphile and contributors
// Adapted from Folia cc6226e4 cadenzaPlacements.ts / cadenzaTypes.ts.
// SPDX-License-Identifier: AGPL-3.0-only. Source/modifications: docs/upstream.md.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.cadenza

import kotlin.math.*

internal enum class CadenzaIntensity { CALM, NORMAL, CHAOTIC }
internal data class CadenzaFragment(val wordIndex: Int, val word: CadenzaWord, val text: String,
    val color: String, val startX: Double, val endX: Double, val startInWord: Int, val endInWord: Int,
    val graphemeCount: Int, val timings: List<CadenzaGlyphTime>, val indexInWord: Int = 0,
    val countInWord: Int = 1, val primary: Boolean = true, val split: Boolean = false)
internal data class CadenzaFragmentLine(val width: Double, val start: Int, val end: Int,
    val fragments: List<CadenzaFragment>)
internal data class CadenzaPlacement(val id: String, val fragment: CadenzaFragment,
    val x: Double, val y: Double, val width: Double, val height: Double, val rotation: Double,
    val scale: Double, val passedRotation: Double, val passedDriftX: Double, val passedDriftY: Double,
    val entryX: Double, val entryY: Double, val emphasis: Double, val interlude: Boolean)

// ECMAScript String.trim, including FEFF but not NEL or Java's control separators.
private fun jsBlank(c: Char) = c in "\t\n\u000b\u000c\r \u00a0\u1680\u2028\u2029\u202f\u205f\u3000\ufeff" || c in '\u2000'..'\u200a'

/** Intact primary fragments alone may become hero; a wrapped word never stacks at the hero. */
private fun emphasisMap(lines: List<CadenzaFragmentLine>, interlude: Boolean): Map<Int, Double> {
    if (interlude) return emptyMap()
    val primary = lines.flatMap { it.fragments }.filter { it.primary }
    val hero = primary.filter { !it.split && it.text.any { c -> !jsBlank(c) } }.map { fragment ->
        val count = maxOf(fragment.graphemeCount, cadenzaGraphemes(fragment.word.text).size, 1)
        val semantic = if (cadenzaIsCjk(fragment.word.text)) .18 else min(count * .08, .36)
        val center = 1 - abs(fragment.wordIndex - (primary.size - 1) / 2.0) / max(primary.size, 1)
        fragment to (semantic + center * .18)
    }.maxByOrNull { it.second } ?: return emptyMap()
    return mapOf(hero.first.wordIndex to 1.46 * (1 + (hero.second - .48).coerceIn(0.0, .52)))
}

private data class CollisionRect(val left: Double, val top: Double, val right: Double, val bottom: Double)
/** Same y-band query and insertion order as the source, including shared-band deduplication. */
private class Occupied(private val bandSize: Double) {
    private val rects = mutableListOf<CollisionRect>()
    private val bands = mutableMapOf<Int, MutableList<Int>>()
    private val marks = mutableListOf<Int>()
    private var query = 0
    private fun band(value: Double) = floor(value / bandSize).toInt()
    fun add(left: Double, baseline: Double, width: Double, height: Double, padding: Double) {
        val r = CollisionRect(left-padding, baseline-height-padding, left+width+padding, baseline+padding)
        val index = rects.size; rects += r; marks += 0
        for (b in band(r.top)..band(r.bottom)) bands.getOrPut(b) { mutableListOf() } += index
    }
    fun overlap(r: CollisionRect): Pair<Boolean, Double> {
        var intersects = false; var area = 0.0; val stamp = ++query
        for (b in band(r.top)..band(r.bottom)) for (index in bands[b].orEmpty()) {
            if (marks[index] == stamp) continue
            marks[index] = stamp
            val q = rects[index]
            val width = max(0.0, min(r.right,q.right) - max(r.left,q.left))
            val height = max(0.0, min(r.bottom,q.bottom) - max(r.top,q.top))
            if (width > 0 && height > 0) { intersects = true; area += width * height }
        }
        return intersects to area
    }
}
private data class PlacementPlan(val fragment: CadenzaFragment, val lineIndex: Int, val fragmentIndex: Int,
    val baseX: Double, val baseY: Double, val emphasis: Double, val width: Double, val height: Double,
    val scale: Double, val collisionWidth: Double, val collisionHeight: Double, val padding: Double)

/** Deterministic preparation; no media/frame history. Coordinates are source CSS px. */
internal fun buildCadenzaPlacements(lines: List<CadenzaFragmentLine>, fontPx: Double, lineHeight: Double,
    maxWidth: Double, intensity: CadenzaIntensity, seed: Double, interlude: Boolean): List<CadenzaPlacement> {
    val totalHeight = max(lines.size,1) * lineHeight
    val margin = if (intensity == CadenzaIntensity.CALM) 4.0 else 6.0
    val baseScale = when (intensity) { CadenzaIntensity.CHAOTIC -> 1.02; CadenzaIntensity.CALM -> 1.0; else -> 1.01 }
    val emphases = emphasisMap(lines,interlude)
    val heroWord = emphases.maxByOrNull { it.value }?.key
    val occupied = Occupied(max(24.0, floor(lineHeight * .9 + .5)))
    val plans = lines.flatMapIndexed { li,line -> line.fragments.mapIndexed { fi,fragment ->
        val emphasis = emphases[fragment.wordIndex] ?: 1.0
        val width = max(fragment.endX - fragment.startX, fontPx * .18)
        val scale = if (emphasis > 1) baseScale * emphasis else baseScale
        val height = fontPx * scale * .95
        PlacementPlan(fragment,li,fi,-line.width/2+fragment.startX,-totalHeight/2+fontPx+li*lineHeight,
            emphasis,width,height,scale,width*scale*if(emphasis>1)1.48 else 1.26,
            height*if(emphasis>1)1.36 else 1.24,margin+if(emphasis>1)10 else 2)
    } }.sortedWith { a,b ->
        val delta = b.emphasis - a.emphasis
        if (abs(delta) > .001) delta.compareTo(0.0)
        else if (a.lineIndex != b.lineIndex) a.lineIndex.compareTo(b.lineIndex)
        else a.fragment.startX.compareTo(b.fragment.startX)
    }
    val hero = plans.firstOrNull { it.fragment.wordIndex == heroWord }
    val heroCenterX = hero?.let { it.width * it.scale / 2 } ?: 0.0
    val heroCenterY = hero?.let { -it.height * .46 } ?: 0.0
    return plans.map { p ->
        val f = p.fragment; val emphasis = p.emphasis; val width = p.width; val height = p.height
        val wordSeed = seed + f.wordIndex * 17 + p.lineIndex * 31 + p.fragmentIndex * 13
        fun random(offset: Int): Double { val x=sin(wordSeed+offset)*10000; return x-floor(x) }
        val step = max(10.0,floor(fontPx*.14+.5))
        val maxRadius = if(emphasis>1) max(20.0,lineHeight*.5) else maxOf(lineHeight*2.2,p.collisionWidth*.75,56.0)
        var preferredX = if(emphasis>1) -width/2 else p.baseX
        var preferredY = if(emphasis>1) 0.0 else p.baseY
        if (!interlude && emphasis <= 1 && hero != null) {
            var dx = preferredX+width/2-heroCenterX; var dy = preferredY-height*.46-heroCenterY
            val distance = hypot(dx,dy)
            if (distance < 1) { dx=if(preferredX>=0)1.0 else -1.0; dy=if(p.lineIndex%2==0)-.65 else .65 }
            val separation = hero.width*hero.scale*.34+width*.52+p.padding*2
            if (distance < separation) {
                val length = max(hypot(dx,dy),1.0); val push=separation-distance
                preferredX+=dx/length*push;preferredY+=dy/length*push*.92
            }
        }
        val horizontalMin = -maxWidth/2-72; val horizontalMax = maxWidth/2+72
        val verticalMin = -max(totalHeight*.9,lineHeight*1.6); val verticalMax = max(totalHeight*.9,lineHeight*1.45)
        var chosenX=preferredX;var chosenY=preferredY;var found=false
        var fallbackX=preferredX;var fallbackY=preferredY;var fallbackScore=Double.POSITIVE_INFINITY
        val angleBase=if(hero!=null&&emphasis<=1)atan2(preferredY,preferredX+width/2) else 0.0
        var radius=0.0
        while(radius<=maxRadius&&!found) {
            val samples=if(radius==0.0)1 else if(emphasis>1)8 else max(12,floor(PI*2*radius/max(step*1.1,10.0)+.5).toInt())
            for (i in 0 until samples) {
                val angle=angleBase+i.toDouble()/samples*PI*2
                val dx=if(radius==0.0)0.0 else cos(angle)*radius
                val dy=if(radius==0.0)0.0 else sin(angle)*radius*if(emphasis>1).8 else .92
                val left=preferredX+dx-p.padding;val top=preferredY+dy-p.collisionHeight-p.padding
                val right=left+p.collisionWidth+p.padding*2;val bottom=top+p.collisionHeight+p.padding*2
                if(left<horizontalMin||right>horizontalMax||top<verticalMin||bottom>verticalMax)continue
                val (intersects,area)=occupied.overlap(CollisionRect(left,top,right,bottom))
                val score=area*2.2+hypot(dx,dy)
                if(score<fallbackScore){fallbackX=preferredX+dx;fallbackY=preferredY+dy;fallbackScore=score}
                if(!intersects){chosenX=preferredX+dx;chosenY=preferredY+dy;found=true;break}
            }
            radius+=step
        }
        if(!found){chosenX=fallbackX;chosenY=fallbackY}
        occupied.add(chosenX,chosenY,p.collisionWidth,p.collisionHeight,p.padding)
        val outX=chosenX+width/2;val outY=chosenY-height*.46;val length=max(hypot(outX,outY),1.0)
        val drift=when { interlude -> 3+random(6)*3;emphasis>1 ->4+random(6)*4
            intensity==CadenzaIntensity.CHAOTIC ->8+random(6)*9;else ->5+random(6)*6 }
        CadenzaPlacement("${f.word.text}-${f.wordIndex}-${p.lineIndex}-${p.fragmentIndex}-${f.indexInWord}",f,
            chosenX,chosenY,width,height,0.0,p.scale,(random(3)-.5)*if(intensity==CadenzaIntensity.CHAOTIC)20 else 12,
            outX/length*drift+(random(7)-.5)*2.4,outY/length*drift*.72+(random(8)-.5)*2,
            0.0,0.0,emphasis,interlude)
    }
}
