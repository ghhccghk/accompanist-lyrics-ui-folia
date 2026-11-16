//! Small-sigma premultiplied colour experiment. Kotlin supplies the normalized
//! Gaussian coefficients, so platform libm cannot change kernel generation.
//! Original implementation of the production SoftwareBlur.kt arithmetic order.
use jni_sys::{jclass, jint, jobject, JNIEnv};
use std::{mem::align_of, panic::catch_unwind};

fn convolve(pixels: &[u32], a: &mut [f64], b: &mut [f64], alpha: &mut [f64],
            kernel: &[f64], result: &mut [u32], width: usize, height: usize) {
    let radius = (kernel.len() / 2) as isize;
    let mut white = true;
    let mut neutral = true;
    for &pixel in pixels {
        if pixel >> 24 != 0 {
            if pixel & 0xffffff != 0xffffff { white = false; }
            if (pixel ^ (pixel >> 8)) & 0xffff != 0 { neutral = false; break; }
        }
    }
    result.fill(0);
    for channel in [24, 16, 8, 0] {
        for (i, &pixel) in pixels.iter().enumerate() {
            let coverage = (pixel >> 24) as f64;
            a[i] = if channel == 24 { coverage }
                else { ((pixel >> channel) & 255) as f64 * coverage / 255.0 };
        }
        // Accumulate adjacent pixels together. Each individual pixel still sees
        // coefficients from -radius to +radius, in the original Double order.
        // Safe equal-length slices let LLVM vectorize across pixels, without
        // reassociation, fast-math, hand-written SIMD or unchecked indexing.
        for y in 0..height {
            let base = y * width;
            b[base..base + width].fill(0.0);
            for d in -radius..=radius {
                let start = (-d).max(0) as usize;
                let end = (width as isize - d).min(width as isize).max(0) as usize;
                if start >= end { continue; }
                let from = (base as isize + start as isize + d) as usize;
                let input = &a[from..from + end - start];
                let output = &mut b[base + start..base + end];
                let weight = kernel[(d + radius) as usize];
                for (sum, &value) in output.iter_mut().zip(input) { *sum += value * weight; }
            }
        }
        for y in 0..height {
            let output = &mut a[y * width..(y + 1) * width];
            output.fill(0.0);
            for d in (-radius).max(-(y as isize))..=radius.min((height - 1 - y) as isize) {
                let from = (y as isize + d) as usize * width;
                let input = &b[from..from + width];
                let weight = kernel[(d + radius) as usize];
                for (sum, &value) in output.iter_mut().zip(input) { *sum += value * weight; }
            }
        }
        if white {
            for i in 0..pixels.len() {
                result[i] = (round(a[i]) << 24) | if a[i] > 0.0 { 0xffffff } else { 0 };
            }
            break;
        } else if neutral && channel == 16 {
            for i in 0..pixels.len() {
                let value = if alpha[i] > 0.0 { a[i] * 255.0 / alpha[i] } else { 0.0 };
                result[i] |= round(value) * 0x010101;
            }
            break;
        } else {
            for i in 0..pixels.len() {
                let value = if channel == 24 { alpha[i] = a[i]; a[i] }
                    else if alpha[i] > 0.0 { a[i] * 255.0 / alpha[i] } else { 0.0 };
                result[i] |= round(value) << channel;
            }
        }
    }
}

fn round(value: f64) -> u32 { (value + 0.5).floor().clamp(0.0, 255.0) as u32 }

/// Probe-only JNI ABI. All buffers are native-order ByteBuffers. No retained
/// addresses, native allocations, threads, or renderer state. Status codes match
/// the mask experiment: 0 success, 1 invalid input, 2 overlap, 3 caught panic.
#[no_mangle]
pub unsafe extern "system" fn Java_top_ghhccghk_accompanist_lyrics_folia_NativeColorExperiment_convolve(
    env: *mut JNIEnv, _class: jclass, source: jobject, scratch_a: jobject,
    scratch_b: jobject, alpha: jobject, kernel: jobject, output: jobject,
    width: jint, height: jint, radius: jint,
) -> jint {
    catch_unwind(|| {
        if env.is_null() || width <= 0 || height <= 0 || !(1..=6).contains(&radius) { return 1; }
        let Some(n) = (width as usize).checked_mul(height as usize) else { return 1; };
        // 32 MiB total workspace ceiling, including source/result and kernel.
        let k = radius as usize * 2 + 1;
        if n > (32 * 1024 * 1024 - k * 8) / 32 { return 1; }
        let table = &**env;
        let (Some(address), Some(capacity), Some(find), Some(method), Some(call), Some(delete), Some(exception)) =
            (table.GetDirectBufferAddress, table.GetDirectBufferCapacity, table.FindClass,
             table.GetMethodID, table.CallBooleanMethodA, table.DeleteLocalRef, table.ExceptionCheck)
            else { return 1; };
        let class = find(env, b"java/nio/ByteBuffer\0".as_ptr().cast());
        if class.is_null() { return 1; }
        let readonly = method(env, class, b"isReadOnly\0".as_ptr().cast(), b"()Z\0".as_ptr().cast());
        if readonly.is_null() { delete(env, class); return 1; }
        let objects = [source, scratch_a, scratch_b, alpha, kernel, output];
        // Also reject non-ByteBuffer jobjects before calling its method: typed
        // FloatBuffer/DoubleBuffer capacities are elements rather than bytes.
        let Some(instance) = table.IsInstanceOf else { delete(env, class); return 1; };
        for (i, &buffer) in objects.iter().enumerate() {
            if buffer.is_null() || instance(env, buffer, class) == 0 {
                delete(env, class); return 1;
            }
            if [1, 2, 3, 5].contains(&i) && call(env, buffer, readonly, std::ptr::null()) != 0 {
                delete(env, class); return 1;
            }
            if exception(env) != 0 { delete(env, class); return 1; }
        }
        delete(env, class);
        let sizes = [n * 4, n * 8, n * 8, n * 8, k * 8, n * 4];
        let alignments = [align_of::<u32>(), align_of::<f64>(), align_of::<f64>(),
                          align_of::<f64>(), align_of::<f64>(), align_of::<u32>()];
        let mut pointers = [std::ptr::null_mut(); 6];
        for i in 0..6 {
            pointers[i] = address(env, objects[i]);
            if pointers[i].is_null() || capacity(env, objects[i]) < sizes[i] as i64 ||
                pointers[i] as usize % alignments[i] != 0 ||
                (pointers[i] as usize).checked_add(sizes[i]).is_none() { return 1; }
        }
        for i in 0..6 { for j in 0..i {
            let (a, b) = (pointers[i] as usize, pointers[j] as usize);
            if a < b + sizes[j] && b < a + sizes[i] { return 2; }
        }}
        let weights = std::slice::from_raw_parts(pointers[4].cast::<f64>(), k);
        if weights.iter().any(|v| !v.is_finite() || *v < 0.0 || *v > 1.0) ||
            (weights.iter().sum::<f64>() - 1.0).abs() > 1e-12 { return 1; }
        convolve(std::slice::from_raw_parts(pointers[0].cast::<u32>(), n),
            std::slice::from_raw_parts_mut(pointers[1].cast::<f64>(), n),
            std::slice::from_raw_parts_mut(pointers[2].cast::<f64>(), n),
            std::slice::from_raw_parts_mut(pointers[3].cast::<f64>(), n), weights,
            std::slice::from_raw_parts_mut(pointers[5].cast::<u32>(), n), width as usize, height as usize);
        0
    }).unwrap_or(3)
}
