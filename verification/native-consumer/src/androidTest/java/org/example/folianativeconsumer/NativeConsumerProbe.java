package org.example.folianativeconsumer;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.util.concurrent.atomic.AtomicReference;

/** Runs against the minified release APK, not a source/project dependency. */
public final class NativeConsumerProbe extends Instrumentation {
    @Override public void onCreate(Bundle arguments) { super.onCreate(arguments); start(); }
    @Override public void onStart() {
        Bundle result = new Bundle();
        Activity activity = null;
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN).setClassName(getTargetContext(), "org.example.folianativeconsumer.ConsumerActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity = startActivitySync(intent);
            final Activity target = activity;
            String state = "";
            long deadline = SystemClock.uptimeMillis() + 30000;
            while (SystemClock.uptimeMillis() < deadline) {
                AtomicReference<String> text = new AtomicReference<>("");
                runOnMainSync(() -> text.set(textOf(target.getWindow().getDecorView())));
                state = text.get();
                if (state.contains("FAILED") || state.contains("Failed:")) throw new AssertionError(state);
                if (state.contains("ABI=1 SurfaceReady")) break;
                SystemClock.sleep(50);
            }
            if (!state.contains("ABI=1 SurfaceReady")) throw new AssertionError("No actual native present: " + state);
            result.putString("stream", "PASS release/R8 packaged AAR ABI=1 and Vulkan TextureView submit/present\n");
            finish(Activity.RESULT_OK,result);
        } catch (Throwable error) {
            result.putString("stream", "FAIL " + error + "\n");
            finish(Activity.RESULT_CANCELED,result);
        } finally {
            if (activity != null) { final Activity target = activity; runOnMainSync(target::finish); }
        }
    }
    private String textOf(View view) {
        StringBuilder out = new StringBuilder();
        if (view instanceof TextView) out.append(((TextView)view).getText());
        if (view instanceof ViewGroup) {
            ViewGroup group=(ViewGroup)view;
            for (int i=0;i<group.getChildCount();i++) out.append(textOf(group.getChildAt(i)));
        }
        return out.toString();
    }
}
