package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.*
import top.ghhccghk.accompanist.lyrics.folia.internal.model.normalize
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama.*
import top.ghhccghk.accompanist.lyrics.folia.internal.text.*
import java.io.File
import java.util.Base64
import javax.imageio.ImageIO
import kotlin.math.*
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class DioramaShapedMaskTest {
    @Test fun completeParagraphMaskOwnershipProbe()=runDesktopComposeUiTest(width=800,height=600) {
        var resolver:FontFamily.Resolver?=null
        setContent {val r=LocalFontFamilyResolver.current;SideEffect{resolver=r}}
        waitForIdle()
        val out=File("../build/verification/diorama-shaped-mask").apply{mkdirs()}
        val controls=File("../verification/diorama-fixtures/shaped-mask-v1/masks.tsv").readLines()
            .filterNot{it.startsWith('#')||it.isBlank()}.map{it.split('\t')}.associateBy{it[0]}
        val sourceUnits=File("../verification/diorama-fixtures/shaped-mask-v1/units.tsv").readLines()
            .filterNot{it.startsWith('#')||it.isBlank()}.map{it.split('\t')}.groupBy{it[0]}
        val reports=mutableListOf("# case oldDifferent oldLostAlpha oldExtraAlpha spanDifferent spanLostAlpha spanExtraAlpha productDifferent productLostAlpha productExtraAlpha")
        val cases=listOf(
            Triple("italic","fij Wavy",listOf("f","i","j"," ","Wavy")),
            Triple("arabic","مرحبا بالعالم",listOf("مر","حبا"," ","بال","عالم")),
            Triple("mixed","Aمرحبا 12B بالعالم",listOf("Aمر","حبا"," ","12B"," ","بال","عالم")),
            Triple("cjk","星光回响",listOf("星光回响")))
        runOnIdle {
            for((name,text,pieces)in cases) {
                assertEquals(text,pieces.joinToString(""))
                val font=when(name){"cjk"->"FoliaParityChatCJK-Bold.otf";"italic"->"FoliaParityLatin-Bold.ttf";else->"FoliaParityBidi-Bold.ttf"}
                val style=TextStyle(fontFamily=FontFamily(Font(File("../verification/assets/$font"),FontWeight.Bold)),fontSize=128.sp,
                    fontStyle=if(name=="italic")FontStyle.Italic else FontStyle.Normal,textAlign=TextAlign.Left,
                    textDirection=TextDirection.ContentOrLtr,color=Color.White,shadow=Shadow.None)
                val measurer=TextMeasurer(resolver!!,Density(1f),LayoutDirection.Ltr)
                val source=KaraokeLine.MainKaraokeLine(pieces.mapIndexed{i,s->KaraokeSyllable(s,700+i*100,800+i*100)},
                    null,KaraokeAlignment.Unspecified,700,2000)
                val core=dioramaCoreLines(normalize(SyncedLyrics(listOf(source)))).single()
                assertSame(source,core.source)
                val line=core.lyric
                val layout=measurer.measure(AnnotatedString(text),style,softWrap=false)
                val advance=layout.getPathForRange(0,text.length).getBounds().width
                val metrics=naturalFontMetrics(style,resolver!!,Density(1f))
                val ratio=dioramaTypographicRatio(style,resolver!!)!!
                val middle=floor(128*ratio*64+.5)/64-64
                val origin=Offset(90f,(180+middle-paintedNaturalBaseline(layout,metrics)).toFloat())
                fun bitmap(paint:DrawScope.()->Unit):ImageBitmap {
                    val image=ImageBitmap(ceil(advance).toInt()+180,360)
                    CanvasDrawScope().draw(Density(1f),LayoutDirection.Ltr,Canvas(image),Size(image.width.toFloat(),360f),paint)
                    return image
                }
                val full=bitmap{drawText(layout,topLeft=origin)}
                val control=controls.getValue(name)
                assertEquals(control[1].toInt(),full.width,"original complete-run advance")
                assertEquals(control[2].toInt(),full.height,"original active band")
                val units=dioramaUnits(line)
                assertEquals(sourceUnits.getValue(name).size,units.size)
                for((unit,record)in units.zip(sourceUnits.getValue(name))) {
                    assertEquals(String(Base64.getDecoder().decode(record[6]),Charsets.UTF_8),unit.text)
                    assertEquals(record[2].toInt(),unit.charStart);assertEquals(record[3].toInt(),unit.charEnd)
                    assertEquals(record[4].toDouble(),unit.start,1e-12);assertEquals(record[5].toDouble(),unit.end,1e-12)
                }
                val old=bitmap{for(unit in units) {
                    val range=layout.getPathForRange(unit.charStart,unit.charEnd).getBounds()
                    clipRect(origin.x+range.left,0f,origin.x+range.right,360f){drawText(layout,topLeft=origin)}
                }}
                val span=bitmap{for(unit in units) {
                    val annotated=AnnotatedString(text,listOf(AnnotatedString.Range(SpanStyle(color=Color.White),unit.charStart,unit.charEnd)))
                    val masked=measurer.measure(annotated,style.copy(color=Color.Transparent),softWrap=false)
                    assertEquals(layout.size,masked.size,"colour-only span preserves full layout")
                    drawText(masked,topLeft=origin)
                }}
                val prepared=DioramaActiveText.prepare(line,style,measurer,resolver!!)
                val columns=dioramaShapedColumns(layout,units,full.width,90)
                val owners=IntArray(full.width)
                columns.flatten().forEach { range -> range.forEach { owners[it]++ } }
                assertTrue(owners.all { it==1 },"one owner for every canonical texel column")
                for(slice in prepared.slices) {
                    val bounds=layout.getPathForRange(slice.unit.charStart,slice.unit.charEnd).getBounds()
                    val oldPivot=bounds.left+max(1,ceil(bounds.width.toDouble()).toInt())/2.0-advance/2
                    assertEquals(oldPivot,slice.centerPx,1e-9,"source unit pivot stays fixed")
                    val left=prepared.lineAdvance/2+slice.centerPx+slice.imageOffsetPx-slice.image.width/2.0
                    assertEquals(round(left),left,1e-9,"integer texture origin preserves complete-run raster phase")
                    for(scale in listOf(.8,1.0,1.3)) {
                        // A texture-local pivot must land on the same original
                        // line pivot under the actual soul scale transform.
                        val pixelPivot=slice.image.width/2.0-slice.imageOffsetPx
                        val transformed=slice.centerPx+slice.imageOffsetPx*scale-
                            slice.image.width/2.0*scale+pixelPivot*scale
                        assertEquals(oldPivot,transformed,1e-9,"soul scale keeps original pivot")
                    }
                }
                val production=bitmap{for(slice in prepared.slices) {
                    val x=90+prepared.lineAdvance/2+slice.centerPx+slice.imageOffsetPx-slice.image.width/2.0
                    drawImage(slice.image,Offset(x.toFloat(),0f))
                }}
                assertEquals(units,prepared.slices.map{it.unit},"source units and ALL internal clocks stay intact")
                fun alpha(im:ImageBitmap)=IntArray(im.width*im.height).also{im.readPixels(it)}.map{it ushr 24}
                val f=alpha(full)
                fun stats(im:ImageBitmap):List<Long> {val a=alpha(im);return listOf(f.indices.count{f[it]!=a[it]}.toLong(),
                    f.indices.sumOf{max(0,f[it]-a[it]).toLong()},f.indices.sumOf{max(0,a[it]-f[it]).toLong()})}
                val result=stats(production)
                assertEquals(listOf(0L,0L,0L),result,"exact complete-run coverage conservation $name")
                val bodyLayout=if(name=="cjk")measurer.measure(AnnotatedString(text),classicBodyRasterStyle(style),softWrap=false)else layout
                val fullBody=bitmap{drawText(bodyLayout,topLeft=origin)}
                val body=bitmap{for(slice in prepared.slices) {
                    val x=90+prepared.lineAdvance/2+slice.centerPx+slice.imageOffsetPx-slice.body.width/2.0
                    drawImage(slice.body,Offset(x.toFloat(),0f))
                }}
                assertEquals(alpha(fullBody),alpha(body),"body raster also conserves its complete shaped run $name")
                ImageIO.write(fullBody.toAwtImage(),"png",out.resolve("$name-body-full.png"))
                ImageIO.write(body.toAwtImage(),"png",out.resolve("$name-body-production.png"))
                reports+=listOf(name,stats(old).joinToString("\t"),stats(span).joinToString("\t"),result.joinToString("\t")).joinToString("\t")
                for((label,im)in listOf("full" to full,"old" to old,"span" to span,"production" to production))
                    ImageIO.write(im.toAwtImage(),"png",out.resolve("$name-$label.png"))
            }
        }
        out.resolve("ownership.tsv").writeText(reports.joinToString("\n")+"\n")
    }
}
