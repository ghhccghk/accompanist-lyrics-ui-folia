package top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily

/** The public resolved Typeface does not expose its OpenType tables. The
 * isolated probe falls back to Paint metrics; CJK Canvas-middle equivalence on
 * Android is UNVERIFIED and remains required before any public Diorama preview. */
internal actual fun dioramaTypographicRatio(style: TextStyle, resolver: FontFamily.Resolver): Double? = null
