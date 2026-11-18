// SPDX-License-Identifier: AGPL-3.0-only
// Independent bounded text-fragment control; no production/Android integration.
// ACES algebra is adapted from the existing DioramaToneMapping.kt port of
// Three0.185.1 (Copyright 2010-2026 three.js authors, MIT; LICENSES notice).
package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.unit.*
import org.jetbrains.skia.*
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama.*
import kotlin.math.*
import java.io.File
import javax.imageio.ImageIO

/** Single shared-cache7 diagnostic, explicit mip0..5, per-fragment isotropic or
 * bounded four-tap anisotropic LOD, linear-light RGB distinct from alpha.
 * No general glyph/near-plane support or original GPU mip input. */
internal class DioramaFragmentControl(private val base:ImageBitmap):AutoCloseable {
    private val masks=mutableListOf(base)
    private val colors=mutableListOf<ImageBitmap>()
    private val children=mutableListOf<org.jetbrains.skia.Shader>()
    private val effect:RuntimeEffect
    private val builder:RuntimeShaderBuilder

    init {
        val pixels=IntArray(base.width*base.height).also{base.readPixels(it)}
        val occupancy=ByteArray(pixels.size*4)
        for(i in pixels.indices) {
            val alpha=pixels[i] ushr 24
            require(alpha==0 || pixels[i] and 0xffffff==0xffffff){"Bounded white source-cache control only"}
            val value=if(alpha>0)255.toByte()else 0
            occupancy[i*4]=value;occupancy[i*4+1]=value;occupancy[i*4+2]=value;occupancy[i*4+3]=255.toByte()
        }
        val opaque=Image.makeRaster(ImageInfo.makeS32(base.width,base.height,ColorAlphaType.OPAQUE),occupancy,base.width*4).toComposeImageBitmap()
        fun reduce(previous:ImageBitmap,w:Int,h:Int,linear:Boolean):ImageBitmap {
            val image=if(linear)ImageBitmap(w,h,config=ImageBitmapConfig.F16,colorSpace=ColorSpaces.LinearSrgb)else ImageBitmap(w,h)
            CanvasDrawScope().draw(Density(1f),LayoutDirection.Ltr,Canvas(image),Size(w.toFloat(),h.toFloat())) {
                drawImage(previous,dstSize=IntSize(w,h),filterQuality=FilterQuality.Low)
            }
            return image
        }
        colors+=reduce(opaque,base.width,base.height,true)
        repeat(5) {
            val w=max(1,masks.last().width/2);val h=max(1,masks.last().height/2)
            masks+=reduce(masks.last(),w,h,false);colors+=reduce(colors.last(),w,h,true)
        }
        val declarations=(0..5).joinToString("\n"){"uniform shader c$it; uniform shader a$it; uniform float2 size$it;"}
        val branches=(0..5).joinToString("\n"){
            "if(level==$it) { float2 uv=p*size$it/baseSize; return float2(toLinearSrgb(c$it.eval(uv).rgb).r,a$it.eval(uv).a); }"
        }
        effect=RuntimeEffect.makeForShader("""
            $declarations
            uniform float2 baseSize;
            uniform float3 hx; uniform float3 hy; uniform float3 hz;
            uniform float3 primary; uniform float3 fogColor; uniform float opacity;
            uniform int diagnostic;
            uniform float maxAniso;
            float2 sampleLevel(int level,float2 p) { $branches return float2(0); }
            float3 aces(float3 c) {
                c=c/.6;
                float3 q=float3(dot(float3(.59719,.35458,.04823),c),dot(float3(.076,.90834,.01566),c),dot(float3(.02840,.13383,.83777),c));
                q=(q*(q+.0245786)-.000090537)/(q*(.983729*q+.4329510)+.238081);
                return clamp(float3(dot(float3(1.60475,-.53108,-.07367),q),dot(float3(-.10208,1.10813,-.00605),q),dot(float3(-.00327,-.07276,1.07602),q)),0,1);
            }
            // Explicit Three output transfer, not destination-context dependent
            // fromLinearSrgb(). Encoded fog/blending follow this numeric OETF.
            float encode(float c) {return c<=.0031308?12.92*c:1.055*pow(c,1.0/2.4)-.055;}
            float3 encoded(float3 c) {return float3(encode(c.r),encode(c.g),encode(c.b));}
            float4 main(float2 p) {
                float3 coord=float3(p,1);float a=dot(hx,coord);float b=dot(hy,coord);float z=dot(hz,coord);
                float2 dx=(hx.xy*z-a*hz.xy)/(z*z);float2 dy=(hy.xy*z-b*hz.xy)/(z*z);
                float determinant=dx.x*dy.y-dx.y*dy.x;
                float rho=max(length(float2(dy.y,dy.x)),length(float2(dx.y,dx.x)))/abs(determinant);
                float lod=clamp(log2(max(rho,1)),0,5);int low=int(floor(lod));
                float2 tex=mix(sampleLevel(low,p),sampleLevel(low<5?low+1:5,p),fract(lod));
                if(maxAniso>1) {
                    // Independent area/longest-axis footprint construction.
                    // Driver-informed investigation, not a claim all GPU
                    // implementations choose this anisotropic sampling rule.
                    float2 tx=float2(dy.y,-dy.x)/determinant;
                    float2 ty=float2(-dx.y,dx.x)/determinant;
                    float sx=dot(tx,tx);float sy=dot(ty,ty);
                    float area=abs(tx.x*ty.y-tx.y*ty.x);
                    float ratio=min(maxAniso,max(sx,sy)/area);
                    float2 axis=sx>=sy?tx:ty;
                    float level=clamp(log2(sqrt(max(sx,sy))/ratio),0,5);
                    int first=int(floor(level));int count=int(floor(ratio+.5));
                    tex=float2(0);
                    for(int i=0;i<4;++i) {
                        if(i<count) {
                            float2 point=p+axis*((float(i)+.5)/float(count)-.5);
                            tex+=mix(sampleLevel(first,point),sampleLevel(first<5?first+1:5,point),fract(level))/float(count);
                        }
                    }
                }
                if(diagnostic==1)return float4(tex.xxx,1);
                if(diagnostic==2)return float4(tex.yyy,1);
                if(diagnostic==3)return float4(aces(primary*tex.x),1);
                if(diagnostic==4)return float4(encoded(aces(primary*tex.x)),1);
                if(diagnostic==5)return float4(primary,1);
                float3 rgb=encoded(aces(primary*tex.x));
                rgb=mix(rgb,fogColor,smoothstep(12,30,z));float alpha=tex.y*opacity;
                return float4(rgb*alpha,alpha);
            }
        """.trimIndent())
        builder=RuntimeShaderBuilder(effect)
        builder.uniform("baseSize",base.width.toFloat(),base.height.toFloat())
        for(i in 0..5) {
            val c=colors[i].asSkiaBitmap().makeShader(sampling=FilterMipmap(FilterMode.LINEAR,MipmapMode.NONE))
            val a=masks[i].asSkiaBitmap().makeShader(sampling=FilterMipmap(FilterMode.LINEAR,MipmapMode.NONE))
            children+=c;children+=a;builder.child("c$i",c);builder.child("a$i",a)
            builder.uniform("size$i",colors[i].width.toFloat(),colors[i].height.toFloat())
        }
    }

