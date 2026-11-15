package com.mocharealm.accompanist.lyrics.folia

import com.mocharealm.accompanist.lyrics.folia.internal.animation.*
import java.io.File
import kotlin.math.roundToLong
import kotlin.test.*

class UpstreamBreathingTest {
    @Test fun originalBrowserFloatAcrossTwoCycles() {
        File("../verification/classic-fixtures/breathing.tsv").readLines()
            .filterNot {it.startsWith("#") || it.isBlank()}.forEach { row ->
                val c=row.split('\t')
                // DOM reflects the last rendered RAF, not the requested wall time.
                // The browser mount epoch is observed, never fitted to a visual trajectory.
                val time=((c[3].toDouble()-c[4].toDouble()).coerceAtLeast(0.0)*1000).roundToLong()
                val frame=sampleLine(time,-1_000_000,null,RenderProfile(Reveal.Normal,0,0),false,1f,1f)
                assertEquals(c[1].toDouble(),frame.breathingY.toDouble(),.0001,row)
                assertEquals(c[2].toDouble(),frame.scale.toDouble(),.000005,row)
            }
    }
}
