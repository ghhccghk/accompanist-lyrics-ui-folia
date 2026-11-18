struct Params {
    matrix: mat2x2<f32>,
    translation: vec2<f32>,
    source_size: vec2<f32>,
    target_size: vec2<f32>,
    mask: f32,
    pad: f32,
    color: vec4<f32>,
}
@group(0) @binding(0) var source: texture_2d<f32>;
@group(0) @binding(1) var source_sampler: sampler;
@group(0) @binding(2) var<uniform> params: Params;
struct Vertex { @builtin(position) position: vec4<f32>, @location(0) uv: vec2<f32> }
@vertex fn vertex(@builtin(vertex_index) index: u32) -> Vertex {
    let points=array<vec2<f32>,6>(vec2(0.,0.),vec2(1.,0.),vec2(0.,1.),vec2(0.,1.),vec2(1.,0.),vec2(1.,1.));
    let uv=points[index];
    let p=params.matrix*(uv*params.source_size)+params.translation;
    var result:Vertex;
    result.position=vec4(2.*p.x/params.target_size.x-1.,1.-2.*p.y/params.target_size.y,0.,1.);
    result.uv=uv;
    return result;
}
@fragment fn fragment(input:Vertex)->@location(0) vec4<f32> {
    let pixel=textureSample(source,source_sampler,input.uv);
    if params.mask>0.5 {
        let a=pixel.a*params.color.a;
        return vec4(params.color.rgb*a,a);
    }
    return pixel*params.color.a;
}
