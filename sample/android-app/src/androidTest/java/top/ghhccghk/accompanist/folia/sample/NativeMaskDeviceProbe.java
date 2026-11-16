package top.ghhccghk.accompanist.folia.sample;

import android.app.Activity;
import android.app.Instrumentation;
import android.os.Build;
import android.os.Bundle;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

/** Explicit device JNI probe, no Activity, player, file picker or UI interaction. */
public final class NativeMaskDeviceProbe extends Instrumentation {
    private boolean expectMissing;

    @Override public void onCreate(Bundle args) {
        super.onCreate(args);
        expectMissing = args != null && "true".equals(args.getString("expectMissing"));
        start();
    }

    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            ClassLoader loader = getTargetContext().getClassLoader();
            Class<?> bridge = loader.loadClass("top.ghhccghk.accompanist.folia.sample.NativeMaskBridgeKt");
            Object load = bridge.getMethod("loadSampleNativeMask").invoke(null);
            String error = (String) load.getClass().getMethod("getError").invoke(load);
            Object processor = load.getClass().getMethod("getProcessor").invoke(load);
            if (expectMissing) {
                if (processor != null || error == null || !error.contains("UnsatisfiedLinkError")) {
                    throw new AssertionError("Expected actual missing-native diagnostic, got " + error);
                }
                result.putString("stream", "EXPECTED MISSING NATIVE: " + error + "\n");
            } else {
                if (processor == null || error != null) throw new AssertionError("Native startup: " + error);
                Method blur = processor.getClass().getMethod("blur", int[].class, int.class, int.class, float[].class);
                blur.setAccessible(true);
                int cases = 0;
                float[] sigmas = {1f, 2.25f, 10f, 20f, 4096f};
                for (int[] size : new int[][]{{1, 1}, {3, 7}, {13, 5}}) {
                    int w = size[0], h = size[1];
                    int[] source = new int[w * h];
                    for (int i = 0; i < source.length; i++) source[i] = ((i * 73 + 255) & 255) << 24;
                    java.util.List<?> masks = (java.util.List<?>) blur.invoke(processor, source, w, h, sigmas);
                    if (masks == null) throw new AssertionError("Kernel fell back instead of using Rust");
                    for (int s = 0; s < sigmas.length; s++) {
                        int[] expected = reference(source, w, h, sigmas[s]);
                        if (!Arrays.equals(expected, (int[]) masks.get(s))) {
                            throw new AssertionError("ARGB mismatch " + w + "x" + h + " sigma=" + sigmas[s]);
                        }
                        cases++;
                    }
                }
                Method release = processor.getClass().getMethod("release");
                release.setAccessible(true);
                release.invoke(processor);
                result.putString("stream", "PASS: real sample loader + ABI + startup self-test + " + cases +
                    " ARGB cases vs independent bounded convolution + release; ABI=" + Arrays.toString(Build.SUPPORTED_ABIS) +
                    "; API=" + Build.VERSION.SDK_INT + ". No UI/frame-performance claim.\n");
            }
            result.putString("status", "PASS");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable failure) {
            Throwable cause = failure instanceof InvocationTargetException ? failure.getCause() : failure;
            result.putString("stream", "FAIL: " + cause + "\n");
            result.putString("status", "FAIL");
            finish(Activity.RESULT_CANCELED, result);
        }
    }

    // Deliberately naive finite convolution, independent of the rolling Rust sum.
    private static int[] reference(int[] pixels, int width, int height, float sigma) {
        int window = Math.max(1, (int) Math.floor((sigma * 3f) * Math.sqrt(2 * Math.PI) / 4 + .5));
        int[] windows = {window, window, window + (window % 2 == 0 ? 1 : 0)};
        double[] input = new double[pixels.length];
        for (int i = 0; i < pixels.length; i++) input[i] = pixels[i] >>> 24;
        for (boolean horizontal : new boolean[]{true, false}) {
            for (int pass = 0; pass < 3; pass++) {
                int n = windows[pass], left = n / 2 + (n % 2 == 0 && pass == 1 ? -1 : 0), right = n - left - 1;
                double[] output = new double[input.length];
                for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
                    if (horizontal) {
                        for (int q = Math.max(0, x - left); q <= Math.min(width - 1, x + right); q++) output[y * width + x] += input[y * width + q];
                    } else {
                        for (int q = Math.max(0, y - left); q <= Math.min(height - 1, y + right); q++) output[y * width + x] += input[q * width + x];
                    }
                }
                input = output;
            }
            double divisor = (double) windows[0] * windows[1] * windows[2];
            for (int i = 0; i < input.length; i++) input[i] = Math.min(255, Math.max(0, Math.floor(input[i] / divisor + .5)));
        }
        int[] output = new int[input.length];
        for (int i = 0; i < output.length; i++) output[i] = ((int) input[i] << 24) | 0xffffff;
        return output;
    }
}
