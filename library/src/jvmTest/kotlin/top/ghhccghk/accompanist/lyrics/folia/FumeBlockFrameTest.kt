package top.ghhccghk.accompanist.lyrics.folia

import top.ghhccghk.accompanist.lyrics.folia.internal.styles.fume.*
import java.io.File
import java.util.Base64
import kotlin.test.*

class FumeBlockFrameTest {
    private fun rows(name: String) = File("../verification/fume-fixtures/$name.tsv").readLines()
        .filterNot { it.startsWith('#') || it.isBlank() }.map { it.split('\t') }
    private fun decode(s: String) = String(Base64.getDecoder().decode(s), Charsets.UTF_8)
    private val lines = rows("inputs").associate { f -> f[0] to FumeLyric(decode(f[1]), f[2].toDouble(), f[3].toDouble(),
        if (f[4] == "-") emptyList() else f[4].split('|').map { d -> val w = d.split('~')
            FumeWord(decode(w[0]), w[1].toDouble(), w[2].toDouble(),
                if (w[3].isEmpty()) emptyList() else w[3].split(';').map { s -> val v = s.split('@'); FumeWord(decode(v[0]), v[1].toDouble(), v[2].toDouble()) })
        }) }
    private data class Frame(val block: FumeArticleBlock, val reveal: FumeReveal, val timing: FumeBlockTiming,
        val theme: FumeFrameTheme, val time: Double, val mode: Int)
    private fun frame(f: List<String>): Frame {
        val line = lines.getValue(f[1]); val hero = f[2].toBoolean(); val mode = f[3].toInt(); val t = f[4].toDouble()
        val block = FumeArticleBlock(0, line, hero, 13.0, 27.0, 320.0, 54.0, 320.0, 32.73, 44.125, 1)
        val theme = FumeFrameTheme(FumeInk(223,229,236), FumeInk(241,123,77), listOf("normal","calm","chaotic")[mode],
            listOf(1.0,.4,1.7)[mode], listOf("amber echo" to FumeInk(48,202,255), "远方" to FumeInk(41,212,117)))
        return Frame(block, FumeReveal(line), fumeBlockTiming(block, t, if (mode == 2) .8 else null, listOf(1.0,.3,0.0)[mode]), theme, t, mode)
    }
    private fun assertNumbers(expected: String, actual: List<Double>, context: String) {
        val numbers = expected.split(',').map(String::toDouble)
        assertEquals(numbers.size, actual.size, context)
        numbers.zip(actual).forEachIndexed { i, (e,a) -> assertEquals(e,a,1e-8,"$context field $i") }
    }
    private fun checkPaint(expected: String, actual: FumePaint, context: String) {
        val p = expected.split(';')
        fun channels(c: FumeInk) = listOf(c.red.toDouble(),c.green.toDouble(),c.blue.toDouble(),c.alpha)
        assertNumbers(p[0],channels(actual.fill),"$context fill")
        assertEquals(p[1].toDouble(),actual.blur,1e-8,"$context blur")
        assertNumbers(p[2],channels(actual.shadow),"$context shadow")
    }
    @Test fun timingAndLineGlowMatchOriginalDraw() {
        for (f in rows("frames")) {
            val a = frame(f); val t = a.timing; val e = f[5].split(',')
            assertNumbers(e.take(10).joinToString(","), listOf(t.waiting,t.active,if(t.dimmed)1.0 else 0.0,
                t.baselineOffset,t.end,t.cutoff,if(t.revealComplete)1.0 else 0.0,if(t.cutoffReached)1.0 else 0.0,t.duration,t.trail),"frame ${f[0]}")
            assertEquals(e[10],t.staticState ?: "-",f.toString())
            val glow = fumeLineGlow(a.block,t,a.theme,a.time)
            if (f[6] == "-") assertNull(glow,f.toString()) else checkPaint(f[6],assertNotNull(glow),"frame ${f[0]} line glow")
        }
    }
    @Test fun everyGlyphColorShadowAndPrintStampMatchRecordedCanvas() {
        val frames = rows("frames").associate { it[0] to frame(it) }
        val expected = rows("glyphs")
        var stamps = 0
        for (f in expected + expected.asReversed()) {
            val a = frames.getValue(f[0]); val i = f[1].toInt()
            val glyph = fumeGlyphFrame(a.block,a.reveal,a.timing,a.theme,a.time,i,8.5+i%3,true)
            checkPaint(f[2],glyph.paint,"frame ${f[0]} glyph $i")
            if (f[3] == "-") assertNull(glyph.stamp,"frame ${f[0]} glyph $i stamp") else {
                val stamp = assertNotNull(glyph.stamp,"frame ${f[0]} glyph $i stamp")
                val values = f[3].split(';',limit=2)
                assertNumbers(values[0],listOf(stamp.centerX,stamp.centerY,stamp.width,stamp.height),"frame ${f[0]} glyph $i stamp")
                checkPaint(values[1],stamp.paint,"frame ${f[0]} glyph $i stamp")
                stamps++
            }
            assertNull(fumeGlyphFrame(a.block,a.reveal,a.timing,a.theme,a.time,i,8.5+i%3,false).stamp)
        }
        assertEquals(317*2,stamps)
    }
    @Test fun retainedTextCrossfadeAndOverviewRestoreMatchOriginal() {
        for (f in rows("frames")) {
            val a = frame(f)
            val layers = fumeStaticLayers(a.block,a.timing,a.theme,a.time,2.4,if(a.mode==2).35 else 0.0)
            if (f[7] == "-") assertNull(layers,f.toString()) else {
                val expected = f[7].split('|'); assertEquals(expected.size,assertNotNull(layers).size)
                expected.zip(layers).forEach { (e,layer) ->
                    val parts = e.split(';'); checkPaint(parts.take(3).joinToString(";"),layer.paint,"frame ${f[0]} static")
                    assertEquals(parts[3].toDouble(),layer.alpha,1e-10,"frame ${f[0]} layer alpha")
                }
            }
        }
    }
}
