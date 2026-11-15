package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.*
import java.awt.image.BufferedImage
import java.io.File
import java.security.MessageDigest
import javax.imageio.ImageIO
import javax.imageio.IIOImage
import javax.imageio.metadata.IIOMetadataNode
import kotlin.random.Random
import kotlin.test.*

/** Sampled media-time recording, never a playback frame-rate benchmark or portable font golden. */
@OptIn(ExperimentalTestApi::class)
class VisualSequenceTest {
    @Test fun continuousCheckpointsReconstructAfterRandomSeeks() = runDesktopComposeUiTest(width = 480, height = 360) {
        fun line(first: String, second: String, start: Int, end: Int) = KaraokeLine.MainKaraokeLine(
            listOf(KaraokeSyllable(first, start, (start + end) / 2),
                KaraokeSyllable(second, (start + end) / 2, end)), "Synthetic caption", KaraokeAlignment.Unspecified, start, end)
        val lyrics = SyncedLyrics(listOf(line("Amber ", "echo", 700, 1_900), line("Silver ", "light", 3_200, 4_100)))
        var snapshot by mutableStateOf(FoliaPlaybackSnapshot(0, false))
        var reducedMotion by mutableStateOf(false)
        setContent {
            FoliaLyricsView(lyrics, { snapshot }, Modifier.fillMaxSize().background(Color(0xFF162435)),
                options = FoliaOptions(reducedMotion = reducedMotion))
        }
        val directory = File("build/verification/sequence").apply { mkdirs() }
        val times = (0L..5_000L step 100).toList()
        val digest = MessageDigest.getInstance("SHA-256")
        fun hash(image: BufferedImage): String {
            digest.reset()
            image.getRGB(0, 0, image.width, image.height, null, 0, image.width).forEach { pixel ->
                digest.update((pixel ushr 24).toByte()); digest.update((pixel ushr 16).toByte())
                digest.update((pixel ushr 8).toByte()); digest.update(pixel.toByte())
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }
        val manifest = StringBuilder("mode,media_ms,sha256\n")
        val normal = mutableMapOf<Long, String>()
        var changedByReducedMotion = 0
        listOf(false, true).forEach { reduced ->
            runOnIdle { reducedMotion = reduced }
            val mode = if (reduced) "reduced-motion" else "classic"
            val writer = ImageIO.getImageWritersByFormatName("gif").next()
            ImageIO.createImageOutputStream(File(directory, "$mode-100ms.gif")).use { output ->
                writer.output = output
                writer.prepareWriteSequence(null)
                try {
                    times.forEach { time ->
                        runOnIdle { snapshot = FoliaPlaybackSnapshot(time, false, snapshot.discontinuity + 1) }
                        waitForIdle()
                        val frame = captureToImage().toAwtImage()
                        val checksum = hash(frame)
                        manifest.append("$mode,$time,$checksum\n")
                        if (!reduced) normal[time] = checksum
                        else if (normal[time] != checksum) changedByReducedMotion++
                        if (time % 500 == 0L) ImageIO.write(frame, "png", File(directory, "$mode-${time}ms.png"))
                        val metadata = writer.getDefaultImageMetadata(javax.imageio.ImageTypeSpecifier(frame), null)
                        val format = metadata.nativeMetadataFormatName
                        val root = metadata.getAsTree(format) as IIOMetadataNode
                        val control = root.getElementsByTagName("GraphicControlExtension").item(0) as IIOMetadataNode
                        control.setAttribute("delayTime", "10")
                        control.setAttribute("disposalMethod", "none")
                        metadata.setFromTree(format, root)
                        writer.writeToSequence(IIOImage(frame, null, metadata), null)
                    }
                    writer.endWriteSequence()
                } finally { writer.dispose() }
            }
        }
        assertTrue(normal.values.distinct().size > 20, "Sequence must contain actual changing frames")
        assertTrue(changedByReducedMotion > 20, "Reduced motion must change animated poses")
        runOnIdle { reducedMotion = false }
        times.shuffled(Random(20261115)).forEach { time ->
            runOnIdle { snapshot = FoliaPlaybackSnapshot(time, false, snapshot.discontinuity + 1) }
            waitForIdle()
            assertEquals(normal[time], hash(captureToImage().toAwtImage()), "Random seek to $time ms")
        }
        File(directory, "checkpoints.csv").writeText(manifest.toString())
        File(directory, "conditions.txt").writeText("""
            480x360 Compose/Skia test surface; density 1; platform default font.
            Original synthetic English text; Standard glow quality; opaque #162435.
            0..5000ms inclusive, 100ms media-time steps; playback paused at each point.
            Two modes: classic and reduced motion; same content and geometry.
            GIF is sampled at 10 images/second, NOT measured rendering performance.
            PNG and CSV preserve full-color checkpoints; GIF uses indexed color.
            SHA-256 hashes compare pixels within this process, not across fonts/OSes.
            JVM: ${System.getProperty("java.version")}; OS: ${System.getProperty("os.name")}
        """.trimIndent())
    }
}
