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
import top.ghhccghk.accompanist.lyrics.folia.internal.rendering.LocalClassicSoftwareLayers
import java.io.File
import java.util.Base64
import java.security.MessageDigest
import javax.imageio.ImageIO
import kotlin.math.*
import kotlin.random.Random
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class PendoloChorusSweepTest {
    private fun rows(name: String) = File("../verification/pendolo-fixtures/$name.tsv").readLines()
        .filterNot { it.startsWith('#') || it.isBlank() }.map { it.split('\t') }
    private fun decode(s: String) = if (s == "-") "" else String(Base64.getDecoder().decode(s), Charsets.UTF_8)
    private fun lyric(key: String): PendoloLyric {
        val input = rows("sweep-inputs").filter { it[1] == key }
        val c = input.single { it[0] == "C" }
        return PendoloLyric(decode(c[5]), c[6].toDouble(), c[7].toDouble(), input.filter { it[0] == "W" }.map {
            PendoloTimedText(decode(it[3]), it[4].toDouble(), it[5].toDouble())
        })
    }

    @Test fun latin() = capture("latin")
    @Test fun wrap() = capture("wrap")
    @Test fun arabic() = capture("arabic")
    @Test fun latinSoftware() = capture("latin",true)
    @Test fun cjkSoftware() = capture("cjk",true)
    @Test fun cjk() = capture("cjk")

    private fun capture(scene: String, software:Boolean=false) = runDesktopComposeUiTest(width = 380, height = 480) {
        val font = when (scene) { "arabic" -> "FoliaParityBidi-Bold.ttf"; "cjk" -> "FoliaParityChatCJK-Bold.otf"; else -> "FoliaParityLatin-Bold.ttf" }
        val style = TextStyle(fontFamily = FontFamily(Font(File("../verification/assets/$font"), FontWeight.Bold)),
            fontWeight = FontWeight.Bold, textDirection = TextDirection.Ltr)
        var t by mutableStateOf(0.0)
        var width by mutableStateOf(150.0)
        var mounted by mutableStateOf(true)
        var coloring by mutableStateOf(true)
        var intensity by mutableStateOf(PendoloIntensity.NORMAL)
        var chorusEnabled by mutableStateOf(true)
        var observed: PendoloPreparedText? = null
        var line by mutableStateOf(lyric("$scene-150"))
        val colors=when(scene){
            "cjk"->listOf(FoliaWordColor("向远方",Color(0xff8ab4f8)),FoliaWordColor("星光",Color(0xffef55ae)))
            "arabic"->listOf(FoliaWordColor("مرحبا",Color(0xffef55ae)))
            else->listOf(FoliaWordColor("Amber",Color(0xffed71b8)),FoliaWordColor("echo",Color(0xff73dea1)),FoliaWordColor("echoes",Color(0xff73dea1)),FoliaWordColor("beyond",Color(0xff8ab4f8)))
        }
        setContent { CompositionLocalProvider(LocalClassicSoftwareLayers provides software) {
            Box(Modifier.fillMaxSize().background(Color(0xff162435))) {
                if (mounted) PendoloSweepTarget(line, { t }, width, style,
                    Modifier.fillMaxSize().padding(24.dp), wordColors=if(coloring)colors else emptyList(), chorus=pendoloChorus(chorusEnabled,true,intensity), onPrepared = { observed = it })
            }
        }
        }
        val out = File("../build/verification/pendolo-chorus-sweep-${if(software) "software" else "kotlin"}").apply { mkdirs() }
        fun hash(): String {
            val image = captureToImage(); val pixels = IntArray(image.width * image.height); image.readPixels(pixels)
            val digest = MessageDigest.getInstance("SHA-256")
            pixels.forEach { p -> for (shift in listOf(24, 16, 8, 0)) digest.update((p ushr shift).toByte()) }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }
        val baseline = linkedMapOf<Triple<PendoloIntensity, Double, Double>, String>()
        val residual = mutableListOf("# width row field original native absoluteError")
        val mismatches = mutableListOf<String>()
        for (level in PendoloIntensity.entries) for (w in listOf(150.0, 300.0)) {
            val key = "$scene-${w.toInt()}"
            runOnIdle { width = w; line = lyric(key); intensity = level }; waitForIdle()
            val actual = checkNotNull(observed)
            val original = rows("sweep").filter { it[0] == "L" && it[1] == key }
            if (actual.rows.size != original.size) mismatches += "$key row count ${original.size}/${actual.rows.size}"
            for ((i, pair) in original.zip(actual.rows).withIndex()) {
                val (e, a) = pair
                if (decode(e[3]) != a.text) mismatches += "$key/$i row text '${decode(e[3])}'/'${a.text}'"
                if (e[5].toInt() != a.graphemeStart || e[6].toInt() != a.graphemeEnd) mismatches += "$key/$i grapheme range"
                val values = listOf("width" to (e[4].toDouble() to a.width)) +
                    e[7].split(',').map(String::toDouble).zip(a.offsets).mapIndexed { j, p -> "offset$j" to p }
                for ((field, v) in values) {
                    val error = abs(v.first - v.second)
                    residual += listOf(w, i, field, v.first, v.second, error).joinToString("\t")
                    if (error > 1.0 / 64) mismatches += "$key/$i/$field error=$error"
                }
            }
            for (time in listOf(0.0, .7, 1.0, 1.3, 1.6, 2.0, 2.4, 2.8, 3.2, 3.6, 4.0)) {
                runOnIdle { t = time }; waitForIdle()
                baseline[Triple(level,w,time)] = hash()
                ImageIO.write(captureToImage().toAwtImage(), "png", out.resolve("${level.name.lowercase()}-$key-${(time * 1000).toInt()}.png"))
            }
        }
        out.resolve("$scene-residual.tsv").writeText(residual.joinToString("\n") + "\n")
        out.resolve("$scene-mismatches.txt").writeText(mismatches.joinToString("\n"))
        for ((key, expected) in baseline.entries.shuffled(Random(19))) {
            runOnIdle { intensity = key.first; width = key.second; t = key.third }; waitForIdle(); assertEquals(expected, hash(), "random seek $scene/$key")
        }
        val paused = hash(); mainClock.advanceTimeBy(2000); waitForIdle(); assertEquals(paused, hash(), "pause $scene")
        runOnIdle { mounted = false }; waitForIdle(); runOnIdle { mounted = true }; waitForIdle(); assertEquals(paused, hash(), "remount $scene")
        out.resolve("$scene-revisit.txt").writeText("66 frames,66 random revisits,pause,remount/resize: pixel identical\n")
        runOnIdle { t=4.0; width=300.0 };waitForIdle();val colored=hash()
        runOnIdle { coloring=false };waitForIdle();val uncolored=hash()
        if(scene=="arabic")assertEquals(uncolored,colored,"Source does not match Arabic as ASCII keywords")
        else assertNotEquals(uncolored,colored,"Matched range must affect actual visible ink")
        runOnIdle { coloring=true };waitForIdle();assertEquals(colored,hash(),"color config restore")
        val emphasized=hash();runOnIdle { chorusEnabled=false };waitForIdle();assertNotEquals(emphasized,hash(),"chorus must affect actual ink");runOnIdle { chorusEnabled=true };waitForIdle();assertEquals(emphasized,hash(),"chorus restore")
        assertTrue(mismatches.isEmpty(), mismatches.joinToString("\n"))
    }
}
