// SPDX-License-Identifier: AGPL-3.0-only
@file:OptIn(top.ghhccghk.accompanist.lyrics.folia.ExperimentalFoliaRendererBridge::class)
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.partita

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.*
import top.ghhccghk.accompanist.lyrics.folia.*
import top.ghhccghk.accompanist.lyrics.folia.FoliaRendererTransform.Companion.translate
import top.ghhccghk.accompanist.lyrics.folia.FoliaRendererTransform.Companion.rotate
import top.ghhccghk.accompanist.lyrics.folia.FoliaRendererTransform.Companion.scale
import top.ghhccghk.accompanist.lyrics.folia.internal.animation.*
import top.ghhccghk.accompanist.lyrics.folia.internal.model.NormalizedLine
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.classic.classicBodyColor
import kotlin.math.*

/** One adapter of the existing prepared cells and pure samplers. No second
 * layout, shaper, glyph clock, player or software convolution is introduced. */
internal class PartitaNativePlan(line:NormalizedLine, private val prepared:PartitaPrepared,
    private val layout:PartitaLayout,private val colors:FoliaColors,private val options:FoliaOptions,
    config:PartitaConfig,private val density:Density,private val enterUs:Long,private val leaveUs:Long?) {
    private val textures=mutableListOf<FoliaRendererTexture>()
    private fun texture(image:ImageBitmap,sigmas:List<Float> = emptyList(),alpha:Float=1f):Int {
        textures+=FoliaRendererTexture(image,sigmas,alpha);return textures.lastIndex
    }
    private val bodies=prepared.rows.map{row->row.map{cell->
        val raster=cell.body.rasterFor(Color.White)
        texture(raster.image) to raster.origin
    }}
    private val halos=prepared.rows.map{row->row.map{cell->cell.glyphs.map{texture(it.image,it.gpuSigmas,colors.active.alpha)}}}
    private val profile=renderProfile(line.startUs,line.endUs,line.fragments.lastOrNull()?.endUs?:line.endUs)
    private val d=density.density
    private data class Guide(val body:Int,val shadow:Int,val size:Size,val padding:Int)
    private fun guide(size:Size):Guide {
        val p=ceil(16*d).toInt()
        val image=ImageBitmap(ceil(size.width).toInt()+2*p,ceil(size.height).toInt()+2*p)
        CanvasDrawScope().draw(density,LayoutDirection.Ltr,Canvas(image),Size(image.width.toFloat(),image.height.toFloat())) {
            drawRect(Color.White,Offset(p.toFloat(),p.toFloat()),size)
        }
        return Guide(texture(image),texture(image,listOf(5*d)),size,p)
    }
    private val guides=if(!config.showGuideLines) emptyList() else prepared.geometry.rows.map{row->
        guide(Size(d,32*d)) to guide(Size(((row.box.width+36)*d).toFloat(),d))
    }
    val scene=FoliaRendererScene(line.source,textures.toList())

    fun frame(time:Long,width:Int,height:Int):FoliaRendererFrame {
        val motion=sampleLine(time,enterUs,leaveUs,profile,options.reducedMotion,1f,d,options.animationIntensity)
        val geometry=prepared.geometry
        val contentWidth=geometry.rows.maxOfOrNull{it.box.left*2+it.box.width}?:0f
        val origin=Offset(((width-contentWidth*d)/2).toFloat(),((height-geometry.height*d)/2).toFloat())
        val p=ceil(180*d).toInt()
        val lineTransform=translate(p.toFloat(),p+motion.breathingY)
            .then(translate(width/2f,height/2f)).then(scale(motion.scale)).then(translate(-width/2f,-height/2f))
        val rows=prepared.rows.mapIndexed{ri,cells->
            val geo=geometry.rows[ri];val row=layout.rows[ri]
            val chunk=samplePartitaChunk(time,enterUs,row,profile,options.reducedMotion)
            val rp=ceil((cells.maxOfOrNull{hypot(it.layout.size.width.toFloat(),it.layout.size.height.toFloat())*it.pose.scale*.75f}?:0f)+160*d).toInt()
            val rowCommands=mutableListOf<FoliaRendererCommand>()
            if(guides.isNotEmpty()&&chunk.guide>0f) {
                val ink=if(chunk.active)colors.active else Color.White.copy(alpha=if(chunk.waiting).14f else .22f)
                val right=ri%2==1
                fun addGuide(g:Guide,horizontal:Boolean,x:Float,y:Float,shadowAlpha:Float) {
                    val image=textures[g.body].image
                    val children=buildList<FoliaRendererCommand>{
                        if(chunk.active)add(FoliaRendererImage(g.shadow,colors.active.copy(alpha=colors.active.alpha*shadowAlpha)))
                        add(FoliaRendererImage(g.body,ink))
                    }
                    val t=translate(rp+x,rp+y).then(scale(if(horizontal)chunk.guide else 1f,if(horizontal)1f else chunk.guide))
                        .then(translate(-(if(horizontal&&right)g.size.width else 0f)-g.padding,-(if(horizontal)0f else g.size.height)-g.padding))
                    rowCommands+=FoliaRendererGroup(image.width,image.height,children,chunk.guide.coerceIn(0f,1f),transform=t)
                }
                addGuide(guides[ri].first,false,if(right)(geo.box.width*d+7*d).toFloat() else -8*d,((geo.box.height+16)*d).toFloat(),69/255f)
                addGuide(guides[ri].second,true,if(right)((geo.box.width+16)*d).toFloat() else -16*d,((geo.box.height+7)*d).toFloat(),53/255f)
            }
            cells.forEachIndexed{wi,cell->
                val box=geo.words[wi]
                val word=sampleWord(time,enterUs,cell.unit.startUs,cell.unit.endUs,profile,cell.pose,options.reducedMotion,options.animationIntensity)
                val wp=ceil(96*d).toInt()
                val children=mutableListOf<FoliaRendererCommand>()
                cell.glyphs.forEachIndexed{gi,g->
                    val a=sampleGraphemeGlow(time,enterUs,cell.unit.startUs,cell.unit.endUs,g.timing.startUs,g.timing.endUs,cell.unit.graphemes.size,profile,g.timing.startSeconds,g.timing.endSeconds)
                    if(a>.001f)children+=FoliaRendererImage(halos[ri][wi][gi],colors.active.copy(alpha=1f),a,translate(wp+g.origin.x,wp+g.origin.y))
                }
                val ink=classicBodyColor(colors.text,colors.active,word.mix)
                val(body,bodyOrigin)=bodies[ri][wi]
                if(word.blur>.01f) {
                    val bp=ceil(32*d).toInt()
                    children+=FoliaRendererGroup(cell.layout.size.width+2*bp,cell.layout.size.height+2*bp,
                        listOf(FoliaRendererImage(body,ink,transform=translate(bp+bodyOrigin.x,bp+bodyOrigin.y))),
                        blurSigma=word.blur*d,transform=translate((wp-bp).toFloat(),(wp-bp).toFloat()))
                }else children+=FoliaRendererImage(body,ink,transform=translate(wp+bodyOrigin.x,wp+bodyOrigin.y))
                val t=translate((rp+(box.left+box.width/2+word.x)*d).toFloat(),(rp+(box.top+box.height/2+word.y)*d).toFloat())
                    .then(rotate(word.rotation)).then(scale(word.scale))
                    .then(translate((-box.width*d/2-wp).toFloat(),(-box.height*d/2+cell.baseline-wp).toFloat()))
                rowCommands+=FoliaRendererGroup(cell.layout.size.width+2*wp,cell.layout.size.height+2*wp,children,word.alpha.coerceIn(0f,1f),transform=t)
            }
            val t=lineTransform.then(translate((origin.x+(geo.box.left+geo.box.width/2+chunk.x)*d).toFloat(),(origin.y+(geo.box.top+geo.box.height/2+chunk.y)*d).toFloat()))
                .then(rotate(chunk.rotation)).then(scale(chunk.scale)).then(translate((-geo.box.width*d/2-rp).toFloat(),(-geo.box.height*d/2-rp).toFloat()))
            FoliaRendererGroup(ceil(geo.box.width*d).toInt()+2*rp,ceil(geo.box.height*d).toInt()+2*rp,rowCommands,chunk.alpha.coerceIn(0f,1f),transform=t)
        }
        return FoliaRendererFrame(time,width,height,FoliaRendererGroup(width+2*p,height+2*p,rows,motion.alpha,motion.blur,translate(-p.toFloat(),-p.toFloat())))
    }
}
