package top.ghhccghk.accompanist.lyrics.folia

import top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama.*
import java.io.File
import java.util.Base64
import kotlin.math.abs
import kotlin.test.*

class DioramaShotPickerTest {
    private fun rows(name:String)=File("../verification/diorama-fixtures/shots-v1/$name.tsv").readLines()
        .filterNot{it.startsWith('#')||it.isBlank()}.map{it.split('\t')}
    private fun decode(s:String)=String(Base64.getDecoder().decode(s),Charsets.UTF_8)
    @Test fun originalWeightsPickerDedupeAndMotionPresets() {
        val input=rows("inputs")
        val lines=input.map{r->DioramaLyric(decode(r[1]),r[2].toDouble(),r[3].toDouble(),
            if(r[4].isEmpty())emptyList()else r[4].split(';').map{it.split(',').let{DioramaTimedText(decode(it[0]),it[1].toDouble(),it[2].toDouble())}})}
        val metadata=input.map{DioramaShotMetadata(it[5]=="1",decode(it[6]).ifEmpty{null},it[7].toIntOrNull())}
        val expected=rows("expected");assertEquals(1404,expected.size)
        val seeds=listOf(1.0,1.0,dioramaHashSeed("DioramaProbe"),dioramaHashSeed("Mirror👩‍🚀"),7.0,12345.0)
        var peak=0.0;val picked=mutableSetOf<DioramaShot>()
        for((case,r)in expected.withIndex()) {
            val mode=DioramaShotMode.valueOf(r[1]);val meta=if(r[0]=="plain")emptyList()else metadata
            val seed=seeds[r[2].toInt()];assertEquals(r[3].toDouble(),seed,"source seed hash")
            val index=r[4].toInt();val shot=dioramaPickShot(index,lines,seed,meta,mode)
            assertEquals(r[5],shot.name,"original shot$case");picked+=shot
            val weights=dioramaShotWeights(index,lines,meta,mode).values.toList()
            for((i,v)in weights.withIndex()) {
                peak=maxOf(peak,abs(v-r[6+i].toDouble()))
                assertEquals(r[6+i].toDouble(),v,1e-12,"source weights$case/$i")
            }
        }
        assertEquals(DioramaShot.entries.toSet(),picked,"all13 source archetypes covered")
        for(r in rows("motion")) {
            val mode=DioramaShotMode.valueOf(r[0])
            assertEquals(r.drop(1).map(String::toDouble),listOf(mode.move,mode.drift,mode.smooth,mode.weave))
        }
        // Stateless O(1) previous-BASE lookback, not recursive previous output.
        for(index in lines.indices.reversed()) {
            val source=expected.first{it[0]=="metadata"&&it[1]=="normal"&&it[2]=="2"&&it[4]==index.toString()}
            assertEquals(source[5],dioramaPickShot(index,lines,seeds[2],metadata).name)
        }
        File("../build/verification/diorama-shots").apply{mkdirs()}.resolve("numeric.txt")
            .writeText("sourceCases=${expected.size} weightsPeak=$peak archetypes=${picked.size}\nActual source defaults and optional metadata; no visual acceptance claim\n")
    }
}
