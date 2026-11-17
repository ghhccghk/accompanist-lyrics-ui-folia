// Copyright (c) 2026 chthollyphile and contributors
// Folia cc6226e4 cappellaIntensity.ts, VisualizerCappella.tsx, CappellaMessageRow.tsx.
// SPDX-License-Identifier: AGPL-3.0-only. Source/adaptations: docs/upstream.md.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.cappella

import kotlin.math.*

internal data class CappellaMotion(val activeScale: Double, val passedScale: Double, val passedOpacity: Double,
    val activeFont: Double, val inactiveFont: Double, val activeX: Double, val activeY: Double,
    val inactiveX: Double, val inactiveY: Double, val activeMin: Double, val inactiveMin: Double)
internal fun cappellaMotion(intensity: CappellaIntensity) = when (intensity) {
    CappellaIntensity.CALM -> CappellaMotion(1.07,.96,.88,1.22,.96,18.0,14.0,16.0,12.0,58.0,44.0)
    CappellaIntensity.NORMAL -> CappellaMotion(1.12,.92,.82,1.34,.94,20.0,16.0,16.0,12.0,64.0,44.0)
    CappellaIntensity.CHAOTIC -> CappellaMotion(1.18,.88,.76,1.4,.92,22.0,17.0,15.0,11.0,68.0,42.0)
}
internal data class CappellaStageSize(val panelX: Double, val panelWidth: Double, val top: Double,
    val groupRatio: Double, val maxTextWidth: Double, val baseFont: Double)
internal fun cappellaStageSize(width: Double, fontScale: Double = 1.0): CappellaStageSize {
    val inset = when { width >= 1024 -> 56.0; width >= 640 -> 32.0; else -> 16.0 }
    val panel = min(max(width - inset * 2,0.0),896.0)
    val ratio = if (width >= 640) .68 else .78
    // The source computes text fit from width-32, even at sm/lg where actual
    // CSS insets differ. Preserve this discrepancy rather than 'fixing' source.
    val text = max(96.0,floor(min(max(width - 32,1.0),896.0) * ratio - 56))
    return CappellaStageSize((width-panel)/2,panel,when {width>=1024->80.0;width>=640->64.0;else->48.0},
        ratio,text,(18*fontScale).coerceIn(15.0,26.0))
}
internal fun cappellaBubbleStyle(size: CappellaStageSize, motion: CappellaMotion,
    active: Boolean, title: Boolean = false): CappellaBubbleStyle {
    val font=size.baseFont*if(title)1.0 else if(active)motion.activeFont else motion.inactiveFont
    val px=if(active)motion.activeX else motion.inactiveX
    val py=if(active)motion.activeY else motion.inactiveY
    return CappellaBubbleStyle(font,size.maxTextWidth,px,py,
        max(if(active)motion.activeMin else motion.inactiveMin,font*1.45+py*2),if(active)72.0 else 0.0)
}
internal data class CappellaRowSize(val bubble: CappellaBubbleSize, val fullBubble: CappellaBubbleSize,
    val style: CappellaBubbleStyle)
internal data class CappellaRowLayout(val message: CappellaMessage, val bubbleX: Double, val bubbleY: Double,
    val bubbleWidth: Double, val bubbleHeight: Double, val avatarX: Double, val avatarY: Double,
    val scale: Double, val opacity: Double, val rowTop: Double, val rowHeight: Double)

/** Settled source targets only. Parent row entry/exit and Framer layout/springs
 * require separate media-time response work; no accumulating frame state here. */
internal fun cappellaStageLayout(messages: List<CappellaMessage>, currentLine: Int, size: CappellaStageSize,
    motion: CappellaMotion, measured: (CappellaMessage) -> CappellaRowSize): List<CappellaRowLayout> {
    var top=size.top
    return messages.map { m ->
        val active=m.line!=null&&m.line==currentLine
        val passed=m.line!=null&&m.line<currentLine
        val row=measured(m);val style=row.style
        val height=max(row.bubble.height,style.minHeight)
        val width=max(row.bubble.width,style.minWidth)
        val overflow=if(active&&motion.activeScale>1)ceil(max(row.fullBubble.height,40.0)*(motion.activeScale-1))else 0.0
        val content=max(40.0,height)
        val rowHeight=content+overflow
        val scale=if(active)motion.activeScale else if(passed)motion.passedScale else 1.0
        val right=m.sender.side==CappellaSide.RIGHT
        val anchor=if(right)size.panelX+size.panelWidth else size.panelX
        val bubbleX=if(right)anchor-(52+width)*scale else anchor+52*scale
        val bottom=top+rowHeight
        CappellaRowLayout(m,bubbleX,bottom-height*scale,width*scale,height*scale,
            if(right)anchor-40*scale else anchor,bottom-40*scale,scale,
            if(passed)motion.passedOpacity else 1.0,top,rowHeight).also { top+=rowHeight+12 }
    }
}

internal fun cappellaEstimatedHeight(row: CappellaRowSize, active: Boolean, motion: CappellaMotion): Double {
    val rendered=max(row.fullBubble.height,row.style.minHeight)
    val overflow=if(active&&motion.activeScale>1)ceil(rendered*(motion.activeScale-1))else 0.0
    return rendered+overflow+12
}
