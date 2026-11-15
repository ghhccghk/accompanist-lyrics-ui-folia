// Folia v0.7.26 VisualizerSubtitleOverlay.tsx/subtitleFontSizes.ts/subtitleTrackStyle.ts.
// SPDX-License-Identifier: AGPL-3.0-only. Source ledger: docs/upstream.md.
package com.mocharealm.accompanist.lyrics.folia.internal.styles.classic

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.mocharealm.accompanist.lyrics.folia.internal.rendering.drawSubtitleBackdrop
import androidx.compose.ui.graphics.graphicsLayer
import com.mocharealm.accompanist.lyrics.folia.internal.rendering.rememberClassicLayer
import com.mocharealm.accompanist.lyrics.folia.internal.rendering.drawClassicLayer
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import com.mocharealm.accompanist.lyrics.folia.*
import com.mocharealm.accompanist.lyrics.folia.internal.animation.sampleSubtitle
import com.mocharealm.accompanist.lyrics.folia.internal.model.NormalizedLine
import com.mocharealm.accompanist.lyrics.folia.internal.text.cssFontBaseline
import com.mocharealm.accompanist.lyrics.folia.internal.text.naturalFontMetrics
import com.mocharealm.accompanist.lyrics.folia.internal.text.paintedLineBaseline
import androidx.compose.ui.graphics.drawscope.clipRect
import com.mocharealm.accompanist.lyrics.folia.internal.timeline.*
import kotlin.math.floor
import kotlin.math.ceil

internal data class SubtitlePaint(val text:String,val layout:TextLayoutResult,val top:Float,val baseline:Float,
    val height:Float,val alpha:Float,val blur:Boolean,val boxWidth:Float)

private fun DrawScope.drawSubtitleText(row:SubtitlePaint,padding:Float,color:Color=Color.Unspecified) {
    repeat(row.layout.lineCount) { line ->
        val target=row.baseline+line*row.height/row.layout.lineCount
        withTransform({translate(0f,target-paintedLineBaseline(row.layout,line))}) {
            clipRect(-padding,row.layout.getLineTop(line)+padding,
                row.layout.size.width+padding*2,row.layout.getLineBottom(line)+padding) {
                if(color==Color.Unspecified) drawText(row.layout,topLeft=Offset(padding,padding))
                else drawText(row.layout,color=color,topLeft=Offset(padding,padding),shadow=Shadow.None)
            }
        }
    }
}

