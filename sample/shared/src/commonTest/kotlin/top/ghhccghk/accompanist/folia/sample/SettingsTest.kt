package top.ghhccghk.accompanist.folia.sample

import top.ghhccghk.accompanist.lyrics.folia.*
import kotlin.test.*

class SettingsTest {
    @Test fun renderResolutionRoundTripsAndOldSettingsKeepFullResolution() {
        for (resolution in RenderResolution.entries) {
            val settings = SampleSettings(renderResolution=resolution)
            assertEquals(settings,SampleSettings.decode(settings.encode()))
            val session = SampleSession()
            session.applySettings(settings)
            assertEquals(resolution,session.settings().renderResolution)
        }
        assertEquals(RenderResolution.Full,SampleSettings.decode("version=1").renderResolution)
        assertEquals(RenderResolution.Full,SampleSettings.decode("version=1\nrenderResolution=NaN").renderResolution)
    }
    @Test fun themeAndItsOwnTuningRoundTripWithoutChangingClassicDefaults() {
        val settings=SampleSettings(lyricTheme=SampleLyricTheme.Partita,
            partitaTuning=PartitaTuning(false,false,12f,150f,true))
        assertEquals(settings,SampleSettings.decode(settings.encode()))
        val old=SampleSettings.decode("version=1")
        assertEquals(SampleLyricTheme.Classic,old.lyricTheme)
        assertEquals(PartitaTuning(),old.partitaTuning)
        val invalid=SampleSettings.decode("version=1\nlyricTheme=Fume\npartitaMin=170\npartitaMax=20")
        assertEquals(SampleLyricTheme.Classic,invalid.lyricTheme)
        assertEquals(20f,invalid.partitaTuning.staggerMin)
        assertEquals(170f,invalid.partitaTuning.staggerMax)
    }
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
