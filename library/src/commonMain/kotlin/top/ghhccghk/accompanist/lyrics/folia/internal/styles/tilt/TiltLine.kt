// Copyright (c) 2026 chthollyphile
// Kotlin/Compose adaptation of VisualizerTilt.tsx at fixed Folia cc6226e4.
// SPDX-License-Identifier: AGPL-3.0-only. Source/modification ledger: docs/upstream.md.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.tilt

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
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.*
import top.ghhccghk.accompanist.lyrics.folia.*
import top.ghhccghk.accompanist.lyrics.folia.internal.animation.*
import top.ghhccghk.accompanist.lyrics.folia.internal.model.NormalizedLine
import top.ghhccghk.accompanist.lyrics.folia.internal.rendering.*
import top.ghhccghk.accompanist.lyrics.folia.internal.text.*
import kotlin.math.*

internal data class TiltRowBox(val text:String,val tilt:Boolean,val box:Rect,val fontSize:Double)
private data class TiltGlyph(val rect:Rect,val raster:ParagraphRaster,val visualIndex:Int,val space:Boolean)
private data class TiltPreparedRow(val segment:TiltSegment,val box:TiltRowBox,val glyphs:List<TiltGlyph>,
    val timings:List<TiltCharTime>,val visibleUs:Long,val yOffset:Double)
private data class TiltPrepared(val rows:List<TiltPreparedRow>,val height:Float,val width:Float)
private class TiltDrawn { var time:Long?=null;var size=Size.Zero }
internal enum class TiltInk { Default, Swap, AccentAll, PrimaryAll }

/** Unlike GlowWord themes, Tilt has independent segment and per-EGC reveal layers.
 * Glyph masks come from one fully shaped paragraph, never UTF-16 character layouts. */
