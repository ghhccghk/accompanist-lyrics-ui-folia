package com.mocharealm.accompanist.lyrics.folia

import com.mocharealm.accompanist.lyrics.folia.internal.text.cssWordText
import com.mocharealm.accompanist.lyrics.folia.internal.text.canvasWordText
import kotlin.test.*

class CssWordTextTest {
    @Test fun mapsOriginalBoundariesThroughCollapsedRuns() {
        val mapped=cssWordText("  a   b  ")
        assertEquals("a b",mapped.text)
        assertContentEquals(intArrayOf(0,0,0,1,2,2,2,3,3,3),mapped.boundaries)
        // 'b' keeps its source timing/range even though preceding spaces disappear.
        assertEquals("b",mapped.text.substring(mapped.boundaries[6],mapped.boundaries[7]))
    }
    @Test fun keepsNonbreakingSpacesAndWholeUnicodeSequences() {
        val source="\u00a0á👩‍🚀\u00a0"
        val mapped=cssWordText(source)
        assertEquals(source,mapped.text)
        assertContentEquals(IntArray(source.length+1){it},mapped.boundaries)
        assertEquals("",cssWordText(" \t\n\r\u000c").text)
        assertEquals("a b",cssWordText("a\t\r\n b").text)
        assertEquals("a    b",canvasWordText("a\t\r\n b"))
    }
}
