package com.mocharealm.accompanist.lyrics.folia.internal.text

import kotlin.math.floor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density

internal data class NaturalFontMetrics(val ascent:Float,val descent:Float)
/** Public font resolver, then platform font metrics; paragraph height has already been rounded. */
internal expect fun naturalFontMetrics(style:TextStyle,resolver:FontFamily.Resolver,density:Density):NaturalFontMetrics

internal fun cssFontBaseline(ascent:Float,descent:Float,lineHeight:Float):Float {
    val a=floor(ascent+.5f);val d=floor(descent+.5f)
    return a+floor((lineHeight-a-d)/2)
}

/** CSS rounds font ascent/descent separately, then floors half-leading for its baseline.
 * Values are logical CSS pixels; return an offset from the word box's top edge.
 */
internal fun cssBaseline(naturalBaseline:Float,naturalHeight:Float,lineHeight:Float):Float {
    val ascent=floor(naturalBaseline+.5f)
    val descent=floor(naturalHeight-naturalBaseline+.5f)
    return ascent+floor((lineHeight-ascent-descent)/2)
}

/** A paragraph's reported baseline can differ from the baseline of its painted blob. */
internal expect fun paintedLineBaseline(layout: androidx.compose.ui.text.TextLayoutResult, line: Int): Float
