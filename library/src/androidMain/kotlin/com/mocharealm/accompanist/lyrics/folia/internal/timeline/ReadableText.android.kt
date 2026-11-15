package com.mocharealm.accompanist.lyrics.folia.internal.timeline

internal actual fun hasReadableText(text:String):Boolean {
    var at=0
    while(at<text.length) {
        val cp=Character.codePointAt(text,at)
        if(Character.isLetter(cp) || Character.getType(cp) in intArrayOf(Character.DECIMAL_DIGIT_NUMBER.toInt(),Character.LETTER_NUMBER.toInt(),Character.OTHER_NUMBER.toInt())) return true
        at+=Character.charCount(cp)
    }
    return false
}
