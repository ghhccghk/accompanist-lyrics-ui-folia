package com.mocharealm.accompanist.lyrics.folia.internal.timeline

internal actual fun hasReadableText(text:String):Boolean = text.codePoints().anyMatch {
    Character.isLetter(it) || Character.getType(it) in intArrayOf(Character.DECIMAL_DIGIT_NUMBER.toInt(),Character.LETTER_NUMBER.toInt(),Character.OTHER_NUMBER.toInt())
}
