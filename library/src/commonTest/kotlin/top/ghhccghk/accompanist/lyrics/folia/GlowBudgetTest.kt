package top.ghhccghk.accompanist.lyrics.folia

import top.ghhccghk.accompanist.lyrics.folia.internal.rendering.GlowBudget
import kotlin.test.*

class GlowBudgetTest {
    @Test fun qualityDegradesByAvailableMaskCapacityWithoutOverflow() {
        assertEquals(2, GlowBudget.layers(FoliaQuality.Standard, 100, 200))
        assertEquals(1, GlowBudget.layers(FoliaQuality.Standard, 100, 199))
        assertEquals(0, GlowBudget.layers(FoliaQuality.Standard, 100, 99))
        assertEquals(1, GlowBudget.layers(FoliaQuality.Reduced, 100, 200))
        assertEquals(0, GlowBudget.layers(FoliaQuality.Minimal, 100, 200))
        assertEquals(1, GlowBudget.layers(FoliaQuality.Standard, Long.MAX_VALUE, Long.MAX_VALUE))
        assertEquals(0, GlowBudget.layers(FoliaQuality.Standard, Long.MAX_VALUE, GlowBudget.primaryBytes))
        assertEquals(GlowBudget.exitingBytes, GlowBudget.perExitBytes(4) * 4)
        for (count in listOf(0, 1, 4, 12, 120, Int.MAX_VALUE)) {
            assertTrue(GlowBudget.perExitBytes(count) * count <= GlowBudget.exitingBytes)
        }
    }
}
