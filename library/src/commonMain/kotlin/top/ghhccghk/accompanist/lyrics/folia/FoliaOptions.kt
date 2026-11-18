package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Immutable
data class FoliaOptions(
    val showTranslation: Boolean = true,
    val showPhonetic: Boolean = true,
    val showNextLine: Boolean = true,
    val offsetMillis: Int = 0,
    val reducedMotion: Boolean = false,
    val quality: FoliaQuality = FoliaQuality.Standard,
    /** Draw the local subtitle light pad when colors.subtitleBackground is nontransparent. */
    val showSubtitleBackground: Boolean = true,
    /** Original classic theme intensity; Normal preserves the existing default. */
    val animationIntensity: FoliaAnimationIntensity = FoliaAnimationIntensity.Normal,
)

enum class FoliaAnimationIntensity { Calm, Normal, Chaotic }

enum class FoliaQuality { Standard, Reduced, Minimal }

@Immutable
/** Status of the current primary line; gaps report zero units and Minimal quality.
 * Exiting and secondary lines are not included in these primary-line counters. */
data class FoliaStatus(
    val requestedQuality: FoliaQuality,
    val actualQuality: FoliaQuality,
    val mainUnitCount: Int,
    val diagnostics: List<String>,
)

sealed interface FoliaStyle {
    /** Experimental text/perspective debug preview. See docs/diorama-preview.md. */
    @Immutable
    data class Diorama(val tuning:DioramaTuning=DioramaTuning()):FoliaStyle
    /** Whole-song article/camera calibration preview, without a background engine. */
    @Immutable
    data class Fume(val tuning:FumeTuning=FumeTuning()):FoliaStyle
    /** Text-only chat calibration preview; no bundled emoji/cover assets. */
    @Immutable
    data class Cappella(val tuning:CappellaTuning=CappellaTuning()):FoliaStyle

    /** Text-wheel calibration preview. No invented beats/gears or audio analysis. */
    @Immutable
    data class Pendolo(val tuning:PendoloTuning=PendoloTuning()):FoliaStyle

    @Immutable
    data class Classic(val tuning: ClassicTuning = ClassicTuning()) : FoliaStyle

    /** Partita's staggered chunk stage. Visual calibration status: docs/themes-parity.md. */
    @Immutable
    data class Partita(val tuning: PartitaTuning = PartitaTuning()) : FoliaStyle

    /** Source-based Tilt stage; visual calibration preview, not final parity.
     * Whole-run shaping is retained; original per-EGC DOM script/width differences
     * remain tracked in docs/themes-parity.md. */
    @Immutable
    data class Tilt(val tuning: TiltTuning = TiltTuning()) : FoliaStyle

    /** Cadenza fragment-cloud stage. Calibration preview; measured differences
     * and source refresh-rate adaptation are recorded in docs/themes-parity.md. */
    @Immutable
    data class Cadenza(val tuning:CadenzaTuning=CadenzaTuning()) : FoliaStyle
}

enum class FumeTrackingMode { Smooth, Stepped }

@Immutable
data class DioramaTuning(
    val fontScale:Float=1f,
    /** Stable path/shot input. No chorus/beat metadata is inferred. */
    val layoutSeed:String="Diorama",
    /** Optional host background for text fog; transparent disables fog. */
    val fogColor:Color=Color.Transparent,
    /** Disable only for host-controlled experiments; platform limits still apply. */
    val enforceRasterBudget:Boolean=true,
    /** Spill prepared glyph textures to session-local disk above the memory
     * target. Current/next windows load asynchronously, never during drawing. */
    val cacheTexturesOnDisk:Boolean=true,
    /** Soft retained ARGB payload target; current window may need more.
     * Does not include blur scratch, GPU copies or retired in-flight frames. */
    val rasterMemoryBudgetBytes:Long=32L*1024*1024,
) {
    init { require(fontScale.isFinite()&&fontScale in .5f..2f);require(fogColor!=Color.Unspecified)
        require(rasterMemoryBudgetBytes in 1..96L*1024*1024) }
}

@Immutable
data class FumeTuning(
    val fontScale:Float=1f,val heroScale:Float=1f,val textHoldRatio:Float=1f,
    val cameraSpeed:Float=1f,val trackingMode:FumeTrackingMode=FumeTrackingMode.Smooth,
    val glowIntensity:Float=1f,val showPrintSymbols:Boolean=true,
    /** Stable article shuffle input, corresponding to the source theme name. */
    val layoutSeed:String="Fume",val chorusLineIndices:Set<Int> = emptySet(),
    val wordColors:List<FoliaWordColor> = emptyList(),
) {
    init {
        require(fontScale.isFinite()&&fontScale in .5f..2f)
        require(heroScale.isFinite()&&heroScale in .82f..1.32f)
        require(textHoldRatio.isFinite()&&textHoldRatio in 0f..1f)
        require(cameraSpeed.isFinite()&&cameraSpeed in .55f..1.85f)
        require(glowIntensity.isFinite()&&glowIntensity in 0f..1.8f)
        require(chorusLineIndices.all{it>=0})
    }
}

