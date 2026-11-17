// Copyright (c) 2026 chthollyphile and contributors
// Folia cc6226e4 VisualizerPendolo.tsx: conditional chorus radial gradient.
// SPDX-License-Identifier: AGPL-3.0-only. No audio/background engine.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.pendolo

import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.unit.IntSize
import top.ghhccghk.accompanist.lyrics.folia.internal.rendering.*
import kotlin.math.*

internal fun DrawScope.drawPendoloHalo(layer:ClassicLayer,width:Double,height:Double,
    frame:PendoloChorusFrame,accent:Color) {
    if(!frame.presentation.active || frame.haloOpacity==0.0)return
    // Original halo inherits the root 16px em, not the main lyric's 28px.
    val left=-17.6f*density;val top=(height/2-11.2).toFloat()*density
    val w=(width+35.2).toFloat()*density;val h=(height+22.4).toFloat()*density
    val sigma=floor(10*frame.presentation.glowMultiplier+.5).toFloat()*density
    val pad=ceil(3*sigma+2);val center=Offset(w*.42f,h*.5f)
    val radius=hypot(max(center.x,w-center.x),max(center.y,h-center.y))
    val brush=Brush.radialGradient(0f to accent.copy(alpha=(.14*frame.presentation.haloOpacity).toFloat()),
        .55f to accent.copy(alpha=(.035*frame.presentation.haloOpacity).toFloat()),.82f to Color.Transparent,
        center=center,radius=radius)
    layer.alpha=frame.haloOpacity.toFloat();layer.blurSigma=sigma
    layer.record(this,IntSize(ceil(w+2*pad).toInt(),ceil(h+2*pad).toInt())) {
        translate(pad,pad){drawRoundRect(brush,cornerRadius=CornerRadius(16*density),size=Size(w,h))}
    }
    scale(frame.haloScale.toFloat(),frame.haloScale.toFloat(),Offset((width/2).toFloat()*density,height.toFloat()*density)) {
        translate(left-pad,top-pad){drawClassicLayer(layer)}
    }
}
