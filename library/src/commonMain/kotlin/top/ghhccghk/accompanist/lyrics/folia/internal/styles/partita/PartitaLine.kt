// Copyright (c) 2026 chthollyphile
// Kotlin/Compose adaptation of PartitaChunk/VisualizerPartita and shared GlowWord.
// SPDX-License-Identifier: AGPL-3.0-only. Full source/modification ledger: docs/upstream.md.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.partita

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.key.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.Canvas as BitmapCanvas
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.*
import top.ghhccghk.accompanist.lyrics.folia.*
import top.ghhccghk.accompanist.lyrics.folia.internal.animation.*
import top.ghhccghk.accompanist.lyrics.folia.internal.model.NormalizedLine
import top.ghhccghk.accompanist.lyrics.folia.internal.rendering.*
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.classic.classicBodyColor
import top.ghhccghk.accompanist.lyrics.folia.internal.text.*
import kotlin.math.*

internal data class PartitaGlyph(val timing: TimedGrapheme, val origin: Offset, val image: ImageBitmap,
    val gpuSigmas:List<Float> = emptyList())
internal data class PartitaCell(val unit: DisplayUnit, val layout: TextLayoutResult, val body: BodyParagraphRaster,
    val pose: Pose, val baseline: Float, val glyphs: List<PartitaGlyph> = emptyList())
internal data class PartitaPrepared(val rows: List<List<PartitaCell>>, val geometry: PartitaGeometry,
    val quality: FoliaQuality)
private class PartitaDrawnFrame { var time: Long?=null; var size=Size.Zero }

