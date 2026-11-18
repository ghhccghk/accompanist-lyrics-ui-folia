package top.ghhccghk.accompanist.folia.sample.importing

import java.io.EOFException
import java.io.InputStream
import java.nio.ByteBuffer
import java.io.PushbackInputStream
import java.nio.charset.CodingErrorAction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Sample-only, bounded metadata reader, not an audio decoder. See docs/embedded-lyrics.md. */
internal suspend fun readEmbeddedLyrics(open: () -> InputStream): EmbeddedLyrics {
    val context = currentCoroutineContext()
    return try {
        open().use { EmbeddedTagReader(it) { context.ensureActive() }.read() }
    } catch (cancelled: CancellationException) { throw cancelled }
    catch (failure: Exception) {
        EmbeddedLyrics(notice = "内嵌歌词读取失败：${failure.message ?: failure.javaClass.simpleName}；可补选外部歌词。")
    }
}

/** No full song reads: cover/audio payloads are skipped; metadata reads <= 16 MiB. */
internal class EmbeddedTagReader(input: InputStream, private val checkActive: () -> Unit = {}) {
    private val input = PushbackInputStream(input, 12)
    private var position = 0L
    private var readBytes = 0
    private var chunks = 0
    private var textBytes = 0
    private val texts = mutableListOf<String>()
    private var notice: String? = null

    fun read(): EmbeddedLyrics {
        val header = bytes(12, eofAllowed = true) ?: return EmbeddedLyrics()
        input.unread(header); position = 0
        when {
            header.ascii(0, 3) == "ID3" -> id3(bytes(10)!!, 0)
            header.ascii(0, 4) == "fLaC" -> { bytes(4); flac() }
            header.ascii(0, 4) == "RIFF" && header.ascii(8, 4) == "WAVE" -> { bytes(12); wave(header.uint(4, false) + 8) }
            header.ascii(4, 4) in setOf("ftyp", "moov", "free", "wide", "mdat") -> mp4(Long.MAX_VALUE, 0)
            header.ascii(0, 4) == "OggS" -> notice = "暂不读取 Ogg 容器的内嵌歌词；可补选外部歌词。"
        }
        return EmbeddedLyrics(texts, notice)
    }

    private fun tick() { checkActive(); require(++chunks <= 8192) { "音频元数据块过多" } }
    private fun bytes(size: Int, eofAllowed: Boolean = false): ByteArray? {
        require(size >= 0 && size <= 16 * MAX_IMPORT_BYTES - readBytes) { "音频元数据超过 16 MiB 读取限制" }
        val result = ByteArray(size)
        var count = 0
        while (count < size) {
            checkActive()
            val n = input.read(result, count, size - count)
            if (n < 0) {
                if (count == 0 && eofAllowed) return null
                throw EOFException("音频元数据被截断")
            }
            if (n == 0) throw EOFException("音频流没有返回数据")
            position += n; readBytes += n; count += n
        }
        return result
    }
    private fun skip(size: Long) {
        require(size >= 0 && size <= 32L * 1024 * MAX_IMPORT_BYTES) { "无效音频元数据长度" }
        var remaining = size
        while (remaining > 0) {
            checkActive()
            val n = input.skip(remaining)
            if (n > 0) { remaining -= n; position += n }
            else {
                val count = minOf(remaining, 8192).toInt()
                bytes(count); remaining -= count
            }
        }
    }
    private fun add(text: String) {
        val clean = text.trimEnd('\u0000')
        if (clean.isBlank() || clean in texts) return
        textBytes += clean.toByteArray(Charsets.UTF_8).size
        require(textBytes <= MAX_IMPORT_BYTES && texts.size < 16) { "内嵌歌词超过 1 MiB 或 16 个字段限制" }
        texts += clean
    }

