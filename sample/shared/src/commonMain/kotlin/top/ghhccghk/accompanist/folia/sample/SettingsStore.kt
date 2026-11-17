package top.ghhccghk.accompanist.folia.sample

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import top.ghhccghk.accompanist.lyrics.folia.*

interface SettingsStore {
    suspend fun read(): String?
    suspend fun write(value: String)
}

@Composable
expect fun rememberSettingsStore(): SettingsStore

data class SampleSettings(val options: FoliaOptions = FoliaOptions(),
    val tuning: ClassicTuning = ClassicTuning(), val fontScale: Float = 1f,
    val lyricTheme: SampleLyricTheme = SampleLyricTheme.Classic,
    val partitaTuning: PartitaTuning = PartitaTuning(),
    val renderResolution: RenderResolution = RenderResolution.Full,
    val tiltTuning: TiltTuning = TiltTuning(),
    val cadenzaTuning:CadenzaTuning=CadenzaTuning(),
    val pendoloTuning:PendoloTuning=PendoloTuning(),
    val cappellaTuning:CappellaTuning=CappellaTuning(),
    val fumeTuning:FumeTuning=FumeTuning()) {
    fun encode(): String = listOf("version=1", "translation=${options.showTranslation}",
        "phonetic=${options.showPhonetic}", "next=${options.showNextLine}", "motion=${options.reducedMotion}",
        "quality=${options.quality}", "offset=${options.offsetMillis}", "rotation=${tuning.enableWordRotation}",
        "spacing=${tuning.wordSpacing}", "breathing=${tuning.breathingFloatMultiplier}",
        "overlap=${tuning.avoidOverlap}", "font=$fontScale", "lyricTheme=$lyricTheme",
        "partitaGuides=${partitaTuning.showGuideLines}","partitaSemantic=${partitaTuning.useSemanticLayout}",
        "partitaMin=${partitaTuning.staggerMin}","partitaMax=${partitaTuning.staggerMax}",
        "partitaOverlap=${partitaTuning.avoidOverlap}","renderResolution=$renderResolution",
        "tiltSplit=${tiltTuning.splitProbability}","tiltProbability=${tiltTuning.tiltStyleProbability}",
        "tiltColors=${tiltTuning.colorScheme}","cadenzaFont=${cadenzaTuning.fontScale}",
        "pendoloLyricsFont=${pendoloTuning.lyricsFontScale}","pendoloSubtitleFont=${pendoloTuning.subtitleFontScale}",
        "cappellaFont=${cappellaTuning.fontScale}",
        "fumeFont=${fumeTuning.fontScale}","fumeHero=${fumeTuning.heroScale}","fumeHold=${fumeTuning.textHoldRatio}",
        "fumeSpeed=${fumeTuning.cameraSpeed}","fumeTracking=${fumeTuning.trackingMode}",
        "fumeGlow=${fumeTuning.glowIntensity}","fumeSymbols=${fumeTuning.showPrintSymbols}",
        "pendoloChorus=${pendoloTuning.chorusLineIndices.sorted().joinToString(",")}",
        "pendoloWords=${pendoloTuning.wordColors.joinToString("|"){entry->
            entry.word.map{it.code.toString(16).padStart(4,'0')}.joinToString("")+"~"+entry.color.toArgb().toUInt().toString(16)
        }}",
        "cadenzaWidth=${cadenzaTuning.widthRatio}","cadenzaGlow=${cadenzaTuning.glowIntensity}",
        "cadenzaMotion=${cadenzaTuning.motionAmount}","cadenzaChorus=${cadenzaTuning.chorusLineIndices.sorted().joinToString(",")}",
        "cadenzaWords=${cadenzaTuning.wordColors.joinToString("|"){entry->
            entry.word.map{it.code.toString(16).padStart(4,'0')}.joinToString("")+"~"+entry.color.toArgb().toUInt().toString(16)
        }}").joinToString("\n")

    companion object {
        fun decode(value: String): SampleSettings {
            val fields = value.lineSequence().mapNotNull { line ->
                val separator = line.indexOf('=')
                if (separator <= 0) null else line.substring(0, separator) to line.substring(separator + 1)
            }.toMap()
            require(fields["version"] == "1") { "未知设置版本，请重新保存显示设置" }
            fun boolean(key: String, default: Boolean) = fields[key]?.toBooleanStrictOrNull() ?: default
            fun number(key: String, default: Float, range: ClosedFloatingPointRange<Float>) =
                fields[key]?.toFloatOrNull()?.takeIf { it.isFinite() && it in range } ?: default
            return SampleSettings(FoliaOptions(boolean("translation", true), boolean("phonetic", true),
                boolean("next", true), fields["offset"]?.toIntOrNull()?.coerceIn(-2_000, 2_000) ?: 0,
                boolean("motion", false), FoliaQuality.entries.find { it.name == fields["quality"] } ?: FoliaQuality.Standard),
                ClassicTuning(boolean("rotation", true), number("breathing", 1f, 0f..2f),
                    number("spacing", .7f, 0f..2f), boolean("overlap", false)), number("font", 1f, .5f..2f),
                SampleLyricTheme.entries.find{it.name==fields["lyricTheme"]}?:SampleLyricTheme.Classic,
                PartitaTuning(boolean("partitaGuides",true),boolean("partitaSemantic",true),
                    minOf(number("partitaMin",20f,0f..180f),number("partitaMax",100f,0f..180f)),
                    maxOf(number("partitaMin",20f,0f..180f),number("partitaMax",100f,0f..180f)),
                    boolean("partitaOverlap",false)),
                RenderResolution.entries.find { it.name == fields["renderResolution"] } ?: RenderResolution.Full,
                TiltTuning(number("tiltSplit",.75f,0f..1f),number("tiltProbability",.35f,0f..1f),
                    TiltColorScheme.entries.find{it.name==fields["tiltColors"]}?:TiltColorScheme.Default),
                CadenzaTuning(number("cadenzaFont",1.12f,.5f..2f),number("cadenzaWidth",.72f,.35f..1f),
                    number("cadenzaGlow",1f,0f..2f),number("cadenzaMotion",1f,0f..2f),
                    fields["cadenzaChorus"].orEmpty().split(',').mapNotNull{it.toIntOrNull()?.takeIf{n->n>=0}}.toSet(),
                    fields["cadenzaWords"].orEmpty().split('|').mapNotNull{entry->
                        val parts=entry.split('~');if(parts.size!=2||parts[0].length%4!=0)return@mapNotNull null
                        val chars=parts[0].chunked(4).map{it.toIntOrNull(16)?:return@mapNotNull null}
                        val color=parts[1].toUIntOrNull(16)?:return@mapNotNull null
                        FoliaWordColor(chars.map{it.toChar()}.joinToString(""),Color(color.toInt()))
                    }),
                PendoloTuning(number("pendoloLyricsFont",1f,.5f..2f),number("pendoloSubtitleFont",1f,.5f..2f),
                    fields["pendoloChorus"].orEmpty().split(',').mapNotNull{it.toIntOrNull()?.takeIf{n->n>=0}}.toSet(),
                    fields["pendoloWords"].orEmpty().split('|').mapNotNull{entry->
                        val parts=entry.split('~');if(parts.size!=2||parts[0].length%4!=0)return@mapNotNull null
                        val chars=parts[0].chunked(4).map{it.toIntOrNull(16)?:return@mapNotNull null}
                        val color=parts[1].toUIntOrNull(16)?:return@mapNotNull null
                        FoliaWordColor(chars.map{it.toChar()}.joinToString(""),Color(color.toInt()))
                    }),CappellaTuning(fontScale=number("cappellaFont",1f,.5f..2f)),
                FumeTuning(fontScale=number("fumeFont",1f,.5f..2f),heroScale=number("fumeHero",1f,.82f..1.32f),
                    textHoldRatio=number("fumeHold",1f,0f..1f),cameraSpeed=number("fumeSpeed",1f,.55f..1.85f),
                    trackingMode=FumeTrackingMode.entries.find{it.name==fields["fumeTracking"]}?:FumeTrackingMode.Smooth,
                    glowIntensity=number("fumeGlow",1f,0f..1.8f),showPrintSymbols=boolean("fumeSymbols",true)))
        }
    }
}

fun SampleSession.settings() = SampleSettings(options, tuning, fontScale, lyricTheme, partitaTuning, renderResolution, tiltTuning,cadenzaTuning,pendoloTuning,cappellaTuning.copy(title="歌词",agents=emptyList()),
    fumeTuning.copy(layoutSeed="Fume",chorusLineIndices=emptySet(),wordColors=emptyList()))
fun SampleSession.applySettings(value: SampleSettings) {
    options = value.options; tuning = value.tuning; fontScale = value.fontScale
    lyricTheme=value.lyricTheme;partitaTuning=value.partitaTuning
    renderResolution=value.renderResolution
    tiltTuning=value.tiltTuning
    cadenzaTuning=value.cadenzaTuning
    pendoloTuning=value.pendoloTuning
    cappellaTuning=value.cappellaTuning
    fumeTuning=value.fumeTuning
}
