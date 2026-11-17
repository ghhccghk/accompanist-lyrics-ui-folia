// SPDX-License-Identifier: AGPL-3.0-only
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.cappella

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.platform.FontLoadResult
import androidx.compose.ui.unit.Density
import kotlin.math.floor

/** Browser probe evidence in verification/cappella-fixtures/canvas-advances.tsv.
 * Shape the complete segment. Only the metric boundary is converted; text remains
 * a whole paragraph in the renderer. Native font/line handles always close. */
internal actual fun cappellaCanvasAdvance(text: String, style: TextStyle,
    resolver: FontFamily.Resolver, density: Density): Double? {
    val loaded=resolver.resolve(style.fontFamily,style.fontWeight?:FontWeight.Normal,
        style.fontStyle?:FontStyle.Normal,style.fontSynthesis?:FontSynthesis.All).value as FontLoadResult
    val typeface=loaded.typeface?:return null
    if(typeface.variationAxes?.isNotEmpty()==true)return null
    val units=typeface.unitsPerEm
    if(units<=0)return null
    return org.jetbrains.skia.Font(typeface,with(density){style.fontSize.toPx()}).use { font ->
        font.isSubpixel=true
        org.jetbrains.skia.TextLine.make(text,font).use lineScope@{ line ->
            val glyphs=line.glyphs
            if(glyphs.any{it.toInt()==0}||font.size<=0)return@lineScope null
            val widths=font.getWidths(glyphs)
            val truncated=widths.sumOf{(it.toDouble()*65536).toLong()/65536.0}
            // Recover the static font's integer design-unit GPOS advance rather
            // than treating Float accumulation noise in line.width as kerning.
            // Variable-font deltas need a separate calibrated boundary.
            val positions=line.positions
            val scale=(font.size*65536).toInt()
            // HarfBuzz's static-font design-to-position multiplier is itself
            // fixed point, built through a Float UPEM division. Retain that
            // boundary instead of multiplying by an idealized Double scale.
            val multiplier=((scale.toLong()*65536).toFloat()/units).toLong()
            val fixedAdjustment=widths.indices.sumOf { i ->
                val next=if(i==widths.lastIndex)line.width.toDouble() else positions[(i+1)*2].toDouble()
                val advance=next-positions[i*2]
                val rounded=floor(widths[i].toDouble()*65536+.5)/65536
                val designUnits=floor((advance-rounded)*units/font.size+.5)
                ((designUnits.toLong()*multiplier+32768) shr 16)/65536.0
            }
            // Final Float boundary observed in the pinned browser probes.
            (truncated+fixedAdjustment).toFloat().toDouble()/density.density
        }
    }
}
