// SPDX-License-Identifier: AGPL-3.0-only
package top.ghhccghk.accompanist.lyrics.folia

import top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama.*
import kotlin.test.*

class DioramaMipPassTest {
    @Test fun completeChainUniquePixelOwnershipAndSamplerHeadroom() {
        for(last in 0..20) {
            val passes=dioramaMipPasses(last)
            assertEquals((0..last).toSet(),passes.flatMap{(it.first..it.last).toList()}.toSet())
            assertTrue(passes.all{(it.last-it.first+1)*2<=14})
            // Fractional LODs and exact pass boundaries belong to one pass.
            for(tick in 0..last*16) {
                val level=tick/16.0
                val owner=passes.single{level>=it.first&&(it.last==last||level<it.last)}
                val low=level.toInt();val high=minOf(last,low+1)
                assertTrue(low in owner.first..owner.last&&high in owner.first..owner.last)
            }
        }
    }
}
