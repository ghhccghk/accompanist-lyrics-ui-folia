//! Experimental CPU coverage kernel. No renderer, font, scene, or media clock.
//! Same six passes and per-axis rounding as the independent Kotlin GlowMask.kt.
use jni_sys::{jclass, jint, jobject, JNIEnv};
use std::{mem::align_of, panic::catch_unwind};
mod color;

fn box_pass(input: &[f64], output: &mut [f64], width: usize, height: usize,
            window: usize, horizontal: bool, origin: isize) {
    let (length, lines, stride) = if horizontal { (width, height, 1) } else { (height, width, width) };
    let left = (window / 2) as isize + origin;
    let right = window as isize - left - 1;
    if !horizontal {
        // Same integer-valued recurrence as Kotlin, in contiguous row order.
        // The previous output row stores rolling totals; no extra native heap,
        // transposition, reduced precision, or reordered additions per pixel.
        for x in 0..width {
            let mut total = 0.0;
            for y in 0..=(right as usize).min(height - 1) {
                total += unsafe { *input.get_unchecked(y * width + x) };
            }
            unsafe { *output.get_unchecked_mut(x) = total; }
        }
        for y in 1..height {
            let remove = y as isize - 1 - left;
            let add = y + right as usize;
            for x in 0..width {
                let mut total = unsafe { *output.get_unchecked((y - 1) * width + x) };
                if remove >= 0 { total -= unsafe { *input.get_unchecked(remove as usize * width + x) }; }
                if add < height { total += unsafe { *input.get_unchecked(add * width + x) }; }
                unsafe { *output.get_unchecked_mut(y * width + x) = total; }
            }
        }
        return;
    }
    for line in 0..lines {
        let base = if horizontal { line * width } else { line };
        let mut total = 0.0;
        for p in 0..=(right as usize).min(length - 1) {
            // Caller establishes width*height-sized, distinct slices; base and
            // stride enumerate a single in-bounds row/column. The clamped
            // window and the explicit guards below prove every read/write.
            total += unsafe { *input.get_unchecked(base + p * stride) };
        }
        for p in 0..length {
            unsafe { *output.get_unchecked_mut(base + p * stride) = total; }
            if p as isize >= left {
                total -= unsafe { *input.get_unchecked(base + (p as isize - left) as usize * stride) };
            }
            let add = p + right as usize + 1;
            if add < length { total += unsafe { *input.get_unchecked(base + add * stride) }; }
        }
    }
}

fn blur(a: &mut [f64], b: &mut [f64], width: usize, height: usize, sigma: f32) {
    // Kotlin evaluates sigma*3 in Float before promoting for sqrt(Double).
    let window = (((sigma * 3.0) as f64 * (2.0 * std::f64::consts::PI).sqrt() / 4.0 + 0.5).floor() as usize).max(1);
    let widths = [window, window, window + usize::from(window % 2 == 0)];
    let divisor = widths.iter().fold(1.0, |n, &v| n * v as f64);
    for horizontal in [true, false] {
        for (pass, &count) in widths.iter().enumerate() {
            let origin = if count % 2 == 0 && pass == 1 { -1 } else { 0 };
            if pass % 2 == 0 { box_pass(a, b, width, height, count, horizontal, origin); }
            else { box_pass(b, a, width, height, count, horizontal, origin); }
        }
        for i in 0..a.len() { a[i] = (b[i] / divisor + 0.5).floor().clamp(0.0, 255.0); }
    }
}

