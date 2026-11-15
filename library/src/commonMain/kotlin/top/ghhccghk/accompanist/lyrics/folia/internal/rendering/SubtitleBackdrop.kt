// Folia VisualizerSubtitleOverlay.tsx, fixed source ledger: docs/upstream.md.
// SPDX-License-Identifier: AGPL-3.0-only
package top.ghhccghk.accompanist.lyrics.folia.internal.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform

/** CSS ellipse radii are percentages of the gradient box, not half its size. */
internal fun DrawScope.drawSubtitleBackdrop(color: Color, box: Size, origin: Offset) {
    val radiusX=box.width*1.15f
    val ratio=box.height*1.3f/radiusX
    val brush=Brush.radialGradient(0f to color.copy(alpha=color.alpha*.96f),
        .62f to color.copy(alpha=color.alpha*.78f),1f to color.copy(alpha=0f),
        center=Offset.Zero,radius=radiusX)
    withTransform({translate(origin.x+box.width/2,origin.y+box.height/2);scale(1f,ratio,Offset.Zero)}) {
        drawRect(brush,Offset(-box.width/2,-box.height/ratio/2),Size(box.width,box.height/ratio))
    }
}
