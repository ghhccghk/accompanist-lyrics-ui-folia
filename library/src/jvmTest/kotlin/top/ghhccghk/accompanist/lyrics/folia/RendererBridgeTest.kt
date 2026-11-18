package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.semantics.SemanticsActions
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.*
import kotlin.test.*

@OptIn(ExperimentalFoliaRendererBridge::class,ExperimentalTestApi::class)
class RendererBridgeTest {
    @Test fun sceneKeepsCoreIdentityAndReconstructsFramesWithoutCpuShadows()=runDesktopComposeUiTest(width=800,height=600){
        val line=KaraokeLine.MainKaraokeLine(listOf(KaraokeSyllable("Amber ",700,1300),KaraokeSyllable("echo",1300,1900)),null,KaraokeAlignment.Unspecified,700,1900)
        val lyrics=SyncedLyrics(listOf(line))
        var playback by mutableStateOf(FoliaPlaybackSnapshot(1500,false))
        var selected:ISyncedLine?=null
        val scenes=mutableListOf<FoliaRendererScene>()
        var current:FoliaRendererFrame?=null
        val target=object:FoliaPartitaRenderTarget{
            @Composable override fun Line(scene:FoliaRendererScene,modifier:Modifier,frame:(Int,Int)->FoliaRendererFrame,onPresented:(FoliaRendererFrame)->Unit){
                if(scenes.lastOrNull()!==scene)scenes+=scene
                Canvas(modifier){val f=frame(size.width.toInt(),size.height.toInt());current=f;onPresented(f)}
            }
        }
        val forbidden=object:ExperimentalFoliaMaskProcessor{
            override fun blur(argb:IntArray,width:Int,height:Int,sigmas:FloatArray):List<IntArray>?=error("Native preparation called CPU mask processor")
        }
        setContent{CompositionLocalProvider(LocalFoliaPartitaRenderTarget provides target,LocalFoliaMaskProcessor provides forbidden){
            FoliaLyricsView(lyrics,{playback},Modifier.fillMaxSize(),style=FoliaStyle.Partita(),
                options=FoliaOptions(showTranslation=false,showPhonetic=false,showNextLine=false),onLineClicked={selected=it})
        }}
        waitForIdle();captureToImage()
        val scene=scenes.single();val first=assertNotNull(current)
        assertSame(line,scene.source)
        assertTrue(scene.textures.any{it.shadowSigmas.size==2},"Two actual GPU shadow radii retained")
        assertTrue(scene.textures.all{it.image.width>0&&it.image.height>0})
        runOnIdle{playback=FoliaPlaybackSnapshot(850,false,1)};waitForIdle();captureToImage()
        assertSame(scene,scenes.last(),"Media updates do not re-shape/upload a scene")
        assertNotEquals(first.root,assertNotNull(current).root)
        runOnIdle{playback=FoliaPlaybackSnapshot(1500,false,2)};waitForIdle();captureToImage()
        assertEquals(first.root,assertNotNull(current).root,"Seek reconstructs from host time")
        onNodeWithContentDescription("Amber echo").performSemanticsAction(SemanticsActions.OnClick)
        runOnIdle{assertSame(line,selected)}
    }
    @Test fun affineCompositionKeepsRotationAndTranslationOrder(){
        val t=FoliaRendererTransform.translate(10f,20f).then(FoliaRendererTransform.rotate(90f)).then(FoliaRendererTransform.scale(2f))
        assertEquals(10f,t.a*0+t.c*0+t.x)
        assertEquals(20f,t.b*0+t.d*0+t.y)
        assertEquals(10f,t.a*3+t.c*0+t.x,.0001f)
        assertEquals(26f,t.b*3+t.d*0+t.y,.0001f)
    }
}
