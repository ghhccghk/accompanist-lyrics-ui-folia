package com.mocharealm.accompanist.folia.sample

import com.mocharealm.accompanist.folia.sample.importing.*
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import kotlin.test.*

class ImportTest {
    @Test fun standardAndEnhancedUsePublishedParser() {
        val standard = parseLrc("test.lrc", "[ti:原创测试]\n[00:01.00]第一句\n[00:03.00]第二句".encodeToByteArray(), 1)
        assertEquals("原创测试", standard.title)
        assertEquals(2, standard.lyrics.lines.size)
        assertEquals(8_000, standard.durationMillis)
        val enhanced = parseLrc("word.lrc", "[00:01.00]<00:01.00>Hello <00:02.00>light<00:03.00>".encodeToByteArray(), 2)
        val line = assertIs<KaraokeLine>(enhanced.lyrics.lines.single())
        assertEquals(2, line.syllables.size)
        assertEquals(2_000, line.syllables.first().end)
    }
    @Test fun rejectsInvalidAndOversizedContent() {
        assertFailsWith<IllegalArgumentException> { parseLrc("bad", "untimed text".encodeToByteArray(), 1) }
        assertFailsWith<IllegalArgumentException> { parseLrc("big", ByteArray(MAX_IMPORT_BYTES + 1), 1) }
        assertFails { parseLrc("encoding", byteArrayOf(0xFF.toByte()), 1) }
    }
    @Test fun staleSuccessFailureAndSceneSwitchAreIgnored() {
        val requests = ImportRequests()
        val first = requests.begin()
        val second = requests.begin()
        assertFalse(requests.accept(first))
        requests.fail(first, "stale")
        assertTrue(requests.loading)
        assertNull(requests.error)
        requests.invalidate()
        assertFalse(requests.accept(second))
        val third = requests.begin()
        requests.fail(third, "explicit error")
        assertEquals("explicit error", requests.error)
        assertFalse(requests.loading)
    }
}
