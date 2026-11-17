package top.ghhccghk.accompanist.lyrics.folia.internal.styles.cappella

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density

/** Optional platform adapter for the pinned browser's Canvas advance boundary.
 * A null result uses ordinary complete-paragraph measurement, never made-up width. */
internal expect fun cappellaCanvasAdvance(text: String, style: TextStyle,
    resolver: FontFamily.Resolver, density: Density): Double?
