package com.mocharealm.accompanist.lyrics.folia

import com.mocharealm.accompanist.lyrics.folia.internal.rendering.bitmapFromArgb
import com.mocharealm.accompanist.lyrics.folia.internal.rendering.blurredMasks
import kotlin.random.Random
import kotlin.test.*

class GlowMaskTest {
    @Test fun sharedScratchMatchesIndependentNaiveConvolution() {
        val width = 9; val height = 7
        val random = Random(20261115)
        val input = IntArray(width * height) { random.nextInt(256) }
        val source = bitmapFromArgb(width, height, IntArray(input.size) { (input[it] shl 24) or 0xFFFFFF })
        val radii = intArrayOf(1, 4, 12, 1)
        val masks = blurredMasks(source, radii)
        radii.forEachIndexed { index, radius ->
            var expected = input.copyOf()
            repeat(3) {
                val horizontal = IntArray(input.size) { position ->
                    val x = position % width; val y = position / width
                    (-radius..radius).sumOf { delta -> if (x + delta in 0 until width) expected[y * width + x + delta] else 0 } / (2 * radius + 1)
                }
                expected = IntArray(input.size) { position ->
                    val x = position % width; val y = position / width
                    (-radius..radius).sumOf { delta -> if (y + delta in 0 until height) horizontal[(y + delta) * width + x] else 0 } / (2 * radius + 1)
                }
            }
            val actual = IntArray(input.size)
            masks[index].readPixels(actual)
            assertContentEquals(expected, IntArray(actual.size) { actual[it] ushr 24 }, "radius=$radius index=$index")
        }
        assertTrue(blurredMasks(source, intArrayOf()).isEmpty())
        assertFailsWith<IllegalArgumentException> { blurredMasks(source, intArrayOf(0)) }
    }
}
