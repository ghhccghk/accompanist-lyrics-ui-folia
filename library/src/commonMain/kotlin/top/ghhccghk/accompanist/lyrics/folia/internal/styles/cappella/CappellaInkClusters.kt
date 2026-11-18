// SPDX-License-Identifier: AGPL-3.0-only
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.cappella

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density

/** UTF-16 source range of one SHAPED cluster, not an EGC or a timed word.
 * Geometry still comes from the complete Compose paragraph. Platform handles
 * are scoped to preparation; no font/shaper objects survive in the paint loop. */
internal data class CappellaInkCluster(val begin: Int, val end: Int)

/** Preparation-only cluster metadata for the complete shown row. An empty list
 * means unavailable on that platform/font/style and retains caret ownership.
 * It never substitutes glyph geometry or discards any original EGC timing. */
internal expect fun cappellaInkClusters(text: String, style: TextStyle,
    resolver: FontFamily.Resolver, density: Density): List<CappellaInkCluster>

/** Original inline spans paint a shared ligature with its FIRST logical span's
 * opacity (including RTL lam-alef). Merge only paint ownership; the reveal plan
 * and all internal character clocks remain independent and unchanged. */
internal fun cappellaClusterOwners(glyphs: List<CappellaInkGlyph>,
    clusters: List<CappellaInkCluster>): List<CappellaInkGlyph> {
    if (glyphs.isEmpty() || clusters.isEmpty()) return glyphs
    val result = mutableListOf<CappellaInkGlyph>()
    var group = glyphs.first()
    for (next in glyphs.drop(1)) {
        if (clusters.any { it.begin < next.begin && it.end > next.begin }) {
            group = group.copy(end = next.end)
        } else {
            result += group
            group = next
        }
    }
    result += group
    return result
}
