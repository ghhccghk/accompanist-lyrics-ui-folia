// SPDX-License-Identifier: AGPL-3.0-only
struct Point {
    xyw: vec4<f32>,
    uv: vec4<f32>,
}
struct Params {
    ink: vec4<f32>,
    size_and_fog_range: vec4<f32>,
    fog_color_and_enabled: vec4<f32>,
    points: array<Point, 6>,
}
struct Vertex {
    @builtin(position) position: vec4<f32>,
    @location(0) uv: vec2<f32>,
}
@group(0) @binding(0) var coverage: texture_2d<f32>;
@group(0) @binding(1) var coverage_sampler: sampler;
@group(0) @binding(2) var<uniform> params: Params;

@vertex fn vertex(@builtin(vertex_index) index: u32) -> Vertex {
    let slot = index % 3u;
    let point = params.points[select(index / 3u + slot, 0u, slot == 0u)];
    let w = point.xyw.z;
    let ndc_numerator = 2.0 * point.xyw.xy / params.size_and_fog_range.xy;
    return Vertex(vec4(ndc_numerator.x - w, w - ndc_numerator.y, 0.0, w), point.uv.xy);
}

@fragment fn fragment(input: Vertex) -> @location(0) vec4<f32> {
    let alpha = textureSample(coverage, coverage_sampler, input.uv).a * params.ink.a;
    var ink = params.ink.rgb;
    if params.fog_color_and_enabled.a > 0.0 {
        let depth = 1.0 / input.position.w;
        let fog = smoothstep(params.size_and_fog_range.z, params.size_and_fog_range.w, depth);
        ink = mix(ink, params.fog_color_and_enabled.rgb, fog);
    }
    return vec4(ink * alpha, alpha);
}
