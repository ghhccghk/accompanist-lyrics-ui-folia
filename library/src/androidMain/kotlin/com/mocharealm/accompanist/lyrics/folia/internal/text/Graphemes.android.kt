package com.mocharealm.accompanist.lyrics.folia.internal.text

import com.ibm.icu.text.BreakIterator
import com.ibm.icu.util.ULocale

internal actual fun graphemeBoundaries(text: String): Set<Int> {
    val iterator = BreakIterator.getCharacterInstance(ULocale.ROOT)
    iterator.setText(text)
    return buildSet {
        var boundary = iterator.first()
        while (boundary != BreakIterator.DONE) { add(boundary); boundary = iterator.next() }
    }
}
