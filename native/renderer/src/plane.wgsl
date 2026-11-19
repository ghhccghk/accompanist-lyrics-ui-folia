// ACES fit adapted from Three.js 0.185.1 (MIT, see LICENSES/MIT-three-0.185.1.txt).
// SPDX-License-Identifier: AGPL-3.0-only AND MIT
struct Point {
    xyw: vec4<f32>,
    uv: vec4<f32>,
}
struct Params {
    ink: vec4<f32>,
    size_and_fog_range: vec4<f32>,
    fog_color_and_enabled: vec4<f32>,
    points: array<Point, 6>,
    sampling: vec4<f32>,
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

fn sample_material(uv: vec2<f32>, du: vec2<f32>, dv: vec2<f32>) -> vec4<f32> {
    let dimensions = vec2<f32>(textureDimensions(coverage));
    let dx = du * dimensions;
    let dy = dv * dimensions;
    let sx = dot(dx, dx);
    let sy = dot(dy, dy);
    let major = max(sx, sy);
    let area = abs(dx.x * dy.y - dx.y * dy.x);
    let ratio = clamp(major / max(area, 1e-8), 1.0, 4.0);
    let lod = max(0.0, log2(max(sqrt(major), 1e-4) / ratio));
    let axis = select(dv, du, sx >= sy);
    let taps = u32(floor(ratio + 0.5));
    var sum = vec4(0.0);
    for (var tap = 0u; tap < taps; tap++) {
        let xy = uv + axis * ((f32(tap) + 0.5) / f32(taps) - 0.5);
        sum += textureSampleLevel(coverage, coverage_sampler, xy, lod);
    }
    return sum / f32(taps);
}

fn tone_map(linear: vec3<f32>) -> vec3<f32> {
    let color = linear / 0.6;
    let v = vec3(dot(color, vec3(.59719, .35458, .04823)),
        dot(color, vec3(.076, .90834, .01566)), dot(color, vec3(.0284, .13383, .83777)));
    let fit = (v * (v + .0245786) - .000090537) / (v * (.983729 * v + .4329510) + .238081);
    let mapped = clamp(vec3(dot(fit, vec3(1.60475, -.53108, -.07367)),
        dot(fit, vec3(-.10208, 1.10813, -.00605)), dot(fit, vec3(-.00327, -.07276, 1.07602))),
        vec3(0.0), vec3(1.0));
    return select(1.055 * pow(mapped, vec3(1.0 / 2.4)) - 0.055,
        12.92 * mapped, mapped <= vec3(0.0031308));
}

@fragment fn fragment(input: Vertex) -> @location(0) vec4<f32> {
    // Recover analytic projective derivatives from the affine uv/w and 1/w.
    let inverse_depth = input.position.w;
    let uv_over_depth = input.uv * inverse_depth;
    let du = (dpdx(uv_over_depth) - input.uv * dpdx(inverse_depth)) / inverse_depth;
    let dv = (dpdy(uv_over_depth) - input.uv * dpdy(inverse_depth)) / inverse_depth;
    var alpha: f32;
    var ink = params.ink.rgb;
    if params.sampling.x > 0.0 {
        let texel = sample_material(input.uv, du, dv);
        alpha = texel.a * params.ink.a;
        ink = tone_map(texel.rgb * ink);
    } else {
        alpha = textureSampleLevel(coverage, coverage_sampler, input.uv, 0.0).a * params.ink.a;
    }
    if params.fog_color_and_enabled.a > 0.0 {
        let depth = 1.0 / input.position.w;
        let fog = smoothstep(params.size_and_fog_range.z, params.size_and_fog_range.w, depth);
        ink = mix(ink, params.fog_color_and_enabled.rgb, fog);
    }
    return vec4(ink * alpha, alpha);
}
