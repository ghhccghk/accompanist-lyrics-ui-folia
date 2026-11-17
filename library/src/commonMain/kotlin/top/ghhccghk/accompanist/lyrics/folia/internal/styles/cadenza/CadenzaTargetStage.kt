// Copyright (c) 2026 chthollyphile and contributors
// Text-only target/continuous-response adaptation of VisualizerCadenza.tsx cc6226e4.
// SPDX-License-Identifier: AGPL-3.0-only. This is INTERNAL calibration, not a public theme.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.cadenza

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.Canvas as BitmapCanvas
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.*
import top.ghhccghk.accompanist.lyrics.folia.internal.rendering.*
import top.ghhccghk.accompanist.lyrics.folia.internal.text.*
import kotlin.math.*

private class CadenzaGlyphShadow(private val layout: TextLayoutResult, private val path: Path?,
    private val density: Density) {
    val raster: ParagraphRaster by lazy {
        val bounds=path?.getBounds()?:Rect(Offset.Zero,Size(layout.size.width.toFloat(),layout.size.height.toFloat()))
        val padding=ceil(64*density.density).toInt()
        val origin=Offset(floor(bounds.left)-padding,floor(bounds.top)-padding)
        val bitmap=ImageBitmap(ceil(bounds.right-origin.x+padding).toInt().coerceAtLeast(1),
            ceil(bounds.bottom-origin.y+padding).toInt().coerceAtLeast(1))
        CanvasDrawScope().draw(density,LayoutDirection.Ltr,BitmapCanvas(bitmap),Size(bitmap.width.toFloat(),bitmap.height.toFloat())) {
            withTransform({translate(-origin.x,-origin.y)}) {
                if(path==null)drawParagraphShadowSource(layout)else clipPath(path){drawParagraphShadowSource(layout)}
            }
        }
        ParagraphRaster(blurredMasks(bitmap,floatArrayOf(20*density.density)).single(),origin)
    }
}
private data class CadenzaPiece(val body: BodyParagraphRaster, val baselineOffset: Float,
    val layoutSize: IntSize, val shadows: List<CadenzaGlyphShadow>, val splitGlow: Boolean)

/** A complete shaped fragment is rasterized once; per-EGC shadows clip that same layout.
 * The source creates separate DOM glyph spans; preserving joining/shaping is an explicit
 * platform adaptation, not evidence that its Arabic pixels already match. */
@Composable
internal fun CadenzaTargetStage(line: CadenzaLyric, time: ()->Double, textStyle: TextStyle,
    modifier: Modifier=Modifier, glow: Double=1.0, intensity: CadenzaIntensity=CadenzaIntensity.NORMAL,
    fontScale: Double=1.12, motion: Boolean=false, motionAmount: Double=1.0,
    onPrepared: (CadenzaPreparedText)->Unit={}) {
    val density=LocalDensity.current;val resolver=LocalFontFamilyResolver.current
    val measurer=rememberTextMeasurer(cacheSize=128)
    val currentTime by rememberUpdatedState(time)
    BoxWithConstraints(modifier.fillMaxSize()) {
        val width=maxWidth.value.toDouble();val height=maxHeight.value.toDouble()
        val prepared=remember(line,width,textStyle,density,measurer,intensity,fontScale){
            prepareCadenzaText(line,width,measurer,textStyle,density,fontScale=fontScale,intensity=intensity)
        }
        val nativeInk=LocalClassicNativeBodyInk.current
        val pieces=remember(prepared,resolver,density,measurer,nativeInk){
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
                    layout.size,paths.map{CadenzaGlyphShadow(layout,it,density)},split)
            }
        }
        val observer by rememberUpdatedState(onPrepared)
        LaunchedEffect(prepared){observer(prepared)}
        val response=remember(prepared,height,intensity,motionAmount,motion){
            if(motion)CadenzaMotionTrack(line,prepared.placements,width,height,intensity,motionAmount,
                endTime=max(max(line.end,cadenzaTiming(line).end),line.start)+1.0)else null
        }
        val lineLayer=rememberClassicLayer()
        val bodyLayers=prepared.placements.associate{it.id to rememberClassicLayer()}
        Canvas(Modifier.fillMaxSize()) {
            val t=currentTime();val targets=response?.sample(t)?:cadenzaTargets(t,line,prepared.placements,width,height,intensity,motionAmount)
            val envelope=cadenzaLineEnvelope(t,line,cadenzaTiming(line))
            val d=density.density
            val padding=ceil(160*d).toInt()
            lineLayer.alpha=envelope.opacity.toFloat()
            lineLayer.blurSigma=if(envelope.blur>.05)(floor(envelope.blur*100+.5)/100*d*envelope.scale).toFloat()else 0f
            lineLayer.record(this,IntSize(size.width.toInt()+padding*2,size.height.toInt()+padding*2)) {
                withTransform({translate(padding.toFloat(),padding.toFloat());scale(envelope.scale.toFloat(),envelope.scale.toFloat(),Offset((width/2*d).toFloat(),(height*.42*d).toFloat()))}) {
                    for(target in targets) {
                        val p=target.placement;val state=target.state;val piece=pieces.getValue(p.id)
                        if(state.bodyAlpha<.015&&state.glowAlpha<.015)continue
                        val anchor=Offset(((state.x+p.width/2)*d).toFloat(),((state.y-p.height*.42)*d).toFloat())
                        withTransform({translate(anchor.x,anchor.y);rotate(state.rotation.toFloat(),Offset.Zero);scale(state.scale.toFloat(),state.scale.toFloat(),Offset.Zero)
                            translate((-p.width/2*d).toFloat(),(p.height*.42*d).toFloat()+piece.baselineOffset)}) {
                            val ink=Color(0xfff17b4d)
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
                                fun channel(a:Int,b:Int)=floor(a+(b-a)*state.activeMix.coerceIn(0.0,1.0)+.5).toInt()
                                val color=Color(channel(223,241),channel(229,123),channel(236,77))
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
