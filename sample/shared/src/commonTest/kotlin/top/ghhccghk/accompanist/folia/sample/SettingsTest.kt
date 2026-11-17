package top.ghhccghk.accompanist.folia.sample

import top.ghhccghk.accompanist.lyrics.folia.*
import kotlin.test.*

class SettingsTest {
    @Test fun cadenzaPreferencesAndKeywordsRoundTripWithoutEnablingUnimplementedThemes() {
        val tuning=CadenzaTuning(.8f,.6f,.4f,0f,setOf(0,5),listOf(FoliaWordColor("星光\nAmber|~",androidx.compose.ui.graphics.Color(0xff5bcefa))))
        val settings=SampleSettings(lyricTheme=SampleLyricTheme.Cadenza,cadenzaTuning=tuning)
        assertEquals(settings,SampleSettings.decode(settings.encode()))
        val session=SampleSession();session.applySettings(settings)
        assertEquals(FoliaStyle.Cadenza(tuning),session.lyricStyle);assertEquals(settings,session.settings())
        assertEquals(CadenzaTuning(),SampleSettings.decode("version=1").cadenzaTuning)
        assertEquals(CadenzaTuning(),SampleSettings.decode("version=1\ncadenzaFont=NaN\ncadenzaWidth=2\ncadenzaGlow=-1\ncadenzaMotion=Infinity\ncadenzaWords=bad\ncadenzaChorus=-1,no").cadenzaTuning)
        assertFailsWith<IllegalArgumentException>{CadenzaTuning(fontScale=Float.NaN)}
        assertFailsWith<IllegalArgumentException>{CadenzaTuning(widthRatio=.1f)}
    }
    @Test fun tiltPreferencesRoundTripAndOldSettingsRemainCompatible() {
        for(colors in TiltColorScheme.entries) {
            val settings=SampleSettings(lyricTheme=SampleLyricTheme.Tilt,tiltTuning=TiltTuning(.2f,.9f,colors))
            assertEquals(settings,SampleSettings.decode(settings.encode()))
            val session=SampleSession();session.applySettings(settings)
            assertEquals(FoliaStyle.Tilt(settings.tiltTuning),session.lyricStyle)
            assertEquals(settings,session.settings())
        }
        assertEquals(TiltTuning(),SampleSettings.decode("version=1").tiltTuning)
        assertEquals(TiltTuning(),SampleSettings.decode("version=1\ntiltSplit=NaN\ntiltProbability=2\ntiltColors=Missing").tiltTuning)
        assertFailsWith<IllegalArgumentException>{TiltTuning(Float.NaN)}
        assertFailsWith<IllegalArgumentException>{TiltTuning(tiltStyleProbability=-1f)}
    }
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