/** Independent chunk stage, sharing only shaped text/GlowWord drawing primitives with classic. */
@Composable
@OptIn(ExperimentalFoliaRendererBridge::class)
internal fun PartitaLine(line: NormalizedLine, playback: () -> FoliaPlaybackSnapshot,
    textStyle: TextStyle, colors: FoliaColors, options: FoliaOptions,
    config: PartitaConfig = PartitaConfig(), modifier: Modifier = Modifier,
    enterUs: Long = line.startUs, leaveUs: Long? = null,
    maskBudgetBytes: Long = GlowBudget.primaryBytes,
    onPrepared: (FoliaQuality, Int) -> Unit = { _, _ -> },
    onLayout: (PartitaGeometry) -> Unit = {},
    onClick: (() -> Unit)? = null, onLongPress: (() -> Unit)? = null) {
    val density = LocalDensity.current
    val resolver = LocalFontFamilyResolver.current
    val renderTarget = LocalFoliaPartitaRenderTarget.current
    // Observe asynchronous font completion, including a stable resolver instance.
    val resolvedFont = resolver.resolve(textStyle.fontFamily,
        textStyle.fontWeight ?: androidx.compose.ui.text.font.FontWeight.Normal,
        textStyle.fontStyle ?: androidx.compose.ui.text.font.FontStyle.Normal,
        textStyle.fontSynthesis ?: androidx.compose.ui.text.font.FontSynthesis.All).value
    val maskProcessor = top.ghhccghk.accompanist.lyrics.folia.LocalFoliaMaskProcessor.current
    val measurer = rememberTextMeasurer(cacheSize = 64)
    val nativeInk = LocalClassicNativeBodyInk.current
    val currentPlayback by rememberUpdatedState(playback)
    BoxWithConstraints(modifier.fillMaxSize()) {
        val cssWidth = maxWidth.value.toDouble()
        val cssHeight = maxHeight.value.toDouble()
        val layout = remember(line,cssHeight,config,options.animationIntensity) {
            val source=buildPartitaLayout(line,cssHeight,options.animationIntensity,config)
            source.copy(rows=source.rows.map { row ->
                row.copy(words=shapeDisplayWords(row.words,row.words.joinToString(""){it.text}))
            })
        }
        val prepared = remember(layout,textStyle,cssWidth,cssHeight,options.quality,config.avoidOverlap,
            colors.active.alpha,measurer,resolver,resolvedFont,density,nativeInk,maskBudgetBytes,maskProcessor,renderTarget) {
            fun prepare(fit: Double): PartitaPrepared {
                val size = partitaFontSize(textStyle.fontSize.value.toDouble(),cssWidth,fit)
                // Blink's glyph cache uses hundredths; CSS line boxes/em lengths
                // retain the used clamp precision independently of that cache.
                val glyphSize = floor(size * 100 + .000001) / 100
                val style = textStyle.copy(fontSize = glyphSize.sp, lineHeight = TextUnit.Unspecified,
                    textAlign = TextAlign.Left, textDirection = classicTextDirection(textStyle,line.text),
                    textMotion = textStyle.textMotion ?: TextMotion.Animated)
                val metrics = naturalFontMetrics(style,resolver,density)
                val rows = layout.rows.map { row ->
                    val poses = partitaWordPoses(row,options.animationIntensity)
                    // Each original PartitaChunk owns its dir attribute. Neutral
                    // chunks must not inherit the direction of a different chunk.
                    val rowStyle=style.copy(textDirection=classicTextDirection(textStyle,row.words.joinToString(" "){it.text}))
                    row.words.mapIndexed { index, unit ->
                        val text = cssWordText(unit.text)
                        val measured = measurer.measure(AnnotatedString(text.text),rowStyle,softWrap=false)
                        val bodyStyle = classicBodyRasterStyle(rowStyle)
                        val bodyLayout = if (bodyStyle == rowStyle) measured else
                            measurer.measure(AnnotatedString(text.text),bodyStyle,softWrap=false)
                        val lineHeight = floor(size * 1.22 * 64) / 64
                        val baseline = cssFontBaseline(metrics.ascent/density.density,metrics.descent/density.density,
                            lineHeight.toFloat()) * density.density - paintedNaturalBaseline(measured,metrics)
                        PartitaCell(unit,measured,BodyParagraphRaster(bodyLayout,density,nativeInk),poses[index],baseline)
                    }
                }
                val widths = rows.map { cells -> cells.map { cell ->
                    ceil(cell.layout.getPathForRange(0,cell.layout.layoutInput.text.length).getBounds().width /
                        density.density * 64.0) / 64
                } }
                val spacing = if (!config.avoidOverlap) null else partitaSpacing(rows.mapIndexed { index, cells ->
                    val row = layout.rows[index]
                    val emStyle = style.copy(fontSize=100.sp)
                    val emWidths = row.words.map { word ->
                        val m=measurer.measure(AnnotatedString(canvasWordText(word.text)),emStyle,softWrap=false)
                        m.getPathForRange(0,m.layoutInput.text.length).getBounds().width / density.density / 100.0
                    }
                    PartitaMeasuredRow(emWidths,cells.map { it.pose },row,
                        isRtlLyric(row.words.joinToString(" ") { it.text }))
                },size)
                return PartitaPrepared(rows,partitaGeometry(layout,widths,size,spacing),FoliaQuality.Minimal)
            }
            var fit = 1.0
            var result = prepare(fit)
            while (config.avoidOverlap && fit > .5) {
                val h = floor(result.geometry.height + .5)
                val w = result.geometry.rows.maxOfOrNull { floor(it.box.width + .5) } ?: 0.0
                val ratio = min((floor(cssHeight * .7 + .5) - 64) / h,
                    if (w > 0) (cssWidth - 64) / w else Double.POSITIVE_INFINITY)
                if (!(ratio < 1)) break
                val next = max(.5, fit * ratio * .98)
                if (next >= fit - .005) break
                fit = next; result = prepare(fit)
            }
            val padding = ceil(72 * density.density).toInt()
            fun visible(cell: PartitaCell) = cell.unit.graphemes.filter { g ->
                val boundaries = cssWordText(cell.unit.text).boundaries
                boundaries[g.end] > boundaries[g.begin]
            }
            fun bounds(cell: PartitaCell, timing: TimedGrapheme): Rect {
                val b=cssWordText(cell.unit.text).boundaries
                return cell.layout.getPathForRange(b[timing.begin],b[timing.end]).getBounds()
            }
            val bytes = result.rows.flatten().sumOf { cell -> visible(cell).sumOf { g ->
                val b=bounds(cell,g)
                (ceil(b.width).toLong()+2L*padding+1)*(ceil(b.height).toLong()+2L*padding+1)*4L
            } }
            val layers = GlowBudget.layers(options.quality,bytes,maskBudgetBytes)
            if (layers == 0) result else result.copy(
                quality=if(layers==2) FoliaQuality.Standard else FoliaQuality.Reduced,
                rows=result.rows.map { row -> row.map { cell ->
                    val duration=line.endUs-line.startUs
                    val radii=if(duration<100_000) floatArrayOf(14f,24f) else if(duration<180_000) floatArrayOf(18f,32f) else floatArrayOf(20f,40f)
                    val b=cssWordText(cell.unit.text).boundaries
                    cell.copy(glyphs=visible(cell).map { g ->
                        val path=cell.layout.getPathForRange(b[g.begin],b[g.end]);val rect=path.getBounds()
                        val origin=Offset(floor(rect.left)-padding,floor(rect.top)-padding)
                        val image=ImageBitmap(ceil(rect.right-origin.x+padding).toInt().coerceAtLeast(1),
                            ceil(rect.bottom-origin.y+padding).toInt().coerceAtLeast(1))
                        CanvasDrawScope().draw(density,LayoutDirection.Ltr,BitmapCanvas(image),Size(image.width.toFloat(),image.height.toFloat())) {
                            withTransform({translate(-origin.x,-origin.y)}) {clipPath(path){drawParagraphShadowSource(cell.layout)}}
                        }
                        val sigmas=List(layers){max(1f,radii[it]*density.density/2)}
                        if(renderTarget!=null) PartitaGlyph(g,origin,image,sigmas)
                        else PartitaGlyph(g,origin,compositeGlowMasks(blurredMasks(image,
                            sigmas.toFloatArray(),maskProcessor),colors.active.alpha))
                    })
                } })
        }
        val notify by rememberUpdatedState(onPrepared)
        val layoutObserver by rememberUpdatedState(onLayout)
        LaunchedEffect(prepared) {
            notify(prepared.quality,prepared.rows.sumOf { it.size });layoutObserver(prepared.geometry)
        }
        val profile=remember(line){renderProfile(line.startUs,line.endUs,line.fragments.lastOrNull()?.endUs?:line.endUs)}
        val drawn=remember(prepared){PartitaDrawnFrame()}
        val click by rememberUpdatedState(onClick)
        val longPress by rememberUpdatedState(onLongPress)
        fun hit(point: Offset): Boolean {
            val time=drawn.time?:return false
            val frame=sampleLine(time,enterUs,leaveUs,profile,options.reducedMotion,1f,density.density,options.animationIntensity)
            if(frame.alpha<=.001f || frame.scale<=0) return false
            val d=density.density
            val center=Offset(drawn.size.width/2,drawn.size.height/2)
            val inLine=(point-Offset(0f,frame.breathingY)-center)/frame.scale+center
            val geometry=prepared.geometry
            val contentWidth=geometry.rows.maxOfOrNull{it.box.left*2+it.box.width}?:0f
            val origin=Offset((drawn.size.width-contentWidth*d)/2,(drawn.size.height-geometry.height*d)/2)
            return prepared.rows.indices.reversed().any { ri ->
                val geo=geometry.rows[ri];val row=layout.rows[ri]
                val chunk=samplePartitaChunk(time,enterUs,row,profile,options.reducedMotion)
                if(chunk.alpha<=.001f || chunk.scale<=0) false else {
                    val relative=inLine-origin-Offset((geo.box.left+geo.box.width/2+chunk.x)*d,(geo.box.top+geo.box.height/2+chunk.y)*d)
                    val angle=-chunk.rotation*PI/180
                    val inRow=Offset((relative.x*cos(angle)-relative.y*sin(angle)).toFloat(),
                        (relative.x*sin(angle)+relative.y*cos(angle)).toFloat())/chunk.scale+Offset(geo.box.width*d/2,geo.box.height*d/2)
                    prepared.rows[ri].indices.reversed().any { wi ->
                        val cell=prepared.rows[ri][wi];val box=geo.words[wi]
                        val word=sampleWord(time,enterUs,cell.unit.startUs,cell.unit.endUs,profile,cell.pose,options.reducedMotion,options.animationIntensity)
                        hitClassicUnit(inRow,Offset.Zero,1f,0f,Offset((box.left+box.width/2)*d,(box.top+box.height/2)*d),
                            word,d,box.width*d,box.height*d)
                    }
                }
            }
        }
        val input=if(onClick==null && onLongPress==null) Modifier else Modifier
            .pointerInput(prepared,options,enterUs,leaveUs) {
                if(renderTarget!=null)partitaNativeGestures(::hit,{click?.invoke()},{longPress?.invoke()})
                else detectTapGestures(onTap={if(hit(it))click?.invoke()},onLongPress={if(hit(it))longPress?.invoke()})
            }.semantics {
                contentDescription=line.text
                onClick {click?.invoke();true}
                onLongClick {longPress?.invoke();true}
            }.onKeyEvent {event->
                if(event.key!=Key.Enter && event.key!=Key.Spacebar) false
                else {if(event.type==KeyEventType.KeyUp)click?.invoke();true}
            }.focusable()
        if(renderTarget!=null) {
            val plan=remember(prepared,layout,colors,options,config,density,enterUs,leaveUs) {
                PartitaNativePlan(line,prepared,layout,colors,options,config,density,enterUs,leaveUs)
            }
            renderTarget.Line(plan.scene,Modifier.fillMaxSize().then(input),
                frame={width,height->plan.frame((currentPlayback().positionMillis-options.offsetMillis)*1000,width,height)},
                onPresented={frame->drawn.time=frame.mediaTimeUs;drawn.size=Size(frame.width.toFloat(),frame.height.toFloat())})
            return@BoxWithConstraints
        }
        val lineLayer=rememberClassicLayer()
        val rowLayers=prepared.rows.map { rememberClassicLayer() }
        val wordLayers=prepared.rows.map { row -> row.map { rememberClassicLayer() } }
        val blurLayers=prepared.rows.map { row -> row.map { rememberClassicLayer() } }
        val halo=remember(colors.active){GlowMaskPaint(colors.active)}
        val guides=remember(prepared.geometry,density,config.showGuideLines,maskProcessor) {
            if(!config.showGuideLines) emptyList() else prepared.geometry.rows.map { row ->
                preparePartitaGuide(Size(density.density,32*density.density),density,maskProcessor) to
                    preparePartitaGuide(Size((row.box.width+36)*density.density,density.density),density,maskProcessor)
            }
        }
        val guideLayers=guides.map{rememberClassicLayer() to rememberClassicLayer()}
        val rowPaddings=remember(prepared,density) { prepared.rows.map { cells ->
            ceil((cells.maxOfOrNull { hypot(it.layout.size.width.toFloat(),it.layout.size.height.toFloat())*it.pose.scale*.75f }?:0f)+160*density.density).toInt()
        } }
        Canvas(Modifier.fillMaxSize().then(input)) {
            val time=(currentPlayback().positionMillis-options.offsetMillis)*1000
            drawn.time=time;drawn.size=size
            val frame=sampleLine(time,enterUs,leaveUs,profile,options.reducedMotion,1f,density.density,options.animationIntensity)
            val d=density.density
            val geometry=prepared.geometry
            val contentWidth=geometry.rows.maxOfOrNull { it.box.left*2+it.box.width }?:0f
            val origin=Offset((size.width-contentWidth*d)/2,(size.height-geometry.height*d)/2)
            val padding=ceil(180*d).toInt()
            lineLayer.alpha=frame.alpha;lineLayer.blurSigma=frame.blur
            lineLayer.forceOffscreen=partitaLineNeedsOffscreen(time,enterUs,leaveUs,profile,options.reducedMotion)
            lineLayer.record(this,IntSize(size.width.toInt()+2*padding,size.height.toInt()+2*padding)) {
                withTransform({translate(padding.toFloat(),padding.toFloat());translate(0f,frame.breathingY);scale(frame.scale,frame.scale,center-Offset(padding.toFloat(),padding.toFloat()))}) {
                    prepared.rows.forEachIndexed { ri,cells ->
                        val row=layout.rows[ri];val geo=geometry.rows[ri]
                        val chunk=samplePartitaChunk(time,enterUs,row,profile,options.reducedMotion)
                        val rp=rowPaddings[ri]
                        val rowLayer=rowLayers[ri]
                        rowLayer.alpha=chunk.alpha.coerceIn(0f,1f)
                        rowLayer.record(this,IntSize(ceil(geo.box.width*d).toInt()+2*rp,ceil(geo.box.height*d).toInt()+2*rp)) {
                            withTransform({translate(rp.toFloat(),rp.toFloat())}) {
                                if(config.showGuideLines && chunk.guide>0f) {
                                    val ink=if(chunk.active)colors.active else Color.White.copy(alpha=if(chunk.waiting).14f else .22f)
                                    val right=ri%2==1
                                    val x=if(right)geo.box.width*d+7*d else -8*d
                                    val y=(geo.box.height+16)*d
                                    withTransform({translate(x,y)}) {
                                        drawPartitaGuide(guides[ri].first,ink,if(chunk.active)colors.active.copy(alpha=colors.active.alpha*69/255) else null,
                                            chunk.guide,false,right,guideLayers[ri].first)
                                    }
                                    withTransform({translate(if(right)(geo.box.width+16)*d else -16*d,(geo.box.height+7)*d)}) {
                                        drawPartitaGuide(guides[ri].second,ink,if(chunk.active)colors.active.copy(alpha=colors.active.alpha*53/255) else null,
                                            chunk.guide,true,right,guideLayers[ri].second)
                                    }
                                }
                                cells.forEachIndexed { wi,cell ->
                                    val box=geo.words[wi]
                                    val word=sampleWord(time,enterUs,cell.unit.startUs,cell.unit.endUs,profile,cell.pose,options.reducedMotion,options.animationIntensity)
                                    val layer=wordLayers[ri][wi]
                                    layer.alpha=word.alpha.coerceIn(0f,1f)
                                    val wp=ceil(96*d).toInt()
                                    layer.record(this,IntSize(cell.layout.size.width+2*wp,cell.layout.size.height+2*wp)) {
                                        withTransform({translate(wp.toFloat(),wp.toFloat())}) {
                                            cell.glyphs.forEach { g ->
                                                val alpha=sampleGraphemeGlow(time,enterUs,cell.unit.startUs,cell.unit.endUs,g.timing.startUs,
                                                    g.timing.endUs,cell.unit.graphemes.size,profile,g.timing.startSeconds,g.timing.endSeconds)
                                                if(alpha>.001f)halo.draw(this,g.image,g.origin,alpha)
                                            }
                                            val ink=classicBodyColor(colors.text,colors.active,word.mix)
                                            if(word.blur>.01f) {
                                                val blur=blurLayers[ri][wi];val bp=ceil(32*d).toInt()
                                                blur.blurSigma=word.blur*d
                                                blur.record(this,IntSize(cell.layout.size.width+2*bp,cell.layout.size.height+2*bp)) {
                                                    cell.body.draw(this,ink,Offset(bp.toFloat(),bp.toFloat()))
                                                }
                                                withTransform({translate(-bp.toFloat(),-bp.toFloat())}){drawClassicLayer(blur)}
                                            } else cell.body.draw(this,ink,Offset.Zero)
                                        }
                                    }
                                    withTransform({translate((box.left+box.width/2+word.x)*d,(box.top+box.height/2+word.y)*d)
                                        rotate(word.rotation,Offset.Zero);scale(word.scale,word.scale,Offset.Zero)
                                        translate(-box.width*d/2-wp,-box.height*d/2+cell.baseline-wp)}){drawClassicLayer(layer)}
                                }
                            }
                        }
                        withTransform({translate(origin.x+(geo.box.left+geo.box.width/2+chunk.x)*d,
                            origin.y+(geo.box.top+geo.box.height/2+chunk.y)*d)
                            rotate(chunk.rotation,Offset.Zero);scale(chunk.scale,chunk.scale,Offset.Zero)
                            translate(-geo.box.width*d/2-rp,-geo.box.height*d/2-rp)}){drawClassicLayer(rowLayer)}
                    }
                }
            }
            withTransform({translate(-padding.toFloat(),-padding.toFloat())}){drawClassicLayer(lineLayer)}
        }
    }
}