    private fun id3(prefix: ByteArray, containerEnd: Long) {
        require(prefix.size >= 10 && prefix.ascii(0, 3) == "ID3") { "无效 ID3 标签" }
        val version = prefix[3].toInt() and 255
        require(version in 2..4) { "不支持 ID3v2.$version" }
        val flags = prefix[5].toInt() and 255
        val size = prefix.syncSafe(6)
        require(size <= 8 * MAX_IMPORT_BYTES) { "ID3 标签超过 8 MiB 限制" }
        val alreadyRead = prefix.size - 10
        require(size >= alreadyRead && (containerEnd == 0L || position - prefix.size + 10 + size <= containerEnd)) {
            "ID3 标签长度无效"
        }
        var data = prefix.copyOfRange(10, prefix.size) + bytes(size - alreadyRead)!!
        if (version == 2 && flags and 0x40 != 0) {
            notice = "暂不读取压缩 ID3v2.2 歌词；可补选外部歌词。"; return
        }
        if (version < 4 && flags and 0x80 != 0) data = data.deUnsync()
        var at = 0
        if (version >= 3 && flags and 0x40 != 0) {
            require(data.size >= 4) { "ID3 扩展头被截断" }
            at = if (version == 4) data.syncSafe(0) else data.uint(0).toInt() + 4
            require(at in 4..data.size) { "ID3 扩展头长度无效" }
        }
        val headerSize = if (version == 2) 6 else 10
        while (at + headerSize <= data.size && data[at] != 0.toByte()) {
            tick()
            val id = data.ascii(at, if (version == 2) 3 else 4)
            val length = when (version) {
                2 -> data.uint(at + 3, length = 3)
                4 -> data.syncSafe(at + 4).toLong()
                else -> data.uint(at + 4)
            }
            require(length <= data.size - at - headerSize) { "ID3 帧 $id 被截断" }
            val formatFlags = if (version >= 3) data[at + 9].toInt() and 255 else 0
            val interesting = id in setOf("USLT", "ULT", "TXXX", "TXX", "SYLT", "SLT")
            if (interesting) {
                val unsupported = if (version == 3) formatFlags and 0xE0 != 0 else formatFlags and 0x4D != 0
                if (id == "SYLT" || id == "SLT" || unsupported) {
                    notice = "存在暂不支持的 SYLT／压缩／加密或分组歌词帧；可补选外部歌词。"
                } else {
                    var body = data.copyOfRange(at + headerSize, at + headerSize + length.toInt())
                    if (version == 4 && (flags and 0x80 != 0 || formatFlags and 2 != 0)) body = body.deUnsync()
                    id3Text(id, body)
                }
            }
            at += headerSize + length.toInt()
        }
    }

    private fun id3Text(id: String, body: ByteArray) {
        require(body.isNotEmpty()) { "ID3 文本帧为空" }
        val encoding = body[0].toInt() and 255
        require(encoding in 0..3) { "未知 ID3 文本编码" }
        val start = if (id == "USLT" || id == "ULT") 4 else 1
        require(body.size >= start) { "ID3 歌词帧缺少语言字段" }
        val width = if (encoding in 1..2) 2 else 1
        var end = start
        while (end + width <= body.size && !(body[end] == 0.toByte() && (width == 1 || body[end + 1] == 0.toByte()))) end += width
        require(end + width <= body.size) { "ID3 歌词描述缺少结束符" }
        val description = decode(body.copyOfRange(start, end), encoding)
        if (id == "TXXX" || id == "TXX") {
            if (description.uppercase() !in setOf("LYRICS", "UNSYNCEDLYRICS", "UNSYNCED LYRICS", "SYNCEDLYRICS")) return
        }
        // UTF-16 text may inherit descriptor BOM, rather than repeat it.
        val littleEndian = encoding == 1 && start + 1 < body.size && body[start] == 0xFF.toByte() && body[start + 1] == 0xFE.toByte()
        add(decode(body.copyOfRange(end + width, body.size), encoding, littleEndian))
    }

    private fun flac() {
        while (true) {
            tick()
            val header = bytes(4)!!
            val size = header.uint(1, length = 3).toInt()
            if (header[0].toInt() and 0x7F == 4) comments(bytes(size)!!)
            else skip(size.toLong())
            if (header[0].toInt() and 0x80 != 0) return
        }
    }

    private fun comments(data: ByteArray) {
        var at = 0
        fun field(): ByteArray {
            require(at + 4 <= data.size) { "FLAC 注释长度缺失" }
            val length = data.uint(at, false); at += 4
            require(length <= data.size - at) { "FLAC 注释被截断" }
            return data.copyOfRange(at, at + length.toInt()).also { at += length.toInt() }
        }
        field() // vendor string
        require(at + 4 <= data.size) { "FLAC 注释数量缺失" }
        val count = data.uint(at, false); at += 4
        require(count <= 8192) { "FLAC 注释过多" }
        repeat(count.toInt()) {
            tick()
            val raw = field()
            val separator = raw.indexOf('='.code.toByte())
            if (separator > 0 && raw.ascii(0, separator).uppercase() in setOf("LYRICS", "UNSYNCEDLYRICS", "SYNCEDLYRICS")) {
                add(decode(raw.copyOfRange(separator + 1, raw.size), 3))
            }
        }
    }

