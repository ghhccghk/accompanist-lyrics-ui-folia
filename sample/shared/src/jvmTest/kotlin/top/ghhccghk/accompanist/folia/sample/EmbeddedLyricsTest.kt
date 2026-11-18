package top.ghhccghk.accompanist.folia.sample

import top.ghhccghk.accompanist.folia.sample.importing.*
import top.ghhccghk.accompanist.folia.sample.playback.*
import top.ghhccghk.accompanist.lyrics.folia.FoliaPlaybackSnapshot
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import java.io.ByteArrayInputStream
import java.io.InputStream
import kotlinx.coroutines.*
import kotlin.test.*

class EmbeddedLyricsTest {
    private val timed = "[00:01.00]Amber 光\n[00:03.00]Echo 回声"
    private fun n(value: Long, size: Int = 4, little: Boolean = false): ByteArray = ByteArray(size) {
        (value shr (8 * if (little) it else size - 1 - it)).toByte()
    }
    private fun s(value: String) = value.toByteArray(Charsets.ISO_8859_1)
    private fun sync(value: Int) = ByteArray(4) { ((value shr (7 * (3 - it))) and 127).toByte() }
    private fun atom(type: String, body: ByteArray) = n(body.size + 8L) + s(type) + body
    private fun uslt(text: String = timed, encoding: Int = 3, description: String = ""): ByteArray {
        val charset = when (encoding) { 0 -> Charsets.ISO_8859_1; 1 -> Charsets.UTF_16; 2 -> Charsets.UTF_16BE; else -> Charsets.UTF_8 }
        return byteArrayOf(encoding.toByte()) + s("eng") + description.toByteArray(charset) +
            ByteArray(if (encoding in 1..2) 2 else 1) + text.toByteArray(charset)
    }
    private fun id3(body: ByteArray = uslt(), version: Int = 4, id: String = "USLT", flags: Int = 0): ByteArray {
        val frame = s(id) + (if (version == 4) sync(body.size) else n(body.size.toLong())) + byteArrayOf(0, flags.toByte()) + body
        return s("ID3") + byteArrayOf(version.toByte(), 0, 0) + sync(frame.size) + frame
    }
    private fun read(bytes: ByteArray) = EmbeddedTagReader(bytes.inputStream()).read()
    private fun source(embedded: EmbeddedLyrics = EmbeddedLyrics(), content: String = "", tagRead: () -> Unit = {}) = object : LyricsSource {
        override val name = "original fixture"
        override suspend fun read() = content.encodeToByteArray()
        override suspend fun embeddedLyrics(): EmbeddedLyrics { tagRead(); return embedded }
    }

    @Test fun id3VersionsAndEncodingsPreserveUnicodeAndDescriptorBoundaries() {
        for (version in 3..4) for (encoding in 1..3) {
            val result = read(id3(uslt(encoding = encoding, description = "中文注释"), version))
            assertEquals(listOf(timed), result.texts, "v$version encoding$encoding")
        }
        assertEquals(listOf("plain latin"), read(id3(uslt("plain latin", 0))).texts)
        val ult = uslt()
        val frame = s("ULT") + n(ult.size.toLong(), 3) + ult
        val v22 = s("ID3") + byteArrayOf(2, 0, 0) + sync(frame.size) + frame
        assertEquals(listOf(timed), read(v22).texts)
    }

    @Test fun customLyricsFramesUnsynchronisationAndUnsupportedSyltAreExplicit() {
        val custom = byteArrayOf(3) + "LYRICS\u0000$timed".encodeToByteArray()
        assertEquals(listOf(timed), read(id3(custom, id = "TXXX")).texts)
        assertTrue(read(id3(byteArrayOf(3) + "OTHER\u0000ignore".encodeToByteArray(), id = "TXXX")).texts.isEmpty())
        val latin = uslt("xÿàz", 0)
        val escaped = latin.flatMap { if (it == 0xFF.toByte()) listOf(it, 0.toByte()) else listOf(it) }.toByteArray()
        assertEquals(listOf("xÿàz"), read(id3(escaped, flags = 2)).texts)
        val sylt = read(id3(byteArrayOf(3, 0, 0, 0), id = "SYLT"))
        assertTrue(sylt.texts.isEmpty()); assertContains(sylt.notice!!, "SYLT")
        val compressed = read(id3(uslt(), flags = 8))
        assertTrue(compressed.texts.isEmpty()); assertNotNull(compressed.notice)
    }

