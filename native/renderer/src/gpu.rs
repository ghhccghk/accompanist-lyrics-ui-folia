// SPDX-License-Identifier: AGPL-3.0-only
use crate::protocol::{Command, Frame, Group, Plane, Transform};
use std::collections::BTreeMap;
use wgpu::util::DeviceExt;

const IDENTITY: Transform = Transform([1., 0., 0., 1., 0., 0.]);

const FORMAT:wgpu::TextureFormat=wgpu::TextureFormat::Rgba8Unorm;
const FLOAT:wgpu::TextureFormat=wgpu::TextureFormat::Rgba32Float;
const BUDGET:u64=192*1024*1024;
#[derive(Clone)]
pub struct Image { pub texture:wgpu::Texture, pub view:wgpu::TextureView, pub width:u32, pub height:u32, format:wgpu::TextureFormat }
impl Image { fn bytes(&self)->u64{self.width as u64*self.height as u64*if self.format==FLOAT{16}else{4}} }

pub struct Gpu {
    device:wgpu::Device, queue:wgpu::Queue,
    layout:wgpu::BindGroupLayout, blur_layout:wgpu::BindGroupLayout, halo_layout:wgpu::BindGroupLayout,
    draw:wgpu::RenderPipeline, present:wgpu::RenderPipeline,
    blur:wgpu::RenderPipeline, blur_float:wgpu::RenderPipeline, halo:wgpu::RenderPipeline,
    sampler:wgpu::Sampler,
    plane_pipelines: Option<[wgpu::RenderPipeline; 2]>,
    images:BTreeMap<u64,Image>, pool:Vec<Image>, resident:u64,
    pub submitted:u64, pub uploaded_bytes:u64, pub peak_bytes:u64,
}
fn texture_entry(binding:u32,filterable:bool)->wgpu::BindGroupLayoutEntry {
    wgpu::BindGroupLayoutEntry{binding,visibility:wgpu::ShaderStages::FRAGMENT,ty:wgpu::BindingType::Texture{
        sample_type:wgpu::TextureSampleType::Float{filterable},view_dimension:wgpu::TextureViewDimension::D2,multisampled:false},count:None}
}
fn uniform_entry(binding:u32,vertex:bool)->wgpu::BindGroupLayoutEntry {
    wgpu::BindGroupLayoutEntry{binding,visibility:if vertex{wgpu::ShaderStages::VERTEX_FRAGMENT}else{wgpu::ShaderStages::FRAGMENT},
        ty:wgpu::BindingType::Buffer{ty:wgpu::BufferBindingType::Uniform,has_dynamic_offset:false,min_binding_size:None},count:None}
}
fn pipeline(device:&wgpu::Device,layout:&wgpu::BindGroupLayout,shader:&str,format:wgpu::TextureFormat,blend:bool)->wgpu::RenderPipeline {
    pipeline_with_blend(device, layout, shader, format, blend.then_some(wgpu::BlendState::PREMULTIPLIED_ALPHA_BLENDING))
}
fn pipeline_with_blend(device: &wgpu::Device, layout: &wgpu::BindGroupLayout, shader: &str,
    format: wgpu::TextureFormat, blend: Option<wgpu::BlendState>) -> wgpu::RenderPipeline {
    let module=device.create_shader_module(wgpu::ShaderModuleDescriptor{label:Some("Folia WGSL"),source:wgpu::ShaderSource::Wgsl(shader.into())});
    let layout=device.create_pipeline_layout(&wgpu::PipelineLayoutDescriptor{label:None,bind_group_layouts:&[layout],push_constant_ranges:&[]});
    device.create_render_pipeline(&wgpu::RenderPipelineDescriptor{
        label:Some("Folia pass"),layout:Some(&layout),
        vertex:wgpu::VertexState{module:&module,entry_point:Some("vertex"),compilation_options:Default::default(),buffers:&[]},
        fragment:Some(wgpu::FragmentState{module:&module,entry_point:Some("fragment"),compilation_options:Default::default(),
            targets:&[Some(wgpu::ColorTargetState{format,blend,write_mask:wgpu::ColorWrites::ALL})]}),
        primitive:Default::default(),depth_stencil:None,multisample:Default::default(),multiview:None,cache:None,
    })
}
fn buffer(device:&wgpu::Device,bytes:&[u8])->wgpu::Buffer {
    device.create_buffer_init(&wgpu::util::BufferInitDescriptor{label:Some("Folia uniforms"),contents:bytes,usage:wgpu::BufferUsages::UNIFORM})
}
fn then(Transform([a, b, c, d, x, y]): Transform,
    Transform([e, f, g, h, u, v]): Transform) -> Result<Transform, i32> {
    let matrix = [a*e+c*f, b*e+d*f, a*g+c*h, b*g+d*h, a*u+c*v+x, b*u+d*v+y];
    if matrix.iter().any(|value| value.abs() > 1_000_000. || !value.is_finite()) {
        return Err(-31);
    }
    Ok(Transform(matrix))
}
fn floats(values:&[f32])->Vec<u8>{values.iter().flat_map(|v|v.to_le_bytes()).collect()}
fn pass(encoder:&mut wgpu::CommandEncoder,target:&wgpu::TextureView,pipe:&wgpu::RenderPipeline,bind:&wgpu::BindGroup,vertices:u32,clear:bool) {
    let mut p=encoder.begin_render_pass(&wgpu::RenderPassDescriptor{label:Some("Folia render"),
        color_attachments:&[Some(wgpu::RenderPassColorAttachment{view:target,resolve_target:None,depth_slice:None,
            ops:wgpu::Operations{load:if clear{wgpu::LoadOp::Clear(wgpu::Color::TRANSPARENT)}else{wgpu::LoadOp::Load},store:wgpu::StoreOp::Store}})],..Default::default()});
    p.set_pipeline(pipe);p.set_bind_group(0,bind,&[]);p.draw(0..vertices,0..1);
}
fn clear(encoder:&mut wgpu::CommandEncoder,target:&wgpu::TextureView) {
    let _p=encoder.begin_render_pass(&wgpu::RenderPassDescriptor{label:Some("Folia clear"),
        color_attachments:&[Some(wgpu::RenderPassColorAttachment{view:target,resolve_target:None,depth_slice:None,
            ops:wgpu::Operations{load:wgpu::LoadOp::Clear(wgpu::Color::TRANSPARENT),store:wgpu::StoreOp::Store}})],..Default::default()});
}
impl Gpu {
    pub fn new(device:wgpu::Device,queue:wgpu::Queue,surface_format:wgpu::TextureFormat)->Self {
        let layout=device.create_bind_group_layout(&wgpu::BindGroupLayoutDescriptor{label:None,entries:&[
            texture_entry(0,true),wgpu::BindGroupLayoutEntry{binding:1,visibility:wgpu::ShaderStages::FRAGMENT,ty:wgpu::BindingType::Sampler(wgpu::SamplerBindingType::Filtering),count:None},uniform_entry(2,true)]});
        let blur_layout=device.create_bind_group_layout(&wgpu::BindGroupLayoutDescriptor{label:None,entries:&[texture_entry(0,false),uniform_entry(1,false)]});
        let halo_layout=device.create_bind_group_layout(&wgpu::BindGroupLayoutDescriptor{label:None,entries:&[texture_entry(0,false),texture_entry(1,false),uniform_entry(2,false)]});
        let draw=pipeline(&device,&layout,include_str!("draw.wgsl"),FORMAT,true);
        let present=pipeline(&device,&layout,include_str!("draw.wgsl"),surface_format,true);
        let blur=pipeline(&device,&blur_layout,include_str!("blur.wgsl"),FORMAT,false);
        let blur_float=pipeline(&device,&blur_layout,include_str!("blur.wgsl"),FLOAT,false);
        let halo=pipeline(&device,&halo_layout,include_str!("halo.wgsl"),FORMAT,false);
        let sampler=device.create_sampler(&wgpu::SamplerDescriptor{mag_filter:wgpu::FilterMode::Linear,min_filter:wgpu::FilterMode::Linear,..Default::default()});
        Self{device,queue,layout,blur_layout,halo_layout,draw,present,blur,blur_float,halo,sampler,plane_pipelines:None,images:BTreeMap::new(),pool:Vec::new(),resident:0,submitted:0,uploaded_bytes:0,peak_bytes:0}
    }
    fn allocate(&mut self,w:u32,h:u32,format:wgpu::TextureFormat)->Result<Image,i32> {
        let bytes=w as u64*h as u64*if format==FLOAT{16}else{4};
        // Never undercount retired in-flight resources by dropping scratch here.
        if self.resident+bytes>BUDGET {return Err(-40);}
        let texture=self.device.create_texture(&wgpu::TextureDescriptor{label:Some("Folia texture"),size:wgpu::Extent3d{width:w,height:h,depth_or_array_layers:1},mip_level_count:1,sample_count:1,dimension:wgpu::TextureDimension::D2,format,
            usage:wgpu::TextureUsages::TEXTURE_BINDING|wgpu::TextureUsages::RENDER_ATTACHMENT|wgpu::TextureUsages::COPY_DST|
                if cfg!(test){wgpu::TextureUsages::COPY_SRC}else{wgpu::TextureUsages::empty()},view_formats:&[]});
        let view=texture.create_view(&Default::default());
        self.resident+=bytes;self.peak_bytes=self.peak_bytes.max(self.resident);
        Ok(Image{texture,view,width:w,height:h,format})
    }
    fn take(&mut self,w:u32,h:u32,format:wgpu::TextureFormat)->Result<Image,i32> {
        if let Some(i)=self.pool.iter().position(|p|p.width==w&&p.height==h&&p.format==format){Ok(self.pool.swap_remove(i))}else{self.allocate(w,h,format)}
    }
    fn recycle(&mut self,image:Image){self.pool.push(image);}
    fn draw_image(&self,encoder:&mut wgpu::CommandEncoder,source:&Image,target:&wgpu::TextureView,size:(u32,u32),matrix:Transform,color:[f32;4],mask:bool,present:bool) {
        let [a,b,c,d,x,y]=matrix.0;
        let uniform=buffer(&self.device,&floats(&[a,b,c,d,x,y,source.width as f32,source.height as f32,size.0 as f32,size.1 as f32,if mask{1.}else{0.},0.,color[0],color[1],color[2],color[3]]));
        let bind=self.device.create_bind_group(&wgpu::BindGroupDescriptor{label:None,layout:&self.layout,entries:&[
            wgpu::BindGroupEntry{binding:0,resource:wgpu::BindingResource::TextureView(&source.view)},
            wgpu::BindGroupEntry{binding:1,resource:wgpu::BindingResource::Sampler(&self.sampler)},
            wgpu::BindGroupEntry{binding:2,resource:uniform.as_entire_binding()}]});
        pass(encoder,target,if present{&self.present}else{&self.draw},&bind,6,false);
    }
    fn draw_plane(&mut self, encoder: &mut wgpu::CommandEncoder, target: &wgpu::TextureView,
        size: (u32, u32), plane: &Plane) -> Result<(), i32> {
        let image = self.images.get(&plane.texture).ok_or(-43)?;
        let pipelines = self.plane_pipelines.get_or_insert_with(|| {
            let add = wgpu::BlendComponent {
                src_factor: wgpu::BlendFactor::One, dst_factor: wgpu::BlendFactor::One,
                operation: wgpu::BlendOperation::Add,
            };
            let shader = include_str!("plane.wgsl");
            [
                pipeline_with_blend(&self.device, &self.layout, shader, FORMAT,
                    Some(wgpu::BlendState::PREMULTIPLIED_ALPHA_BLENDING)),
                pipeline_with_blend(&self.device, &self.layout, shader, FORMAT,
                    Some(wgpu::BlendState {color: add, alpha: add})),
            ]
        });
        let mut data = [0.; 64];
        data[..4].copy_from_slice(&plane.color);
        data[4..6].copy_from_slice(&[size.0 as f32, size.1 as f32]);
        if let Some([r, g, b, near, far]) = plane.fog {
            data[6] = near;
            data[7] = far;
            data[8..11].copy_from_slice(&[r, g, b]);
            data[11] = 1.;
        }
        for (index, &[x, y, w, u, v]) in plane.vertices.iter().enumerate() {
            let start = 12 + index * 8;
            data[start..start + 8].copy_from_slice(&[x, y, w, 0., u, v, 0., 0.]);
        }
        let uniform = buffer(&self.device, &floats(&data));
        let bind = self.device.create_bind_group(&wgpu::BindGroupDescriptor {
            label: Some("Diorama text plane"), layout: &self.layout, entries: &[
                wgpu::BindGroupEntry { binding: 0, resource: wgpu::BindingResource::TextureView(&image.view) },
                wgpu::BindGroupEntry { binding: 1, resource: wgpu::BindingResource::Sampler(&self.sampler) },
                wgpu::BindGroupEntry { binding: 2, resource: uniform.as_entire_binding() },
            ],
        });
        let count = (plane.vertices.len() as u32 - 2) * 3;
        pass(encoder, target, &pipelines[plane.additive as usize], &bind, count, false);
        Ok(())
    }
    fn blurred(&mut self,encoder:&mut wgpu::CommandEncoder,source:&Image,sigma:f32,gaussian_small:bool)->Result<Image,i32> {
        if !sigma.is_finite()||!(0.0..=128.).contains(&sigma){return Err(-41);}
        let window=((sigma as f64*3.*(2.*std::f64::consts::PI).sqrt()/4.+0.5).floor() as i32).max(1);
        let a=self.take(source.width,source.height,FLOAT)?;
        let b=self.take(source.width,source.height,FLOAT)?;
        let output=self.take(source.width,source.height,FORMAT)?;
        let small=gaussian_small&&sigma<2.;let passes=if small{2}else{6};
        for index in 0..passes {
            let phase=index%3;
            let count=if small{2*(3.*sigma).ceil() as i32+1}else{window+if phase==2&&window%2==0{1}else{0}};
            let left=count/2+if !small&&count%2==0&&phase==1{-1}else{0};
            let from=if index==0{source}else if index%2==1{&a}else{&b};
            let last=index==passes-1;
            let to=if last{&output}else if index%2==0{&a}else{&b};
            let ints:[i32;4]=[if small{index}else if index<3{0}else{1},left,count,if (small&&last)||(!small&&phase==2){1}else{0}];
            let mut bytes=ints.into_iter().flat_map(i32::to_le_bytes).collect::<Vec<_>>();
            bytes.extend(floats(&[if small{sigma}else{0.},0.,0.,0.]));
            let uniform=buffer(&self.device,&bytes);
            let bind=self.device.create_bind_group(&wgpu::BindGroupDescriptor{label:None,layout:&self.blur_layout,entries:&[
                wgpu::BindGroupEntry{binding:0,resource:wgpu::BindingResource::TextureView(&from.view)},wgpu::BindGroupEntry{binding:1,resource:uniform.as_entire_binding()}]});
            pass(encoder,&to.view,if last{&self.blur}else{&self.blur_float},&bind,3,true);
        }
        self.recycle(a);self.recycle(b);Ok(output)
    }
    pub fn upload(&mut self,id:u64,width:u32,height:u32,pixels:&[u8],sigmas:&[f32],ink:f32)->Result<(),i32> {
        if id==0||id>>16==0||self.images.contains_key(&id)||self.images.len()>=4096||sigmas.len()>2||!ink.is_finite()||!(0.0..=1.).contains(&ink)||sigmas.iter().any(|s|!s.is_finite()||!(1.0..=128.).contains(s)){return Err(-42);}
        let expected=(width as usize).checked_mul(height as usize).and_then(|v|v.checked_mul(4)).ok_or(-42)?;
        if pixels.len()!=expected {return Err(-42);}
        let source=self.allocate(width,height,FORMAT)?;
        self.queue.write_texture(wgpu::TexelCopyTextureInfo{texture:&source.texture,mip_level:0,origin:wgpu::Origin3d::ZERO,aspect:wgpu::TextureAspect::All},pixels,
            wgpu::TexelCopyBufferLayout{offset:0,bytes_per_row:Some(width*4),rows_per_image:Some(height)},wgpu::Extent3d{width,height,depth_or_array_layers:1});
        self.uploaded_bytes+=expected as u64;
        let result=if sigmas.is_empty(){source}else{
            let mut encoder=self.device.create_command_encoder(&Default::default());
            let first=self.blurred(&mut encoder,&source,sigmas[0],false)?;
            let second=if sigmas.len()==2{Some(self.blurred(&mut encoder,&source,sigmas[1],false)?)}else{None};
            let output=self.take(width,height,FORMAT)?;
            let mut bytes=ink.to_le_bytes().to_vec();bytes.extend((sigmas.len() as u32).to_le_bytes());bytes.extend([0u8;8]);
            let uniform=buffer(&self.device,&bytes);
            let bind=self.device.create_bind_group(&wgpu::BindGroupDescriptor{label:None,layout:&self.halo_layout,entries:&[
                wgpu::BindGroupEntry{binding:0,resource:wgpu::BindingResource::TextureView(&first.view)},
                wgpu::BindGroupEntry{binding:1,resource:wgpu::BindingResource::TextureView(&second.as_ref().unwrap_or(&first).view)},
                wgpu::BindGroupEntry{binding:2,resource:uniform.as_entire_binding()}]});
            pass(&mut encoder,&output.view,&self.halo,&bind,3,true);
            self.queue.submit(Some(encoder.finish()));
            self.recycle(first);if let Some(second)=second{self.recycle(second);}self.recycle(source);output
        };
        self.images.insert(id,result);Ok(())
    }
    fn group(&mut self,encoder:&mut wgpu::CommandEncoder,group:&Group)->Result<Image,i32> {
        let image=self.take(group.width,group.height,FORMAT)?;
        clear(encoder,&image.view);
        if group.opacity>0. {self.commands(encoder,&image.view,(group.width,group.height),&group.commands,IDENTITY)?;}
        if group.sigma>0.01&&group.opacity>0. {
            let output=self.blurred(encoder,&image,group.sigma,true)?;self.recycle(image);Ok(output)
        }else{Ok(image)}
    }
    fn commands(&mut self,encoder:&mut wgpu::CommandEncoder,target:&wgpu::TextureView,size:(u32,u32),commands:&[Command],parent:Transform)->Result<(),i32> {
        for command in commands {
            match command {
                Command::Image{texture,color,transform}=>{
                    let image=self.images.get(texture).ok_or(-43)?;
                    self.draw_image(encoder,image,target,size,then(parent,*transform)?,*color,true,false);
                },
                Command::Plane(plane) => {
                    if parent != IDENTITY { return Err(-31); }
                    self.draw_plane(encoder, target, size, plane)?;
                },
                Command::Group(g)=>{
                    if g.opacity==0. {continue;}
                    // Retain the parent transform through non-isolated groups. Sampling
                    // a texture at every layer spreads narrow guides and softens text.
                    if g.direct {
                        self.commands(encoder,target,size,&g.commands,then(parent,g.transform)?)?;
                        continue;
                    }
                    let image=self.group(encoder,g)?;
                    self.draw_image(encoder,&image,target,size,then(parent,g.transform)?,[1.,1.,1.,g.opacity],false,false);
                    self.recycle(image);
                }
            }
        }Ok(())
    }
    pub fn render(&mut self,frame:&Frame,target:&wgpu::TextureView)->Result<(),i32> {
        let mut encoder=self.device.create_command_encoder(&Default::default());
        let image=self.take(frame.width,frame.height,FORMAT)?;
        clear(&mut encoder,&image.view);
        self.commands(&mut encoder,&image.view,(frame.width,frame.height),&frame.commands,IDENTITY)?;
        clear(&mut encoder,target);
        self.draw_image(&mut encoder,&image,target,(frame.width,frame.height),IDENTITY,[1.;4],false,true);
        self.queue.submit(Some(encoder.finish()));self.submitted=frame.id;self.recycle(image);Ok(())
    }
    fn wait_for_submitted_work(&self) -> Result<(), i32> {
        self.device.poll(wgpu::PollType::Wait {
            submission_index: None,
            timeout: Some(std::time::Duration::from_secs(2)),
        }).map_err(|_| -44)?;
        Ok(())
    }

