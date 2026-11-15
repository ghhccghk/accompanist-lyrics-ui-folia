package com.mocharealm.accompanist.lyrics.folia.internal.text

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.Density

internal actual fun classicBodyRasterStyle(style:TextStyle):TextStyle = style

internal actual fun naturalFontMetrics(style:TextStyle,resolver:FontFamily.Resolver,density:Density):NaturalFontMetrics {
    val paint=android.graphics.Paint().apply {
        typeface=resolver.resolve(style.fontFamily,style.fontWeight?:FontWeight.Normal,
            style.fontStyle?:FontStyle.Normal,style.fontSynthesis?:FontSynthesis.All).value as android.graphics.Typeface
        textSize=with(density){style.fontSize.toPx()}
    }
    return NaturalFontMetrics(-paint.fontMetrics.ascent,paint.fontMetrics.descent)
}

// SkParagraph rounds the baseline when building a text blob; Android exposes its
// already integral line baseline directly. Keep this platform detail out of CSS geometry.
internal actual fun paintedLineBaseline(layout: androidx.compose.ui.text.TextLayoutResult, line: Int): Float = layout.getLineBaseline(line)

internal actual fun paintedNaturalBaseline(layout: androidx.compose.ui.text.TextLayoutResult, metrics: NaturalFontMetrics): Float =
    layout.getLineBaseline(0)
