// Copyright (c) 2026 chthollyphile
// PartitaChunk.tsx guide spans (1px, 32px, 10px box shadow).
// SPDX-License-Identifier: AGPL-3.0-only
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.partita

import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.unit.*
import top.ghhccghk.accompanist.lyrics.folia.internal.rendering.*
import kotlin.math.ceil

internal data class PartitaGuide(val size: Size, val padding: Int, val shadow: ImageBitmap)

internal fun preparePartitaGuide(size: Size, density: Density): PartitaGuide {
    val padding=ceil(16*density.density).toInt()
    val mask=ImageBitmap(ceil(size.width).toInt()+2*padding,ceil(size.height).toInt()+2*padding)
    CanvasDrawScope().draw(density,LayoutDirection.Ltr,Canvas(mask),Size(mask.width.toFloat(),mask.height.toFloat())) {
        drawRect(Color.White,Offset(padding.toFloat(),padding.toFloat()),size)
    }
    return PartitaGuide(size,padding,blurredMasks(mask,floatArrayOf(5*density.density)).single())
}

/** Shadow and solid form one guide, whose opacity/scale applies to the group. */
internal fun DrawScope.drawPartitaGuide(guide: PartitaGuide, ink: Color, shadowInk: Color?,
    progress: Float, horizontal: Boolean, right: Boolean, layer: ClassicLayer) {
    val p=guide.padding
    layer.alpha=progress.coerceIn(0f,1f)
    layer.record(this,IntSize(guide.shadow.width,guide.shadow.height)) {
        shadowInk?.let {drawImage(guide.shadow,colorFilter=ColorFilter.tint(it))}
        drawRect(ink,Offset(p.toFloat(),p.toFloat()),guide.size)
    }
    val origin=if(horizontal) Offset(if(right)guide.size.width else 0f,0f) else Offset(0f,guide.size.height)
    withTransform({
        scale(if(horizontal)progress else 1f,if(horizontal)1f else progress,Offset.Zero)
        translate(-origin.x-p,-origin.y-p)
    }) {drawClassicLayer(layer)}
}
