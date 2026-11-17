// Copyright (c) 2026 chthollyphile and contributors
// Text-only target/continuous-response adaptation of VisualizerCadenza.tsx cc6226e4.
// SPDX-License-Identifier: AGPL-3.0-only. Shared internal renderer for calibration/public preview.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.cadenza

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.Canvas as BitmapCanvas
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.*
import top.ghhccghk.accompanist.lyrics.folia.internal.rendering.*
import top.ghhccghk.accompanist.lyrics.folia.FoliaQuality
import top.ghhccghk.accompanist.lyrics.folia.internal.text.*
import kotlin.math.*

private class CadenzaGlyphShadow(private val layout: TextLayoutResult, private val path: Path?,
    private val density: Density) {
    private val bounds=path?.getBounds()?:Rect(Offset.Zero,Size(layout.size.width.toFloat(),layout.size.height.toFloat()))
    private val padding=ceil(64*density.density).toInt()
    private val origin=Offset(floor(bounds.left)-padding,floor(bounds.top)-padding)
    private val width=ceil(bounds.right-origin.x+padding).toInt().coerceAtLeast(1)
    private val height=ceil(bounds.bottom-origin.y+padding).toInt().coerceAtLeast(1)
    val retainedBytes:Long get()=width.toLong()*height*4
    val raster: ParagraphRaster by lazy {
        val bitmap=ImageBitmap(width,height)
        CanvasDrawScope().draw(density,LayoutDirection.Ltr,BitmapCanvas(bitmap),Size(bitmap.width.toFloat(),bitmap.height.toFloat())) {
            withTransform({translate(-origin.x,-origin.y)}) {
                if(path==null)drawParagraphShadowSource(layout)else clipPath(path){drawParagraphShadowSource(layout)}
            }
        }
        ParagraphRaster(blurredMasks(bitmap,floatArrayOf(20*density.density)).single(),origin)
    }
}
private data class CadenzaPiece(val body: BodyParagraphRaster, val baselineOffset: Float,
    val layoutSize: IntSize, val shadows: List<CadenzaGlyphShadow>, val splitGlow: Boolean,
    val wordShadow:CadenzaGlyphShadow)
private class CadenzaDrawn { var time:Double?=null }

/** A complete shaped fragment is rasterized once; per-EGC shadows clip that same layout.
 * The source creates separate DOM glyph spans; preserving joining/shaping is an explicit
 * platform adaptation, not evidence that its Arabic pixels already match. */
