package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import top.ghhccghk.accompanist.lyrics.folia.internal.model.normalize
import top.ghhccghk.accompanist.lyrics.folia.internal.timeline.VisualTimeline
import top.ghhccghk.accompanist.lyrics.folia.internal.timeline.SubtitleHistory
import top.ghhccghk.accompanist.lyrics.folia.internal.timeline.SubtitleSource
import top.ghhccghk.accompanist.lyrics.folia.internal.timeline.resolveSubtitleContent
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.classic.ClassicLine
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.classic.ClassicSubtitles
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.classic.ClassicStage
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.classic.classicHeightBudget
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.partita.PartitaLine
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.partita.PartitaConfig
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.partita.partitaGraphemeCount
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.tilt.*
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.cadenza.*
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.pendolo.*
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.cappella.*
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.fume.*
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama.*
import top.ghhccghk.accompanist.lyrics.folia.internal.model.SourceRef
import top.ghhccghk.accompanist.lyrics.folia.internal.timeline.PrimaryEpoch
import top.ghhccghk.accompanist.lyrics.folia.internal.rendering.GlowBudget
import top.ghhccghk.accompanist.lyrics.folia.internal.waiting.*
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/** Host owns media time and seeking. Whole runs are shaped by Compose, never per UTF-16 char. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FoliaLyricsView(
    lyrics: SyncedLyrics,
    playback: () -> FoliaPlaybackSnapshot,
    modifier: Modifier = Modifier,
    contentKey: Any = lyrics.id,
    typography: FoliaTypography = FoliaTypography(),
    colors: FoliaColors = FoliaColors(),
    options: FoliaOptions = FoliaOptions(),
    style: FoliaStyle = FoliaStyle.Classic(),
    onStatus: (FoliaStatus) -> Unit = {},
    onLineClicked: (ISyncedLine) -> Unit = {},
    onLinePressed: (ISyncedLine) -> Unit = {},
) {
    val document = remember(IdentityKey(lyrics), contentKey) { normalize(lyrics) }
    val tilt = style is FoliaStyle.Tilt
    val index = remember(document,tilt) { VisualTimeline(document.lines,if(tilt)450_000 else null) }
    val cadenzaLines=remember(document,style is FoliaStyle.Cadenza){
        if(style is FoliaStyle.Cadenza)document.lines.map(::cadenzaLyric)else null
    }
    val cadenzaIndex=remember(cadenzaLines){cadenzaLines?.let(::CadenzaTimeline)}
    val cappellaCore=remember(document,style){(style as? FoliaStyle.Cappella)?.let{c->
        cappellaCoreLines(document,c.tuning.agents.associate{SourceRef(it.lineIndex,it.accompanimentIndex) to it.agentId})
    }}
    val cappellaLines=remember(cappellaCore){cappellaCore?.map{it.stage}}
    val cappellaCounts=remember(cappellaLines){cappellaLines?.map{CappellaRevealPlan(it).characters.size}}
    val fumeLines=remember(document,(style as? FoliaStyle.Fume)?.tuning?.chorusLineIndices,style is FoliaStyle.Fume){
        (style as? FoliaStyle.Fume)?.let{f->document.lines.map{line->
            fumeLyric(line).copy(chorus=!line.accompaniment&&line.ref.index in f.tuning.chorusLineIndices)
        }}
    }
    val fumeCounts=remember(fumeLines){fumeLines?.map{FumeReveal(it).graphemes.size}}
    // Unknown/invalid vocal intervals cannot safely establish silence.
    val waitingTimeline = remember(document) {
        WaitingTimeline(if(document.diagnostics.isEmpty()) document.lines else emptyList())
    }
    val subtitleHistory = remember(document) { SubtitleHistory(document.lines) }
    val currentPlayback by rememberUpdatedState(playback)
    val selection by remember(index, cadenzaIndex, cappellaLines, fumeLines, options.offsetMillis) { derivedStateOf {
        val us=(currentPlayback().positionMillis-options.offsetMillis)*1000
        val classic=index.at(us)
        if(fumeLines!=null){
            val i=fumeLines.indexOfLast{us/1e6>=it.start&&us/1e6<=it.renderEnd}
            classic.copy(primary=if(i<0)null else PrimaryEpoch(i,(fumeLines[i].start*1e6).toLong(),
                (fumeLines[i].renderEnd*1e6).toLong()),exiting=emptyList(),secondary=emptyList())
        }else if(cappellaLines!=null){
            val i=cappellaCurrentLine(cappellaLines,us/1e6)
            classic.copy(primary=if(i<0)null else PrimaryEpoch(i,(cappellaLines[i].start*1e6).toLong(),
                (cappellaLines[i].renderEnd*1e6).toLong()),exiting=emptyList(),secondary=emptyList())
        }else if(cadenzaIndex==null)classic else {
            val selected=cadenzaIndex.at(us/1e6)
            classic.copy(primary=selected.active?.let{i->PrimaryEpoch(i,(selected.entered!!*1e6).toLong(),
                (cadenzaTiming(cadenzaLines!![i]).end*1e6).toLong())},exiting=emptyList(),
                secondary=(classic.secondary+listOfNotNull(classic.primary?.line)).distinct().filter{
                    it!=selected.active&&document.lines[it].accompaniment&&
                        us>=document.lines[it].startUs&&us<=document.lines[it].endUs},next=selected.upcoming)
        }
    } }
    val waiting by remember(waitingTimeline,index,cadenzaIndex,cappellaLines,fumeLines,options.offsetMillis) { derivedStateOf {
        val time=(currentPlayback().positionMillis-options.offsetMillis)*1000
        waitingTimeline.visibleAt(time,selection)
    } }
    val subtitles by remember(index,cadenzaIndex,cappellaLines,fumeLines,subtitleHistory,options) {derivedStateOf {
        val time=(currentPlayback().positionMillis-options.offsetMillis)*1000
        val active=selection.primary?.line
        resolveSubtitleContent(document.lines,active,subtitleHistory.recent(time,active),buildList {
            if(options.showTranslation) add(SubtitleSource.Translation)
            if(options.showPhonetic) add(SubtitleSource.Phonetic)
            if(options.showNextLine) add(SubtitleSource.Next)
        },visible=options.showTranslation||options.showPhonetic||options.showNextLine,allowFallback=options.showNextLine)
    }}
    // Preparation callbacks belong to this document/epoch, including a null primary in gaps.
    // Never carry a previous line's count or budget fallback into the next selection.
    var quality by remember(document, selection.primary, options.quality, style) { mutableStateOf(FoliaQuality.Minimal) }
    var unitCount by remember(document, selection.primary, options.quality, style) { mutableIntStateOf(0) }
    var themeDiagnostic by remember(document,style){mutableStateOf<String?>(null)}
    val statusObserver by rememberUpdatedState(onStatus)
    LaunchedEffect(document, selection.primary, quality, options.quality, unitCount,themeDiagnostic) {
        statusObserver(FoliaStatus(options.quality, quality, unitCount,
            document.diagnostics.map { "${it.ref.index}:${it.reason}" } + listOfNotNull(themeDiagnostic) +
                if (unitCount > 0 && quality != options.quality) listOf("GlowBudgetExceeded") else emptyList()))
    }
    BoxWithConstraints(modifier) {
        // Upstream classic uses the full viewport for 6vw, then a centred 70vh
        // stage with p-8. Subtitles are an overlay and must not move the main line.
        val heightBudget = with(LocalDensity.current) { classicHeightBudget(maxHeight.toPx(), density).toDp() }
        val responsiveMain = typography.secondary.copy(
            fontSize = (if(style is FoliaStyle.Partita)
                (maxWidth.value*.055f).coerceIn(40f,72f)*typography.scale
                else (maxWidth.value * .06f).coerceIn(36f, 72f) * typography.scale).sp, fontWeight = FontWeight.Bold)
        // A caller can supply only a font family/weight for a theme. Resolve
        // its missing size before switching to classic/Partita text preparation.
        val mainStyle=typography.main?.let { if(it.fontSize==TextUnit.Unspecified)it.copy(fontSize=responsiveMain.fontSize)else it }?:responsiveMain
        val main = selection.primary?.line?.let(document.lines::get)
        if(style is FoliaStyle.Diorama){
            var ready by remember(document){mutableStateOf(false)}
            val coreLines=remember(document){dioramaCoreLines(document)}
            LaunchedEffect(ready,selection.primary,options.quality,style){
                unitCount=if(ready)selection.primary?.line?.let{dioramaUnits(coreLines[it].lyric).size}?:0 else 0
                quality=if(unitCount==0)FoliaQuality.Minimal else options.quality
            }
            if(document.lines.none{it.text.isNotBlank()})BasicText("暂无可显示歌词",style=mainStyle.copy(color=colors.secondary))
            else DioramaPlaybackStage(lyrics,{(currentPlayback().positionMillis-options.offsetMillis)/1000.0},
                typography.main?:typography.secondary,Modifier.fillMaxSize(),scale=typography.scale,
                tuning=style.tuning,colors=colors,options=options,
                onClick=onLineClicked,onLongPress=onLinePressed,
                onState={isReady,diagnostic->
                    ready=isReady;themeDiagnostic=diagnostic
                    if(!isReady){unitCount=0;quality=FoliaQuality.Minimal}
                })
        }else if(style is FoliaStyle.Fume){
            val tuning=style.tuning;val lines=fumeLines!!
            // The stage owns preparation invalidation. Paint-only changes or a
            // subtitle style must not reset readiness without restarting its job.
            var ready by remember(document){mutableStateOf(false)}
            LaunchedEffect(ready,selection.primary,options.quality,style){
                unitCount=if(ready)selection.primary?.line?.let{fumeCounts!![it]}?:0 else 0
                quality=if(unitCount==0)FoliaQuality.Minimal else options.quality
            }
            fun androidx.compose.ui.graphics.Color.ink()=FumeInk((red*255+.5f).toInt(),(green*255+.5f).toInt(),(blue*255+.5f).toInt(),alpha.toDouble())
            val theme=remember(colors,tuning.wordColors,tuning.glowIntensity,options.quality,options.animationIntensity){
                FumeFrameTheme(colors.text.ink(),colors.active.ink(),options.animationIntensity.name.lowercase(),
                    if(options.quality==FoliaQuality.Minimal)0.0 else tuning.glowIntensity.toDouble(),
                    tuning.wordColors.map{it.word to it.color.ink()})
            }
            if(lines.none{it.text.isNotBlank()})BasicText("暂无可显示歌词",style=mainStyle.copy(color=colors.secondary))
            else FumeArticleStage(lines,{(currentPlayback().positionMillis-options.offsetMillis)/1000.0},
                {lines.indexOfLast{line->val t=(currentPlayback().positionMillis-options.offsetMillis)/1000.0;t>=line.start&&t<=line.renderEnd}},
                typography.main?:typography.secondary,theme,Modifier.fillMaxSize(),
                fontScale=tuning.fontScale.toDouble()*typography.scale,heroScale=tuning.heroScale.toDouble(),
                hold=tuning.textHoldRatio.toDouble(),cameraSpeed=tuning.cameraSpeed.toDouble(),
                stepped=tuning.trackingMode==FumeTrackingMode.Stepped,reducedMotion=options.reducedMotion,
                showStamp=tuning.showPrintSymbols&&options.quality!=FoliaQuality.Minimal,layoutSeed=tuning.layoutSeed,
                onClick={onLineClicked(document.lines[it].source)},onLongPress={onLinePressed(document.lines[it].source)},
                onState={isReady,diagnostic->ready=isReady;themeDiagnostic=diagnostic})
        }else if(style is FoliaStyle.Cappella) {
            val tuning=style.tuning
            val core=cappellaCore!!;val lines=cappellaLines!!
            val intensity=when(options.animationIntensity){
                FoliaAnimationIntensity.Calm->CappellaIntensity.CALM
                FoliaAnimationIntensity.Normal->CappellaIntensity.NORMAL
                FoliaAnimationIntensity.Chaotic->CappellaIntensity.CHAOTIC
            }
            if(core.isEmpty())BasicText("暂无可显示歌词",style=mainStyle.copy(color=colors.secondary))
            else CappellaTargetStage(lines,tuning.title.trim().ifEmpty{"歌词"},
                {(currentPlayback().positionMillis-options.offsetMillis)/1000.0},
                {cappellaCurrentLine(lines,(currentPlayback().positionMillis-options.offsetMillis)/1000.0)},
                typography.main?:typography.secondary.copy(fontWeight=FontWeight.Bold),Modifier.fillMaxSize(),
                intensity=intensity,palette=CappellaPalette(colors.text,colors.active,colors.secondary,tuning.background),
                fontScale=tuning.fontScale.toDouble()*typography.scale,reducedMotion=options.reducedMotion,
                playbackMotion=true,glowEnabled=options.quality!=FoliaQuality.Minimal,
                onClick={onLineClicked(core[it].source)},onLongPress={onLinePressed(core[it].source)},
                onRows={
                    val i=cappellaCurrentLine(lines,(currentPlayback().positionMillis-options.offsetMillis)/1000.0)
                    unitCount=if(i<0)0 else cappellaCounts!![i]
                    quality=if(unitCount==0)FoliaQuality.Minimal else options.quality
                })
        } else if(style is FoliaStyle.Pendolo) {
            val tuning=style.tuning
            val core=remember(document,tuning.chorusLineIndices){pendoloCoreLines(document,tuning.chorusLineIndices)}
            val lines=remember(core){core.map{it.stage}}
            val mode=when {
                options.showTranslation && options.showPhonetic->PendoloSubtitleMode.BOTH
                options.showTranslation->PendoloSubtitleMode.TRANSLATION
                options.showPhonetic->PendoloSubtitleMode.ROMANIZATION
                else->PendoloSubtitleMode.NONE
            }
            val intensity=when(options.animationIntensity){
                FoliaAnimationIntensity.Calm->PendoloIntensity.CALM
                FoliaAnimationIntensity.Normal->PendoloIntensity.NORMAL
                FoliaAnimationIntensity.Chaotic->PendoloIntensity.CHAOTIC
            }
            if(core.isEmpty()) BasicText("暂无可显示歌词",style=mainStyle.copy(color=colors.secondary))
            else PendoloPlaybackWheel(lines,{(currentPlayback().positionMillis-options.offsetMillis)/1000.0},
                typography.main?:typography.secondary.copy(fontWeight=FontWeight.Bold),Modifier.fillMaxSize(),intensity,
                reducedMotion=options.reducedMotion,mode=mode,wordColors=tuning.wordColors,
                palette=PendoloPalette(colors.text,colors.secondary,colors.active),
                typeScale=PendoloTypeScale(tuning.lyricsFontScale.toDouble()*typography.scale,
                    tuning.subtitleFontScale.toDouble()*typography.scale),subtitleStyle=typography.subtitle,
                glowEnabled=options.quality!=FoliaQuality.Minimal,
                onClick={onLineClicked(core[it].source)},onLongPress={onLinePressed(core[it].source)},
                onRows={rows->
                    unitCount=rows.filter{it.item.active}.sumOf{it.main.rows.sumOf{row->row.graphemeEnd-row.graphemeStart}}
                    quality=if(unitCount==0)FoliaQuality.Minimal else options.quality
                })
        } else if(style is FoliaStyle.Classic) {
        val tuning = style.tuning
        ClassicStage(Modifier.fillMaxSize()) {
                selection.exiting.forEach { epoch -> key(epoch) {
                    ClassicLine(document.lines[epoch.line], playback, mainStyle, colors, options, tuning,
                        heightBudget = heightBudget, enterUs = epoch.enterUs, leaveUs = epoch.leaveUs,
                        maskBudgetBytes = GlowBudget.perExitBytes(index.maxExitingCount))
                } }
                if (document.lines.isEmpty()) BasicText("暂无可显示歌词",
                    modifier = Modifier.fillMaxWidth(),
                    style = mainStyle.copy(color = colors.secondary, textAlign = TextAlign.Center))
                else if(main != null) key(selection.primary) {
                    ClassicLine(main, playback, mainStyle, colors, options, tuning, heightBudget,
                        Modifier.fillMaxWidth(),
                        enterUs = selection.primary!!.enterUs,
                        onPrepared = { actual, count -> quality = actual; unitCount = count },
                        onClick = { onLineClicked(main.source) }, onLongPress = { onLinePressed(main.source) })
                }
                waiting?.let { gap ->
                    WaitingBreathingDots((gap.startUs/1000).toInt(),(gap.endUs/1000).toInt(),
                        { (currentPlayback().positionMillis-options.offsetMillis)
                            .coerceIn(Int.MIN_VALUE.toLong(),Int.MAX_VALUE.toLong()).toInt() },
                        Modifier.semantics { contentDescription="等待演唱" },
                        reducedMotion=options.reducedMotion,
                        defaults=WaitingDotsDefaults(breathingDotsColor=colors.active))
                }
        }
        } else if(style is FoliaStyle.Partita) {
            val t=style.tuning
            val config=PartitaConfig(t.useSemanticLayout,t.staggerMin.toDouble(),t.staggerMax.toDouble(),t.avoidOverlap,t.showGuideLines)
            Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) {
                selection.exiting.forEach { epoch ->key(epoch) {
                    val source=document.lines[epoch.line]
                    val size=if(typography.main==null && partitaGraphemeCount(source)>40)
                        mainStyle.copy(fontSize=mainStyle.fontSize*.8f) else mainStyle
                    PartitaLine(source,playback,size,colors,options,config,enterUs=epoch.enterUs,leaveUs=epoch.leaveUs,
                        maskBudgetBytes=GlowBudget.perExitBytes(index.maxExitingCount))
                } }
                if(document.lines.isEmpty()) BasicText("暂无可显示歌词",style=mainStyle.copy(color=colors.secondary))
                else if(main!=null) key(selection.primary) {
                    val size=if(typography.main==null && partitaGraphemeCount(main)>40)
                        mainStyle.copy(fontSize=mainStyle.fontSize*.8f) else mainStyle
                    PartitaLine(main,playback,size,colors,options,config,enterUs=selection.primary!!.enterUs,
                        onPrepared={actual,count->quality=actual;unitCount=count},
                        onClick={onLineClicked(main.source)},onLongPress={onLinePressed(main.source)})
                }
                waiting?.let {gap->
                    WaitingBreathingDots((gap.startUs/1000).toInt(),(gap.endUs/1000).toInt(),
                        {(currentPlayback().positionMillis-options.offsetMillis).coerceIn(Int.MIN_VALUE.toLong(),Int.MAX_VALUE.toLong()).toInt()},
                        Modifier.semantics{contentDescription="等待演唱"},reducedMotion=options.reducedMotion,
                        defaults=WaitingDotsDefaults(breathingDotsColor=colors.active))
                }
            }
        } else if(style is FoliaStyle.Cadenza) {
            val tuning=style.tuning
            Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) {
                if(document.lines.isEmpty())BasicText("暂无可显示歌词",style=mainStyle.copy(color=colors.secondary))
                else if(main!=null)key(selection.primary) {
                    val primary=selection.primary!!
                    val source=cadenzaLines!![primary.line].copy(chorus=main.ref.index in tuning.chorusLineIndices)
                    val palette=remember(colors,tuning.wordColors){CadenzaPalette(colors.text,colors.active,tuning.wordColors.map{it.word to it.color})}
                    val intensity=when(options.animationIntensity){
                        FoliaAnimationIntensity.Calm->CadenzaIntensity.CALM
                        FoliaAnimationIntensity.Normal->CadenzaIntensity.NORMAL
                        FoliaAnimationIntensity.Chaotic->CadenzaIntensity.CHAOTIC
                    }
                    CadenzaTargetStage(source,{(currentPlayback().positionMillis-options.offsetMillis)/1000.0},
                        typography.main?:typography.secondary.copy(fontWeight=FontWeight.Bold),Modifier.fillMaxSize(),
                        glow=tuning.glowIntensity.toDouble(),intensity=intensity,fontScale=tuning.fontScale.toDouble()*typography.scale,
                        widthRatio=tuning.widthRatio.toDouble(),motion=true,motionAmount=tuning.motionAmount.toDouble(),palette=palette,
                        reducedMotion=options.reducedMotion,quality=options.quality,motionStart=primary.enterUs/1e6,
                        onQuality={actual,count->quality=actual;unitCount=count},
                        onClick={onLineClicked(main.source)},onLongPress={onLinePressed(main.source)})
                }
                waiting?.let{gap->WaitingBreathingDots((gap.startUs/1000).toInt(),(gap.endUs/1000).toInt(),
                    {(currentPlayback().positionMillis-options.offsetMillis).coerceIn(Int.MIN_VALUE.toLong(),Int.MAX_VALUE.toLong()).toInt()},
                    Modifier.semantics{contentDescription="等待演唱"},reducedMotion=options.reducedMotion,
                    defaults=WaitingDotsDefaults(breathingDotsColor=colors.active))}
            }
        } else if(style is FoliaStyle.Tilt) {
            val t=style.tuning
            val config=TiltConfig(t.splitProbability.toDouble(),t.tiltStyleProbability.toDouble())
            val ink=when(t.colorScheme) {
                TiltColorScheme.Default->TiltInk.Default;TiltColorScheme.Swap->TiltInk.Swap
                TiltColorScheme.AccentAll->TiltInk.AccentAll;TiltColorScheme.PrimaryAll->TiltInk.PrimaryAll
            }
            val tiltStyle=typography.main?:typography.secondary.copy(fontWeight=null)
            Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) {
                selection.exiting.forEach {epoch->key(epoch) {
                    TiltLine(document.lines[epoch.line],playback,tiltStyle,colors,options,config,ink,
                        fontScale=typography.scale,enterUs=epoch.enterUs,leaveUs=epoch.leaveUs)
                }}
                if(document.lines.isEmpty()) BasicText("暂无可显示歌词",style=mainStyle.copy(color=colors.secondary))
                else if(main!=null)key(selection.primary) {
                    TiltLine(main,playback,tiltStyle,colors,options,config,ink,fontScale=typography.scale,
                        enterUs=selection.primary!!.enterUs,
                        onPrepared={count->quality=options.quality;unitCount=count},
                        onClick={onLineClicked(main.source)},onLongPress={onLinePressed(main.source)})
                }
                waiting?.let {gap->
                    WaitingBreathingDots((gap.startUs/1000).toInt(),(gap.endUs/1000).toInt(),
                        {(currentPlayback().positionMillis-options.offsetMillis).coerceIn(Int.MIN_VALUE.toLong(),Int.MAX_VALUE.toLong()).toInt()},
                        Modifier.semantics{contentDescription="等待演唱"},reducedMotion=options.reducedMotion,
                        defaults=WaitingDotsDefaults(breathingDotsColor=colors.active))
                }
            }
        }
        if(style !is FoliaStyle.Pendolo) ClassicSubtitles(subtitles,document.lines,playback,typography,colors,options,maxWidth,
            selection.primary?.enterUs?:subtitles.line?.let {document.lines[it].startUs},
            Modifier.align(Alignment.BottomCenter).padding(bottom=112.dp),partita=style is FoliaStyle.Partita) {
            selection.secondary.forEach { secondary ->
                val line = document.lines[secondary]
                BasicText(line.text, Modifier.padding(top = 12.dp).combinedClickable(
                    onClick = { onLineClicked(line.source) }, onLongClick = { onLinePressed(line.source) }),
                    typography.secondary.copy(color = colors.secondary, textAlign = TextAlign.Center))
            }
        }
    }
}

@Composable
fun FoliaLyricsView(
    lyrics: SyncedLyrics,
    currentPosition: () -> Int,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    discontinuity: Long = 0,
    onLineClicked: (ISyncedLine) -> Unit = {},
) = FoliaLyricsView(lyrics, { FoliaPlaybackSnapshot(currentPosition().toLong(), isPlaying, discontinuity) },
    modifier = modifier, onLineClicked = onLineClicked)

/** remember() normally uses structural equality, but callbacks must retain the new input identity. */
private class IdentityKey(private val value: Any) {
    override fun equals(other: Any?) = other is IdentityKey && value === other.value
    override fun hashCode() = 0
}
