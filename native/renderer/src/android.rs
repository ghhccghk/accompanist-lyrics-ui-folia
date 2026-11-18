// SPDX-License-Identifier: AGPL-3.0-only
use crate::{dimensions, Sessions, ABI_VERSION};
use jni_sys::{jclass, jint, jlong, jobject, JNIEnv};
use raw_window_handle::{AndroidNdkWindowHandle, DisplayHandle, HandleError, HasDisplayHandle, HasWindowHandle, RawWindowHandle, WindowHandle};
use std::{ffi::c_void, panic::{catch_unwind, AssertUnwindSafe}, ptr::NonNull, sync::{Mutex, OnceLock}};

#[link(name = "android")]
extern "C" {
    fn ANativeWindow_fromSurface(env: *mut JNIEnv, surface: jobject) -> *mut c_void;
    fn ANativeWindow_release(window: *mut c_void);
}
struct Window(usize);
impl Drop for Window { fn drop(&mut self) { unsafe { ANativeWindow_release(self.0 as *mut c_void); } } }
impl HasWindowHandle for Window {
    fn window_handle(&self) -> Result<WindowHandle<'_>, HandleError> {
        let p = NonNull::new(self.0 as *mut c_void).ok_or(HandleError::Unavailable)?;
        Ok(unsafe { WindowHandle::borrow_raw(RawWindowHandle::AndroidNdk(AndroidNdkWindowHandle::new(p))) })
    }
}
impl HasDisplayHandle for Window {
    fn display_handle(&self) -> Result<DisplayHandle<'_>, HandleError> { Ok(DisplayHandle::android()) }
}

