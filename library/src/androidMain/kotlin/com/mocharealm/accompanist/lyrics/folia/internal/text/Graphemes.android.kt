package com.mocharealm.accompanist.lyrics.folia.internal.text

import com.ibm.icu.text.BreakIterator
import com.ibm.icu.util.ULocale
import com.ibm.icu.lang.UCharacter
import com.ibm.icu.lang.UCharacterDirection

internal actual fun isRtlLyric(text: String): Boolean {
    var offset=0
    while(offset<text.length) {
        val cp=Character.codePointAt(text,offset)
        if(UCharacter.isLetter(cp)) {
            val direction=UCharacter.getDirection(cp)
            return direction==UCharacterDirection.RIGHT_TO_LEFT || direction==UCharacterDirection.RIGHT_TO_LEFT_ARABIC
        }
        offset+=Character.charCount(cp)
    }
    return false
}

internal actual fun graphemeBoundaries(text: String): Set<Int> {
    val iterator = BreakIterator.getCharacterInstance(ULocale.ROOT)
    iterator.setText(text)
    return buildSet {
        var boundary = iterator.first()
        while (boundary != BreakIterator.DONE) { add(boundary); boundary = iterator.next() }
    }
}

internal actual fun segmentWords(text: String): List<WordSegment> {
    val iterator = BreakIterator.getWordInstance(ULocale.ROOT)
    iterator.setText(text)
    return buildList {
        var begin = iterator.first()
        var end = iterator.next()
        while (end != BreakIterator.DONE) {
            add(WordSegment(text.substring(begin, end), iterator.ruleStatus >= BreakIterator.WORD_NUMBER))
            begin = end; end = iterator.next()
        }
    }
}
