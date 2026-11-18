// SPDX-License-Identifier: AGPL-3.0-only
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.cappella

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.platform.FontLoadResult
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.*
import org.jetbrains.skia.Font
import org.jetbrains.skia.FontFeature
import org.jetbrains.skia.Point
import org.jetbrains.skia.shaper.*

internal actual fun cappellaInkClusters(text: String, style: TextStyle,
    resolver: FontFamily.Resolver, density: Density): List<CappellaInkCluster> {
    if (text.isEmpty() || style.letterSpacing.let { it != TextUnit.Unspecified && it.value != 0f }) return emptyList()
    val loaded = resolver.resolve(style.fontFamily, style.fontWeight ?: FontWeight.Normal,
        style.fontStyle ?: FontStyle.Normal, style.fontSynthesis ?: FontSynthesis.All).value as? FontLoadResult ?: return emptyList()
    val face = loaded.typeface ?: return emptyList()
    val result = mutableListOf<CappellaInkCluster>()
    // Match Compose's ParagraphBuilder parser, not HarfBuzz's different
    // "liga=0" syntax. A valid host "liga 0" must not throw or merge fi.
    val options = ShapingOptions.DEFAULT.withFeatures(FontFeature.parseW3(style.fontFeatureSettings.orEmpty()))
        .withLeftToRight(style.textDirection != TextDirection.Rtl && style.textDirection != TextDirection.ContentOrRtl)
    Font(face, with(density) { style.fontSize.toPx() }).use { font ->
        font.isSubpixel = true
        Shaper.makeShapeDontWrapOrReorder().use { shaper ->
            shaper.shape(text, font, options, Float.POSITIVE_INFINITY, object : RunHandler {
                override fun beginLine() {}
                override fun runInfo(info: RunInfo?) {}
                override fun commitRunInfo() {}
                override fun runOffset(info: RunInfo?): Point = Point.ZERO
                override fun commitRun(info: RunInfo?, glyphs: ShortArray?, positions: Array<Point?>?, clusters: IntArray?) {
                    if (info == null || clusters == null || glyphs == null || glyphs.any { it.toInt() == 0 }) return
                    // A fallback shaper may choose a different face from Compose.
                    // Do not merge clocks based on unverified fallback glyphs.
                    val sameFace = info.font.use { it.typeface?.use { runFace -> runFace.uniqueId == face.uniqueId } == true }
                    if (!sameFace) return
                    val edges = (clusters.toList() + info.rangeEnd).distinct().sorted()
                    result += edges.zipWithNext { a, b -> CappellaInkCluster(a, b) }
                }
                override fun commitLine() {}
            })
        }
    }
    return result
}
