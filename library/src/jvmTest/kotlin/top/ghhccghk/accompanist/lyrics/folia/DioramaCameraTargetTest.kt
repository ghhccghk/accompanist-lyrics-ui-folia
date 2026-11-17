package top.ghhccghk.accompanist.lyrics.folia

import top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama.*
import java.io.File
import kotlin.test.*

class DioramaCameraTargetTest {
    private fun rows(name: String) = File("../verification/diorama-fixtures/$name.tsv").readLines()
        .filterNot { it.startsWith('#') || it.isBlank() }.map { it.split('\t') }
    @Test fun exactOriginalPrivateTargetBlocksAndThirteenShotOffsets() {
        val inputs = rows("camera-target-inputs"); val expected = rows("camera-target-values")
        fun words(tokens: List<String>) = tokens.mapIndexed { i,t -> DioramaTimedText(t,.7+i*.8,1.5+i*.8) }
        val lines = listOf(
            DioramaLyric("Synthetic mirror plane",.7,3.1,words(listOf("Synthetic","mirror","plane"))),
            DioramaLyric("مرحبا بالعالم",.7,2.3,words(listOf("مرحبا","بالعالم"))),
            DioramaLyric("星光 回响 向远方",.7,3.1,words(listOf("星光","回响","向远方"))),
            DioramaLyric("Micro",1.0,1.02),
            DioramaLyric("Last end",1.0,1.17,listOf(DioramaTimedText("Last",1.0,4.0),DioramaTimedText("end",1.1,1.17))),
            DioramaLyric("",1.0,1.0))
        val prepared = lines.map(::DioramaCameraLine)
        val seed = dioramaHashSeed("DioramaProbe"); val path = buildDioramaPath(12,seed)
        assertEquals(2808,inputs.size); assertEquals(inputs.size,expected.size)
        var peak = 0.0
        val results = inputs.mapIndexed { i,r ->
            val li = r[0].toInt(); val line = prepared[li]; val index = li+3
            val t = line.target(r[2].toDouble(),path[index],dioramaTextPlacement(index,seed),seed,index,
                DioramaShot.valueOf(r[1]),r[4].toDouble(),r[3].toDouble()/600,moveScale=r[5].toDouble(),driftScale=r[6].toDouble())
            val values = listOf(t.eye.x,t.eye.y,t.eye.z,t.look.x,t.look.y,t.look.z,
                t.progress,t.wordProgress,t.settle,t.truck,line.renderEnd)
            for ((field,value) in values.withIndex()) {
                val source = expected[i][field].toDouble()
                peak = maxOf(peak,kotlin.math.abs(source-value))
                assertEquals(source,value,1e-9,"sourceTarget$i field$field")
            }
            t
        }
        for (i in inputs.indices.reversed()) {
            val r=inputs[i];val li=r[0].toInt();val index=li+3
            assertEquals(results[i],prepared[li].target(r[2].toDouble(),path[index],dioramaTextPlacement(index,seed),seed,index,
                DioramaShot.valueOf(r[1]),r[4].toDouble(),r[3].toDouble()/600,moveScale=r[5].toDouble(),driftScale=r[6].toDouble()))
        }
        File("../build/verification/diorama-camera-targets").apply{mkdirs()}.resolve("numeric.txt")
            .writeText("sourceCases=${inputs.size} peak=$peak\nTarget only, not complete CameraRig\n")
    }
}
