package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.ui.graphics.Color
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama.DioramaFog
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama.dioramaFogMixChannel
import java.io.File
import kotlin.math.*
import kotlin.test.*

class DioramaFogTest {
    @Test fun originalThreeScalarUniformsAndBoundedBrushInterpolation() {
        val rows=File("../verification/diorama-fixtures/fog-v1/fog.tsv").readLines()
            .filterNot{it.isBlank()||it.startsWith('#')}.map{it.split('\t').map(String::toDouble)}
        var factorPeak=0.0;var algebraPeak=0.0;var packedPeak=0.0;var brushPeak=0.0
        fun channels(c:Color)=listOf(c.red.toDouble(),c.green.toDouble(),c.blue.toDouble())
        for(row in rows) {
            val fog=DioramaFog(Color(row[6].toFloat(),row[7].toFloat(),row[8].toFloat()))
            val body=Color(row[3].toFloat(),row[4].toFloat(),row[5].toFloat())
            val t=((row[1]-12)/18).coerceIn(0.0,1.0)
            val factor=fog.factor(row[1]);factorPeak=max(factorPeak,abs(factor-row[2]))
            assertEquals(row[2],factor,1e-12,"original Three.MathUtils.smoothstep depth=${row[1]}")
            val packed=channels(fog.mixEncoded(body,factor))
            val lower=floor(t*32).toInt().coerceAtMost(31);val mix=t*32-lower
            val a=channels(fog.mixEncoded(body,fog.factor(12+18*lower/32.0)))
            val b=channels(fog.mixEncoded(body,fog.factor(12+18*(lower+1)/32.0)))
            for(channel in 0..2) {
                val expected=row[9+channel]
                // Independently captured original Three endpoints: no Compose
                // packing. Separate exact shader algebra from Color storage.
                val raw=dioramaFogMixChannel(row[3+channel],row[6+channel],factor)
                algebraPeak=max(algebraPeak,abs(raw-expected))
                assertEquals(expected,raw,1e-12,"original encoded shader channel")
                packedPeak=max(packedPeak,abs(packed[channel]-expected))
                brushPeak=max(brushPeak,abs(a[channel]+(b[channel]-a[channel])*mix-expected))
                assertEquals(expected,packed[channel],.5/255+1e-6,"encoded fragment RGB, NOT linear-color fog")
                // Keep the original gates: input bytes and half-float stops now
                // fit comfortably, rather than widening a double-packing gate.
                assertEquals(expected,a[channel]+(b[channel]-a[channel])*mix,.5/255+6.0/(8*32*32)+1e-6)
            }
        }
        assertEquals(1221,rows.size)
        assertFailsWith<IllegalArgumentException>{DioramaFog(Color.White,12.0,12.0)}
        assertFailsWith<IllegalArgumentException>{DioramaFog(Color.Unspecified)}
        File("../build/verification/diorama-fog-kotlin").apply{mkdirs()}.resolve("math.txt")
            .writeText("originalRecords=${rows.size} factorPeak=$factorPeak algebraPeak=$algebraPeak packedRGBPeak=$packedPeak brushRGBPeak=$brushPeak\n")
    }
}
