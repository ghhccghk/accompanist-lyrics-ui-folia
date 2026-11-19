// SPDX-License-Identifier: AGPL-3.0-only
// Keep straight linear RGB and coverage independent at all levels.
@group(0) @binding(0) var source: texture_2d<f32>;
@group(0) @binding(1) var linear_sampler: sampler;
@group(0) @binding(2) var<uniform> output_size: vec4<f32>;

@vertex fn vertex(@builtin(vertex_index) index: u32) -> @builtin(position) vec4<f32> {
    let points = array(vec2(-1., -1.), vec2(3., -1.), vec2(-1., 3.));
    return vec4(points[index], 0., 1.);
}

@fragment fn fragment(@builtin(position) position: vec4<f32>) -> @location(0) vec4<f32> {
    if output_size.z > 0.0 {
        let rgba = textureLoad(source, vec2<i32>(position.xy), 0);
        let linear = select(pow((rgba.rgb + 0.055) / 1.055, vec3(2.4)),
            rgba.rgb / 12.92, rgba.rgb <= vec3(0.04045));
        return vec4(linear, rgba.a);
    }
    return textureSampleLevel(source, linear_sampler, position.xy / output_size.xy, 0.0);
}
