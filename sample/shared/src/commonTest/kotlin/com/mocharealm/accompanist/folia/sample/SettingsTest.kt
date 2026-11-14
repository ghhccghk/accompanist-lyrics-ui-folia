package com.mocharealm.accompanist.folia.sample

import com.mocharealm.accompanist.lyrics.folia.*
import kotlin.test.*

class SettingsTest {
    @Test fun roundTripPreservesEveryDisplayPreference() {
        val settings = SampleSettings(FoliaOptions(false, false, false, -340, true, FoliaQuality.Reduced),
            ClassicTuning(false, .2f, 1.2f, true), 1.5f)
        assertEquals(settings, SampleSettings.decode(settings.encode()))
    }
    @Test fun invalidExternalValuesAreBoundedOrDefaulted() {
        val value = SampleSettings.decode("version=1\nfont=NaN\nspacing=Infinity\nbreathing=-3\noffset=999999\nquality=Other")
        assertEquals(1f, value.fontScale)
        assertEquals(.7f, value.tuning.wordSpacing)
        assertEquals(1f, value.tuning.breathingFloatMultiplier)
        assertEquals(2_000, value.options.offsetMillis)
        assertEquals(FoliaQuality.Standard, value.options.quality)
        assertFailsWith<IllegalArgumentException> { SampleSettings.decode("version=2") }
    }
}
