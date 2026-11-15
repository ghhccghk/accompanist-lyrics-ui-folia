package com.mocharealm.accompanist.lyrics.folia.internal.text

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.platform.FontLoadResult
import androidx.compose.ui.unit.Density

internal actual fun naturalFontMetrics(style:TextStyle,resolver:FontFamily.Resolver,density:Density):NaturalFontMetrics {
    val loaded=resolver.resolve(style.fontFamily,style.fontWeight?:FontWeight.Normal,
        style.fontStyle?:FontStyle.Normal,style.fontSynthesis?:FontSynthesis.All).value as FontLoadResult
    org.jetbrains.skia.Font(loaded.typeface,with(density){style.fontSize.toPx()}).use {font->
        val metrics=font.metrics
        return NaturalFontMetrics(-metrics.ascent,metrics.descent)
    }
}

// SkParagraph rounds the baseline when building a text blob; Android exposes its
// already integral line baseline directly. Keep this platform detail out of CSS geometry.
internal actual fun paintedLineBaseline(layout: androidx.compose.ui.text.TextLayoutResult, line: Int): Float = kotlin.math.floor(layout.getLineBaseline(line)+.5f)