// No native state or retained Java address. All memory belongs to the caller.
// Status: 0 OK; 1 invalid argument/buffer; 2 overlapping buffers; 3 panic.
#[no_mangle]
pub unsafe extern "system" fn Java_top_ghhccghk_accompanist_lyrics_folia_NativeMaskExperiment_blur(
    env: *mut JNIEnv, _class: jclass, source: jobject, scratch_a: jobject, scratch_b: jobject,
    sigmas: jobject, output: jobject, width: jint, height: jint, count: jint,
) -> jint {
    catch_unwind(|| {
        if env.is_null() || width <= 0 || height <= 0 || count <= 0 || count > 16 { return 1; }
        let Some(n) = (width as usize).checked_mul(height as usize) else { return 1; };
        if n > 16 * 1024 * 1024 { return 1; }
        let table = &**env;
        let (Some(address), Some(capacity)) = (table.GetDirectBufferAddress, table.GetDirectBufferCapacity) else { return 1; };
        let (Some(find_class), Some(method), Some(call), Some(delete), Some(exception)) =
            (table.FindClass, table.GetMethodID, table.CallBooleanMethodA, table.DeleteLocalRef, table.ExceptionCheck)
            else { return 1; };
        let class = find_class(env, b"java/nio/ByteBuffer\0".as_ptr().cast());
        if class.is_null() { return 1; }
        let readonly = method(env, class, b"isReadOnly\0".as_ptr().cast(), b"()Z\0".as_ptr().cast());
        if readonly.is_null() { delete(env, class); return 1; }
        for buffer in [scratch_a, scratch_b, output] {
            if buffer.is_null() || call(env, buffer, readonly, std::ptr::null()) != 0 || exception(env) != 0 {
                delete(env, class); return 1;
            }
        }
        delete(env, class);
        let objects = [source, scratch_a, scratch_b, sigmas, output];
        let sizes = [n * 4, n * 8, n * 8, count as usize * 4, n * count as usize * 4];
        let alignments = [align_of::<u32>(), align_of::<f64>(), align_of::<f64>(), align_of::<f32>(), align_of::<u32>()];
        let mut pointers = [std::ptr::null_mut(); 5];
        for i in 0..5 {
            if objects[i].is_null() { return 1; }
            pointers[i] = address(env, objects[i]);
            let cap = capacity(env, objects[i]);
            if pointers[i].is_null() || cap < sizes[i] as i64 || pointers[i] as usize % alignments[i] != 0 { return 1; }
            if (pointers[i] as usize).checked_add(sizes[i]).is_none() { return 1; }
        }
        for i in 0..5 { for j in 0..i {
            let (a, b) = (pointers[i] as usize, pointers[j] as usize);
            if a < b + sizes[j] && b < a + sizes[i] { return 2; }
        }}
        let radii = std::slice::from_raw_parts(pointers[3].cast::<f32>(), count as usize);
        if radii.iter().any(|v| !v.is_finite() || !(1.0..=4096.0).contains(v)) { return 1; }
        let pixels = std::slice::from_raw_parts(pointers[0].cast::<u32>(), n);
        let a = std::slice::from_raw_parts_mut(pointers[1].cast::<f64>(), n);
        let b = std::slice::from_raw_parts_mut(pointers[2].cast::<f64>(), n);
        let result = std::slice::from_raw_parts_mut(pointers[4].cast::<u32>(), n * count as usize);
        for (r, &sigma) in radii.iter().enumerate() {
            for i in 0..n { a[i] = (pixels[i] >> 24) as f64; }
            blur(a, b, width as usize, height as usize, sigma);
            for i in 0..n { result[r * n + i] = ((a[i] as u32) << 24) | 0x00ffffff; }
        }
        0
    }).unwrap_or(3)
}

#[cfg(test)]
mod tests {
    use super::*;
    #[test] fn one_pixel_and_large_windows() {
        for sigma in [1.0, 2.0, 10.0, 4096.0] {
            let mut a = [255.0]; let mut b = [0.0];
            blur(&mut a, &mut b, 1, 1, sigma);
            assert!(a[0] >= 0.0 && a[0] <= 255.0);
        }
    }
    #[test] fn transparent_is_transparent() {
        let mut a = [0.0; 21]; let mut b = [0.0; 21];
        blur(&mut a, &mut b, 3, 7, 2.25);
        assert_eq!(a, [0.0; 21]);
    }
}

// Optional sample runtime entry points; retain the standalone benchmark ABI.
#[no_mangle]
pub extern "system" fn Java_top_ghhccghk_accompanist_folia_sample_NativeMaskBridge_version(
    _env: *mut JNIEnv, _class: jclass,
) -> jint { 1 }

#[no_mangle]
pub unsafe extern "system" fn Java_top_ghhccghk_accompanist_folia_sample_NativeMaskBridge_blur(
    env: *mut JNIEnv, class: jclass, source: jobject, a: jobject, b: jobject,
    sigmas: jobject, output: jobject, width: jint, height: jint, count: jint,
) -> jint {
    Java_top_ghhccghk_accompanist_lyrics_folia_NativeMaskExperiment_blur(
        env, class, source, a, b, sigmas, output, width, height, count)
}
