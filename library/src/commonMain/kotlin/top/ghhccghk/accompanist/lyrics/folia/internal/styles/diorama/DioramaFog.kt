// Copyright (c) 2026 chthollyphile and contributors
// SPDX-License-Identifier: AGPL-3.0-only
// Folia cc6226e4 dioramaSceneConstants.ts / DioramaScene.tsx fog attachment.
// Shader order/algebra: Three.js0.185.1 meshbasic.glsl.js, fog_{vertex,fragment},
// UniformsUtils.getUnlitUniformColorSpace; (c) three.js authors, MIT, see LICENSES.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama

import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.drawscope.*
import kotlin.math.*

/** Encoded sRGB fog uniform for a direct-to-screen original MeshBasicMaterial.
 * Original fog follows colorspace_fragment, not the linear material-color mix.
 * This only fogs existing lyric planes, never creates a background/depth engine. */
internal data class DioramaFog(val color: Color, val near: Double = 12.0, val far: Double = 30.0) {
    init {
        require(color!=Color.Unspecified)
        require(near.isFinite()&&far.isFinite()&&near>=0&&far>near)
    }
    val encodedColor=color.convert(ColorSpaces.Srgb).copy(alpha=1f)
    fun factor(depth: Double): Double {
        val t=((depth-near)/(far-near)).coerceIn(0.0,1.0)
        return t*t*(3-2*t)
    }
    fun mixEncoded(body: Color, factor: Double): Color {
        val a=body.convert(ColorSpaces.Srgb)
        fun mix(x: Float,y: Float)=dioramaFogMixChannel(x.toDouble(),y.toDouble(),factor).toFloat()
        // sRGB Color packs immediately to bytes. Keep the interpolated stops in
        // extended sRGB (same transfer function, half-float storage), so the
        // brush does not incur a second full byte rounding before rasterizing.
        return Color(mix(a.red,encodedColor.red),mix(a.green,encodedColor.green),mix(a.blue,encodedColor.blue),
            colorSpace=ColorSpaces.ExtendedSrgb)
    }
}

internal fun dioramaFogMixChannel(body: Double, fog: Double, factor: Double): Double =
    body+(fog-body)*factor

/** Fog depth is affine in plane coordinates, perspective-correct after the
 * homography. 32 linear brush intervals approximate ONLY smoothstep: max error
 * <= max|f''|/(8*32^2) = .000732421875, <=.186768 of an8-bit channel.
 * Color stops additionally incur half-float packing (<=1/4096 in [0,1]);
 * the input palette and final output framebuffer have their own quantization.
 * No frame
 * sampling, per-pixel CPU pass, new glyph raster or whole-song mask is created.
 * Alpha coverage/life stays separate from this per-fragment RGB operation. */
internal fun DrawScope.drawDioramaFogMask(image: ImageBitmap, view: DioramaPerspective,
    origin: DioramaVec, x: DioramaVec, y: DioramaVec, alpha: Float, color: Color,
    additive: Boolean, fog: DioramaFog) {
    val z0=(origin-view.eye).dot(view.forward)
    val zx=x.dot(view.forward);val zy=y.dot(view.forward)
    val dx=zx*image.width;val dy=zy*image.height
    val low=z0+min(0.0,dx)+min(0.0,dy)
    val high=z0+max(0.0,dx)+max(0.0,dy)
    fun constant(ink: Color)=drawImage(image,alpha=alpha,colorFilter=ColorFilter.tint(ink),
        blendMode=if(additive)BlendMode.Plus else BlendMode.SrcOver)
    if(high<=fog.near){constant(color);return}
    if(low>=fog.far){constant(fog.encodedColor);return}
    // Only a numerical guard for a front-parallel/degenerate depth gradient.
    if(high-low<=1e-9){constant(fog.mixEncoded(color,fog.factor(z0)));return}
    val n2=zx*zx+zy*zy
    fun atDepth(z: Double)=Offset((zx*(z-z0)/n2).toFloat(),(zy*(z-z0)/n2).toFloat())
    val stops=Array(33){i->
        val t=i/32.0
        t.toFloat() to fog.mixEncoded(color,t*t*(3-2*t))
    }
    val brush=Brush.linearGradient(*stops,start=atDepth(fog.near),end=atDepth(fog.far),tileMode=TileMode.Clamp)
    val bounds=Rect(0f,0f,image.width.toFloat(),image.height.toFloat())
    val canvas=drawContext.canvas
    canvas.saveLayer(bounds,Paint().apply{blendMode=if(additive)BlendMode.Plus else BlendMode.SrcOver})
    try {
        drawImage(image,alpha=alpha)
        drawRect(brush,Offset.Zero,bounds.size,blendMode=BlendMode.SrcIn)
    } finally { canvas.restore() }
}
