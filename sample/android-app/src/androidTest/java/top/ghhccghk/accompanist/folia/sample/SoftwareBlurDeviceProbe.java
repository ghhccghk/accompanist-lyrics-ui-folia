package top.ghhccghk.accompanist.folia.sample;

import android.graphics.Bitmap;
import android.os.Build;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.security.MessageDigest;

/** app_process entry point: profiles a supplied APK in a separate process, without
 * installing it, stopping playback, starting an Activity, or reading user media. */
public final class SoftwareBlurDeviceProbe {
    public static void main(String[] args) {
        try { run(); }
        catch (Throwable failure) { failure.printStackTrace(); System.exit(1); }
    }

    private static void run() throws Exception {
        String base = "top.ghhccghk.accompanist.lyrics.folia.internal.rendering.";
        Class<?> imageType = Class.forName("androidx.compose.ui.graphics.ImageBitmap");
        Method bitmap = Class.forName(base + "Bitmap_androidKt").getMethod(
            "bitmapFromArgb", int.class, int.class, int[].class);
        Method blur = Class.forName(base + "SoftwareBlurKt").getMethod(
            "blurPremultiplied", imageType, float.class);
        Method unwrap = Class.forName("androidx.compose.ui.graphics.AndroidImageBitmap_androidKt")
            .getMethod("asAndroidBitmap", imageType);
        System.out.println("CPU bitmap fallback only; no UI, audio, GPU, or frame-rate claim. API=" +
            Build.VERSION.SDK_INT + "; ABI=" + Build.SUPPORTED_ABIS[0]);
        for (int[] size : new int[][]{{97, 43}, {1200, 500}}) {
            int w = size[0], h = size[1];
            int[] source = new int[w * h];
            for (int y = h / 3; y < h * 2 / 3; y++) {
                for (int x = w / 4; x < w * 3 / 4; x++) {
                    source[y * w + x] = (64 + (x * 13 + y * 73) % 192) << 24 |
                        ((x * 47) & 255) << 16 | ((y * 71) & 255) << 8 | ((x + y) & 255);
                }
            }
            Object input = bitmap.invoke(null, w, h, source);
            for (float sigma : new float[]{.25f, .5f, 2f, 6f}) {
                Object result = null;
                for (int i = 0; i < 8; i++) result = blur.invoke(null, input, sigma);
                double[] ms = new double[15];
                for (int i = 0; i < ms.length; i++) {
                    long start = System.nanoTime();
                    result = blur.invoke(null, input, sigma);
                    ms[i] = (System.nanoTime() - start) / 1e6;
                }
                Bitmap output = (Bitmap) unwrap.invoke(null, result.getClass().getMethod("getImage").invoke(result));
                int ow = output.getWidth(), oh = output.getHeight();
                int[] pixels = new int[ow * oh];
                output.getPixels(pixels, 0, ow, 0, 0, ow, oh);
                Object origin = null;
                for (Method m : result.getClass().getMethods()) {
                    if (m.getName().startsWith("getOrigin-")) origin = m.invoke(result);
                }
                if (origin == null) throw new AssertionError("Missing output origin");
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                for (int pixel : pixels) {
                    digest.update((byte) (pixel >>> 24)); digest.update((byte) (pixel >>> 16));
                    digest.update((byte) (pixel >>> 8)); digest.update((byte) pixel);
                }
                StringBuilder sha256 = new StringBuilder();
                for (byte b : digest.digest()) sha256.append(String.format("%02x", b & 255));
                Arrays.sort(ms);
                System.out.println(w + "x" + h + " sigma=" + sigma + " output=" + ow + "x" + oh +
                    " origin=" + origin + " argbHash=" + Arrays.hashCode(pixels) +
                    " sha256=" + sha256 +
                    " p50Ms=" + ms[7] + " p95Ms=" + ms[14]);
            }
        }
    }
}
