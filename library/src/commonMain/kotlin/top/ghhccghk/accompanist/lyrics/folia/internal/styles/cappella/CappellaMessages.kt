// Copyright (c) 2026 chthollyphile and contributors
// Folia cc6226e4 cappellaMessages.ts, cappellaMessageSenders.ts, cappellaIntensity.ts.
// SPDX-License-Identifier: AGPL-3.0-only. See docs/upstream.md.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.cappella

internal enum class CappellaSide { LEFT, RIGHT }
internal enum class CappellaIntensity { CALM, NORMAL, CHAOTIC }
internal data class CappellaSender(val side: CappellaSide, val avatar: Int)
internal data class CappellaMessage(val id: String, val text: String, val sender: CappellaSender,
    val line: Int? = null)

private fun jsSpace(c: Char) = c in "\t\n\u000b\u000c\r \u00a0\u1680\u2028\u2029\u202f\u205f\u3000\ufeff" || c in '\u2000'..'\u200a'
internal fun cappellaAgentId(value: String?): String? = value?.trim(::jsSpace)?.takeIf { it.isNotEmpty() }

/** ECMAScript decimal notation bounds, over the platform's shortest round-trip
 * representation. In particular 1.0 is seeded as "1", not "1.0". */
internal fun cappellaNumber(value: Double): String {
    if (value == 0.0) return "0"
    if (!value.isFinite()) return value.toString()
    val raw = value.toString().lowercase()
    val negative = raw.startsWith('-')
    val unsigned = raw.removePrefix("-")
    val exponent = unsigned.substringAfter('e', "0").toInt()
    val mantissa = unsigned.substringBefore('e')
    val digits = mantissa.replace(".", "").trimEnd('0')
    val decimal = mantissa.substringBefore('.').length + exponent
    val output = when {
        decimal in 1..21 -> if (decimal >= digits.length) digits.padEnd(decimal, '0')
            else digits.take(decimal) + "." + digits.drop(decimal)
        decimal in -5..0 -> "0." + "0".repeat(-decimal) + digits
        else -> digits.take(1) + (if (digits.length > 1) "." + digits.drop(1) else "") +
            "e" + (if (decimal - 1 >= 0) "+" else "") + (decimal - 1)
    }
    return (if (negative) "-" else "") + output
}

/** FNV1a over UTF-16 code units, not UTF-8 bytes or Unicode codepoints. */
internal fun cappellaSeed(vararg parts: String): Double {
    var hash = 2166136261u
    for (c in parts.joinToString("|")) hash = (hash xor c.code.toUInt()) * 16777619u
    return hash.toDouble() / 4294967295.0
}

private data class Sequence(val every: Int, val carry: Double, val sides: List<CappellaSide>, val flip: Double)
private fun sequence(intensity: CappellaIntensity): Sequence {
    val l = CappellaSide.LEFT; val r = CappellaSide.RIGHT
    return when (intensity) {
        CappellaIntensity.CALM -> Sequence(7, .92, listOf(l,l,r,l,r), .08)
        CappellaIntensity.NORMAL -> Sequence(5, .68, listOf(l,r,l,r,r), .18)
        CappellaIntensity.CHAOTIC -> Sequence(3, .36, listOf(l,r,r,l,r,l), .42)
    }
}

/** Source configuration with emoji disabled / an empty image pool. Messages remain
 * real title/lyric messages; this does not substitute invented image reactions. */
internal fun buildCappellaMessages(lines: List<CappellaLine>, title: String,
    intensity: CappellaIntensity = CappellaIntensity.NORMAL): List<CappellaMessage> {
    val right = CappellaSender(CappellaSide.RIGHT, 8)
    val agents = lines.mapNotNull { cappellaAgentId(it.agent) }.distinct()
    val byAgent = if (agents.size < 2) emptyMap() else agents.mapIndexed { i, id ->
        id to if (i == 0) right else CappellaSender(CappellaSide.LEFT, (i - 1) % 5)
    }.toMap()
    val config = sequence(intensity)
    var sideCursor = 0; var leftCursor = 0
    var last: CappellaSender? = null
    return buildList {
        add(CappellaMessage("title", title, right))
        lines.forEachIndexed { i, line ->
            val agent = byAgent[cappellaAgentId(line.agent)]
            // Array.from counts codepoints here (not graphemes).
            val compactCount = line.text.filterNot(::jsSpace).let { s ->
                s.indices.count { j -> !s[j].isLowSurrogate() || j == 0 || !s[j - 1].isHighSurrogate() }
            }
            val force = agent == null && (i + 1) % config.every == 0
            val start = cappellaNumber(line.start)
            val carry = agent == null && compactCount <= 12 && last != null &&
                cappellaSeed("carry", start, "$i") <= config.carry
            val base = config.sides[sideCursor % config.sides.size]
            val flip = !force && cappellaSeed("flip", start, "$i") < config.flip
            val resolved = if (flip) { if (base == CappellaSide.LEFT) CappellaSide.RIGHT else CappellaSide.LEFT } else base
            val sender = agent ?: if (force) right else if (carry) last!! else
                CappellaSender(resolved, if (resolved == CappellaSide.LEFT) leftCursor else 8)
            add(CappellaMessage("line-$start-$i", line.text, sender, i))
            when {
                agent != null -> last = sender
                force -> { sideCursor = 0; last = null }
                !carry -> {
                    if (sender.side == CappellaSide.LEFT) leftCursor++
                    sideCursor++; last = sender
                }
                else -> last = sender
            }
        }
    }
}

/** Source uses array order rather than assuming input times are sorted. */
internal fun cappellaVisibleLine(lines: List<CappellaLine>, time: Double): Int =
    lines.indexOfLast { time >= it.start }

/** The native text measurer supplies real message heights; the source viewport
 * rule still keeps at least two messages, with a hard maximum of twenty. */
internal fun visibleCappellaMessages(messages: List<CappellaMessage>, visibleLine: Int,
    viewportHeight: Double, currentLine: Int, estimateHeight: (CappellaMessage, Boolean) -> Double): List<CappellaMessage> {
    val eligible = messages.filter { it.line == null || it.line <= visibleLine }
    val usable = maxOf(200.0, viewportHeight - 240)
    var height = 0.0
    val result = ArrayDeque<CappellaMessage>()
    for (m in eligible.asReversed()) {
        val next = estimateHeight(m, m.line != null && m.line == currentLine)
        if (height + next > usable && result.size >= 2) break
        height += next; result.addFirst(m)
        if (result.size >= 20) break
    }
    return result.toList()
}
