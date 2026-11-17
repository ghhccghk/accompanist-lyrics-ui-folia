// Copyright (c) 2026 chthollyphile and contributors
// wordColoring.ts resolveWordColor (default target-contains-token mode), cc6226e4.
// SPDX-License-Identifier: AGPL-3.0-only. Source/adaptations: docs/upstream.md.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.cadenza

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

internal data class CadenzaPalette(val primary: Color=Color(0xffdfe5ec),val accent:Color=Color(0xfff17b4d),
    val words:List<Pair<String,Color>> = emptyList()) {
    fun active(text:String):Color {
        val current=text.trim(::jsSpace)
        if(current.isEmpty())return accent
        return words.firstOrNull{(entry,_)->
            val target=entry.trim(::jsSpace)
            target.isNotEmpty()&&if(cadenzaIsCjk(current))target.contains(current)else {
                val normalized=asciiWord(current)
                normalized.isNotEmpty()&&target.split(Regex("[\\s\\u00a0\\u1680\\u2000-\\u200a\\u2028\\u2029\\u202f\\u205f\\u3000\\ufeff]+"))
                    .map(::asciiWord).contains(normalized)
            }
        }?.second?:accent
    }
    fun encoded(text:String)="#"+active(text).toArgb().toUInt().toString(16).padStart(8,'0')
}

private fun asciiWord(s:String)=s.lowercase().filter{it in 'a'..'z'||it in '0'..'9'||it=='_'}
private fun jsSpace(c:Char)=c in "\t\n\u000b\u000c\r \u00a0\u1680\u2028\u2029\u202f\u205f\u3000\ufeff"||c in '\u2000'..'\u200a'
internal fun cadenzaInk(encoded:String):Color {
    val hex=encoded.removePrefix("#")
    require(hex.length==6||hex.length==8){"Internal Cadenza color must be encoded RGB/ARGB"}
    return Color((hex.toLong(16) or if(hex.length==6)0xff000000L else 0).toInt())
}