@Composable
internal fun TiltLine(line:NormalizedLine,playback:()->FoliaPlaybackSnapshot,textStyle:TextStyle,
    colors:FoliaColors,options:FoliaOptions,config:TiltConfig=TiltConfig(),ink:TiltInk=TiltInk.Default,
    fontScale:Float=1f,modifier:Modifier=Modifier,enterUs:Long=line.startUs,leaveUs:Long?=null,
    onPrepared:(Int)->Unit={},onLayout:(List<TiltRowBox>)->Unit={},
    onClick:(()->Unit)?=null,onLongPress:(()->Unit)?=null) {
    val density=LocalDensity.current;val resolver=LocalFontFamilyResolver.current
    val measurer=rememberTextMeasurer(cacheSize=64)
    val nativeInk=LocalClassicNativeBodyInk.current
    val currentPlayback by rememberUpdatedState(playback)
    BoxWithConstraints(modifier.fillMaxSize()) {
        val width=maxWidth.value.toDouble()
        val prepared=remember(line,config,ink,fontScale,width,textStyle,colors,density,resolver,measurer,nativeInk,enterUs) {
            fun style(font:Double,tilt:Boolean,tracking:Boolean):TextStyle = textStyle.copy(
                fontSize=(floor(font*100+.000001)/100).sp,
                fontWeight=textStyle.fontWeight?:if(tilt)FontWeight.Light else FontWeight.Normal,
                fontStyle=if(tilt)FontStyle.Italic else FontStyle.Normal,
                // VisualizerTilt inherits the host direction (LTR in the pinned
                // visualizer shell); the Unicode bidi algorithm orders RTL runs.
                // Forcing the lyric-first RTL policy from classic changes the
                // source visual order for standalone Arabic segments.
                textDirection=textStyle.textDirection,textAlign=TextAlign.Left,
                textMotion=textStyle.textMotion?:TextMotion.Animated,lineHeight=TextUnit.Unspecified,
                letterSpacing=if(tracking)(if(tilt).15f else .08f).em else 0.em)
            // Pinned Tilt appends `italic` after a stack ending in sans-serif.
            // Chromium rejects that font assignment and retains its previous
            // Canvas font (initially 10px sans-serif). Model the actual source
            // assignment order per line, without importing its global cache
            // history into seek/resize. This upstream quirk is documented.
            var measurementStyle=TextStyle(fontFamily=FontFamily.SansSerif,fontSize=10.sp,
                fontWeight=FontWeight.Normal,textMotion=TextMotion.Animated)
            val layout=buildTiltLayout(line.text,line.startUs/1e6,config,width,fontScale.toDouble()) { text,tilt,size ->
                if(!tilt)measurementStyle=style(size,false,false)
                val measured=measurer.measure(text,measurementStyle,softWrap=false)
                measured.getPathForRange(0,text.length).getBounds().width/density.density.toDouble()
            }
            val words=line.fragments.map { TiltWord(line.text.substring(it.begin,it.end),it.startUs/1e6,it.endUs/1e6) }
            val times=layout.segments.map { segment ->
                val selected=tiltWordRange(segment.charOffset,segment.text,line.text,words).map{words[it]}
                (selected.firstOrNull()?.start?:line.startUs/1e6) to (selected.lastOrNull()?.end?:line.endUs/1e6)
            }
            fun fixed(v:Double)=floor(v*1000+.5)/1000
            var top=0f
            val rows=layout.segments.mapIndexed { index,segment ->
                val base=fontScale*layout.scale
                val boost=if(segment.shortLast)1.18 else 1.0
                val scale=base*boost
                val used=(fixed(6.875*scale)*width/100).coerceIn(fixed(3.125*scale)*16,fixed(5.625*scale)*16)
                val styled=style(used,segment.isTilt,true)
                // CSS Tilt's character spans contain NBSP instead of collapsible spaces.
                val text=buildString {graphemeBoundaries(segment.text).sorted().zipWithNext { a,b ->
                    val part=segment.text.substring(a,b)
                    append(if(part.all{it.isWhitespace()})"\u00A0".repeat(part.length) else part)
                } }
                val measured=measurer.measure(text,styled,softWrap=false)
                val bodyStyle=classicBodyRasterStyle(styled)
                val bodyLayout=if(bodyStyle==styled)measured else measurer.measure(text,bodyStyle,softWrap=false)
                val body=BodyParagraphRaster(bodyLayout,density,nativeInk)
                val color=when(ink) {
                    TiltInk.Default->if(segment.isTilt)colors.active else colors.text
                    TiltInk.Swap->if(segment.isTilt)colors.text else colors.active
                    TiltInk.AccentAll->colors.active;TiltInk.PrimaryAll->colors.text
                }
                val metrics=naturalFontMetrics(styled,resolver,density)
                val lineHeight=floor(used*(if(segment.isTilt)1.25 else 1.35)*64)/64*density.density
                val baseline=cssFontBaseline(metrics.ascent/density.density,metrics.descent/density.density,
                    (lineHeight/density.density).toFloat())*density.density-paintedNaturalBaseline(measured,metrics)
                val boundaries=graphemeBoundaries(text).sorted()
                var visual=0
                val glyphs=boundaries.zipWithNext { begin,end ->
                    val clip=measured.getPathForRange(begin,end)
                    val bounds=clip.getBounds()
                    val space=text.substring(begin,end).all{it.isWhitespace()}
                    val origin=Offset(floor(bounds.left)-2,floor(bounds.top)-2)
                    val image=ImageBitmap(ceil(bounds.right-origin.x+2).toInt().coerceAtLeast(1),
                        ceil(bounds.bottom-origin.y+2).toInt().coerceAtLeast(1))
                    CanvasDrawScope().draw(density,LayoutDirection.Ltr,BitmapCanvas(image),Size(image.width.toFloat(),image.height.toFloat())) {
                        withTransform({translate(-origin.x,-origin.y)}) {clipPath(clip){body.draw(this,color,Offset.Zero)}}
                    }
                    TiltGlyph(bounds.translate(0f,baseline),ParagraphRaster(image,origin+Offset(0f,baseline)),visual,space)
                        .also {if(!space)visual++}
                }
                val rowWidth=ceil(measured.getPathForRange(0,text.length).getBounds().width/density.density*64)/64*density.density
                val box=TiltRowBox(segment.text,segment.isTilt,Rect(Offset(0f,top),Size(rowWidth,lineHeight.toFloat())),used)
                val from=times.drop(index).minOf{it.first}
                val visible=max(enterUs,((from-.25)*1e6).roundToLong())
                val yOffset=min(width*.06875*base,90*base)/6*density.density
                TiltPreparedRow(segment,box,glyphs,
                    buildTiltCharTimings(segment.charOffset,segment.text,times[index].first,times[index].second,line.text,words),visible,yOffset)
                    .also{top+=lineHeight.toFloat()+(if(width>=640)16 else 12)*density.density}
            }
            val height=(top-(if(width>=640)16 else 12)*density.density).coerceAtLeast(0f)
            TiltPrepared(rows,height,rows.maxOfOrNull{it.box.box.width}?:0f)
        }
        val notify by rememberUpdatedState(onPrepared);val layoutObserver by rememberUpdatedState(onLayout)
        LaunchedEffect(prepared){notify(prepared.rows.sumOf{it.glyphs.count{g->!g.space}});layoutObserver(prepared.rows.map{it.box})}
        val drawn=remember(prepared){TiltDrawn()}
        val click by rememberUpdatedState(onClick);val press by rememberUpdatedState(onLongPress)
        fun hit(point:Offset):Boolean {
            val time=drawn.time?:return false
            if(leaveUs!=null && time>=leaveUs+450_000)return false
            return prepared.rows.any { row ->
                if(time<row.visibleUs)false else {
                    val frame=sampleTiltRow(time,row.visibleUs,leaveUs,row.segment.isTilt,options.reducedMotion)
                    if(frame.alpha<=.001f)false else {
                        val origin=Offset((drawn.size.width-row.box.box.width)/2,
                            (drawn.size.height-prepared.height)/2+row.box.box.top+frame.y*density.density)
                        val center=origin+Offset(row.box.box.width/2,row.box.box.height/2)
                        Rect(origin,Size(row.box.box.width,row.box.box.height)).contains((point-center)/frame.scale+center)
                    }
                }
            }
        }
        val input=if(onClick==null && onLongPress==null)Modifier else Modifier.pointerInput(prepared,options,leaveUs) {
            detectTapGestures(onTap={if(hit(it))click?.invoke()},onLongPress={if(hit(it))press?.invoke()})
        }.semantics {contentDescription=line.text;onClick{click?.invoke();true};onLongClick{press?.invoke();true}}
            .onKeyEvent {if(it.key!=Key.Enter&&it.key!=Key.Spacebar)false else {if(it.type==KeyEventType.KeyUp)click?.invoke();true}}.focusable()
        val lineLayer=rememberClassicLayer()
        val rowLayers=prepared.rows.map{rememberClassicLayer()}
        Canvas(Modifier.fillMaxSize().then(input)) {
            val time=(currentPlayback().positionMillis-options.offsetMillis)*1000
            drawn.time=time;drawn.size=size
            val exit=if(leaveUs!=null && time>=leaveUs)easeInOut(motionElapsedUs(time-leaveUs)/450_000.0) else 0f
            val enter=if(options.reducedMotion)1f else easeOut(motionElapsedUs(time-enterUs)/300_000.0)
            val pad=ceil(128*density.density).toInt()
            lineLayer.alpha=enter*(1-exit)
            lineLayer.record(this,IntSize(size.width.toInt()+2*pad,size.height.toInt()+2*pad)) {
                withTransform({translate(pad.toFloat(),pad.toFloat())}) {
                    prepared.rows.forEachIndexed { ri,row ->
                        val frame=sampleTiltRow(time,row.visibleUs,leaveUs,row.segment.isTilt,options.reducedMotion)
                        if(frame.alpha>0f) {
                            val layer=rowLayers[ri]
                            val rp=pad
                            layer.alpha=frame.alpha
                            layer.record(this,IntSize(ceil(row.box.box.width).toInt()+2*rp,ceil(row.box.box.height).toInt()+2*rp)) {
                                withTransform({translate(rp.toFloat(),rp.toFloat())}) {
                                    row.glyphs.forEach { glyph ->
                                        val timing=row.timings.getOrNull(glyph.visualIndex)
                                        val char=sampleTiltChar(time,row.visibleUs,row.segment.isTilt,glyph.visualIndex,
                                            glyph.space,timing,row.yOffset,options.reducedMotion)
                                        withTransform({translate(0f,char.y);scale(char.scale,char.scale,glyph.rect.center)}) {
                                            drawImage(glyph.raster.image,glyph.raster.origin,alpha=char.alpha)
                                        }
                                    }
                                }
                            }
                            withTransform({translate((size.width-2*pad-row.box.box.width)/2,
                                (size.height-2*pad-prepared.height)/2+row.box.box.top+frame.y*density.density)
                                scale(frame.scale,frame.scale,Offset(row.box.box.width/2,row.box.box.height/2));translate(-rp.toFloat(),-rp.toFloat())}) {drawClassicLayer(layer)}
                        }
                    }
                }
            }
            withTransform({translate(-pad.toFloat(),-pad.toFloat())}){drawClassicLayer(lineLayer)}
        }
    }
}