@Composable
internal fun ClassicSubtitles(content:SubtitleContent,lines:List<NormalizedLine>,playback:()->FoliaPlaybackSnapshot,
    typography:FoliaTypography,colors:FoliaColors,options:FoliaOptions,viewportWidth:Dp,enterUs:Long?,modifier:Modifier=Modifier,
    onLayout:(Float,List<SubtitlePaint>)->Unit={_,_->},additionalContent:@Composable ColumnScope.()->Unit={}) {
    val density=LocalDensity.current
    val fontResolver=LocalFontFamilyResolver.current
    val currentPlayback by rememberUpdatedState(playback)
    fun frame()=sampleSubtitle((currentPlayback().positionMillis-options.offsetMillis)*1000,enterUs,content.rows.isNotEmpty(),options.reducedMotion)
    Column(modifier.fillMaxWidth().padding(horizontal=16.dp).graphicsLayer {
        // Alpha below one on a layout-sized graphicsLayer clips overflowing blur.
        // Apply group alpha inside the explicitly padded drawing layer instead.
        translationY=frame().outerY*density.density
    },horizontalAlignment=Alignment.CenterHorizontally) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val measurer=rememberTextMeasurer()
            val explicitSize=typography.subtitle.fontSize.takeIf {it.isSp}?.value?:18f
            val paints=mutableListOf<SubtitlePaint>()
            val backdrop=options.showSubtitleBackground && colors.subtitleBackground.alpha>0f
            val padX=if(backdrop) 6*density.density else 0f
            val padY=if(backdrop) 2*density.density else 0f
            var total=padY
            fun prepare(value:String,fontSize:Float,weight:FontWeight,upcoming:Boolean,alpha:Float,gap:Float) {
                val style=typography.subtitle.copy(fontSize=(floor(fontSize*100+.00001f)/100).sp,fontWeight=weight,
                    color=colors.secondary,lineHeight=TextUnit.Unspecified,
                    textMotion=typography.subtitle.textMotion?:TextMotion.Animated)
                val text=AnnotatedString(value)
                // Relative CSS line-height uses Blink's nearest 1/64px used font
                // size first (15.84px -> 15.84375px), then floors the product.
                val usedFont=floor(fontSize*density.fontScale*64+.5f)/64
                val lineHeight=floor(usedFont*1.5f*64)/64*density.density
                val natural=naturalFontMetrics(style,fontResolver,density)
                val available=minOf(with(density){maxWidth.toPx()}-padX*2,
                    (if(upcoming) 672f else 896f)*density.density).coerceAtLeast(1f)
                val layout=measurer.measure(text,style.copy(
                    lineHeight=(lineHeight/density.density/density.fontScale).sp,textAlign=TextAlign.Center,
                    lineHeightStyle=LineHeightStyle(LineHeightStyle.Alignment.Center,LineHeightStyle.Trim.None,LineHeightStyle.Mode.Tight)),
                    maxLines=if(upcoming) 1 else Int.MAX_VALUE,overflow=if(upcoming) TextOverflow.Ellipsis else TextOverflow.Clip,
                    softWrap=!upcoming,constraints=Constraints(maxWidth=available.toInt().coerceAtLeast(1)))
                val unwrapped=measurer.measure(text,style,softWrap=false)
                val intrinsic=unwrapped.getPathForRange(0,text.length).getBounds().width
                val boxWidth=minOf(available,ceil(intrinsic/density.density*64)/64*density.density)
                total+=floor(gap*density.fontScale*64)/64*density.density
                val baseline=cssFontBaseline(natural.ascent/density.density,natural.descent/density.density,lineHeight/density.density)*density.density
                paints+=SubtitlePaint(value,layout,total,baseline,lineHeight*layout.lineCount,alpha,upcoming,boxWidth)
                total+=lineHeight*layout.lineCount
            }
            content.rows.forEachIndexed {index,row->
                val secondary=index>0
                val size=(viewportWidth.value*.026f).coerceIn(18f,20f)*typography.scale*explicitSize/18*(if(secondary) .88f else 1f)
                prepare(row.text,size,typography.subtitle.fontWeight?:if(secondary) FontWeight.Normal else FontWeight.Medium,
                    row.source==SubtitleSource.Next,if(secondary) .82f else 1f,if(secondary) size*.25f else 0f)
            }
            content.upcoming.forEachIndexed {index,line->
                val size=(viewportWidth.value*.02f).coerceIn(14f,16f)*typography.scale*explicitSize/18
                prepare(lines[line].text,size,typography.subtitle.fontWeight?:FontWeight.Normal,true,1f,if(index>0) 8f/density.fontScale else 0f)
            }
            total+=padY
            val blockHeight=total
            if(paints.lastOrNull()?.blur==true) {
                // A truncated last row has no propagated inline baseline. The parent
                // 16px/1.5 strut contributes its descent below the inline-block edge.
                val strut=naturalFontMetrics(typography.subtitle.copy(fontSize=16.sp),fontResolver,density)
                val strutHeight=24*density.fontScale
                total+=((strutHeight-cssFontBaseline(strut.ascent/density.density,strut.descent/density.density,strutHeight))*density.density-padY).coerceAtLeast(0f)
            }
            val layers=paints.map {rememberClassicLayer()}
            val shadows=paints.map {rememberClassicLayer()}
            val contentLayer=rememberClassicLayer()
            val backdropLayer=rememberClassicLayer()
            val outerLayer=rememberClassicLayer()
            val observer by rememberUpdatedState(onLayout)
            SideEffect {observer(total,paints)}
            Canvas(Modifier.fillMaxWidth().height(with(density){ceil(total).toDp()}).semantics {
                this[SemanticsProperties.Text]=paints.map {AnnotatedString(it.text)}
            }) {
                // Recorded layers have their own DrawScope.size. Placement always
                // refers to this viewport, never the padded layer dimensions.
                val viewport=size
                val origin=viewport.height-total
                val outerPadding=160*density.density
                outerLayer.alpha=frame().outerAlpha
                outerLayer.record(this,size=IntSize(ceil(viewport.width+outerPadding*2).toInt(),ceil(viewport.height+outerPadding*2).toInt())) {
                withTransform({translate(outerPadding,outerPadding)}) {
                if(backdrop && paints.isNotEmpty()) {
                    val box=Size((paints.maxOf {it.boxWidth}+padX*2)+80*density.density,blockHeight+48*density.density)
                    // Pinned Tailwind 4.3.3 resolves blur-2xl to 40px (not 24px).
                    val blurPadding=120*density.density
                    backdropLayer.blurSigma=40*density.density
                    backdropLayer.record(this,size=IntSize(ceil(box.width+blurPadding*2).toInt(),ceil(box.height+blurPadding*2).toInt())) {
                        drawSubtitleBackdrop(colors.subtitleBackground,box,Offset(blurPadding,blurPadding))
                    }
                    withTransform({translate((viewport.width-box.width)/2-blurPadding,origin-24*density.density-blurPadding)}) {drawClassicLayer(backdropLayer)}
                }
                val f=frame()
                val contentPadding=16*density.density
                contentLayer.alpha=f.innerAlpha
                contentLayer.record(this,size=IntSize(ceil(viewport.width+contentPadding*2).toInt(),ceil(viewport.height+contentPadding*2).toInt())) {
                withTransform({translate(contentPadding,contentPadding)}) {
                paints.forEachIndexed {i,row->
                    val layer=layers[i];val padding=ceil(8*density.density).toInt()
                    layer.alpha=row.alpha
                    layer.blurSigma=if(row.blur) density.density else 0f
                    layer.record(this,size=IntSize(row.layout.size.width+padding*2,ceil(row.height).toInt()+padding*2)) {
                        if(colors.subtitleBackground.alpha>0f && typography.subtitle.shadow==null) {
                            val shadow=shadows[i]
                            // CSS text-shadow radius=2px => Gaussian sigma=1px.
                            shadow.blurSigma=density.density
                            shadow.record(this,size=IntSize(row.layout.size.width+padding*2,ceil(row.height).toInt()+padding*2)) {
                                drawSubtitleText(row,padding.toFloat(),colors.subtitleBackground.copy(alpha=colors.subtitleBackground.alpha*.24f))
                            }
                            withTransform({translate(0f,density.density)}) {drawClassicLayer(shadow)}
                        }
                        drawSubtitleText(row,padding.toFloat())
                    }
                    withTransform({translate((viewport.width-row.layout.size.width)/2-padding,origin+row.top-padding)}) {drawClassicLayer(layer)}
                }
                }
                }
                withTransform({translate(-contentPadding,-contentPadding+f.innerY*density.density)}) {drawClassicLayer(contentLayer)}
                }
                }
                withTransform({translate(-outerPadding,-outerPadding)}) {drawClassicLayer(outerLayer)}
            }
        }
        Column(Modifier.graphicsLayer {alpha=frame().outerAlpha},horizontalAlignment=Alignment.CenterHorizontally) {
            additionalContent()
        }
    }
}
