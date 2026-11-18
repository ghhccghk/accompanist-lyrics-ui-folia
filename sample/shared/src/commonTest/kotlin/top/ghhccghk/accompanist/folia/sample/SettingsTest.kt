package top.ghhccghk.accompanist.folia.sample

import top.ghhccghk.accompanist.lyrics.folia.*
import kotlin.test.*

class SettingsTest {
    @Test fun rendererChoiceIsRememberedPerThemeAndOldSettingsRemainKotlin() {
        val session=SampleSession()
        assertEquals(SampleRendererBackend.Kotlin,session.rendererBackend)
        session.lyricTheme=SampleLyricTheme.Partita
        session.setRendererBackend(SampleRendererBackend.NativePreferred)
        session.lyricTheme=SampleLyricTheme.Classic
        assertEquals(SampleRendererBackend.Kotlin,session.rendererBackend)
        session.setRendererBackend(SampleRendererBackend.NativeRequired)
        val saved=session.settings()
        assertEquals(saved,SampleSettings.decode(saved.encode()))
        val restored=SampleSession().also { it.applySettings(SampleSettings.decode(saved.encode())) }
        restored.lyricTheme=SampleLyricTheme.Partita
        assertEquals(SampleRendererBackend.NativePreferred,restored.rendererBackend)
        restored.lyricTheme=SampleLyricTheme.Classic
        assertEquals(SampleRendererBackend.NativeRequired,restored.rendererBackend)
        assertTrue(SampleSettings.decode("version=1").rendererBackends.isEmpty())
        assertTrue(SampleSettings.decode("version=1\nrenderer.Unknown=NativeRequired\nrenderer.Partita=Unknown").rendererBackends.isEmpty())
    }
    @Test fun dioramaDebugPreviewPreferencesAreCompatible(){
        val settings=SampleSettings(lyricTheme=SampleLyricTheme.Diorama,dioramaTuning=DioramaTuning(fontScale=1.3f,enforceRasterBudget=false,
            cacheTexturesOnDisk=false,rasterMemoryBudgetBytes=16L*1024*1024))
        assertEquals(settings,SampleSettings.decode(settings.encode()))
        val session=SampleSession();session.applySettings(settings)
        assertEquals(FoliaStyle.Diorama(settings.dioramaTuning),session.lyricStyle)
        assertEquals(settings,session.settings());assertFalse(session.demo)
        assertEquals(DioramaTuning(),SampleSettings.decode("version=1").dioramaTuning)
        assertEquals(DioramaTuning(),SampleSettings.decode("version=1\ndioramaFont=NaN").dioramaTuning)
        assertEquals(DioramaTuning(),SampleSettings.decode("version=1\ndioramaRasterMemory=-1\ndioramaDiskCache=invalid").dioramaTuning)
    }
    @Test fun fumeControlsRoundTripAndRealThemeIsSelectable() {
        val tuning=FumeTuning(1.5f,1.2f,.3f,1.8f,FumeTrackingMode.Stepped,.7f,false)
        val settings=SampleSettings(lyricTheme=SampleLyricTheme.Fume,fumeTuning=tuning)
        assertEquals(settings,SampleSettings.decode(settings.encode()))
        val session=SampleSession();session.applySettings(settings)
        assertEquals(FoliaStyle.Fume(tuning),session.lyricStyle);assertEquals(settings,session.settings());assertFalse(session.demo)
        assertEquals(FumeTuning(),SampleSettings.decode("version=1\nfumeFont=NaN\nfumeHero=5\nfumeHold=-1\nfumeSpeed=0\nfumeGlow=Infinity\nfumeTracking=Missing").fumeTuning)
        assertFailsWith<IllegalArgumentException>{FumeTuning(cameraSpeed=Float.NaN)}
    }
    @Test fun cappellaDisplayPreferenceAndContentTitleRemainSeparate() {
        val settings=SampleSettings(lyricTheme=SampleLyricTheme.Cappella,cappellaTuning=CappellaTuning(fontScale=1.5f))
        assertEquals(settings,SampleSettings.decode(settings.encode()))
        val session=SampleSession();session.applySettings(settings)
        val style=assertIs<FoliaStyle.Cappella>(session.lyricStyleForTitle("Track title"))
        assertEquals("Track title",style.tuning.title);assertEquals(1.5f,style.tuning.fontScale)
        assertEquals(settings,session.settings());assertFalse(session.demo)
        assertEquals(CappellaTuning(),SampleSettings.decode("version=1\ncappellaFont=NaN").cappellaTuning)
        assertFailsWith<IllegalArgumentException>{CappellaTuning(fontScale=Float.NaN)}
        assertFailsWith<IllegalArgumentException>{CappellaLineAgent(-1,"guest")}
    }
    @Test fun pendoloTuningAndSourceHintsRoundTrip() {
        val tuning=PendoloTuning(1.5f,.75f,setOf(1,4),listOf(FoliaWordColor("星光|~\n",androidx.compose.ui.graphics.Color(0xff71b8ed))))
        val settings=SampleSettings(lyricTheme=SampleLyricTheme.Pendolo,pendoloTuning=tuning)
        assertEquals(settings,SampleSettings.decode(settings.encode()))
        val session=SampleSession();session.applySettings(settings)
        assertEquals(FoliaStyle.Pendolo(tuning),session.lyricStyle);assertEquals(settings,session.settings())
        assertEquals(PendoloTuning(),SampleSettings.decode("version=1").pendoloTuning)
        assertEquals(PendoloTuning(),SampleSettings.decode("version=1\npendoloLyricsFont=NaN\npendoloSubtitleFont=3\npendoloChorus=-1,bad\npendoloWords=bad").pendoloTuning)
        assertFailsWith<IllegalArgumentException>{PendoloTuning(lyricsFontScale=Float.NaN)}
    }

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
        // Unknown external theme identifiers still fall back safely.
        val invalid=SampleSettings.decode("version=1\nlyricTheme=Unknown\npartitaMin=170\npartitaMax=20")
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