@Composable
internal fun CadenzaTargetStage(line: CadenzaLyric, time: ()->Double, textStyle: TextStyle,
    modifier: Modifier=Modifier, glow: Double=1.0, intensity: CadenzaIntensity=CadenzaIntensity.NORMAL,
    fontScale: Double=1.12, motion: Boolean=false, motionAmount: Double=1.0,
    palette:CadenzaPalette=CadenzaPalette(), widthRatio:Double=.72,
    reducedMotion:Boolean=false,quality:FoliaQuality=FoliaQuality.Standard,motionStart:Double=line.start,
    onQuality:(FoliaQuality,Int)->Unit={_,_->},onClick:(()->Unit)?=null,onLongPress:(()->Unit)?=null,
    onPrepared: (CadenzaPreparedText)->Unit={}) {
    val density=LocalDensity.current;val resolver=LocalFontFamilyResolver.current
    val measurer=rememberTextMeasurer(cacheSize=128)
    val currentTime by rememberUpdatedState(time)
    BoxWithConstraints(modifier.fillMaxSize()) {
        val width=maxWidth.value.toDouble();val height=maxHeight.value.toDouble()
        val prepared=remember(line,width,textStyle,density,measurer,intensity,fontScale,palette,widthRatio){
            prepareCadenzaText(line,width,measurer,textStyle,density,fontScale=fontScale,intensity=intensity,
                widthRatio=widthRatio,color=palette::encoded)
        }
        val nativeInk=LocalClassicNativeBodyInk.current
        val rawPieces=remember(prepared,resolver,density,measurer,nativeInk,quality){
            val metrics=naturalFontMetrics(prepared.style,resolver,density)
            val font=prepared.size.fontPx.toFloat()*density.density
            val cssBase=cssFontBaseline(metrics.ascent,metrics.descent,floor(font*64)/64)
            prepared.placements.associate{p->
                val layout=measurer.measure(p.fragment.text,prepared.style,softWrap=false)
                val baseline=paintedNaturalBaseline(layout,metrics)
                val coverage=rasterizeParagraph(layout,density)
                val pixels=IntArray(coverage.image.width*coverage.image.height);coverage.image.readPixels(pixels)
                val ink=alphaBounds(pixels,coverage.image.width,coverage.image.height)
                val measuredAscent=ink?.let{baseline-it.top-coverage.origin.y}?.takeIf{it!=0f}?:font*.78f
                val bodyStyle=classicBodyRasterStyle(prepared.style)
                val bodyLayout=if(bodyStyle==prepared.style)layout else measurer.measure(p.fragment.text,bodyStyle,softWrap=false)
                val split=cadenzaTiming(line).reveal==CadenzaReveal.NORMAL&&!cadenzaIsCjk(p.fragment.text)&&cadenzaGraphemes(p.fragment.text).size>1
                val paths=if(split)graphemeBoundaries(p.fragment.text).sorted().zipWithNext{a,b->layout.getPathForRange(a,b)}else listOf(null)
                p.id to CadenzaPiece(BodyParagraphRaster(bodyLayout,density,nativeInk),cssBase-measuredAscent-baseline,
                    layout.size,paths.map{CadenzaGlyphShadow(layout,it,density)},split,CadenzaGlyphShadow(layout,null,density))
            }
        }
        val actualQuality=remember(rawPieces,quality){
            val full=rawPieces.values.sumOf{p->p.shadows.sumOf{it.retainedBytes}}
            val reduced=rawPieces.values.sumOf{it.wordShadow.retainedBytes}
            when {
                quality==FoliaQuality.Standard&&full<=GlowBudget.primaryBytes->FoliaQuality.Standard
                quality!=FoliaQuality.Minimal&&reduced<=GlowBudget.primaryBytes->FoliaQuality.Reduced
                else->FoliaQuality.Minimal
            }
        }
        val pieces=remember(rawPieces,actualQuality){rawPieces.mapValues{(_,p)->when(actualQuality){
            FoliaQuality.Standard->p
            FoliaQuality.Reduced->p.copy(shadows=listOf(p.wordShadow),splitGlow=false)
            FoliaQuality.Minimal->p.copy(shadows=emptyList(),splitGlow=false)
        }}}
        val observer by rememberUpdatedState(onPrepared)
        LaunchedEffect(prepared){observer(prepared)}
        val qualityObserver by rememberUpdatedState(onQuality)
        LaunchedEffect(prepared,actualQuality){qualityObserver(actualQuality,prepared.placements.size)}
        val response=remember(prepared,height,intensity,motionAmount,motion,motionStart,reducedMotion){
            if(motion&&!reducedMotion)CadenzaMotionTrack(line,prepared.placements,width,height,intensity,motionAmount,
                startTime=motionStart,endTime=maxOf(line.end,cadenzaTiming(line).end,motionStart)+1.0)else null
        }
        fun targets(t:Double):List<CadenzaWordTarget> {
            val values=response?.sample(t)?:cadenzaTargets(t,line,prepared.placements,width,height,intensity,motionAmount)
            return if(!reducedMotion)values else values.map{v->val p=v.placement
                v.copy(state=v.state.copy(x=width/2+p.x,y=height*.42+p.y,rotation=0.0,scale=p.scale,blur=0.0))}
        }
        fun envelope(t:Double)=cadenzaLineEnvelope(t,line,cadenzaTiming(line)).let{if(reducedMotion)it.copy(scale=1.0,blur=0.0)else it}
        val drawn=remember(prepared){CadenzaDrawn()}
        val click by rememberUpdatedState(onClick);val press by rememberUpdatedState(onLongPress)
        fun hit(point:Offset):Boolean {
            val t=drawn.time?:return false;val e=envelope(t);if(e.opacity<.015)return false
            val d=density.density;val center=Offset((width/2*d).toFloat(),(height*.42*d).toFloat())
            val unscaled=(point-center)/e.scale.toFloat()+center
            return targets(t).any{target->
                val p=target.placement;val s=target.state;val piece=pieces.getValue(p.id)
                if(s.bodyAlpha<.015||s.scale<=0)false else {
                    val anchor=Offset(((s.x+p.width/2)*d).toFloat(),((s.y-p.height*.42)*d).toFloat())
                    val q=(unscaled-anchor)/s.scale.toFloat();val angle=-s.rotation*PI/180
                    val local=Offset((q.x*cos(angle)-q.y*sin(angle)).toFloat(),(q.x*sin(angle)+q.y*cos(angle)).toFloat())
                    val left=(-p.width/2*d).toFloat();val top=(p.height*.42*d).toFloat()+piece.baselineOffset
                    Rect(left,top,left+piece.layoutSize.width,top+piece.layoutSize.height).contains(local)
                }
            }
        }
        val input=if(onClick==null&&onLongPress==null)Modifier else Modifier.pointerInput(prepared,reducedMotion,response){
            detectTapGestures(onTap={if(hit(it))click?.invoke()},onLongPress={if(hit(it))press?.invoke()})
        }.semantics {contentDescription=line.text;onClick{click?.invoke();true};onLongClick{press?.invoke();true}}
            .onKeyEvent{if(it.key!=Key.Enter&&it.key!=Key.Spacebar)false else {if(it.type==KeyEventType.KeyUp)click?.invoke();true}}.focusable()
        val lineLayer=rememberClassicLayer()
        val bodyLayers=prepared.placements.associate{it.id to rememberClassicLayer()}
        Canvas(Modifier.fillMaxSize().then(input)) {
            val t=currentTime();drawn.time=t;val targets=targets(t)
            val envelope=envelope(t)
            val d=density.density
            val padding=ceil(160*d).toInt()
            lineLayer.alpha=envelope.opacity.toFloat()
            lineLayer.blurSigma=if(envelope.blur>.05)(floor(envelope.blur*100+.5)/100*d*envelope.scale).toFloat()else 0f
            lineLayer.record(this,IntSize(size.width.toInt()+padding*2,size.height.toInt()+padding*2)) {
                withTransform({translate(padding.toFloat(),padding.toFloat());scale(envelope.scale.toFloat(),envelope.scale.toFloat(),Offset((width/2*d).toFloat(),(height*.42*d).toFloat()))}) {
                    for(target in targets) {
                        val p=target.placement;val state=target.state;val piece=pieces.getValue(p.id)
                        if(state.bodyAlpha<.015&&state.glowAlpha<.015)continue
                        val ink=cadenzaInk(p.fragment.color)
                        (if(reducedMotion)null else cadenzaChorusRipple(line,target))?.let{ring->
                            drawCircle(ink, (ring.radius*d).toFloat(),Offset((ring.x*d).toFloat(),(ring.y*d).toFloat()),
                                alpha=ring.alpha.toFloat(),style=Stroke((ring.stroke*d).toFloat()))
                        }
                        val anchor=Offset(((state.x+p.width/2)*d).toFloat(),((state.y-p.height*.42)*d).toFloat())
                        withTransform({translate(anchor.x,anchor.y);rotate(state.rotation.toFloat(),Offset.Zero);scale(state.scale.toFloat(),state.scale.toFloat(),Offset.Zero)
                            translate((-p.width/2*d).toFloat(),(p.height*.42*d).toFloat()+piece.baselineOffset)}) {
                            for((index,shadow)in piece.shadows.withIndex()) {
                                val value=if(piece.splitGlow)cadenzaCharGlow(t,p.fragment.word,p.fragment.startInWord+index,
                                    max(p.fragment.graphemeCount,piece.shadows.size),p.fragment.timings)else cadenzaWordGlow(t,cadenzaTiming(line),p.fragment.word)
                                val alphas=cadenzaShadowAlphas(value*state.glowAlpha.coerceIn(0.0,1.0)*max(glow,0.0))
                                if(alphas.isNotEmpty())for(alpha in alphas) {
                                    val raster=shadow.raster
                                    drawImage(raster.image,raster.origin,alpha=alpha.toFloat(),colorFilter=ColorFilter.tint(ink))
                                }
                            }
                            val layer=bodyLayers.getValue(p.id)
                            layer.alpha=state.bodyAlpha.toFloat();layer.blurSigma=if(state.blur>.05)(floor(state.blur*100+.5)/100*d).toFloat()else 0f
                            val pad=ceil(64*d).toInt()
                            layer.record(this,IntSize(piece.layoutSize.width+2*pad,piece.layoutSize.height+2*pad)) {
                                fun channel(a:Float,b:Float)=floor((a+(b-a)*state.activeMix.coerceIn(0.0,1.0))*255+.5).toInt()
                                val color=Color(channel(palette.primary.red,ink.red),channel(palette.primary.green,ink.green),channel(palette.primary.blue,ink.blue))
                                piece.body.draw(this,color,Offset(pad.toFloat(),pad.toFloat()))
                            }
                            withTransform({translate(-pad.toFloat(),-pad.toFloat())}){drawClassicLayer(layer)}
                        }
                    }
                }
            }
            withTransform({translate(-padding.toFloat(),-padding.toFloat())}){drawClassicLayer(lineLayer)}
        }
    }
}
