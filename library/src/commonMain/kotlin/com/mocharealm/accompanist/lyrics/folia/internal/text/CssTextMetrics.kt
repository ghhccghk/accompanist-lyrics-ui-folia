package com.mocharealm.accompanist.lyrics.folia.internal.text

import kotlin.math.floor

/** CSS rounds font ascent/descent separately, then floors half-leading for its baseline.
 * Values are logical CSS pixels; return an offset from the word box's top edge.
 */
internal fun cssBaseline(naturalBaseline:Float,naturalHeight:Float,lineHeight:Float):Float {
    val ascent=floor(naturalBaseline+.5f)
    val descent=floor(naturalHeight-naturalBaseline+.5f)
    return ascent+floor((lineHeight-ascent-descent)/2)
}