    fn clear_scratch(&mut self) {
        for image in self.pool.drain(..) {
            self.resident -= image.bytes();
        }
    }

    pub fn resize(&mut self) -> Result<(), i32> {
        // The pool is size-keyed. Repeated viewport changes would otherwise
        // retain full-size attachments until the next lyric scene retires.
        if !self.pool.is_empty() {
            self.wait_for_submitted_work()?;
            self.clear_scratch();
        }
        Ok(())
    }

    pub fn drop_scene(&mut self, scene: u64) -> Result<(), i32> {
        // Scene replacement is preparation, never a normal frame. Wait before
        // releasing retired GPU allocations and reducing scratch accounting.
        self.wait_for_submitted_work()?;
        self.images.retain(|id, image| {
            if *id >> 16 != scene { return true; }
            self.resident -= image.bytes();
            false
        });
        self.clear_scratch();
        Ok(())
    }
    pub fn stats(&self)->[i64;5]{[self.submitted as i64,self.images.len() as i64,self.resident as i64,self.peak_bytes as i64,self.uploaded_bytes as i64]}
}

#[cfg(test)]
mod tests {
    use super::*;
    fn reference(input:&[u8],width:usize,height:usize,sigma:f32,gaussian:bool)->Vec<u8>{
        let mut a:Vec<f64>=input.iter().map(|v|*v as f64).collect();
        if gaussian&&sigma<2. {
            let radius=(3.*sigma).ceil() as i32;
            let mut weights:Vec<_>=(-radius..=radius).map(|i|(-0.5*(i as f64).powi(2)/(sigma as f64).powi(2)).exp()).collect();
            let norm:f64=weights.iter().sum();for w in &mut weights{*w/=norm;}
            for axis in 0..2 {
                let mut b=vec![0.;a.len()];
                for y in 0..height {for x in 0..width {for (i,weight) in weights.iter().enumerate(){
                    let d=i as i32-radius;let(px,py)=if axis==0{(x as i32+d,y as i32)}else{(x as i32,y as i32+d)};
                    if px>=0&&py>=0&&(px as usize)<width&&(py as usize)<height{b[y*width+x]+=a[py as usize*width+px as usize]*weight;}
                }}}a=b;
            }
        }else{
            let window=((sigma as f64*3.*(2.*std::f64::consts::PI).sqrt()/4.+0.5).floor() as i32).max(1);
            for axis in 0..2 {for phase in 0..3 {
                let count=window+if phase==2&&window%2==0{1}else{0};let left=count/2+if count%2==0&&phase==1{-1}else{0};
                let mut b=vec![0.;a.len()];
                for y in 0..height {for x in 0..width {for i in 0..count {
                    let d=i-left;let(px,py)=if axis==0{(x as i32+d,y as i32)}else{(x as i32,y as i32+d)};
                    if px>=0&&py>=0&&(px as usize)<width&&(py as usize)<height{b[y*width+x]+=a[py as usize*width+px as usize]/count as f64;}
                }}}
                if phase==2{for v in &mut b{*v=(*v+0.5).floor();}}a=b;
            }}
        }
        a.iter().map(|v|(*v+0.5).floor().clamp(0.,255.) as u8).collect()
    }
    fn read(gpu:&Gpu,image:&Image)->Vec<u8>{
        let stride=(image.width*4).div_ceil(256)*256;
        let buffer=gpu.device.create_buffer(&wgpu::BufferDescriptor{label:None,size:(stride*image.height) as u64,usage:wgpu::BufferUsages::COPY_DST|wgpu::BufferUsages::MAP_READ,mapped_at_creation:false});
        let mut e=gpu.device.create_command_encoder(&Default::default());
        e.copy_texture_to_buffer(wgpu::TexelCopyTextureInfo{texture:&image.texture,mip_level:0,origin:wgpu::Origin3d::ZERO,aspect:wgpu::TextureAspect::All},
            wgpu::TexelCopyBufferInfo{buffer:&buffer,layout:wgpu::TexelCopyBufferLayout{offset:0,bytes_per_row:Some(stride),rows_per_image:Some(image.height)}},wgpu::Extent3d{width:image.width,height:image.height,depth_or_array_layers:1});
        gpu.queue.submit(Some(e.finish()));
        let(tx,rx)=std::sync::mpsc::channel();buffer.slice(..).map_async(wgpu::MapMode::Read,move|v|tx.send(v).unwrap());
        gpu.device.poll(wgpu::PollType::Wait{submission_index:None,timeout:Some(std::time::Duration::from_secs(10))}).unwrap();rx.recv().unwrap().unwrap();
        let bytes=buffer.slice(..).get_mapped_range();let mut out=Vec::new();
        for y in 0..image.height as usize{out.extend_from_slice(&bytes[y*stride as usize..y*stride as usize+image.width as usize*4]);}out
    }
    fn open_gpu() -> Gpu {
        let instance=wgpu::Instance::new(&wgpu::InstanceDescriptor{backends:wgpu::Backends::VULKAN,..Default::default()});
        let adapter=pollster::block_on(instance.request_adapter(&Default::default())).expect("A real Vulkan adapter is required for this explicit test");
        let(device,queue)=pollster::block_on(adapter.request_device(&wgpu::DeviceDescriptor{
            required_limits:wgpu::Limits::downlevel_defaults().using_resolution(adapter.limits()),
            ..Default::default()
        })).unwrap();
        Gpu::new(device,queue,FORMAT)
    }

