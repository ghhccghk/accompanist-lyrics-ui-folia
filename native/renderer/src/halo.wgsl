struct Params { alpha:f32, layers:u32, pad:vec2<u32> }
@group(0) @binding(0) var first:texture_2d<f32>;
@group(0) @binding(1) var second:texture_2d<f32>;
@group(0) @binding(2) var<uniform> params:Params;
@vertex fn vertex(@builtin(vertex_index) index:u32)->@builtin(position) vec4<f32> {
    let p=array<vec2<f32>,3>(vec2(-1.,-1.),vec2(3.,-1.),vec2(-1.,3.));return vec4(p[index],0.,1.);
}
@fragment fn fragment(@builtin(position) position:vec4<f32>)->@location(0) vec4<f32> {
    let p=vec2<i32>(position.xy);
    var a=u32(floor(textureLoad(first,p,0).a*255.*params.alpha+0.5));
    if params.layers==2u {
        let b=u32(floor(textureLoad(second,p,0).a*255.*params.alpha+0.5));
        a=b+(a*(256u-b))/256u;
    }
    return vec4<f32>(f32(a)/255.);
}
