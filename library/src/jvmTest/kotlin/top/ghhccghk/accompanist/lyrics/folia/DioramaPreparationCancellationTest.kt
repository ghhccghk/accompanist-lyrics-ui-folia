package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.runtime.SideEffect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.*
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import kotlinx.coroutines.CancellationException
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama.*
import java.io.File
import kotlin.math.ceil
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class DioramaPreparationCancellationTest {
    @Test fun cancellationPropagatesInsideOwnershipRasterHaloAndCorePreparation() =
        runDesktopComposeUiTest(width=800,height=600) {
            var resolver:FontFamily.Resolver?=null
            setContent {val r=LocalFontFamilyResolver.current;SideEffect{resolver=r}}
            waitForIdle()
            runOnIdle {
                val fontResolver=assertNotNull(resolver)
                val style=TextStyle(fontFamily=FontFamily(Font(File("../verification/assets/FoliaParityLatin-Bold.ttf"),FontWeight.Bold)))
                val measurer=TextMeasurer(fontResolver,Density(1f),LayoutDirection.Ltr)
                val line=DioramaLyric("Synthetic mirror plane",.7,2.3)
                val cancel=CancellationException("cancel previous Diorama input")
                fun interrupted(at:Int, action:(()->Unit)->Unit) {
                    var calls=0
                    val thrown=assertFailsWith<CancellationException> {
                        action { calls++;if(calls==at)throw cancel }
                    }
                    assertSame(cancel,thrown,"do not swallow, wrap, or replace cancellation")
                    assertEquals(at,calls,"no further work after the cancellation callback")
                }
                val layout=measurer.measure(line.text,style.copy(fontSize=128.sp),softWrap=false)
                val units=dioramaUnits(line)
                val width=ceil(layout.getPathForRange(0,line.text.length).getBounds().width).toInt()+180
                // One call per unit plus periodic checks within the column
                // scan: cancellation need not wait until a long line ends.
                for(at in listOf(1,units.size+1,units.size+3)) interrupted(at) {
                    dioramaShapedColumns(layout,units,width,90,it)
                }
                var count=0
                val baseline=DioramaActiveText.prepare(line,style,measurer,fontResolver){count++}
                assertTrue(count>units.size*10,"checks must reach per-unit halo passes")
                for(at in listOf(1,2,3,count/2,count-1,count)) interrupted(at) {
                    DioramaActiveText.prepare(line,style,measurer,fontResolver,it)
                }
                for(at in listOf(1,2,3,5,10)) interrupted(at) {
                    prepareDioramaHalo(baseline.slices.first().image,it)
                }
                val source=SyncedLine(line.text,null,700,2300)
                interrupted(6) { checkpoint ->
                    DioramaTextPlayback.prepare(SyncedLyrics(listOf(source)),style,measurer,fontResolver,
                        800.0,600.0,seedName="DioramaProbe",endTime=3.0,checkpoint=checkpoint)
                }
                var work=0
                val recovered=DioramaTextPlayback.prepare(SyncedLyrics(listOf(source)),style,measurer,fontResolver,
                    800.0,600.0,seedName="DioramaProbe",endTime=3.0,checkpoint={work++})
                assertSame(source,recovered.sourceAt(1.0).source,"a cancelled candidate never replaces source identity")
                val preparedWork=work
                val image=ImageBitmap(800,600)
                for(time in listOf(1.0,2.0,.8,1.0)) {
                    CanvasDrawScope().draw(Density(1f),LayoutDirection.Ltr,Canvas(image),Size(800f,600f)) {
                        recovered.draw(this,time,DioramaLinearColor(.74,.78,.84),DioramaLinearColor(.88,.2,.08))
                    }
                }
                assertEquals(preparedWork,work,"display/seek must never retain or invoke preparation checkpoints")
            }
        }
}
