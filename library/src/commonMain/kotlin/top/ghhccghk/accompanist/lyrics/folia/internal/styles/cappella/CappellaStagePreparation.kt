// Copyright (c) 2026 chthollyphile and contributors
// Folia cc6226e4 VisualizerCappella.tsx/cappellaMessageLayout.ts preparation boundary.
// SPDX-License-Identifier: AGPL-3.0-only. See docs/upstream.md.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.cappella

import androidx.compose.ui.text.*
import androidx.compose.ui.unit.Density

/** Platform text measurement is independent of the source browser oracle.
 * Only 32 message/style preparations retained, matching the source cache limit. */
internal class CappellaStagePreparation(val lines: List<CappellaLine>, title: String,
    val size: CappellaStageSize, val intensity: CappellaIntensity, private val measurer: TextMeasurer,
    private val textStyle: TextStyle, private val density: Density,
    private val resolver: androidx.compose.ui.text.font.FontFamily.Resolver) {
    val messages = buildCappellaMessages(lines,title,intensity)
    val motion = cappellaMotion(intensity)
    private data class Prepared(val style: CappellaBubbleStyle, val full: CappellaBubbleSize,
        val plan: CappellaRevealPlan?, val sizes: List<CappellaBubbleSize>?)
    private val cache = linkedMapOf<Pair<String,Boolean>,Prepared>()
    private fun prepare(message: CappellaMessage, active: Boolean): Prepared {
        val key=message.id to active
        cache.remove(key)?.let { cache[key]=it; return it }
        val config=cappellaBubbleStyle(size,motion,active,message.line==null)
        val measure=CappellaBubbleMeasure(measurer,textStyle,density,config,resolver)
        val full=if(message.line==null)measure.titleSize(message.text)else measure.size(message.text)
        val plan=if(active)CappellaRevealPlan(lines[message.line!!])else null
        val result=Prepared(config,full,plan,plan?.let(measure::sizes))
        cache[key]=result
        if(cache.size>32)cache.remove(cache.keys.first())
        return result
    }
    fun rowSize(message: CappellaMessage, active: Boolean, time: Double): CappellaRowSize {
        val p=prepare(message,active)
        val current=if(p.plan!=null&&p.sizes!=null)p.sizes[p.plan.bubbleCount(time).coerceIn(p.sizes.indices)]else p.full
        return CappellaRowSize(current,p.full,p.style)
    }
    fun at(time: Double, currentLine: Int, viewportHeight: Double): List<CappellaRowLayout> {
        val visible=cappellaVisibleLine(lines,time)
        val selected=visibleCappellaMessages(messages,visible,viewportHeight,currentLine) { m,active ->
            // Original title estimates use the ceil pretext size, not its DOM size.
            val row=rowSize(m,active,time)
            val estimate=if(m.line==null){val measure=CappellaBubbleMeasure(measurer,textStyle,density,row.style,resolver)
                row.copy(fullBubble=measure.size(m.text))}else row
            cappellaEstimatedHeight(estimate,active,motion)
        }
        return cappellaStageLayout(selected,currentLine,size,motion) { m->rowSize(m,m.line!=null&&m.line==currentLine,time) }
    }
}
