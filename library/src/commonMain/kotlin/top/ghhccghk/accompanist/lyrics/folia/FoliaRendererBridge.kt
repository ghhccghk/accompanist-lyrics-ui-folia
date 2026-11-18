package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import kotlin.math.cos
import kotlin.math.sin

/** Versioned, optional renderer boundary. The base library never loads native code. */
@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
annotation class ExperimentalFoliaRendererBridge

@ExperimentalFoliaRendererBridge
const val FOLIA_RENDERER_SCENE_VERSION: Int = 1

/** Fully shaped CPU coverage uploaded only when a preparation changes. Shadow
 * sigmas request GPU preparation, never a CPU mask/colour processor invocation. */
@ExperimentalFoliaRendererBridge
data class FoliaRendererTexture(val image: ImageBitmap, val shadowSigmas: List<Float> = emptyList(), val shadowInkAlpha: Float = 1f)

@ExperimentalFoliaRendererBridge
class FoliaRendererScene(val source: ISyncedLine, val textures: List<FoliaRendererTexture>)

/** Column-vector affine transform, in physical pixels; then means matrix multiplication. */
@ExperimentalFoliaRendererBridge
data class FoliaRendererTransform(val a:Float=1f,val b:Float=0f,val c:Float=0f,val d:Float=1f,val x:Float=0f,val y:Float=0f) {
    fun then(o:FoliaRendererTransform)=FoliaRendererTransform(a*o.a+c*o.b,b*o.a+d*o.b,a*o.c+c*o.d,b*o.c+d*o.d,a*o.x+c*o.y+x,b*o.x+d*o.y+y)
    companion object {
        fun translate(x:Float,y:Float)=FoliaRendererTransform(x=x,y=y)
        fun scale(x:Float,y:Float=x)=FoliaRendererTransform(a=x,d=y)
        fun rotate(degrees:Float):FoliaRendererTransform {
            val r=degrees.toDouble()*kotlin.math.PI/180
            return FoliaRendererTransform(cos(r).toFloat(),sin(r).toFloat(),-sin(r).toFloat(),cos(r).toFloat())
        }
    }
}

@ExperimentalFoliaRendererBridge
sealed interface FoliaRendererCommand

@ExperimentalFoliaRendererBridge
data class FoliaRendererImage(val texture:Int,val color:Color,val opacity:Float=1f,
    val transform:FoliaRendererTransform=FoliaRendererTransform()):FoliaRendererCommand

@ExperimentalFoliaRendererBridge
data class FoliaRendererGroup(val width:Int,val height:Int,val commands:List<FoliaRendererCommand>,
    val opacity:Float=1f,val blurSigma:Float=0f,val transform:FoliaRendererTransform=FoliaRendererTransform()):FoliaRendererCommand

@ExperimentalFoliaRendererBridge
data class FoliaRendererFrame(val mediaTimeUs:Long,val width:Int,val height:Int,val root:FoliaRendererGroup)

/** Called on the UI thread. Implementations must freeze frame packets before
 * worker dispatch; neither playback nor Compose state may be read by workers.
 * Presentation acknowledgements are delivered on UI and drive hit geometry. */
@ExperimentalFoliaRendererBridge
interface FoliaPartitaRenderTarget {
    val overlaysVisible:Boolean get()=true
    @Composable
    fun Line(scene:FoliaRendererScene, modifier:Modifier,
        frame:(width:Int,height:Int)->FoliaRendererFrame,
        onPresented:(FoliaRendererFrame)->Unit)
}

@ExperimentalFoliaRendererBridge
val LocalFoliaPartitaRenderTarget = staticCompositionLocalOf<FoliaPartitaRenderTarget?> { null }
