package top.ghhccghk.accompanist.lyrics.folia.internal.text

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.platform.FontLoadResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.PlatformParagraphStyle
import androidx.compose.ui.text.FontRasterizationSettings
import androidx.compose.ui.text.FontSmoothing
import androidx.compose.ui.text.FontHinting

@OptIn(ExperimentalTextApi::class)
internal actual fun classicBodyRasterStyle(style:TextStyle):TextStyle {
    if(!System.getProperty("os.name").startsWith("Windows") || style.platformStyle?.paragraphStyle!=null) return style
    // Match the pinned Windows browser's foreground coverage. Text-shadow input
    // stays grayscale; explicit host paragraph settings always take precedence.
    return style.copy(platformStyle=PlatformTextStyle(style.platformStyle?.spanStyle,
        PlatformParagraphStyle(FontRasterizationSettings(FontSmoothing.SubpixelAntiAlias,FontHinting.Normal,true,false))))
}

internal actual fun naturalFontMetrics(style:TextStyle,resolver:FontFamily.Resolver,density:Density):NaturalFontMetrics {
    val loaded=resolver.resolve(style.fontFamily,style.fontWeight?:FontWeight.Normal,
        style.fontStyle?:FontStyle.Normal,style.fontSynthesis?:FontSynthesis.All).value as FontLoadResult
    org.jetbrains.skia.Font(loaded.typeface,with(density){style.fontSize.toPx()}).use {font->
        val metrics=font.metrics
        return NaturalFontMetrics(-metrics.ascent,metrics.descent)
    }
}

// SkParagraph reports offset + roundedHeight - descent, but paints the blob at
// offset + round(ascent). The cursor's unrounded top exposes that height delta.
// Classic subtitles use uniform styling, Center leading and Trim.None; each
// previous line's unrounded bottom gives the next integer paint origin.
// getLineBottom floors floating-point noise (20.999998 -> 20), so cannot be
// used for that origin. No private paragraph fields or per-glyph paint needed.
// Keep this renderer detail separate from the original CSS row geometry.
internal actual fun paintedLineBaseline(layout: androidx.compose.ui.text.TextLayoutResult, line: Int): Float {
    val origin = if (line == 0) 0f else kotlin.math.floor(
        layout.getCursorRect(layout.getLineStart(line - 1)).bottom + .5f)
    val cursorTop = layout.getCursorRect(layout.getLineStart(line)).top
    return origin + kotlin.math.floor(layout.getLineBaseline(line) - cursorTop + .5f)
}

// Native TextLine baseline sweeps match natural-height SkParagraph alpha exactly
// at rounded raw ascent, including sizes where rounded reported baseline differs.
internal actual fun paintedNaturalBaseline(layout: androidx.compose.ui.text.TextLayoutResult, metrics: NaturalFontMetrics): Float =
    kotlin.math.floor(metrics.ascent+.5f)