    #[test]
    fn real_vulkan_resize_and_scene_retirement_reclaim_texture_budget() {
        let mut gpu = open_gpu();
        let first_scene = (1 << 16) | 1;
        let second_scene = (2 << 16) | 1;
        let pixels = vec![64; 32 * 32 * 4];
        for id in [first_scene, second_scene] {
            gpu.upload(id, 32, 32, &pixels, &[], 1.).unwrap();
        }
        let prepared_bytes = (pixels.len() * 2) as u64;
        let target = gpu.allocate(128, 128, FORMAT).unwrap();
        let target_bytes = target.bytes();
        for (index, width) in [64, 80, 96, 112].into_iter().enumerate() {
            let frame = Frame {
                id: index as u64 + 1, width, height: 64, commands: vec![Command::Image {
                    texture: second_scene, color: [1.; 4], transform: IDENTITY,
                }],
            };
            gpu.render(&frame, &target.view).unwrap();
            assert_eq!(gpu.resident, prepared_bytes + target_bytes + (width * 64 * 4) as u64);
            gpu.resize().unwrap();
            assert_eq!(gpu.resident, prepared_bytes + target_bytes);
            assert_eq!(gpu.images.len(), 2);
        }
        assert_eq!(gpu.allocate(8192, 8192, FORMAT).err(), Some(-40));
        assert_eq!(gpu.resident, prepared_bytes + target_bytes);
        gpu.drop_scene(1).unwrap();
        assert_eq!(gpu.images.keys().copied().collect::<Vec<_>>(), [second_scene]);
        assert_eq!(gpu.resident, pixels.len() as u64 + target_bytes);
        gpu.drop_scene(2).unwrap();
        assert_eq!(gpu.resident, target_bytes);
    }

