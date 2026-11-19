@file:OptIn(top.ghhccghk.accompanist.lyrics.folia.ExperimentalFoliaRendererBridge::class)
package top.ghhccghk.accompanist.lyrics.folia.nativeandroid

import top.ghhccghk.accompanist.lyrics.folia.*
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

internal data class TextureUpload(val id:Long,val width:Int,val height:Int,val rgba:ByteArray,
    val sigma1:Float,val sigma2:Float,val inkAlpha:Float)
internal class SceneUpload(val id:Long,scene:FoliaRendererScene,budget:Long,preserveColor:Boolean = false) {
    val byteCount=scene.textures.sumOf { it.image.width.toLong()*it.image.height*4 }
    init { require(byteCount<=budget) { "PreparedTextureBudgetExceeded:required=$byteCount available=$budget" } }
    val textures:List<TextureUpload> = scene.textures.mapIndexed{index,texture->
        val image=texture.image
        require(image.width in 1..8192&&image.height in 1..8192&&index<65535)
        val count=Math.multiplyExact(image.width,image.height)
        require(count<=8*1024*1024)
        val argb=IntArray(count);image.readPixels(argb)
        val rgba=ByteArray(Math.multiplyExact(count,4))
        for (i in argb.indices) {
            val pixel = argb[i]
            val alpha = (pixel ushr 24).toByte()
            val offset = i * 4
            if (preserveColor) {
                val rgb = if (alpha == 0.toByte()) 0 else pixel
                rgba[offset] = (rgb ushr 16).toByte()
                rgba[offset + 1] = (rgb ushr 8).toByte()
                rgba[offset + 2] = rgb.toByte()
            } else {
                rgba[offset] = alpha
                rgba[offset + 1] = alpha
                rgba[offset + 2] = alpha
            }
            rgba[offset + 3] = alpha
        }
        require(texture.shadowSigmas.size<=2)
        TextureUpload((id shl 16)+(index+1),image.width,image.height,rgba,
            texture.shadowSigmas.getOrElse(0){0f},texture.shadowSigmas.getOrElse(1){0f},texture.shadowInkAlpha)
    }
}
internal data class FrameEntry(val scene:SceneUpload,val frame:FoliaRendererFrame,val acknowledge:()->Unit)
internal data class NativeScenePacket(val id:Long,val width:Int,val height:Int,val scenes:List<SceneUpload>,
    val frames:List<FrameEntry>,val bytes:ByteArray)

internal object ScenePacketEncoder {
    fun encode(id:Long,width:Int,height:Int,scenes:List<SceneUpload>,frames:List<FrameEntry>):NativeScenePacket {
        val out=ByteArrayOutputStream(8192)
        fun int(v:Int){out.write(v);out.write(v ushr 8);out.write(v ushr 16);out.write(v ushr 24)}
        fun long(v:Long){int(v.toInt());int((v ushr 32).toInt())}
        fun float(v:Float){require(v.isFinite());int(v.toBits())}
        fun matrix(t:FoliaRendererTransform){float(t.a);float(t.b);float(t.c);float(t.d);float(t.x);float(t.y)}
        var nodes=0
        var hasPlanes = false
        fun command(c:FoliaRendererCommand,scene:Long,depth:Int) {
            require(depth<=16&&++nodes<=4096)
            when(c){
                is FoliaRendererGroup->{
                    // v2 adds a direct group opcode. v1 groups keep isolated semantics.
                    int(if (c.allowDirectDraw && c.opacity == 1f && c.blurSigma <= .01f) 4 else 1);int(c.width);int(c.height);float(c.opacity);float(c.blurSigma);matrix(c.transform)
                    c.commands.forEach{command(it,scene,depth+1)};int(3)
                }
                is FoliaRendererPlane -> {
                    hasPlanes = true
                    require(c.vertices.size in 3..6 && c.texture in 0 until 65535)
                    int(5)
                    long((scene shl 16) + c.texture + 1)
                    val fog = c.fog
                    val material = c.mipMaterial
                    int((if (c.additive) 1 else 0) or (if (fog != null) 2 else 0) or (if (material != null) 4 else 0))
                    val color = c.color.convert(ColorSpaces.Srgb)
                    float(material?.first ?: color.red); float(material?.second ?: color.green)
                    float(material?.third ?: color.blue); float(color.alpha * c.opacity)
                    if (fog != null) {
                        val ink = fog.color.convert(ColorSpaces.Srgb)
                        float(ink.red); float(ink.green); float(ink.blue); float(fog.near); float(fog.far)
                    }
                    int(c.vertices.size)
                    for (vertex in c.vertices) {
                        float(vertex.x); float(vertex.y); float(vertex.w); float(vertex.u); float(vertex.v)
                    }
                }
                is FoliaRendererImage->{
                    int(2);long((scene shl 16)+(c.texture+1))
                    val color=c.color.convert(ColorSpaces.Srgb)
                    float(color.red);float(color.green);float(color.blue);float(color.alpha*c.opacity);matrix(c.transform)
                }
            }
            require(out.size()<=512*1024)
        }
        int(0x31475246);int(2);long(id);int(width);int(height)
        frames.forEach{command(it.frame.root,it.scene.id,0)};int(3)
        val bytes = out.toByteArray()
        if (hasPlanes) bytes[4] = FOLIA_RENDERER_SCENE_VERSION.toByte()
        return NativeScenePacket(id,width,height,scenes,frames,bytes)
    }
}
internal fun ByteArray.directBuffer():ByteBuffer=ByteBuffer.allocateDirect(size).also{it.put(this);it.flip()}
