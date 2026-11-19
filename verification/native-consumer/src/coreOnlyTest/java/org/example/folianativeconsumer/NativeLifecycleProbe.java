package org.example.folianativeconsumer;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.util.concurrent.atomic.AtomicBoolean;

/** Runtime smoke check for the standalone Kotlin artifact in a minified APK. */
public final class NativeLifecycleProbe extends Instrumentation {
    @Override public void onCreate(Bundle arguments) { super.onCreate(arguments); start(); }
    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN).setClassName(getTargetContext(),
                    "org.example.folianativeconsumer.ConsumerActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Activity activity = startActivitySync(intent);
            AtomicBoolean ready = new AtomicBoolean();
            long deadline = SystemClock.uptimeMillis() + 15000;
            do {
                runOnMainSync(() -> ready.set(hasLyrics(activity.getWindow().getDecorView())));
                if (!ready.get()) SystemClock.sleep(50);
            } while (!ready.get() && SystemClock.uptimeMillis() < deadline);
            runOnMainSync(activity::finish);
            if (!ready.get()) throw new AssertionError("No main lyric from core Maven artifact");
            result.putString("stream", "PASS release/R8 Kotlin-only Android consumer\n");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable error) {
            result.putString("stream", "FAIL " + android.util.Log.getStackTraceString(error));
            finish(Activity.RESULT_CANCELED, result);
        }
    }
    private boolean hasLyrics(View view) {
        if (view instanceof TextView && ((TextView) view).getText().toString().equals("CoreLyricsReady")) return true;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) if (hasLyrics(group.getChildAt(i))) return true;
        }
        return false;
    }
}
