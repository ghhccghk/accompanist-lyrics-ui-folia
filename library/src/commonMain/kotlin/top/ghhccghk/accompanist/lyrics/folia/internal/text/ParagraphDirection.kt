// Folia classic/Visualizer.tsx dir={resolveLyricTextDirection(activeLine.fullText)}.
// SPDX-License-Identifier: AGPL-3.0-only. Source ledger: docs/upstream.md.
package top.ghhccghk.accompanist.lyrics.folia.internal.text

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection

/** Host overrides remain explicit; the default belongs to the entire lyric line. */
internal fun classicTextDirection(style: TextStyle, lineText: String): TextDirection =
    if (style.textDirection != TextDirection.Unspecified) style.textDirection
    else if (isRtlLyric(lineText)) TextDirection.Rtl else TextDirection.Ltr
