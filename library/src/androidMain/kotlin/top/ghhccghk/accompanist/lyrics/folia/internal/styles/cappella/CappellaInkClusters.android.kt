// SPDX-License-Identifier: AGPL-3.0-only
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.cappella

import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.os.LocaleList
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.*
import java.text.Bidi

internal actual fun cappellaInkClusters(text: String, style: TextStyle,
    resolver: FontFamily.Resolver, density: Density): List<CappellaInkCluster> {
    // Public cluster-advance API was added in 29. Older devices retain the
    // existing complete-row/caret painter, rather than calling hidden APIs.
    if (Build.VERSION.SDK_INT < 29 || text.isEmpty()) return emptyList()
    if (style.letterSpacing.let { it != TextUnit.Unspecified && it.value != 0f }) return emptyList()
    val face = resolver.resolve(style.fontFamily, style.fontWeight ?: FontWeight.Normal,
        style.fontStyle ?: FontStyle.Normal, style.fontSynthesis ?: FontSynthesis.All).value as? Typeface ?: return emptyList()
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
        typeface = face
        textSize = with(density) { style.fontSize.toPx() }
        fontFeatureSettings = style.fontFeatureSettings
        style.localeList?.takeUnless { it.isEmpty() }?.let {
            textLocales = LocaleList.forLanguageTags(it.joinToString(",") { locale -> locale.toLanguageTag() })
        }
    }
    val direction = when (style.textDirection) {
        TextDirection.Ltr -> Bidi.DIRECTION_LEFT_TO_RIGHT
        TextDirection.Rtl -> Bidi.DIRECTION_RIGHT_TO_LEFT
        TextDirection.ContentOrRtl -> Bidi.DIRECTION_DEFAULT_RIGHT_TO_LEFT
        else -> Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT
    }
    val bidi = Bidi(text, direction)
    val advances = FloatArray(text.length)
    val chars = text.toCharArray()
    val result = mutableListOf<CappellaInkCluster>()
    for (run in 0 until bidi.runCount) {
        val start = bidi.getRunStart(run); val end = bidi.getRunLimit(run)
        paint.getTextRunAdvances(chars, start, end - start, start, end - start,
            bidi.getRunLevel(run) and 1 != 0, advances, start)
        var begin = start
        for (i in start + 1 until end) if (advances[i] != 0f) {
            result += CappellaInkCluster(begin, i)
            begin = i
        }
        result += CappellaInkCluster(begin, end)
    }
    return result
}
