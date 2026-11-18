// Frozen independent prior implementation from 0d836a5; TEST ONLY.
// Retains original mip coordinates/branches, including >16 child samplers.
// SPDX-License-Identifier: AGPL-3.0-only
// Independent fragment sampling adapter. ACES algebra from the existing
// DioramaToneMapping.kt port of Three0.185.1 (c) 2010-2026 three.js authors, MIT;
// full notice in LICENSES. Sources/controls/limitations: docs/upstream.md.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.*
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.FilterMipmap
import org.jetbrains.skia.FilterMode
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.MipmapMode
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder
import kotlin.math.max

/** Immutable prepared image pyramid + owned shader state; uniforms change at
 * draw time, never glyphs/mips. Full color kept separate from coverage, including
 * color at zero-alpha lower mip texels. No original fixture is a runtime input. */
internal class DioramaSinglePassControl(base:ImageBitmap):DioramaPlainSampler {
    private val width=base.width
    private val height=base.height
    private val masks=mutableListOf(base)
    private val colors=mutableListOf<ImageBitmap>()
    private val children=mutableListOf<org.jetbrains.skia.Shader>()
    private var effect:RuntimeEffect?=null
    private var builder:RuntimeShaderBuilder?=null
    private var closed=false
    init {
        try {
            val pixels=IntArray(width*height).also{base.readPixels(it)}
            val rgba=ByteArray(pixels.size*4)
            for(i in pixels.indices) {
                // readPixels exposes straight sRGB. Transparent original base
                // texels have zero color; keep colored glyphs, not just white.
                val value=if(pixels[i] ushr 24>0)pixels[i]else 0
                rgba[i*4]=(value shr 16).toByte();rgba[i*4+1]=(value shr 8).toByte()
                rgba[i*4+2]=value.toByte();rgba[i*4+3]=255.toByte()
            }
            val opaque=Image.makeRaster(ImageInfo.makeS32(width,height,ColorAlphaType.OPAQUE).withColorType(ColorType.RGBA_8888),rgba,width*4)
                .use{it.toComposeImageBitmap()}
            fun reduce(previous:ImageBitmap,w:Int,h:Int,linear:Boolean):ImageBitmap {
                val image=if(linear)ImageBitmap(w,h,config=ImageBitmapConfig.F16,colorSpace=ColorSpaces.LinearSrgb)else ImageBitmap(w,h)
                CanvasDrawScope().draw(Density(1f),LayoutDirection.Ltr,Canvas(image),Size(w.toFloat(),h.toFloat())) {
                    drawImage(previous,dstSize=IntSize(w,h),filterQuality=FilterQuality.Low)
                }
                return image
            }
            try{colors+=reduce(opaque,width,height,true)}finally{opaque.asSkiaBitmap().close()}
            while(masks.last().width>1||masks.last().height>1) {
                val w=max(1,masks.last().width/2);val h=max(1,masks.last().height/2)
                masks+=reduce(masks.last(),w,h,false);colors+=reduce(colors.last(),w,h,true)
            }
            val last=colors.lastIndex
            val declarations=(0..last).joinToString("\n"){"uniform shader c$it; uniform shader a$it; uniform float2 size$it;"}
            val branches=(0..last).joinToString("\n") {
                "if(level==$it){float2 uv=p*size$it/baseSize;return float4(toLinearSrgb(c$it.eval(uv).rgb),a$it.eval(uv).a);}"
            }
            val program=RuntimeEffect.makeForShader("""
                $declarations
                uniform float2 baseSize;
                uniform float3 hx;uniform float3 hy;uniform float3 hz;
                uniform float3 primary;uniform float opacity;
                uniform int toneMapped;uniform int fogEnabled;
                uniform float3 fogColor;uniform float2 fogRange;
                float4 sampleLevel(int level,float2 p){$branches return float4(0);}
                float3 aces(float3 c){
                    c=c/.6;
                    float3 q=float3(dot(float3(.59719,.35458,.04823),c),dot(float3(.076,.90834,.01566),c),dot(float3(.02840,.13383,.83777),c));
                    q=(q*(q+.0245786)-.000090537)/(q*(.983729*q+.4329510)+.238081);
                    return clamp(float3(dot(float3(1.60475,-.53108,-.07367),q),dot(float3(-.10208,1.10813,-.00605),q),dot(float3(-.00327,-.07276,1.07602),q)),0,1);
                }
                float encode(float c){return c<=.0031308?12.92*c:1.055*pow(c,1.0/2.4)-.055;}
                float3 encoded(float3 c){return float3(encode(c.r),encode(c.g),encode(c.b));}
                float4 main(float2 p){
                    float3 coord=float3(p,1);float a=dot(hx,coord);float b=dot(hy,coord);float z=dot(hz,coord);
                    float2 dx=(hx.xy*z-a*hz.xy)/(z*z);float2 dy=(hy.xy*z-b*hz.xy)/(z*z);
                    float determinant=dx.x*dy.y-dx.y*dy.x;
                    float2 tx=float2(dy.y,-dy.x)/determinant;
                    float2 ty=float2(-dx.y,dx.x)/determinant;
                    float sx=dot(tx,tx);float sy=dot(ty,ty);
                    float area=abs(tx.x*ty.y-tx.y*ty.x);
                    float ratio=min(4.0,max(sx,sy)/area);
                    float2 axis=sx>=sy?tx:ty;
                    float level=clamp(log2(sqrt(max(sx,sy))/ratio),0,$last);
                    int first=int(floor(level));int count=int(floor(ratio+.5));
                    float4 tex=float4(0);
                    for(int i=0;i<4;++i){if(i<count){
                        float2 point=p+axis*((float(i)+.5)/float(count)-.5);
                        tex+=mix(sampleLevel(first,point),sampleLevel(first<$last?first+1:$last,point),fract(level))/float(count);
                    }}
                    float3 linear=primary*tex.rgb;
                    float3 rgb=encoded(toneMapped==1?aces(linear):linear);
                    if(fogEnabled==1)rgb=mix(rgb,fogColor,smoothstep(fogRange.x,fogRange.y,z));
                    float alpha=tex.a*opacity;return float4(rgb*alpha,alpha);
                }
            """.trimIndent())
            effect=program
            val uniforms=RuntimeShaderBuilder(program);builder=uniforms
            uniforms.uniform("baseSize",width.toFloat(),height.toFloat())
            for(i in 0..last) {
                val c=colors[i].asSkiaBitmap().makeShader(sampling=FilterMipmap(FilterMode.LINEAR,MipmapMode.NONE)).also{children+=it}
                val a=masks[i].asSkiaBitmap().makeShader(sampling=FilterMipmap(FilterMode.LINEAR,MipmapMode.NONE)).also{children+=it}
                uniforms.child("c$i",c);uniforms.child("a$i",a)
                uniforms.uniform("size$i",colors[i].width.toFloat(),colors[i].height.toFloat())
            }
        }catch(failure:Throwable){close();throw failure}
    }

