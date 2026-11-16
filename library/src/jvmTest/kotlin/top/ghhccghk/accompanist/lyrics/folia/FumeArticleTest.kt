package top.ghhccghk.accompanist.lyrics.folia

import top.ghhccghk.accompanist.lyrics.folia.internal.styles.fume.*
import java.io.File
import java.util.Base64
import kotlin.math.ceil
import kotlin.test.*

class FumeArticleTest {
    private fun rows(name: String) = File("../verification/fume-fixtures/$name.tsv").readLines()
        .filterNot { it.startsWith("#") || it.isBlank() }.map { it.split('\t') }
    private val fixtureCounter: FumeLineCounter = { text, _, font, width ->
        text.split('\n').sumOf { maxOf(1, ceil(fumeGraphemes(it).size * font * .57 / width).toInt()) }
    }
    @Test fun originalArticleSelectionAndPlacementWithExplicitMeasurementSeam() {
        val inputs = rows("article-inputs").groupBy { it[0] }.mapValues { (_, rows) -> rows.map { f ->
            FumeLyric(String(Base64.getDecoder().decode(f[2]), Charsets.UTF_8), f[3].toDouble(), f[4].toDouble(), chorus = f[5].toBoolean())
        } }
        val layouts = rows("article-layout").groupBy { it.take(5) }
        assertEquals(120, layouts.size)
        for ((key, expected) in layouts) {
            val lines = inputs.getValue(key[0])
            val viewport = FumeViewport(key[1].toDouble(), key[2].toDouble())
            fun build() = buildFumeArticle(lines, viewport, "FumeFixture", key[3].toDouble(), key[4].toDouble(), fixtureCounter)!!
            val article = build()
            val f = expected.first()
            assertEquals(f[5].toInt(), article.columns, key.toString())
            val values = listOf(article.width, article.height, article.viewportHeight, article.bounds.left,
                article.bounds.top, article.bounds.right, article.bounds.bottom, article.firstStart, article.lastRenderEnd)
            values.forEachIndexed { i, v -> assertEquals(f[8 + i].toDouble(), v, 1e-8, "$key metric $i") }
            assertEquals(f[6].toDouble(), article.gap, 1e-10)
            assertEquals(expected.size, article.blocks.size, key.toString())
            expected.zip(article.blocks).forEach { (e, b) ->
                assertEquals(e[18].toInt(), b.sourceIndex, "$key order")
                assertEquals(e[19].toBoolean(), b.hero, "$key variant")
                listOf(b.x, b.y, b.width, b.height, b.innerWidth, b.fontPx, b.lineHeight, b.lineCount.toDouble())
                    .forEachIndexed { i, v -> assertEquals(e[20 + i].toDouble(), v, 1e-8, "$key block ${b.sourceIndex} field $i") }
            }
            assertEquals(article, build(), "Rebuild after arbitrary measurement history")
            buildFumeArticle(lines, FumeViewport(280.0, 900.0), "OtherTheme", countLines = fixtureCounter)
            assertEquals(article, build(), "Resize round-trip restores full layout")
            // Complete blocks must never overlap, including two-column hero spans.
            article.blocks.forEachIndexed { i, a -> article.blocks.drop(i + 1).forEach { b ->
                assertFalse(a.x < b.x + b.width - 1e-8 && a.x + a.width > b.x + 1e-8 &&
                    a.y < b.y + b.height - 1e-8 && a.y + a.height > b.y + 1e-8, "$key overlapping $a / $b")
            } }
        }
    }
    @Test fun missingAndInvalidViewportsDoNotMakeInventedBlocks() {
        val line = FumeLyric("Only a synthetic line", 0.0, 1.0)
        assertNull(buildFumeArticle(emptyList(), FumeViewport(800.0, 600.0), "Fixture", countLines = fixtureCounter))
        for (v in listOf(FumeViewport(0.0, 600.0), FumeViewport(800.0, -1.0), FumeViewport(Double.NaN, 600.0)))
            assertNull(buildFumeArticle(listOf(line), v, "Fixture", countLines = fixtureCounter))
        val empty = buildFumeArticle(listOf(line.copy(text = " \t\n")), FumeViewport(800.0, 600.0), "Fixture", countLines = fixtureCounter)!!
        assertTrue(empty.blocks.isEmpty())
        assertEquals(Double.POSITIVE_INFINITY, empty.firstStart)
        assertEquals(Double.NEGATIVE_INFINITY, empty.lastRenderEnd)
    }
}