struct Session {
    surface: wgpu::Surface<'static>,
    device: wgpu::Device,
    queue: wgpu::Queue,
    config: wgpu::SurfaceConfiguration,
    errors: std::sync::Arc<std::sync::atomic::AtomicBool>,
    lost: std::sync::Arc<std::sync::atomic::AtomicBool>,
    recoveries: u32,
}
impl Session {
    async fn new(window: Window, width: u32, height: u32) -> Result<Self, i32> {
        let instance = wgpu::Instance::new(&wgpu::InstanceDescriptor { backends: wgpu::Backends::VULKAN, ..Default::default() });
        let surface = instance.create_surface(window).map_err(|_| -10)?;
        let adapter = instance.request_adapter(&wgpu::RequestAdapterOptions { compatible_surface: Some(&surface), ..Default::default() }).await.map_err(|_| -11)?;
        let (device, queue) = adapter.request_device(&wgpu::DeviceDescriptor { required_limits: wgpu::Limits::downlevel_defaults().using_resolution(adapter.limits()), ..Default::default() }).await.map_err(|_| -12)?;
        let caps = surface.get_capabilities(&adapter);
        // Match Android/Compose encoded colour blending. Never silently select
        // an sRGB render attachment with a different blend transfer function.
        let format = caps.formats.iter().copied().find(|f| !f.is_srgb()).ok_or(-13)?;
        // Android Vulkan commonly exposes INHERIT only. A nonopaque TextureView
        // supplies the premultiplied Android compositor contract in that case.
        let alpha_mode = [wgpu::CompositeAlphaMode::PreMultiplied, wgpu::CompositeAlphaMode::Inherit].into_iter().find(|a| caps.alpha_modes.contains(a)).ok_or(-14)?;
        let mut config = surface.get_default_config(&adapter, width, height).ok_or(-13)?;
        config.format = format; config.alpha_mode = alpha_mode;
        config.present_mode = wgpu::PresentMode::Fifo;
        config.desired_maximum_frame_latency = 2;
        let errors = std::sync::Arc::new(std::sync::atomic::AtomicBool::new(false));
        let lost = std::sync::Arc::new(std::sync::atomic::AtomicBool::new(false));
        let e = errors.clone(); device.on_uncaptured_error(std::sync::Arc::new(move |_| { e.store(true, std::sync::atomic::Ordering::Release); }));
        let l = lost.clone(); device.set_device_lost_callback(move |_,_| { l.store(true,std::sync::atomic::Ordering::Release); });
        surface.configure(&device,&config);
        Ok(Self { surface, device, queue, config, errors, lost, recoveries: 0 })
    }
    fn frame(&mut self, width: u32, height: u32) -> Result<i32,i32> {
        use std::sync::atomic::Ordering::Acquire;
        if self.errors.load(Acquire) || self.lost.load(Acquire) { return Err(-20); }
        if self.config.width != width || self.config.height != height {
            self.config.width=width; self.config.height=height;
            self.surface.configure(&self.device,&self.config);
        }
        let frame = match self.surface.get_current_texture() {
            Ok(f) => f,
            Err(wgpu::SurfaceError::Timeout) => { self.recoveries+=1; return if self.recoveries<=3 {Ok(1)} else {Err(-21)}; },
            Err(wgpu::SurfaceError::Lost | wgpu::SurfaceError::Outdated) => {
                self.recoveries+=1; if self.recoveries>3 {return Err(-22);}
                self.surface.configure(&self.device,&self.config); return Ok(1);
            },
            Err(_) => return Err(-23),
        };
        let view = frame.texture.create_view(&Default::default());
        let mut encoder = self.device.create_command_encoder(&Default::default());
        { let _pass = encoder.begin_render_pass(&wgpu::RenderPassDescriptor {
            color_attachments: &[Some(wgpu::RenderPassColorAttachment { view:&view, resolve_target:None, depth_slice:None,
                ops:wgpu::Operations {load:wgpu::LoadOp::Clear(wgpu::Color::TRANSPARENT),store:wgpu::StoreOp::Store} })],
            ..Default::default()
        }); }
        self.queue.submit(Some(encoder.finish())); frame.present(); self.recoveries=0; Ok(0)
    }
}
fn registry() -> &'static Mutex<Sessions<Session>> {
    static S: OnceLock<Mutex<Sessions<Session>>> = OnceLock::new();
    S.get_or_init(|| Mutex::new(Sessions { next:0, values:Default::default() }))
}
fn ffi(f: impl FnOnce() -> Result<i64,i32>) -> i64 {
    // Covers unwind panics only, not abort/OOM/driver faults.
    match catch_unwind(AssertUnwindSafe(f)) { Ok(Ok(v))=>v, Ok(Err(e))=>e as i64, Err(_)=>-99 }
}
#[no_mangle]
pub extern "system" fn Java_top_ghhccghk_accompanist_lyrics_folia_nativeandroid_RendererJni_abiVersion(_: *mut JNIEnv,_:jclass)->jint { ABI_VERSION }
#[no_mangle]
pub unsafe extern "system" fn Java_top_ghhccghk_accompanist_lyrics_folia_nativeandroid_RendererJni_create(env:*mut JNIEnv,_:jclass,surface:jobject,width:jint,height:jint)->jlong {
    ffi(|| {
        let (w,h)=dimensions(width,height)?;
        if surface.is_null() {return Err(-2);}
        let pointer=ANativeWindow_fromSurface(env,surface);
        if pointer.is_null() {return Err(-10);}
        let session=pollster::block_on(Session::new(Window(pointer as usize),w,h))?;
        registry().lock().map_err(|_|-98)?.insert(session)
    })
}
#[no_mangle]
pub extern "system" fn Java_top_ghhccghk_accompanist_lyrics_folia_nativeandroid_RendererJni_frame(_: *mut JNIEnv,_:jclass,handle:jlong,width:jint,height:jint)->jint {
    ffi(|| { let(w,h)=dimensions(width,height)?; registry().lock().map_err(|_|-98)?.get_mut(handle)?.frame(w,h).map(i64::from) }) as i32
}
#[no_mangle]
pub extern "system" fn Java_top_ghhccghk_accompanist_lyrics_folia_nativeandroid_RendererJni_destroy(_: *mut JNIEnv,_:jclass,handle:jlong)->jint {
    ffi(|| { registry().lock().map_err(|_|-98)?.remove(handle); Ok(0) }) as i32
}