    @Test fun id3ExtendedHeadersBomInheritanceAndLegacyTagUnsync() {
        val raw = uslt(encoding = 1, description = "词")
        // The text need not repeat the UTF-16 BOM established by the descriptor.
        val withoutTextBom = raw.toMutableList().also { bytes ->
            val start = 4 + "词".toByteArray(Charsets.UTF_16).size + 2
            bytes.removeAt(start); bytes.removeAt(start)
        }.toByteArray()
        assertEquals(listOf(timed), read(id3(withoutTextBom)).texts)
        val base = id3(version = 3)
        val ext = n(6) + ByteArray(6)
        val data = ext + base.copyOfRange(10, base.size)
        val extended = s("ID3") + byteArrayOf(3, 0, 0x40) + sync(data.size) + data
        assertEquals(listOf(timed), read(extended).texts)
        val latinBase = id3(uslt("xÿàz", 0), version = 3)
        val escaped = latinBase.copyOfRange(10, latinBase.size).flatMap {
            if (it == 0xFF.toByte()) listOf(it, 0.toByte()) else listOf(it)
        }.toByteArray()
        assertEquals(listOf("xÿàz"), read(s("ID3") + byteArrayOf(3, 0, 0x80.toByte()) + sync(escaped.size) + escaped).texts)
    }

    @Test fun flacVorbisCommentsIgnoreCoverAndNonLyricsAndRetainTimedAlternatives() {
        fun field(text: String) = text.encodeToByteArray().let { n(it.size.toLong(), little = true) + it }
        val comments = field("original generator") + n(3, little = true) + field("TITLE=Not lyrics") +
            field("LYRICS=plain version") + field("UNSYNCEDLYRICS=$timed")
        val data = s("fLaC") + byteArrayOf(0) + n(34, 3) + ByteArray(34) +
            byteArrayOf(0x84.toByte()) + n(comments.size.toLong(), 3) + comments
        assertEquals(listOf("plain version", timed), read(data).texts)
    }

    @Test fun mp4LyricsAtomFollowsSkippedAudioAndRejectsUnrelatedData() {
        val unrelated = atom("covr", atom("data", n(1) + n(0) + "not lyrics".encodeToByteArray()))
        val lyrics = atom("©lyr", atom("data", n(1) + n(0) + timed.encodeToByteArray()))
        val tree = atom("moov", atom("udta", atom("meta", n(0) + atom("ilst", unrelated + lyrics))))
        val bytes = atom("ftyp", s("M4A ") + n(0)) + atom("mdat", ByteArray(4000)) + tree
        assertEquals(listOf(timed), read(bytes).texts)
        // Extended-size top-level atom and UTF-16BE lyrics.
        val utf16 = atom("moov", atom("udta", atom("meta", n(0) + atom("ilst",
            atom("©lyr", atom("data", n(2) + n(0) + timed.toByteArray(Charsets.UTF_16BE)))))))
        assertEquals(listOf(timed), read(n(1) + s("free") + n(0) + n(16) + utf16).texts)
    }

    @Test fun wavId3ChunkAfterPcmUsesRiffPadding() {
        val chunk = id3()
        val payload = s("WAVE") + s("JUNK") + n(1, little = true) + byteArrayOf(0, 0) +
            s("data") + n(20, little = true) + ByteArray(20) +
            s("id3 ") + n(chunk.size.toLong(), little = true) + chunk + ByteArray(chunk.size % 2)
        assertEquals(listOf(timed), read(s("RIFF") + n(payload.size.toLong(), little = true) + payload).texts)
    }

    @Test fun boundedReadsMalformedDataVisibleAndCancellationClosesStream() = runBlocking {
        val malformed = s("ID3") + byteArrayOf(4, 0, 0) + sync(9 * MAX_IMPORT_BYTES) + byteArrayOf(0, 0)
        val oversized = readEmbeddedLyrics { malformed.inputStream() }
        assertTrue(oversized.texts.isEmpty()); assertContains(oversized.notice!!, "8 MiB")
        val invalidUtf8 = readEmbeddedLyrics { id3(byteArrayOf(3) + s("eng") + byteArrayOf(0, 0xC3.toByte())).inputStream() }
        assertNotNull(invalidUtf8.notice)
        val brokenAtom = atom("ftyp", s("M4A ")) + n(7) + s("moov")
        assertNotNull(readEmbeddedLyrics { brokenAtom.inputStream() }.notice)
        var closed = false
        // Cancellation from within an opened stream must propagate, not become a tag warning.
        val cancelled = object : ByteArrayInputStream(id3()) {
            override fun read(buffer: ByteArray, offset: Int, len: Int): Int { throw CancellationException("test cancellation") }
            override fun close() { closed = true }
        }
        try { readEmbeddedLyrics { cancelled }; fail("cancellation swallowed") }
        catch (_: CancellationException) { assertTrue(closed) }
        val hugeUnknown = object : InputStream() {
            val prefix = (atom("ftyp", s("M4A ")) + n(100_000_008) + s("mdat") + ByteArray(100)).inputStream()
            var read = 0
            override fun read() = error("single byte read")
            override fun read(b: ByteArray, off: Int, len: Int): Int { read += len; return prefix.read(b, off, len) }
            override fun skip(n: Long) = prefix.skip(n)
        }
        assertNotNull(readEmbeddedLyrics { hugeUnknown }.notice)
        assertTrue(hugeUnknown.read < 20000, "must not read an entire audio payload")
    }

