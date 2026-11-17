package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.pendolo.*
import java.io.File
import java.util.Base64
import java.security.MessageDigest
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class PendoloWheelTargetTest {
    @Test fun latin() = capture("latin")
    @Test fun wrap() = capture("wrap")
    @Test fun arabic() = capture("arabic")
    @Test fun cjk() = capture("cjk")
    private fun capture(scene: String) = runDesktopComposeUiTest(width = 800, height = 600) {
        fun decode(s: String) = String(Base64.getDecoder().decode(s), Charsets.UTF_8)
        val f = File("../verification/cadenza-fixtures/sceneInputs.tsv").readLines().first { it.startsWith("$scene\t") }.split('\t')
        val subtitles = when (scene) {
            "arabic" -> PendoloSubtitleSource("بالعالم", "مرحبا")
            "cjk" -> PendoloSubtitleSource("星光", "向远方")
            else -> PendoloSubtitleSource("beyond", "Amber echoes")
        }
        val lines = (0 until 8).map { i -> PendoloStageLine(PendoloLyric(decode(f[1]), f[2].toDouble() + i*4, f[3].toDouble() + i*4,
            f[4].split('|').map { token -> val w = token.split('~'); PendoloTimedText(decode(w[0]), w[1].toDouble()+i*4, w[2].toDouble()+i*4) }), subtitles) }
        val font = when (scene) { "arabic" -> "FoliaParityBidi-Bold.ttf"; "cjk" -> "FoliaParityChatCJK-Bold.otf"; else -> "FoliaParityLatin-Bold.ttf" }
        val style = TextStyle(fontFamily = FontFamily(Font(File("../verification/assets/$font"), FontWeight.Bold)), fontWeight = FontWeight.Bold, textDirection = TextDirection.Ltr)
        var width by mutableStateOf(380)
        var target by mutableStateOf(-1.0)
        var t by mutableStateOf(0.0)
        var mode by mutableStateOf(PendoloSubtitleMode.NONE)
        var mounted by mutableStateOf(true)
        var observed: List<PendoloStageRow> = emptyList()
        setContent {
            Box(Modifier.fillMaxSize().background(Color(0xff162435))) {
                if (mounted) PendoloWheelTarget(lines, { t }, target, style, Modifier.width(width.dp).height(600.dp), mode, onRows = { observed = it })
            }
        }
        val output = File("../build/verification/pendolo-stage-kotlin").apply { mkdirs() }
        val source = File("../verification/pendolo-fixtures/stage-targets.tsv").readLines().filterNot { it.startsWith('#') || it.isBlank() }.map { it.split('\t') }
        val captures = source.filter { it[0] == "C" && it[1].startsWith("$scene-") }
        val expected = source.filter { it[0] == "R" }.groupBy { it[1] }
        val hashes = linkedMapOf<String, String>()
        val residual = mutableListOf("# file row field original native error")
        val mismatches = mutableListOf<String>()
        val measurements = mutableListOf("# file sourceIndex rowText64 tracking lineRight pathWidth cursorEnd")
        fun image() = captureToImage().toAwtImage().getSubimage(0, 0, width, 600)
        fun hash(): String {
            val pic = image(); val pixels = pic.getRGB(0,0,pic.width,pic.height,null,0,pic.width)
            val digest = MessageDigest.getInstance("SHA-256")
            pixels.forEach { p -> for (shift in listOf(24,16,8,0)) digest.update((p ushr shift).toByte()) }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }
        fun set(r: List<String>) {
            runOnIdle { width = r[1].split('-')[1].toInt(); t = r[2].toDouble(); target = r[3].toDouble(); mode = PendoloSubtitleMode.valueOf(r[4].uppercase()) }
            waitForIdle()
        }
        for (r in captures) {
            set(r)
            val key = r[1]; val original = expected[key].orEmpty()
            if (original.map { it[3].toInt() } != observed.map { it.item.index }) mismatches += "$key visible indices"
            for ((e, a) in original.zip(observed)) {
                for (body in a.main.rows) {
                    val p = body.paragraph
                    measurements += listOf(key, a.item.index, Base64.getEncoder().encodeToString(body.text.toByteArray(Charsets.UTF_8)),
                        p.layoutInput.style.letterSpacing, p.getLineRight(0), p.getPathForRange(0, body.text.length).getBounds().width,
                        p.getCursorRect(body.text.length).left).joinToString("\t")
                }
                val sourceValues = listOf(e[4],e[5],e[6],e[7],e[8],e[9],e[10]).map(String::toDouble) + e[11].split(',').map(String::toDouble) + e[13].toDouble()
                val nativeValues = listOf(a.item.x,a.item.y,a.item.angleDeg,a.item.scale,a.item.alpha,a.reservedHeight,a.blockHeight) + a.contentRect + a.width
                for ((field, pair) in sourceValues.zip(nativeValues).withIndex()) {
                    val error = abs(pair.first - pair.second)
                    residual += listOf(key,a.item.index,field,pair.first,pair.second,error).joinToString("\t")
                    val tolerance = if (field <= 5) 1e-8 else 1.0/64
                    if (error > tolerance) mismatches += "$key/${a.item.index}/$field error=$error"
                }
            }
            ImageIO.write(image(), "png", output.resolve("$key.png")); hashes[key] = hash()
        }
        output.resolve("$scene-residual.tsv").writeText(residual.joinToString("\n")+"\n")
        output.resolve("$scene-mismatches.txt").writeText(mismatches.joinToString("\n"))
        output.resolve("$scene-measurements.tsv").writeText(measurements.joinToString("\n")+"\n")
        for (r in captures.shuffled(Random(317))) { set(r); assertEquals(hashes[r[1]], hash(), "seek/resize ${r[1]}") }
        val paused = hash(); mainClock.advanceTimeBy(2000); waitForIdle(); assertEquals(paused,hash(),"pause $scene")
        runOnIdle { mounted = false }; waitForIdle(); runOnIdle { mounted = true }; waitForIdle(); assertEquals(paused,hash(),"remount $scene")
        output.resolve("$scene-revisit.txt").writeText("20 frames,20 random seek/resize revisits,pause,remount: pixel identical\n")
        assertTrue(mismatches.isEmpty(), mismatches.take(35).joinToString("\n"))
    }
}