    #[test] fn real_vulkan_shadow_and_small_gaussian_match_f64_reference(){
        let mut gpu = open_gpu();
        let(w,h)=(31usize,17usize);
        let alpha:Vec<u8>=(0..w*h).map(|i|if i%11==0{1}else if i%7==0{255}else{((i*137+29)%173) as u8}).collect();
        let rgba:Vec<u8>=alpha.iter().flat_map(|a|[*a;4]).collect();
        gpu.upload(1<<16|1,w as u32,h as u32,&rgba,&[],1.).unwrap();
        let source=gpu.images.get(&(1<<16|1)).unwrap().clone();
        for (sigma,gaussian) in [(1.,false),(2.25,false),(10.,false),(20.,false),(40.,false),(128.,false),(0.25,true),(0.7,true),(1.6,true)]{
            let mut encoder=gpu.device.create_command_encoder(&Default::default());let output=gpu.blurred(&mut encoder,&source,sigma,gaussian).unwrap();gpu.queue.submit(Some(encoder.finish()));
            let actual=read(&gpu,&output);let expected=reference(&alpha,w,h,sigma,gaussian);
            let worst=expected.iter().enumerate().map(|(i,e)|actual[i*4+3].abs_diff(*e)).max().unwrap();
            // At most one 8-bit rounding step per completed axis differs when
            // moving f64 accumulation to shader f32; fixed before measuring.
            assert!(worst<=2,"sigma={sigma} gaussian={gaussian} max error={worst}");
            gpu.recycle(output);
        }
        gpu.upload(2<<16|1,w as u32,h as u32,&rgba,&[10.,20.],0.37).unwrap();
        let actual=read(&gpu,gpu.images.get(&(2<<16|1)).unwrap());let a=reference(&alpha,w,h,10.,false);let b=reference(&alpha,w,h,20.,false);
        for i in 0..w*h{let x=(a[i] as f32*0.37).round() as u32;let y=(b[i] as f32*0.37).round() as u32;let expected=(y+x*(256-y)/256) as u8;assert!(actual[i*4+3].abs_diff(expected)<=4);}
    }
}
