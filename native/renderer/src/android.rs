// SPDX-License-Identifier: AGPL-3.0-only
use crate::{dimensions, Sessions, ABI_VERSION};
use jni_sys::{jclass, jint, jlong, jobject, jfloat, jlongArray, JNIEnv};
use raw_window_handle::{AndroidNdkWindowHandle, DisplayHandle, HandleError, HasDisplayHandle, HasWindowHandle, RawWindowHandle, WindowHandle};
use std::{ffi::c_void, panic::{catch_unwind, AssertUnwindSafe}, ptr::NonNull, sync::{Arc, Mutex, OnceLock, atomic::{AtomicBool, Ordering}}};

#[link(name = "android")]
extern "C" {
    fn ANativeWindow_fromSurface(env: *mut JNIEnv, surface: jobject) -> *mut c_void;
    fn ANativeWindow_release(window: *mut c_void);
}
#[link(name = "log")]
extern "C" {
    fn __android_log_write(priority: i32, tag: *const std::ffi::c_char, text: *const std::ffi::c_char) -> i32;
}
fn log_gpu_error(error: impl std::fmt::Display) {
    if let Ok(message) = std::ffi::CString::new(error.to_string()) {
        unsafe { __android_log_write(6, c"FoliaRenderer".as_ptr(), message.as_ptr()); }
    }
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
    recoveries: u32,
    gpu: crate::gpu::Gpu,
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
        let failed = Arc::new(AtomicBool::new(false));
        let error_flag = failed.clone();
        device.on_uncaptured_error(Arc::new(move |error| {
            error_flag.store(true, Ordering::Release);
            log_gpu_error(error);
        }));
        let lost_flag = failed.clone();
        device.set_device_lost_callback(move |reason, message| {
            if reason != wgpu::DeviceLostReason::Destroyed {
                lost_flag.store(true, Ordering::Release);
                log_gpu_error(message);
            }
        });
        surface.configure(&device,&config);
        crate::gpu::check_health(&failed)?;
        if !adapter.get_texture_format_features(wgpu::TextureFormat::Rgba32Float).allowed_usages.contains(wgpu::TextureUsages::RENDER_ATTACHMENT){return Err(-15);}
        let text_format = adapter.get_texture_format_features(crate::gpu::TEXT_COMPOSITE);
        let supports_text_composite = text_format.allowed_usages.contains(
            wgpu::TextureUsages::TEXTURE_BINDING | wgpu::TextureUsages::RENDER_ATTACHMENT | wgpu::TextureUsages::COPY_DST)
            && text_format.flags.contains(wgpu::TextureFormatFeatureFlags::BLENDABLE | wgpu::TextureFormatFeatureFlags::FILTERABLE);
        let gpu=crate::gpu::Gpu::new(device.clone(),queue.clone(),format,supports_text_composite,failed);
        gpu.check_health()?;
        Ok(Self { surface, device, queue, config, recoveries: 0, gpu })
    }
    fn frame(&mut self, width: u32, height: u32, scene:Option<&crate::protocol::Frame>) -> Result<i32,i32> {
        self.gpu.check_health()?;
        if self.config.width != width || self.config.height != height {
            self.gpu.resize()?;
            self.config.width=width; self.config.height=height;
            self.surface.configure(&self.device,&self.config);
            self.gpu.check_health()?;
        }
        let frame = match self.surface.get_current_texture() {
            Ok(f) => f,
            Err(wgpu::SurfaceError::Timeout) => { self.recoveries+=1; return if self.recoveries<=3 {Ok(1)} else {Err(-21)}; },
            Err(wgpu::SurfaceError::Lost | wgpu::SurfaceError::Outdated) => {
                self.recoveries+=1; if self.recoveries>3 {return Err(-22);}
                self.surface.configure(&self.device,&self.config); self.gpu.check_health()?; return Ok(1);
            },
            Err(_) => return Err(-23),
        };
        self.gpu.check_health()?;
        let view = frame.texture.create_view(&Default::default());
        if let Some(scene)=scene {
            self.gpu.render(scene,&view)?;
        } else {
            let mut encoder = self.device.create_command_encoder(&Default::default());
            { let _pass = encoder.begin_render_pass(&wgpu::RenderPassDescriptor {
                color_attachments: &[Some(wgpu::RenderPassColorAttachment { view:&view, resolve_target:None, depth_slice:None,
                    ops:wgpu::Operations {load:wgpu::LoadOp::Clear(wgpu::Color::TRANSPARENT),store:wgpu::StoreOp::Store} })],
                ..Default::default()
            }); }
            self.gpu.check_health()?;
            let commands = encoder.finish();
            self.gpu.check_health()?;
            self.queue.submit(Some(commands));
        }
        self.gpu.check_health()?;
        frame.present();
        self.gpu.check_health()?;
        self.recoveries=0; Ok(0)
    }
}
// The table owns a reference to each slot; GPU work and destruction only
// hold the slot lock. A failed slot no longer owns a Session, even if the
// host has not yet called destroy. An unwind is caught before releasing the
// slot lock and the failed GPU is never used for another operation.
type Slot<T> = Arc<Mutex<Option<T>>>;
pub(crate) struct SessionTable<T>(Mutex<Sessions<Slot<T>>>);
impl<T> SessionTable<T> {
    pub fn new() -> Self { Self(Mutex::new(Sessions { next:0, values:Default::default() })) }
    pub fn insert(&self, session:T) -> Result<i64,i32> {
        let slot = Arc::new(Mutex::new(Some(session)));
        // Keep a reference outside the table so insertion failure cannot drop
        // GPU objects under the global lock.
        let result = self.0.lock().map_err(|_|-98)?.insert(slot.clone());
        result
    }
    pub fn run<R>(&self, handle:i64, op:impl FnOnce(&mut T)->Result<R,i32>) -> Result<R,i32> {
        let slot = self.0.lock().map_err(|_|-98)?.get(handle)?.clone();
        let mut entry = slot.lock().map_err(|_|-98)?;
        let session = entry.as_mut().ok_or(-4)?;
        let result = catch_unwind(AssertUnwindSafe(|| op(session))).unwrap_or(Err(-99));
        if let Err(code) = result {
            // Preserve the operation error for the host. Log a cleanup panic
            // separately; it must not hide the first failure or poison a lock.
            if let Err(cleanup) = Self::retire(&mut entry) {
                log_gpu_error(format_args!("Session {handle}: operation {code}, cleanup {cleanup}"));
            }
        }
        result
    }
    fn retire(entry: &mut Option<T>) -> Result<(), i32> {
        let session = entry.take();
        catch_unwind(AssertUnwindSafe(|| drop(session))).map_err(|_|-99)
    }
    pub fn destroy(&self, handle:i64) -> Result<(),i32> {
        let slot = self.0.lock().map_err(|_|-98)?.remove(handle);
        if let Some(slot) = slot {
            // Removal prevents new lookups. Previously looked-up calls race
            // destroy for the slot lock: the holder of that lock may finish,
            // while anyone who acquires it after retirement sees no Session.
            let mut entry = slot.lock().map_err(|_|-98)?;
            Self::retire(&mut entry)?;
        }
        Ok(())
    }
}
fn registry() -> &'static SessionTable<Session> {
    static S: OnceLock<SessionTable<Session>> = OnceLock::new();
    S.get_or_init(SessionTable::new)
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
        registry().insert(session)
    })
}
#[no_mangle]
pub extern "system" fn Java_top_ghhccghk_accompanist_lyrics_folia_nativeandroid_RendererJni_frame(_: *mut JNIEnv,_:jclass,handle:jlong,width:jint,height:jint)->jint {
    ffi(|| { let(w,h)=dimensions(width,height)?; registry().run(handle, |s| s.frame(w,h,None)).map(i64::from) }) as i32
}
unsafe fn copied_buffer(env:*mut JNIEnv,buffer:jobject,length:jint,max:usize)->Result<Vec<u8>,i32> {
    if buffer.is_null()||length<=0||length as usize>max{return Err(-50);}
    let capacity=((**env).GetDirectBufferCapacity.ok_or(-50)?)(env,buffer);
    let pointer=((**env).GetDirectBufferAddress.ok_or(-50)?)(env,buffer);
    if pointer.is_null()||capacity<length as i64{return Err(-50);}
    Ok(std::slice::from_raw_parts(pointer as *const u8,length as usize).to_vec())
}
#[no_mangle]
pub unsafe extern "system" fn Java_top_ghhccghk_accompanist_lyrics_folia_nativeandroid_RendererJni_upload(env:*mut JNIEnv,_:jclass,handle:jlong,id:jlong,width:jint,height:jint,buffer:jobject,length:jint,sigma1:jfloat,sigma2:jfloat,ink:jfloat)->jint {
    ffi(||{
        let(w,h)=dimensions(width,height)?;
        let bytes=(w as usize).checked_mul(h as usize).and_then(|n|n.checked_mul(4)).ok_or(-50)?;
        if length as usize!=bytes||bytes>32*1024*1024{return Err(-50);}
        if !sigma1.is_finite()||!sigma2.is_finite()||sigma1<0.||sigma2<0.||sigma1==0.&&sigma2!=0.{return Err(-50);}
        let data=copied_buffer(env,buffer,length,32*1024*1024)?;
        let sigmas=if sigma1==0.{vec![]}else if sigma2==0.{vec![sigma1]}else{vec![sigma1,sigma2]};
        registry().run(handle, |s| s.gpu.upload(id as u64,w,h,&data,&sigmas,ink))?;Ok(0)
    }) as i32
}
#[no_mangle]
pub unsafe extern "system" fn Java_top_ghhccghk_accompanist_lyrics_folia_nativeandroid_RendererJni_render(env:*mut JNIEnv,_:jclass,handle:jlong,buffer:jobject,length:jint)->jint {
    ffi(||{
        let data=copied_buffer(env,buffer,length,crate::protocol::MAX_PACKET)?;
        let frame=crate::protocol::parse(&data)?;
        registry().run(handle, |s| s.frame(frame.width,frame.height,Some(&frame))).map(i64::from)
    }) as i32
}
#[no_mangle]
pub extern "system" fn Java_top_ghhccghk_accompanist_lyrics_folia_nativeandroid_RendererJni_dropScene(_: *mut JNIEnv,_:jclass,handle:jlong,scene:jlong)->jint {
    ffi(||{registry().run(handle, |s| s.gpu.drop_scene(scene as u64))?;Ok(0)}) as i32
}
#[no_mangle]
pub unsafe extern "system" fn Java_top_ghhccghk_accompanist_lyrics_folia_nativeandroid_RendererJni_stats(env:*mut JNIEnv,_:jclass,handle:jlong)->jlongArray {
    catch_unwind(AssertUnwindSafe(|| {
    let values=match registry().run(handle, |s| Ok(s.gpu.stats())) { Ok(values)=>values, Err(_)=>return std::ptr::null_mut() };
    let Some(new_array)=(**env).NewLongArray else{return std::ptr::null_mut()};
    let Some(set_region)=(**env).SetLongArrayRegion else{return std::ptr::null_mut()};
    let array=new_array(env,values.len() as i32);
    if !array.is_null(){set_region(env,array,0,values.len() as i32,values.as_ptr());}array
    })).unwrap_or(std::ptr::null_mut())
}
#[no_mangle]
pub extern "system" fn Java_top_ghhccghk_accompanist_lyrics_folia_nativeandroid_RendererJni_destroy(_: *mut JNIEnv,_:jclass,handle:jlong)->jint {
    ffi(|| { registry().destroy(handle)?; Ok(0) }) as i32
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::{sync::mpsc, thread, time::{Duration, Instant}};

    #[test]
    fn destroy_unlinks_before_waiting_for_active_operation() {
        let table = SessionTable::new();
        let a = table.insert(10).unwrap();
        let b = table.insert(20).unwrap();
        let (entered, active) = mpsc::channel();
        let (finish, resume) = mpsc::channel();
        thread::scope(|scope| {
            let render = scope.spawn(|| table.run(a, move |resource| {
                entered.send(()).unwrap();
                resume.recv_timeout(Duration::from_secs(5)).unwrap();
                Ok(*resource)
            }));
            active.recv_timeout(Duration::from_secs(5)).unwrap();
            let destroy = scope.spawn(|| table.destroy(a));
            let deadline = Instant::now() + Duration::from_secs(2);
            while table.0.lock().unwrap().get(a).is_ok() {
                assert!(Instant::now() < deadline, "destroy did not unlink the active slot");
                thread::yield_now();
            }
            assert!(!destroy.is_finished());
            assert_eq!(table.run(a, |_| Ok(())), Err(-4));
            assert_eq!(table.run(b, |value| Ok(*value)), Ok(20));
            finish.send(()).unwrap();
            assert_eq!(render.join().unwrap(), Ok(10));
            assert_eq!(destroy.join().unwrap(), Ok(()));
        });
    }
}
