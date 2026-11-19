package org.example.folianativeconsumer;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.TextView;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/** Public Android/AAR APIs against the actual minified coordinate consumer. */
public final class NativeLifecycleProbe extends Instrumentation {
    private Activity activity;
    private String availability;
    @Override public void onCreate(Bundle arguments){
        super.onCreate(arguments);
        availability=arguments.getString("availability");
        start();
    }
    private Intent intent(){return new Intent(Intent.ACTION_MAIN).setClassName(getTargetContext(),"org.example.folianativeconsumer.ConsumerActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);}
    private String state(){AtomicReference<String> text=new AtomicReference<>("");runOnMainSync(()->text.set(textOf(activity.getWindow().getDecorView())));return text.get();}
    private String await(String... tokens){
        long until=SystemClock.uptimeMillis()+30000;String current="";
        while(SystemClock.uptimeMillis()<until){current=state();boolean found=true;for(String token:tokens)found &= current.contains(token);if(found)return current;SystemClock.sleep(50);}
        throw new AssertionError("Expected "+Arrays.toString(tokens)+" but got "+current);
    }
    private void update(Intent request){runOnMainSync(()->getTargetContext().startActivity(request));}
    private List<TextureView> textures(){List<TextureView> views=new ArrayList<>();runOnMainSync(()->findTextures(activity.getWindow().getDecorView(),views));return views;}
    private long value(String state,String name){java.util.regex.Matcher match=Pattern.compile(name+"=(\\d+)").matcher(state);if(!match.find())throw new AssertionError("Missing "+name+": "+state);return Long.parseLong(match.group(1));}
    private void stable(){SystemClock.sleep(500);String paused=state();for(int i=0;i<10;i++){SystemClock.sleep(100);if(!paused.equals(state()))throw new AssertionError("Paused frame changed: "+paused+" -> "+state());}}
    private void pointer(long down, int action, float x, float y) {
        MotionEvent event = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action, x, y, 0);
        try { sendPointerSync(event); }
        finally { event.recycle(); }
    }
    private void touch(float x, float y, long hold) {
        long down = SystemClock.uptimeMillis();
        pointer(down, MotionEvent.ACTION_DOWN, x, y);
        SystemClock.sleep(hold);
        pointer(down, MotionEvent.ACTION_UP, x, y);
    }
    private void assertCancelled(String label, boolean drag) {
        long down = SystemClock.uptimeMillis();
        pointer(down, MotionEvent.ACTION_DOWN, 430, 326);
        if (drag) {
            pointer(down, MotionEvent.ACTION_MOVE, 430, 406);
            pointer(down, MotionEvent.ACTION_UP, 430, 406);
        } else {
            pointer(down, MotionEvent.ACTION_CANCEL, 430, 326);
        }
        SystemClock.sleep(ViewConfiguration.getLongPressTimeout() + 100);
        if (!label.equals(state())) throw new AssertionError("Cancelled gesture fired: " + state());
    }
    private void awaitWorkers(long expected) {
        long deadline = SystemClock.uptimeMillis() + 8000;
        while (SystemClock.uptimeMillis() < deadline) {
            long count = Thread.getAllStackTraces().keySet().stream()
                    .filter(thread -> thread.getName().equals("Folia renderer")).count();
            if (count == expected) return;
            SystemClock.sleep(50);
        }
        throw new AssertionError("Expected " + expected + " renderer workers after disposing views");
    }
    private void assertSourceAction(String text, long mediaMs) {
        long deadline = SystemClock.uptimeMillis() + 10000;
        while (SystemClock.uptimeMillis() < deadline) {
            AccessibilityNodeInfo line = findDescription(getUiAutomation().getRootInActiveWindow(), text);
            if (line != null && line.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                await("CLICK source=true media=" + mediaMs);
                return;
            }
            SystemClock.sleep(50);
        }
        throw new AssertionError("No Kotlin lyric action for " + text + ": " + state());
    }
    private void checkAvailability(String mode) {
        activity = startActivitySync(intent().putExtra("backend", "Kotlin").putExtra("mediaMs", 1500L));
        await("Kotlin Kotlin", "media=1500");
        assertSourceAction("Amber", 1500);
        if (mode.equals("present")) {
            update(intent().putExtra("backend", "NativeRequired").putExtra("mediaMs", 1500L));
            await("NativeGpu Ready:PartitaMainAndGlow", "sampled=1500000");
        } else if (mode.equals("absent")) {
            update(intent().putExtra("backend", "NativePreferred").putExtra("mediaMs", 1500L));
            await("Kotlin Fallback", "RuntimeUnavailable:", "media=1500");
            assertSourceAction("Amber", 1500);
            update(intent().putExtra("backend", "NativePreferred").putExtra("mediaMs", 3400L));
            assertSourceAction("Silver", 3400);
            update(intent().putExtra("backend", "NativeRequired").putExtra("mediaMs", 3400L));
            await("Failed Failed", "RuntimeUnavailable:", "media=3400");
            if (!textures().isEmpty()) throw new AssertionError("Unavailable runtime attached a surface");
        } else if (!mode.equals("kotlin")) {
            throw new AssertionError("Unknown availability mode: " + mode);
        }
    }
    private void checkSessionFault() throws ReflectiveOperationException {
        activity = startActivitySync(intent().putExtra("backend", "NativePreferred"));
        Class<?> jni = Class.forName("top.ghhccghk.accompanist.lyrics.folia.nativeandroid.RendererJni");
        var upload = jni.getMethod("upload", long.class, long.class, int.class, int.class,
                ByteBuffer.class, int.class, float.class, float.class, float.class);
        var destroy = jni.getMethod("destroy", long.class);
        for (String backend : new String[]{"NativePreferred", "NativeRequired"}) {
            update(intent().putExtra("backend", backend).putExtra("mediaMs", 1500L));
            await("NativeGpu Ready:PartitaMainAndGlow", "sampled=1500000");
            TextureView surface = textures().get(0);
            Field handle = surface.getClass().getDeclaredField("handle");
            handle.setAccessible(true);
            long id = handle.getLong(surface);
            if (id <= 0) throw new AssertionError("No live session");
            // The byte buffer is valid; a reserved zero texture ID fails inside
            // the session's upload operation. The normal worker detects the
            // retired slot on its next frame. No fault switch in the AAR.
            int uploadFailure = (int) upload.invoke(null, id, 0L, 1, 1, ByteBuffer.allocateDirect(4), 4, 0f, 0f, 1f);
            if (uploadFailure != -42) throw new AssertionError("Expected Upload:-42; got " + uploadFailure);
            update(intent().putExtra("backend", backend).putExtra("mediaMs", 1516L));
            boolean preferred = backend.equals("NativePreferred");
            await(preferred ? "Kotlin Fallback" : "Failed Failed", "Failed:Render:-4", "media=1516");
            if (preferred) assertSourceAction("Amber", 1516);
            awaitWorkers(0);
            if ((int) destroy.invoke(null, id) != 0) throw new AssertionError("Duplicate destroy failed");
        }
    }
    @Override public void onStart(){
        Bundle result=new Bundle();
        try{
            getUiAutomation(); // Enable accessibility before the composition is created.
            if (availability != null) {
                if (availability.equals("session-fault")) checkSessionFault();
                else checkAvailability(availability);
                result.putString("stream", "PASS API " + android.os.Build.VERSION.SDK_INT + " availability=" + availability + "\n");
                finish(Activity.RESULT_OK, result);
                return;
            }
            activity=startActivitySync(intent());
            String initial=await("ABI=2 NativeGpu Ready:PartitaMainAndGlow","sampled=1500000");stable();
            long uploads=value(initial,"uploads");
            update(intent().putExtra("mediaMs",850L));await("NativeGpu","sampled=850000");
            update(intent().putExtra("mediaMs",1500L));String restored=await("NativeGpu","sampled=1500000");
            if(value(restored,"uploads")!=uploads)throw new AssertionError("Seeking uploaded prepared textures again");
            update(intent().putExtra("mediaMs",1600L).putExtra("offset",100));await("NativeGpu","sampled=1500000","media=1600");
            update(intent().putExtra("mediaMs",1600L).putExtra("backend","NativePreferred").putExtra("theme","Classic"));
            await("Kotlin Fallback","UnsupportedTheme:","media=1600");
            if(!textures().isEmpty())throw new AssertionError("Fallback retained native view");
            update(intent().putExtra("mediaMs",1600L).putExtra("backend","NativeRequired").putExtra("theme","Classic"));
            await("Failed Failed","UnsupportedTheme:","media=1600");
            if(!textures().isEmpty())throw new AssertionError("Required failure retained native view");
            for(String backend:new String[]{"NativePreferred","NativeRequired"}){
                update(intent().putExtra("mediaMs",1600L).putExtra("backend",backend).putExtra("budget",1L));
                await(backend.equals("NativePreferred")?"Kotlin Fallback":"Failed Failed","PreparedTextureBudgetExceeded","media=1600");
            }
            update(intent().putExtra("mediaMs",1500L));await("NativeGpu","sampled=1500000");
            AccessibilityNodeInfo line=null;
            long accessibilityDeadline=SystemClock.uptimeMillis()+10000;
            while(line==null&&SystemClock.uptimeMillis()<accessibilityDeadline){
                line=findDescription(getUiAutomation().getRootInActiveWindow(),"Amber echo");
                if(line==null)line=findDescription(getUiAutomation().getRootInActiveWindow(),"Amber");
                if(line==null)SystemClock.sleep(50);
            }
            if(line==null||!line.performAction(AccessibilityNodeInfo.ACTION_CLICK))
                throw new AssertionError("Missing accessible line action: "+state());
            await("CLICK source=true");
            touch(10,590,50);await("BLANK media=1500");
            // Fixed 800x600 geometry. The second word spans this point in the
            // baseline fixture at 1500 ms. Use real MotionEvents for both actions.
            touch(430, 326, 50); await("CLICK source=true");
            touch(430, 326, ViewConfiguration.getLongPressTimeout() + 200);
            await("LONG source=true");
            touch(10, 590, 50);
            String blank = await("BLANK media=1500");
            assertCancelled(blank, true);
            assertCancelled(blank, false);
            update(intent().putExtra("mediaMs",1501L));await("sampled=1501000");
            update(intent().putExtra("mediaMs",1700L).putExtra("width",640).putExtra("height",480).putExtra("fontScale",1.5f).putExtra("reduced",true).putExtra("guides",false));
            await("NativeGpu","sampled=1700000");List<TextureView> resized=textures();
            if(resized.size()!=1)throw new AssertionError("Expected one surface");
            runOnMainSync(()->{if(resized.get(0).getWidth()!=640||resized.get(0).getHeight()!=480)throw new AssertionError("Resize not applied");});
            Instrumentation.ActivityMonitor monitor=addMonitor("org.example.folianativeconsumer.ConsumerActivity",null,false);
            runOnMainSync(()->activity.recreate());Activity recreated=monitor.waitForActivityWithTimeout(10000);removeMonitor(monitor);
            if(recreated==null||recreated==activity)throw new AssertionError("Activity did not recreate");
            activity=recreated;await("NativeGpu","sampled=1700000");stable();
            if(textures().get(0)==resized.get(0))throw new AssertionError("Recreation reused retired view");
            update(intent().putExtra("instances",2));await("slot=0 ABI=2 NativeGpu","slot=1 ABI=2 NativeGpu");
            if(textures().size()!=2)throw new AssertionError("Two independent surfaces missing");
            update(intent().putExtra("instances",1).putExtra("mediaMs",1502L));await("sampled=1502000");
            if(textures().size()!=1)throw new AssertionError("Removed instance still attached");
            update(intent().putExtra("active",false).putExtra("mediaMs",1503L));SystemClock.sleep(400);String inactive=state();SystemClock.sleep(500);
            if(!inactive.equals(state())||state().contains("sampled=1503000"))throw new AssertionError("Inactive surface submitted");
            update(intent().putExtra("active",true).putExtra("mediaMs",1503L));await("NativeGpu","sampled=1503000");stable();
            awaitWorkers(1);
            for (int i = 0; i < 3; i++) {
                update(intent().putExtra("backend", "Kotlin"));
                await("Kotlin Kotlin");
                awaitWorkers(0);
                long time = 1500L + i;
                update(intent().putExtra("mediaMs", time));
                await("NativeGpu", "sampled=" + time * 1000);
                awaitWorkers(1);
            }
            // A large surface used to exhaust GPU scratch at the overlapping line transition.
            long previousFrame = 0;
            for (long time : new long[]{3300, 3450, 3550, 850, 1500, 3300}) {
                update(intent().putExtra("width", 2160).putExtra("height", 2400).putExtra("mediaMs", time));
                String drawn = await("NativeGpu", "Ready:PartitaMainAndGlow", "sampled=" + time * 1000);
                long frame = value(drawn, "frame");
                if (frame <= previousFrame) throw new AssertionError("Large viewport frame did not advance: " + drawn);
                previousFrame = frame;
            }
            TextureView largeSurface = textures().get(0);
            runOnMainSync(() -> {
                if (largeSurface.getWidth() != 2160 || largeSurface.getHeight() != 2400) {
                    throw new AssertionError("Large viewport resize did not reach the native surface");
                }
            });
            result.putString("stream","PASS release/R8 AAR: Partita, paused/seek/offset, explicit capability/budget fallback and required failure, source semantics, tap/long press/drag/cancel/blank input, resize/font/reduced-motion, recreation, two instances, inactive/resume, repeated worker retirement, large viewport transitions\n");finish(Activity.RESULT_OK,result);
        }catch(Throwable error){result.putString("stream","FAIL "+android.util.Log.getStackTraceString(error)+"\n");finish(Activity.RESULT_CANCELED,result);}
        finally{if(activity!=null)runOnMainSync(()->activity.finish());}
    }
    private AccessibilityNodeInfo findDescription(AccessibilityNodeInfo node,String text){if(node==null)return null;CharSequence description=node.getContentDescription();if(description!=null&&description.toString().contains(text))return node;for(int i=0;i<node.getChildCount();i++){AccessibilityNodeInfo found=findDescription(node.getChild(i),text);if(found!=null)return found;}return null;}
    private static void findTextures(View v,List<TextureView> out){if(v instanceof TextureView)out.add((TextureView)v);if(v instanceof ViewGroup){ViewGroup group=(ViewGroup)v;for(int i=0;i<group.getChildCount();i++)findTextures(group.getChildAt(i),out);}}
    private String textOf(View view){StringBuilder out=new StringBuilder();if(view instanceof TextView)out.append(((TextView)view).getText());if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)out.append(textOf(group.getChildAt(i)));}return out.toString();}
}