/** Explicit identity of a core source line; null accompanimentIndex is the main
 * line. No identity is inferred from left/right alignment. Last duplicate wins. */
@Immutable
data class CappellaLineAgent(val lineIndex:Int,val agentId:String,val accompanimentIndex:Int?=null) {
    init { require(lineIndex>=0);require(accompanimentIndex==null||accompanimentIndex>=0) }
}

@Immutable
data class CappellaTuning(
    val fontScale:Float=1f,
    /** Host-supplied title; blank uses the explicit local fallback "歌词". */
    val title:String="歌词",
    val background:Color=Color(0xff162435),
    val agents:List<CappellaLineAgent> = emptyList(),
) {
    init { require(fontScale.isFinite()&&fontScale in .5f..2f) }
}

@Immutable
data class PendoloTuning(
    val lyricsFontScale:Float=1f,
    val subtitleFontScale:Float=1f,
    /** Explicit top-level core indices; accompaniment is not chorus. */
    val chorusLineIndices:Set<Int> = emptySet(),
    val wordColors:List<FoliaWordColor> = emptyList(),
) {
    init {
        require(lyricsFontScale.isFinite() && lyricsFontScale in .5f..2f)
        require(subtitleFontScale.isFinite() && subtitleFontScale in .5f..2f)
        require(chorusLineIndices.all{it>=0})
    }
}

@Immutable
data class FoliaWordColor(val word:String,val color:Color)

@Immutable
data class CadenzaTuning(
    val fontScale:Float=1.12f,
    val widthRatio:Float=.72f,
    val glowIntensity:Float=1f,
    val motionAmount:Float=1f,
    /** Explicit top-level lyrics-core line indices, not inferred from accompaniment. */
    val chorusLineIndices:Set<Int> = emptySet(),
    val wordColors:List<FoliaWordColor> = emptyList(),
) {
    init {
        require(fontScale.isFinite()&&fontScale in .5f..2f)
        require(widthRatio.isFinite()&&widthRatio in .35f..1f)
        require(glowIntensity.isFinite()&&glowIntensity in 0f..2f)
        require(motionAmount.isFinite()&&motionAmount in 0f..2f)
        require(chorusLineIndices.all{it>=0})
    }
}

enum class TiltColorScheme { Default, Swap, AccentAll, PrimaryAll }

@Immutable
data class TiltTuning(
    val splitProbability: Float = .75f,
    val tiltStyleProbability: Float = .35f,
    val colorScheme: TiltColorScheme = TiltColorScheme.Default,
) {
    init {
        require(splitProbability.isFinite() && splitProbability in 0f..1f)
        require(tiltStyleProbability.isFinite() && tiltStyleProbability in 0f..1f)
    }
}

@Immutable
data class PartitaTuning(
    val showGuideLines: Boolean = true,
    val useSemanticLayout: Boolean = true,
    /** CSS logical pixels, independent of lyric font size. */
    val staggerMin: Float = 20f,
    val staggerMax: Float = 100f,
    val avoidOverlap: Boolean = false,
) {
    init {
        require(staggerMin.isFinite() && staggerMax.isFinite())
        require(staggerMin in 0f..180f && staggerMax in staggerMin..180f)
    }
}

@Immutable
data class ClassicTuning(
    val enableWordRotation: Boolean = true,
    val breathingFloatMultiplier: Float = 1f,
    val wordSpacing: Float = .7f,
    val avoidOverlap: Boolean = false,
) {
    init {
        require(breathingFloatMultiplier.isFinite() && breathingFloatMultiplier in 0f..2f)
        require(wordSpacing.isFinite() && wordSpacing in 0f..2f)
    }
}

@Immutable
data class FoliaTypography(
    /** null selects the responsive clamp(36sp, 6% of width in dp, 72sp). */
    val main: TextStyle? = null,
    val secondary: TextStyle = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Medium),
    val subtitle: TextStyle = TextStyle(fontSize = 18.sp),
    val scale: Float = 1f,
) {
    init { require(scale.isFinite() && scale in .5f..2f) }
}

@Immutable
data class FoliaColors(
    val text: Color = Color(0xFFDFE5EC),
    val active: Color = Color(0xFFFFFFFF),
    val secondary: Color = Color(0xFFADB9C8),
    /** Host theme color for the subtitle shadow and optional local light pad.
     * Transparent preserves the existing transparent overlay and layout. */
    val subtitleBackground: Color = Color.Transparent,
)