    /** Numeric color-management ablation: no camera/material/fog or alpha. */
    fun exportChildColorControls() {
        val out=File("../build/verification/diorama-fragment-color-kotlin").apply{mkdirs()}
        for((name,expression)in listOf("direct" to "c","to-linear" to "toLinearSrgb(c)")) {
            RuntimeEffect.makeForShader("uniform shader child; half4 main(float2 p) {half3 c=child.eval(p).rgb;return half4($expression,1);}").use { effect ->
                RuntimeShaderBuilder(effect).use { builder ->
                    builder.child("child",children[8])
                    builder.makeShader().use { shader ->
                        val im=ImageBitmap(colors[4].width,colors[4].height)
                        CanvasDrawScope().draw(Density(1f),LayoutDirection.Ltr,Canvas(im),Size(im.width.toFloat(),im.height.toFloat())) {
                            drawRect(ShaderBrush(shader.asComposeShader()))
                        }
                        ImageIO.write(im.toAwtImage(),"png",out.resolve("child-$name.png"))
                    }
                }
            }
        }
    }

    fun draw(scope:DrawScope,view:DioramaPerspective,p:DioramaDepartingPlane,color:DioramaLinearColor,alpha:Float,diagnostic:Int=0,maxAniso:Float=1f) {
        require(maxAniso==1f || maxAniso==4f){"Only the two bounded sampler controls are supported"}
        val scale=dioramaFrameFit(p.text.worldWidth,5.2,view.fov,view.width/view.height)*p.placement.scale
        val x=dioramaPlaneAxis(p.frame,p.placement,1.0,0.0)*(scale*.62/p.text.fontPx)
        val y=dioramaPlaneAxis(p.frame,p.placement,0.0,-1.0)*(scale*.62/p.text.fontPx)
        val center=dioramaComposeLocal(p.frame,p.placement.offsetR,p.placement.offsetU,0.0)
        val origin=center-x*(base.width/2.0)-y*(base.height/2.0)
        require(!dioramaClipPlane(view,origin,x,y,base.width.toDouble(),base.height.toDouble()).needsClip)
        val h=view.planeHomography(origin,x,y)
        for((name,start)in listOf("hx" to 0,"hy" to 3,"hz" to 6))builder.uniform(name,h[start].toFloat(),h[start+1].toFloat(),h[start+2].toFloat())
        builder.uniform("primary",color.red.toFloat(),color.green.toFloat(),color.blue.toFloat())
        builder.uniform("fogColor",22/255f,36/255f,53/255f);builder.uniform("opacity",alpha)
        builder.uniform("diagnostic",diagnostic)
        builder.uniform("maxAniso",maxAniso)
        val matrix=Matrix().apply {
            this[0,0]=h[0].toFloat();this[1,0]=h[1].toFloat();this[3,0]=h[2].toFloat()
            this[0,1]=h[3].toFloat();this[1,1]=h[4].toFloat();this[3,1]=h[5].toFloat()
            this[0,3]=h[6].toFloat();this[1,3]=h[7].toFloat();this[3,3]=h[8].toFloat()
        }
        builder.makeShader().use { shader ->
            with(scope){withTransform({transform(matrix)}) {
                drawRect(ShaderBrush(shader.asComposeShader()),size=Size(base.width.toFloat(),base.height.toFloat()))
            }}
        }
    }
    override fun close(){builder.close();effect.close();children.forEach{it.close()}}
}
