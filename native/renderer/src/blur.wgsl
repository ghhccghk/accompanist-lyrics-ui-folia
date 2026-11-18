// Same three finite boxes, opposite even-window phase, transparent exterior,
// and axis-end 8-bit quantisation as the base Kotlin CSS shadow implementation.
struct Params { axis:i32, left:i32, count:i32, quantize:i32, sigma:f32, pad0:f32, pad1:f32, pad2:f32 }
@group(0) @binding(0) var source:texture_2d<f32>;
@group(0) @binding(1) var<uniform> params:Params;
@vertex fn vertex(@builtin(vertex_index) index:u32)->@builtin(position) vec4<f32> {
    let p=array<vec2<f32>,3>(vec2(-1.,-1.),vec2(3.,-1.),vec2(-1.,3.));
    return vec4(p[index],0.,1.);
}
@fragment fn fragment(@builtin(position) position:vec4<f32>)->@location(0) vec4<f32> {
    let at=vec2<i32>(position.xy);
    let size=vec2<i32>(textureDimensions(source));
    var sum=vec4<f32>(0.);
    var norm=0.;
    for(var i=0;i<params.count;i=i+1) {
        var p=at;
        if params.axis==0 {p.x+=i-params.left;}else{p.y+=i-params.left;}
        var weight=1.;
        if params.sigma>0. {let d=f32(i-params.left);weight=exp(-0.5*d*d/(params.sigma*params.sigma));}
        norm+=weight;
        if all(p>=vec2(0)) && all(p<size) {sum+=textureLoad(source,p,0)*weight;}
    }
    var result=sum/norm;
    if params.quantize!=0 {result=floor(clamp(result,vec4(0.),vec4(1.))*255.+0.5)/255.;}
    return result;
}