    private fun wave(end: Long) {
        while (position + 8 <= end) {
            tick()
            val header = bytes(8)!!
            val size = header.uint(4, false)
            require(size <= end - position) { "WAV 块长度无效" }
            if (header.ascii(0, 4).lowercase() == "id3 ") {
                require(size >= 10) { "WAV ID3 块过短" }
                val chunkEnd = position + size
                id3(bytes(10)!!, chunkEnd)
                skip(chunkEnd - position)
            } else skip(size)
            if (size % 2 == 1L) skip(1)
        }
    }

    private fun mp4(end: Long, depth: Int, lyrics: Boolean = false) {
        require(depth <= 8) { "MP4 元数据嵌套过深" }
        while (position < end) {
            tick()
            require(end - position >= 8) { "MP4 atom 头被截断" }
            val start = position
            val header = bytes(8, eofAllowed = end == Long.MAX_VALUE) ?: return
            var length = header.uint(0)
            val type = header.ascii(4, 4)
            val extended = if (length == 1L) bytes(8)!! else null
            if (extended != null) {
                require(extended.uint(0) <= 7) { "MP4 atom 长度超出读取范围" }
                length = (extended.uint(0) shl 32) or extended.uint(4)
            }
            if (length == 0L && end == Long.MAX_VALUE) return // remainder of file, no following metadata
            val atomEnd = if (length == 0L) end else start + length
            require(atomEnd >= position && atomEnd <= end) { "MP4 atom 长度无效" }
            when {
                type in setOf("moov", "udta", "ilst", "©lyr") || type == "meta" -> {
                    if (type == "meta") {
                        require(atomEnd - position >= 4) { "MP4 meta 头缺失" }
                        bytes(4)
                        mp4(atomEnd, depth + 1)
                    } else {
                        mp4(atomEnd, depth + 1, type == "©lyr")
                    }
                }
                type == "data" && lyrics -> {
                    val size = atomEnd - position
                    require(size <= MAX_IMPORT_BYTES + 8) { "MP4 歌词字段过大" }
                    val payload = bytes(size.toInt())!!
                    require(payload.size >= 8) { "MP4 歌词 data 头缺失" }
                    when (payload.uint(0) and 0xFFFFFF) {
                        1L -> add(decode(payload.copyOfRange(8, payload.size), 3))
                        2L -> add(decode(payload.copyOfRange(8, payload.size), 2))
                        else -> notice = "暂不支持此 MP4 歌词文本编码；可补选外部歌词。"
                    }
                }
            }
            skip(atomEnd - position)
        }
        require(position == end) { "MP4 子 atom 越界" }
    }

    private fun decode(data: ByteArray, encoding: Int, littleEndian: Boolean = false): String {
        require(data.size <= MAX_IMPORT_BYTES) { "内嵌歌词字段超过 1 MiB" }
        val charset = when (encoding) {
            0 -> Charsets.ISO_8859_1
            1 -> if (data.size >= 2 && (data[0] == 0xFE.toByte() && data[1] == 0xFF.toByte() || data[0] == 0xFF.toByte() && data[1] == 0xFE.toByte()))
                Charsets.UTF_16 else if (littleEndian) Charsets.UTF_16LE else Charsets.UTF_16BE
            2 -> Charsets.UTF_16BE
            else -> Charsets.UTF_8
        }
        return charset.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(data)).toString().removePrefix("\uFEFF")
    }
}

private fun ByteArray.ascii(at: Int, size: Int) = String(this, at, size, Charsets.ISO_8859_1)
private fun ByteArray.uint(at: Int, bigEndian: Boolean = true, length: Int = 4): Long {
    require(at >= 0 && at + length <= size) { "元数据整数被截断" }
    var value = 0L
    for (i in 0 until length) value = (value shl 8) or (this[at + if (bigEndian) i else length - 1 - i].toLong() and 255)
    return value
}
private fun ByteArray.syncSafe(at: Int): Int {
    require(at >= 0 && at + 4 <= size) { "ID3 长度被截断" }
    var result = 0
    repeat(4) { require(this[at + it].toInt() and 0x80 == 0) { "ID3 syncsafe 长度无效" }; result = (result shl 7) or this[at + it].toInt() }
    return result
}
private fun ByteArray.deUnsync(): ByteArray {
    val result = ByteArray(size); var count = 0; var at = 0
    while (at < size) {
        result[count++] = this[at]
        if (this[at] == 0xFF.toByte() && at + 1 < size && this[at + 1] == 0.toByte()) at++
        at++
    }
    return result.copyOf(count)
}
