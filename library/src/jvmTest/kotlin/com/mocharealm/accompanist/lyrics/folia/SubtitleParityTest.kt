package com.mocharealm.accompanist.lyrics.folia

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import com.mocharealm.accompanist.lyrics.folia.internal.model.normalize
import com.mocharealm.accompanist.lyrics.folia.internal.timeline.*
import com.mocharealm.accompanist.lyrics.folia.internal.styles.classic.ClassicSubtitles
import com.mocharealm.accompanist.lyrics.folia.internal.styles.classic.SubtitlePaint
import java.io.File
import javax.imageio.ImageIO
import com.mocharealm.accompanist.lyrics.folia.internal.rendering.LocalClassicSoftwareLayers
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class SubtitleParityTest {
    @Test fun wide()=capture(800)
    @Test fun narrow()=capture(380)
    @Test fun softwareNarrow()=capture(380,true)
    @Test fun backdropWide()=capture(800,backdrop=true)
    @Test fun backdropNarrow()=capture(380,backdrop=true)
    @Test fun backdropSoftwareNarrow()=capture(380,true,true)
    @Test fun backdropPadWide()=capture(800,backdrop=true,padOnly=true)
    @Test fun backdropPadNarrow()=capture(380,backdrop=true,padOnly=true)
    @Test fun shadowWide()=capture(800,shadowOnly=true)
    @Test fun shadowNarrow()=capture(380,shadowOnly=true)
    private fun capture(width:Int,software:Boolean=false,backdrop:Boolean=false,padOnly:Boolean=false,shadowOnly:Boolean=false)=runDesktopComposeUiTest(width=width,height=600) {
        val lines=File("../verification/classic-fixtures/subtitle-scenes.tsv").readLines().filterNot{it.startsWith("#")||it.isBlank()}.map {row->
            val c=row.split('\t');normalize(SyncedLyrics(listOf(SyncedLine(c[2],c[3],c[0].toInt(),c[1].toInt())))).lines.single().copy(phonetic=c[4])
        }
        val index=VisualTimeline(lines);val history=SubtitleHistory(lines)
        var snapshot by mutableStateOf(FoliaPlaybackSnapshot(0,false))
        var layout by mutableStateOf("translation")
        var measuredHeight=0f
        var measuredRows=emptyList<SubtitlePaint>()
        val font=FontFamily(Font(File("../verification/assets/FoliaParityLatin-Bold.ttf"),FontWeight.Bold))
        val typography=FoliaTypography(subtitle=TextStyle(fontFamily=font,fontWeight=FontWeight.Bold,shadow=if(padOnly) Shadow.None else null))
        setContent {
          CompositionLocalProvider(LocalClassicSoftwareLayers provides software) {
            val time=snapshot.positionMillis*1000;val epoch=index.at(time).primary
            val sources=if(layout=="translation") listOf(SubtitleSource.Translation) else listOf(SubtitleSource.Current,SubtitleSource.Translation,SubtitleSource.Phonetic)
            val content=resolveSubtitleContent(lines,epoch?.line,history.recent(time,epoch?.line),sources)
            Box(Modifier.fillMaxSize().background(Color(0xFF162435))) {
                ClassicSubtitles(content,lines,{snapshot},typography,FoliaColors(secondary=if(padOnly||shadowOnly) Color.Transparent else FoliaColors().secondary,
                    subtitleBackground=if(backdrop||shadowOnly) Color(0xFF48707E) else Color.Transparent),FoliaOptions(showSubtitleBackground=!shadowOnly),width.dp,
                    epoch?.enterUs?:content.line?.let{lines[it].startUs},Modifier.align(Alignment.BottomCenter).padding(bottom=112.dp),
                    onLayout={height,rows->measuredHeight=height;measuredRows=rows})
            }
        }
          }
        val tag=if(backdrop) "backdrop-" else ""
        val output=File("../build/verification/subtitle-${tag}${if(padOnly) "pad-" else if(shadowOnly) "shadow-" else ""}${if(software) "software-" else ""}kotlin").apply {mkdirs()}
        for(mode in listOf("translation","current+translation+romanization")) {
            runOnIdle {layout=mode}
            for(time in if(padOnly||shadowOnly) listOf(1500) else listOf(0,50,120,240,700,725,750,800,850,1000,1500,2500,3200,3250,3350,3500,4800,5000,5050,5150,5500)) {
                runOnIdle{snapshot=FoliaPlaybackSnapshot(time.toLong(),false,snapshot.discontinuity+1)};waitForIdle()
                val capture=captureToImage().toAwtImage()
                ImageIO.write(capture,"png",output.resolve("$width-$mode-${time}ms.png"))
                if(padOnly) {
                    val reference=ImageIO.read(File("../verification/classic-fixtures/subtitle-backdrop/$width-$mode-pad.png"))
                    var maxError=0;var totalError=0L
                    for(y in 0 until 600) for(x in 0 until width) {
                        val a=capture.getRGB(x,y);val b=reference.getRGB(x,y)
                        for(shift in listOf(0,8,16)) {
                            val delta=kotlin.math.abs(((a ushr shift)and 255)-((b ushr shift)and 255))
                            maxError=maxOf(maxError,delta);totalError+=delta
                        }
                    }
                    // Preserve this measured quantization residual; full-frame visual
                    // acceptance remains separate from this regression guard.
                    assertTrue(maxError<=2,"Backdrop paint regression: max delta $maxError/255")
                    output.resolve("$width-$mode-error.txt").writeText("max=$maxError; whole-canvas RGB MAE=${totalError.toDouble()/(width*600*3)}\n")
                }
                if(time in listOf(1500,2500,3500,5500)) {
                    output.resolve("$width-$mode-${time}-geometry.tsv").writeText("text\tcanvasWidth\tparagraphWidth\tlineLeft\tlineRight\tfirstBaseline\tcssBaseline\tlineTop\tlineBottom\n"+measuredRows.joinToString("\n") {r->
                        listOf(r.text,r.layout.size.width,r.layout.multiParagraph.width,r.layout.getLineLeft(0),r.layout.getLineRight(0),r.layout.firstBaseline,r.baseline,r.layout.getLineTop(0),r.layout.getLineBottom(0)).joinToString("\t")
                    })
                    if(!backdrop && !shadowOnly && width==800 && mode=="current+translation+romanization" && time==1500) {
                        val original=File("../verification/classic-fixtures/subtitle-ink.tsv").readLines()
                            .filterNot{it.startsWith("#")||it.isBlank()}.map{it.split('\t')}
                            .single{it[0]=="800"&&it[1]==mode&&it[2]=="1500"&&it[3]=="0"}
                        val inkY=(425 until 455).filter {y->(0 until width).any {x->
                            val pixel=capture.getRGB(x,y)
                            maxOf(((pixel ushr 16)and 255)-22,((pixel ushr 8)and 255)-36,(pixel and 255)-53)>20
                        }}
                        assertEquals(original[4].toInt(),inkY.first(),"Painted ink top, beyond reported baseline geometry")
                        assertEquals(original[5].toInt(),inkY.last(),"Painted ink bottom")
                    }
                    val reference=File("../verification/classic-fixtures/subtitle-${tag}layout.tsv").readLines().filterNot{it.startsWith("#")||it.isBlank()}.map{it.split('\t')}
                        .filter{it[0].toInt()==width&&it[1]==mode&&it[2].toInt()==time}
                    assertEquals(reference.size,measuredRows.size)
                    reference.forEach {c->
                        val row=measuredRows[c[3].toInt()];val top=600-112-measuredHeight+row.top
                        val baseline=top+row.baseline+row.height-row.height/row.layout.lineCount
                        assertEquals(c[4],row.text)
                        listOf(top,row.height,baseline).forEachIndexed {i,value->assertEquals(c[i+5].toDouble(),value.toDouble(),.02,"subtitle geometry column ${i+5}: $c")}
                    }
                }
            }
        }
    }
}
