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
const val FOLIA_RENDERER_SCENE_VERSION: Int = 4

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

/** An isolated group clips to its bounds and composites its children together.
 * With [allowDirectDraw], a fully opaque, unfiltered group may instead replay its
 * children under its transform with no group clip or intermediate raster. */
@ExperimentalFoliaRendererBridge
data class FoliaRendererGroup(val width:Int,val height:Int,val commands:List<FoliaRendererCommand>,
    val opacity:Float=1f,val blurSigma:Float=0f,val transform:FoliaRendererTransform=FoliaRendererTransform(),
    val allowDirectDraw:Boolean=false):FoliaRendererCommand

/** A vertex in homogeneous viewport coordinates with a perpendicular camera depth of [w]. */
@ExperimentalFoliaRendererBridge
data class FoliaRendererPlaneVertex(val x: Float, val y: Float, val w: Float, val u: Float, val v: Float)

@ExperimentalFoliaRendererBridge
data class FoliaRendererPlaneFog(val color: Color, val near: Float, val far: Float)

/** A convex, already clipped text mask. These commands belong directly in an
 * identity viewport root and are replayed in painter's order. */
@ExperimentalFoliaRendererBridge
data class FoliaRendererPlane(
    val texture: Int,
    val vertices: List<FoliaRendererPlaneVertex>,
    val color: Color,
    val opacity: Float,
    val additive: Boolean,
    val fog: FoliaRendererPlaneFog?,
    /** Linear material RGB before ACES. Null requests level-zero mask tinting. */
    val mipMaterial: Triple<Float, Float, Float>? = null,
) : FoliaRendererCommand

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

/** A frame can be deferred while the clock is between two raster windows. */
@ExperimentalFoliaRendererBridge
interface FoliaDioramaRenderTarget {
    val overlaysVisible: Boolean

    @Composable
    fun Stage(scene: FoliaRendererScene, modifier: Modifier,
        frame: (width: Int, height: Int) -> FoliaRendererFrame?,
        onPresented: (FoliaRendererFrame) -> Unit)
}

@ExperimentalFoliaRendererBridge
val LocalFoliaDioramaRenderTarget = staticCompositionLocalOf<FoliaDioramaRenderTarget?> { null }
