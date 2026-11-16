// Kotlin text measurement adapter for fixed Folia cc6226e4 fumeTextMeasure.ts.
// SPDX-License-Identifier: AGPL-3.0-only. Source/modifications: docs/upstream.md.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.fume

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import kotlin.math.floor

/** Widths are logical viewport pixels; measurements are real whole shaped paragraphs.
 * The caller owns the measurer/cache and invalidates this adapter with density or font changes. */
internal class FumeTextMeasure(private val measurer: TextMeasurer, private val baseStyle: TextStyle,
    private val density: Density) {
    internal fun style(fontPx: Double, hero: Boolean, lineHeight: Double? = null): TextStyle = baseStyle.copy(
        // Chromium Canvas's hundredth-pixel font boundary, retaining unrounded sizes in geometry.
        fontSize = (floor(fontPx * 100 + 1e-9) / 100).sp,
        fontWeight = baseStyle.fontWeight ?: FontWeight(if (hero) 780 else 640),
        textAlign = TextAlign.Left,
        lineHeight = lineHeight?.sp ?: TextUnit.Unspecified)

    fun countLines(text: String, hero: Boolean, fontPx: Double, width: Double): Int {
        // pre-wrap hangs trailing ASCII spaces; don't trim the actual drawing/timing input.
        val natural = measurer.measure(AnnotatedString(text.trimEnd(' ', '\t')), style(fontPx, hero, null), softWrap = false)
        if (natural.lineCount <= 1 && natural.getLineRight(0) - natural.getLineLeft(0) <= width * density.density)
            return natural.lineCount
        return layout(text, hero, fontPx, width).lineCount
    }

    fun layout(text: String, hero: Boolean, fontPx: Double, width: Double, lineHeight: Double? = null): TextLayoutResult =
        measurer.measure(AnnotatedString(text), style(fontPx, hero, lineHeight),
            constraints = Constraints(maxWidth = floor(width * density.density).toInt().coerceAtLeast(1)))

    fun naturalLayout(text: String, hero: Boolean, fontPx: Double): TextLayoutResult =
        measurer.measure(AnnotatedString(text), style(fontPx, hero), softWrap = false)
}
