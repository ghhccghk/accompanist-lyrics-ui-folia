package top.ghhccghk.accompanist.lyrics.folia;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Random;
import java.security.MessageDigest;

/** Same probe runs on JVM and Android app_process. Synthetic pixels only.
 * Reflection invokes the actual optimized Kotlin baseline, not an older copy.
 * Native timing includes crop/readback, JNI, transfers and output bitmap creation.
 * Does not install an app, open a UI, or access the user's media. */
public final class NativeColorExperiment {
    public static native int convolve(ByteBuffer source, ByteBuffer a, ByteBuffer b,
        ByteBuffer alpha, ByteBuffer kernel, ByteBuffer output, int width, int height, int radius);
    private static Method bitmap, blur, read, getWidth, getHeight, getImage, getOrigin;
    private static Workspace workspace;
    private static Object sampleProcessor;
    private static Method sampleConvolve;
    private static long checksum;
    private static int parityCases;

    private static ByteBuffer buffer(int bytes) {
        return ByteBuffer.allocateDirect(bytes).order(ByteOrder.nativeOrder());
    }
    private static final class Workspace {
        final int n;
        final ByteBuffer source, a, b, alpha, kernel, output;
        Workspace(int n) {
            this.n = n; source = buffer(n*4); a = buffer(n*8); b = buffer(n*8);
            alpha = buffer(n*8); kernel = buffer(13*8); output = buffer(n*4);
        }
    }
    private static Object image(int w, int h, int[] pixels) throws Exception {
        return bitmap.invoke(null, w, h, pixels);
    }
    private static int width(Object image) throws Exception { return (Integer)getWidth.invoke(image); }
    private static int height(Object image) throws Exception { return (Integer)getHeight.invoke(image); }
    private static int[] pixels(Object image) throws Exception {
        int w=width(image), h=height(image); int[] values = new int[w*h];
        read.invoke(image, values, 0, 0, w, h, 0, w); return values;
    }
    private static final class Result {
        final Object image; final int left, top;
        Result(Object image, int left, int top) { this.image=image; this.left=left; this.top=top; }
    }
    private static Result kotlin(Object source, float sigma) throws Exception {
        Object result = blur.invoke(null, source, sigma);
        // Offset is an inline class: its JVM getter returns packed float bits.
        long origin = (Long)getOrigin.invoke(result);
        return new Result(getImage.invoke(result), (int)Float.intBitsToFloat((int)(origin>>>32)),
            (int)Float.intBitsToFloat((int)origin));
    }
    private static Result rust(Object sourceImage, float sigma, boolean cold) throws Exception {
        int w=width(sourceImage), h=height(sourceImage); int[] full=pixels(sourceImage);
        int left=w, right=-1, top=h, bottom=-1;
        for(int y=0;y<h;y++) for(int x=0;x<w;x++) if(full[y*w+x]>>>24 != 0) {
            left=Math.min(left,x); right=Math.max(right,x); top=Math.min(top,y); bottom=Math.max(bottom,y);
        }
        if(right<left) return new Result(image(1,1,new int[]{0}),0,0);
        int radius=Math.max(1,(int)Math.ceil(3*sigma));
        left=Math.max(0,left-radius); right=Math.min(w-1,right+radius);
        top=Math.max(0,top-radius); bottom=Math.min(h-1,bottom+radius);
        int cw=right-left+1, ch=bottom-top+1, n=cw*ch;
        int[] cropped=full;
        if(cw!=w || ch!=h) {
            cropped=new int[n];
            for(int y=0;y<ch;y++) System.arraycopy(full,(top+y)*w+left,cropped,y*cw,cw);
        }
        double[] weights=new double[2*radius+1]; double norm=0;
        for(int i=0;i<weights.length;i++) {
            weights[i]=Math.exp(-.5*Math.pow(i-radius,2)/(sigma*sigma)); norm+=weights[i];
        }
        for(int i=0;i<weights.length;i++) weights[i]/=norm;
        if(sampleProcessor!=null) {
            int[] output=(int[])sampleConvolve.invoke(sampleProcessor,cropped,cw,ch,weights);
            if(output==null) throw new AssertionError("Sample processor unexpectedly fell back");
            return new Result(image(cw,ch,output),left,top);
        }
        Workspace work;
        if(cold) work=new Workspace(n);
        else {
            if(workspace==null || workspace.n<n) workspace=new Workspace(n);
            work=workspace;
        }
        work.source.asIntBuffer().put(cropped); work.kernel.asDoubleBuffer().put(weights);
        int status=convolve(work.source,work.a,work.b,work.alpha,work.kernel,work.output,cw,ch,radius);
        if(status!=0) throw new AssertionError("Native status="+status);
        int[] output=new int[n]; work.output.asIntBuffer().get(output);
        return new Result(image(cw,ch,output),left,top);
    }
    private static String sha(int[] values) throws Exception {
        MessageDigest digest=MessageDigest.getInstance("SHA-256");
        for(int p:values) { digest.update((byte)(p>>>24));digest.update((byte)(p>>>16));
            digest.update((byte)(p>>>8));digest.update((byte)p); }
        StringBuilder result=new StringBuilder(); for(byte b:digest.digest()) result.append(String.format("%02x",b&255));
        return result.toString();
    }
    private static void compare(Result expected, Result actual, String context) throws Exception {
        if(expected.left!=actual.left || expected.top!=actual.top || width(expected.image)!=width(actual.image) ||
            height(expected.image)!=height(actual.image) || !Arrays.equals(pixels(expected.image),pixels(actual.image)))
            throw new AssertionError("Pixel/geometry mismatch: "+context);
        parityCases++;
    }
    private static Object synthetic(int w,int h,String mode,boolean padded) throws Exception {
        Random random=new Random(20261116); int[] p=new int[w*h];
        for(int y=0;y<h;y++) for(int x=0;x<w;x++) {
            if(padded && (x<w/4 || x>=w*3/4 || y<h/3 || y>=h*2/3)) continue;
            int rgb=mode.equals("white") ? 0xffffff : mode.equals("neutral") ? random.nextInt(256)*0x010101 : random.nextInt(0x1000000);
            // Include invisible coloured pixels and alpha 1/2/127/128/254/255.
            int a=new int[]{0,1,2,127,128,254,255}[(x+y)%7];
            p[y*w+x]=(a<<24)|rgb;
        }
        return image(w,h,p);
    }
    private static void invalidArguments() {
        Workspace w=new Workspace(3);
        w.kernel.asDoubleBuffer().put(new double[]{0,1,0});
        check(convolve(w.source,w.a,w.b,w.alpha,w.kernel,w.output,3,1,1),0);
        check(convolve(w.source,w.a,w.b,w.alpha,w.kernel,w.source,3,1,1),2);
        check(convolve(w.source,w.a,w.a,w.alpha,w.kernel,w.output,3,1,1),2);
        check(convolve(w.source,w.a,w.b,w.alpha,w.kernel,w.output,0,1,1),1);
        check(convolve(w.source,w.a,w.b,w.alpha,w.kernel,w.output,Integer.MAX_VALUE,1,1),1);
        check(convolve(w.source,w.a,w.b,w.alpha,w.kernel,w.output,3,1,0),1);
        check(convolve(w.source,w.a,w.b,w.alpha,w.kernel,w.output,3,1,7),1);
        check(convolve(ByteBuffer.allocate(12),w.a,w.b,w.alpha,w.kernel,w.output,3,1,1),1);
        check(convolve(w.source,w.a,w.b,w.alpha,w.kernel,buffer(4),3,1,1),1);
        check(convolve(w.source,w.a.asReadOnlyBuffer(),w.b,w.alpha,w.kernel,w.output,3,1,1),1);
        check(convolve(w.source,w.a,w.b.asReadOnlyBuffer(),w.alpha,w.kernel,w.output,3,1,1),1);
        check(convolve(w.source,w.a,w.b,w.alpha.asReadOnlyBuffer(),w.kernel,w.output,3,1,1),1);
        check(convolve(w.source,w.a,w.b,w.alpha,w.kernel,w.output.asReadOnlyBuffer(),3,1,1),1);
        ByteBuffer unaligned=buffer(25);unaligned.position(1);unaligned=unaligned.slice();
        check(convolve(w.source,unaligned,w.b,w.alpha,w.kernel,w.output,3,1,1),1);
        check(convolve(w.source,w.a,w.b,w.alpha,w.kernel.asReadOnlyBuffer(),w.output,3,1,1),0);
        check(convolve(null,w.a,w.b,w.alpha,w.kernel,w.output,3,1,1),1);
        for(double invalid:new double[]{Double.NaN,Double.POSITIVE_INFINITY,-1,2,.5}) {
            w.kernel.putDouble(8,invalid);
            check(convolve(w.source,w.a,w.b,w.alpha,w.kernel,w.output,3,1,1),1);
        }
    }
    private static void check(int actual,int expected) { if(actual!=expected) throw new AssertionError(actual+" != "+expected); }
    public static void main(String[] args) throws Exception {
        if(args.length<2) throw new IllegalArgumentException("absolute library path; android|jvm; optional parity-only|sample");
        System.load(new File(args[0]).getCanonicalPath());
        String base="top.ghhccghk.accompanist.lyrics.folia.internal.rendering.";
        Class<?> imageType=Class.forName("androidx.compose.ui.graphics.ImageBitmap");
        bitmap=Class.forName(base+(args[1].equals("android") ? "Bitmap_androidKt" : "Bitmap_jvmKt"))
            .getMethod("bitmapFromArgb",int.class,int.class,int[].class);
        blur=Class.forName(base+"SoftwareBlurKt").getMethod("blurPremultiplied",imageType,float.class);
        getWidth=imageType.getMethod("getWidth");getHeight=imageType.getMethod("getHeight");
        read=imageType.getMethod("readPixels",int[].class,int.class,int.class,int.class,int.class,int.class,int.class);
        Class<?> result=Class.forName(base+"BlurredPixels");getImage=result.getMethod("getImage");
        for(Method m:result.getMethods()) if(m.getName().startsWith("getOrigin-")) getOrigin=m;
        if(getOrigin==null) throw new AssertionError("No inline origin accessor");
        System.out.println("environment,"+System.getProperty("java.version")+","+System.getProperty("os.name")+","+System.getProperty("os.arch"));
        invalidArguments();
        if(args.length>2 && args[2].equals("sample")) {
            Class<?> processor=Class.forName("top.ghhccghk.accompanist.folia.sample.JniColourProcessor");
            sampleProcessor=processor.getConstructor().newInstance();
            sampleConvolve=processor.getMethod("convolve",int[].class,int.class,int.class,double[].class);
            Class<?> bridge=Class.forName("top.ghhccghk.accompanist.folia.sample.NativeColourBridge");
            if(!bridge.getMethod("version").invoke(null).equals(1)) throw new AssertionError("Sample colour ABI");
        }
        for(int[] size:new int[][]{{1,1},{1,9},{9,1},{3,7},{97,43},{320,160}})
            for(String mode:new String[]{"white","neutral","colour"}) for(boolean padded:new boolean[]{false,true}) {
                Object source=synthetic(size[0],size[1],mode,padded);
                for(float sigma:new float[]{.02f,.25f,.5f,.6f,1f,1.99f})
                    compare(kotlin(source,sigma),rust(source,sigma,false),Arrays.toString(size)+mode+sigma+padded);
            }
        System.out.println("parityCases,"+parityCases+",ABI-validation,passed");
        if(sampleProcessor!=null) {
            int[] limitInput=new int[1024*1024]; // Rejected before any native workspace allocation.
            if(sampleConvolve.invoke(sampleProcessor,limitInput,1024,1024,new double[]{0,1,0})!=null)
                throw new AssertionError("32MiB budget must fall back");
            Object stats=sampleProcessor.getClass().getMethod("stats").invoke(sampleProcessor);
            String error=(String)stats.getClass().getMethod("getError").invoke(stats);
            if(error==null || !error.contains("32 MiB")) throw new AssertionError("Missing budget error: "+error);
            sampleProcessor.getClass().getMethod("release").invoke(sampleProcessor);
            System.out.println("sample-bridge,216 exact pixels/geometry cases,ABI and budget fallback and release passed; no UI or player claim");
            return;
        }
        if(args.length>2 && args[2].equals("parity-only")) return;
        System.out.println("width,height,mode,sigma,path,p50Ms,p95Ms,retainedDirectBytes,sha256");
        for(int[] size:new int[][]{{97,43},{640,320},{1200,500}}) for(String mode:new String[]{"white","neutral","colour"}) {
            Object source=synthetic(size[0],size[1],mode,true);
            for(float sigma:new float[]{.25f,.5f,1f,1.99f}) {
                workspace=null; int warmups=12, samples=25;
                for(int i=0;i<warmups;i++) { kotlin(source,sigma);rust(source,sigma,false); }
                compare(kotlin(source,sigma),rust(source,sigma,false),"benchmark");
                double[][] times=new double[2][samples]; Result[] results=new Result[2];
                for(int i=0;i<samples;i++) for(int j=0;j<2;j++) {
                    int path=(i%2==0)?j:1-j;long start=System.nanoTime();
                    results[path]=path==0?kotlin(source,sigma):rust(source,sigma,false);
                    times[path][i]=(System.nanoTime()-start)/1e6;
                    checksum+=width(results[path].image);
                }
                compare(results[0],results[1],"timed");
                for(int path=0;path<2;path++) {
                    Arrays.sort(times[path]);
                    System.out.println(size[0]+","+size[1]+","+mode+","+sigma+","+(path==0?"Kotlin":"Rust-JNI-warm")+","+
                        times[path][12]+","+times[path][23]+","+(path==0?0:workspace.n*32+104)+","+sha(pixels(results[path].image)));
                }
                // First-use workspace allocation is visible separately, not hidden in warm timings.
                double[] cold=new double[5]; Result c=null;
                for(int i=0;i<cold.length;i++) { long start=System.nanoTime();c=rust(source,sigma,true);cold[i]=(System.nanoTime()-start)/1e6; }
                compare(results[0],c,"cold");Arrays.sort(cold);
                System.out.println(size[0]+","+size[1]+","+mode+","+sigma+",Rust-JNI-cold,"+cold[2]+","+cold[4]+",0,"+sha(pixels(c.image)));
            }
        }
        System.out.println("finalParityCases,"+parityCases+",checksum,"+checksum);
    }
}
