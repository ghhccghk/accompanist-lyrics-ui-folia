package top.ghhccghk.accompanist.lyrics.folia

import top.ghhccghk.accompanist.lyrics.folia.internal.styles.cadenza.*
import java.io.File
import java.util.Base64
import kotlin.random.Random
import kotlin.test.*

class CadenzaEnvelopesTest {
    private fun rows(name: String) = File("../verification/cadenza-fixtures/$name.tsv").readLines()
        .filterNot { it.startsWith('#') || it.isBlank() }.map { it.split('\t') }
    private fun word(f: String): CadenzaWord { val a=f.split(',');return CadenzaWord(
        String(Base64.getDecoder().decode(a[0]),Charsets.UTF_8),a[1].toDouble(),a[2].toDouble()) }
    private val lines = rows("envelopeInputs").associate { a -> a[0] to CadenzaLyric("Amber echo",a[1].toDouble(),a[2].toDouble(),
        a[7].split('|').map(::word),if(a[3]=="-")null else CadenzaHints(a[3].toDouble(),a[4].toDouble(),
            CadenzaTransition.valueOf(a[5].uppercase()),CadenzaReveal.valueOf(a[6].uppercase()))) }
    private fun numbers(expected: String, actual: List<Double>, label: String) {
        val values=expected.split(',').map(String::toDouble);assertEquals(values.size,actual.size,label)
        values.zip(actual).forEachIndexed { i,(e,a)-> assertEquals(e,a,1e-10,"$label field$i") }
    }
    @Test fun timingPreservesAbsentHintSemanticsAndSourceSeconds() {
        for(a in rows("timings")) {
            val t=cadenzaTiming(lines.getValue(a[0]));assertEquals(a[2],t.reveal.name.lowercase())
            numbers(listOf(a[1],a[3],a[4],a[5],a[6]).joinToString(","),
                listOf(t.end,t.lastWordEnd,t.transition.enter,t.transition.exit,t.transition.hold),"timing ${a[0]}")
        }
        val absent=lines.values.first { it.end-it.start<.1 && it.hints==null }
        assertEquals(CadenzaReveal.NORMAL,cadenzaTiming(absent).reveal)
        assertEquals(CadenzaLineEnvelope(),cadenzaLineEnvelope(2.0,null,cadenzaTiming(absent)))
    }
    @Test fun wordLineAndGlyphCurvesMatchUntouchedOriginalInRandomSeekOrder() {
        val explicit=listOf(CadenzaGlyphTime("Am",2.0,2.001),CadenzaGlyphTime("b",2.001,2.4),
            CadenzaGlyphTime("e",2.2,2.1),CadenzaGlyphTime("r",2.4,2.4))
        val rows=rows("envelopes")
        for(a in rows+rows.shuffled(Random(183))) {
            val line=lines.getValue(a[0]);val w=line.words[a[1].toInt()];val t=a[2].toDouble();val timing=cadenzaTiming(line)
            val label="${a[0]}/${a[1]} at $t"
            assertEquals(a[3],cadenzaWordStatus(t,timing,w).name.lowercase(),label)
            numbers(a.subList(4,8).joinToString(","),listOf(cadenzaWordProgress(t,timing.reveal,w),cadenzaWordGlow(t,timing,w),
                cadenzaBodyMix(t,timing,w),cadenzaPassedDrift(t,w)),label)
            val env=cadenzaLineEnvelope(t,line,timing);numbers(a[8],listOf(env.opacity,env.scale,env.blur),"$label line")
            numbers(a[9],(0..5).map{cadenzaCharGlow(t,w,it,6)},"$label glyph")
            numbers(a[10],(0..5).map{cadenzaCharGlow(t,w,it,6,explicit)},"$label explicit glyph")
        }
    }
    @Test fun shadowThresholdAndLayersMatchSourceIncludingIgnoredBlurScale() {
        val pattern=Regex("rgba\\(241, 123, 77, ([^)]+)\\)")
        for(a in rows("shadows"))numbers(pattern.findAll(a[2]).map{it.groupValues[1]}.joinToString(",").ifEmpty{"0"},
            cadenzaShadowAlphas(a[0].toDouble()).ifEmpty{listOf(0.0)},"shadow ${a[0]}/${a[1]}")
    }
}