    override fun draw(scope:DrawScope,view:DioramaPerspective,origin:DioramaVec,x:DioramaVec,y:DioramaVec,
        color:DioramaLinearColor,alpha:Float,fog:DioramaFog?,toneMapping:DioramaToneMapping) {
        check(!closed){"Plain sampler is closed"}
        if(alpha<=0)return
        val uniforms=checkNotNull(builder)
        val h=view.planeHomography(origin,x,y)
        for((name,start)in listOf("hx" to 0,"hy" to 3,"hz" to 6))uniforms.uniform(name,h[start].toFloat(),h[start+1].toFloat(),h[start+2].toFloat())
        uniforms.uniform("primary",color.red.toFloat(),color.green.toFloat(),color.blue.toFloat());uniforms.uniform("opacity",alpha)
        uniforms.uniform("toneMapped",if(toneMapping==DioramaToneMapping.AcesFilmic)1 else 0)
        uniforms.uniform("fogEnabled",if(fog==null)0 else 1)
        val fogColor=fog?.encodedColor?:Color.Black
        uniforms.uniform("fogColor",fogColor.red,fogColor.green,fogColor.blue)
        uniforms.uniform("fogRange",fog?.near?.toFloat()?:0f,fog?.far?.toFloat()?:1f)
        scope.withDioramaPlane(view,origin,x,y,width.toDouble(),height.toDouble()) {
            uniforms.makeShader().use{drawRect(ShaderBrush(it.asComposeShader()),size=Size(width.toFloat(),height.toFloat()))}
        }
    }

    override fun close() {
        if(closed)return
        closed=true
        builder?.close();builder=null;effect?.close();effect=null
        children.forEach{it.close()};children.clear()
        colors.forEach{it.asSkiaBitmap().close()};colors.clear()
        // The original paragraph image is shared with departing snapshots.
        masks.drop(1).forEach{it.asSkiaBitmap().close()};masks.clear()
    }
}