    @Test fun externalPrecedenceTimedTagSelectionPlainFallbackAndTranslation() = runBlocking {
        val audio = source(EmbeddedLyrics(listOf("plain first", timed)))
        val loaded = loadSongContent(SongFiles(audio))
        assertEquals("内嵌计时歌词", loaded.sourceLabel); assertEquals(listOf(1000, 3000), loaded.lyrics!!.lines.map { it.start })
        val enhanced = "[00:01.00]<00:01.00>Amber <00:01.50>光\n[00:03.00]Echo"
        val wordTimed = loadSongContent(SongFiles(source(EmbeddedLyrics(listOf(enhanced)))))
        val first = assertIs<KaraokeLine>(wordTimed.lyrics!!.lines.first())
        assertTrue(first.syllables.any { it.start == 1500 })
        val external = source(content = "[00:02.00]External")
        val mustNotReadTags = source(tagRead = { error("explicit lyrics must bypass metadata") })
        assertEquals(2000, loadSongContent(SongFiles(mustNotReadTags, external)).lyrics!!.lines.first().start)
        val translation = source(content = "[00:01.00]光影\n[00:03.00]世界")
        val translated = loadSongContent(SongFiles(audio, translation = translation))
        assertEquals(listOf("光影", "世界"), translated.lyrics!!.lines.map { (it as SyncedLine).translation })
        val plain = loadSongContent(SongFiles(source(EmbeddedLyrics(listOf("No timestamp\n原创文字")))))
        assertNull(plain.lyrics); assertEquals("No timestamp\n原创文字", plain.plainText); assertNotNull(plain.notice)
        assertNull(loadSongContent(SongFiles(source())).plainText)
        assertFailsWith<IllegalArgumentException> { loadSongContent(SongFiles(audio, source(content = "broken"))) }
        assertFailsWith<IllegalArgumentException> { loadSongContent(SongFiles(source(), translation = translation)) }
        Unit
    }

    private class Player : LocalAudioPlayer {
        override val metadata = SongMetadata("Generated audio")
        override val durationMillis = 12000L
        override var snapshot = FoliaPlaybackSnapshot(0, false)
        var closed = false
        override fun poll() {}
        override fun play() { snapshot = snapshot.copy(isPlaying = true) }
        override fun pause() { snapshot = snapshot.copy(isPlaying = false) }
        override fun seek(positionMillis: Long) { snapshot = snapshot.copy(positionMillis = positionMillis, discontinuity = snapshot.discontinuity + 1) }
        override fun close() { closed = true }
    }

    @Test fun contentSwapPreservesTransportAndRejectsStaleTagRequests() = runBlocking {
        val session = SongSession(); val first = Player()
        val audio = source(EmbeddedLyrics(listOf(timed)))
        assertTrue(session.openContent(SongFiles(audio), { first }, ::loadSongContent))
        session.control { it.seek(2200); it.pause() }; val snapshot = session.snapshot
        assertTrue(session.attachContent(SongFiles(audio, source(content = "[00:01.00]External")), ::loadSongContent))
        assertEquals(snapshot, session.snapshot); assertSame(first, session.current!!.player)
        assertEquals("外部歌词", session.current!!.content.sourceLabel)
        assertTrue(session.attachContent(SongFiles(audio), ::loadSongContent))
        assertEquals("内嵌计时歌词", session.current!!.content.sourceLabel)
        assertEquals(snapshot, session.snapshot)
        val gate = CompletableDeferred<Unit>(); var preparedStale = false
        val stale = async(start = CoroutineStart.UNDISPATCHED) {
            session.openContent(SongFiles(audio), { preparedStale = true; Player() }) { gate.await(); loadSongContent(it) }
        }
        val fresh = Player()
        assertTrue(session.openContent(SongFiles(source()), { fresh }, ::loadSongContent))
        gate.complete(Unit); assertFalse(stale.await()); assertSame(fresh, session.current!!.player)
        assertTrue(first.closed)
        // Stale parsing must not acquire an audio device at all.
        assertFalse(preparedStale)
        session.close(); assertTrue(fresh.closed)
    }

}
