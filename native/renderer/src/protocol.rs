// SPDX-License-Identifier: AGPL-3.0-only
//! Bounded little-endian scene commands. All bytes are owned before GPU dispatch.
pub const MAGIC: u32 = 0x31475246;
const SCENE_VERSION: u32 = 4;
pub const MAX_PACKET: usize = 512 * 1024;
pub const MAX_NODES: usize = 4096;
pub const MAX_FRAME_BYTES: u64 = 192 * 1024 * 1024;

#[derive(Clone, Copy, Debug, PartialEq)]
pub struct Transform(pub [f32;6]);
#[derive(Debug)]
pub enum Command {
    Image { texture:u64, color:[f32;4], transform:Transform },
    Group(Group),
    Plane(Plane),
}
#[derive(Debug)]
pub struct Plane {
    pub texture: u64,
    pub color: [f32; 4],
    pub additive: bool,
    pub mip_material: bool,
    pub fog: Option<[f32; 5]>,
    pub vertices: Vec<[f32; 5]>,
}
#[derive(Debug)]
pub struct Group {
    pub width:u32, pub height:u32, pub opacity:f32, pub sigma:f32,
    pub transform:Transform, pub commands:Vec<Command>,
    pub direct: bool,
}
pub struct Frame { pub id:u64, pub width:u32, pub height:u32, pub commands:Vec<Command>, pub projective: bool }
struct Reader<'a> { bytes:&'a [u8], at:usize, count:usize, pixels:u64, version:u32 }
impl Reader<'_> {
    fn u32(&mut self)->Result<u32,i32> {
        let end=self.at.checked_add(4).ok_or(-30)?;
        let b=self.bytes.get(self.at..end).ok_or(-30)?;self.at=end;
        Ok(u32::from_le_bytes(b.try_into().map_err(|_|-30)?))
    }
    fn u64(&mut self)->Result<u64,i32> { let low=self.u32()? as u64; Ok(low|((self.u32()? as u64)<<32)) }
    fn float(&mut self,min:f32,max:f32)->Result<f32,i32> {
        let f=f32::from_bits(self.u32()?); if f.is_finite()&&f>=min&&f<=max {Ok(f)}else{Err(-31)}
    }
    fn matrix(&mut self)->Result<Transform,i32> {
        let mut m=[0.;6];for f in &mut m{*f=self.float(-1_000_000.,1_000_000.)?;}Ok(Transform(m))
    }
    fn commands(&mut self,depth:usize)->Result<Vec<Command>,i32> {
        if depth>16 {return Err(-32);}
        let mut result=Vec::new();
        loop {
            let op=self.u32()?;
            if op==3 {return Ok(result);}
            self.count+=1; if self.count>MAX_NODES {return Err(-32);}
            match op {
                1 | 4 if op == 1 || self.version >= 2 => {
                    let (width,height)=crate::dimensions(self.u32()? as i32,self.u32()? as i32)?;
                    self.pixels=self.pixels.checked_add(width as u64*height as u64).ok_or(-33)?;
                    if self.pixels*4>MAX_FRAME_BYTES {return Err(-33);}
                    let opacity=self.float(0.,1.)?;let sigma=self.float(0.,512.)?;let transform=self.matrix()?;
                    let direct = op == 4;
                    if direct && (opacity != 1. || sigma > 0.01) { return Err(-31); }
                    let commands=self.commands(depth+1)?;
                    result.push(Command::Group(Group {width,height,opacity,sigma,transform,commands,direct}));
                },
                2=>{
                    let texture=self.u64()?; if texture==0{return Err(-34);}
                    let mut color=[0.;4];for f in &mut color{*f=self.float(0.,1.)?;}
                    result.push(Command::Image{texture,color,transform:self.matrix()?});
                },
                5 if self.version >= 3 => {
                    let texture = self.u64()?;
                    if texture == 0 { return Err(-34); }
                    let flags = self.u32()?;
                    if flags > if self.version >= 4 { 7 } else { 3 } { return Err(-31); }
                    let mip_material = flags & 4 != 0;
                    let mut color = [0.; 4];
                    for channel in &mut color[..3] { *channel = self.float(0., if mip_material { 16. } else { 1. })?; }
                    color[3] = self.float(0., 1.)?;
                    let fog = if flags & 2 != 0 {
                        let mut values = [0.; 5];
                        for channel in &mut values[..3] { *channel = self.float(0., 1.)?; }
                        values[3] = self.float(0., 1_000_000.)?;
                        values[4] = self.float(0., 1_000_000.)?;
                        if values[3] >= values[4] { return Err(-31); }
                        Some(values)
                    } else { None };
                    let count = self.u32()? as usize;
                    if !(3..=6).contains(&count) { return Err(-32); }
                    let mut vertices = Vec::with_capacity(count);
                    for _ in 0..count {
                        vertices.push([
                            self.float(-100_000_000., 100_000_000.)?,
                            self.float(-100_000_000., 100_000_000.)?,
                            self.float(0.0001, 1_000_000.)?,
                            self.float(0., 1.)?, self.float(0., 1.)?,
                        ]);
                    }
                    result.push(Command::Plane(Plane { texture, color, additive: flags & 1 != 0, mip_material, fog, vertices }));
                }
                _=>return Err(-30),
            }
        }
    }
}
// Projective text draws in viewport coordinates and never enters an RGBA8
// isolated group. Keeping v3 homogeneous makes its F16 contract explicit.
fn projective_command(command: &Command) -> bool {
    match command {
        Command::Plane(_) => true,
        Command::Group(group) => group.direct && group.transform.0 == [1., 0., 0., 1., 0., 0.]
            && group.commands.iter().all(projective_command),
        Command::Image { .. } => false,
    }
}

pub fn parse(bytes:&[u8])->Result<Frame,i32> {
    if bytes.len()>MAX_PACKET {return Err(-30);}
    let mut r=Reader{bytes,at:0,count:0,pixels:0,version:0};
    if r.u32()?!=MAGIC {return Err(-35);}
    r.version = r.u32()?;
    if !(1..=SCENE_VERSION).contains(&r.version) {return Err(-35);}
    let id=r.u64()?;let(width,height)=crate::dimensions(r.u32()? as i32,r.u32()? as i32)?;
    let commands=r.commands(0)?;
    if r.at!=bytes.len(){return Err(-30);}
    let projective = r.version >= 3;
    if projective && !commands.iter().all(projective_command) { return Err(-31); }
    Ok(Frame{id,width,height,commands,projective})
}

#[cfg(test)] mod tests {
    use super::*;
    fn base()->Vec<u8>{[MAGIC,1,7,0,800,600].into_iter().flat_map(u32::to_le_bytes).collect()}
    fn push(b:&mut Vec<u8>,v:u32){b.extend(v.to_le_bytes());}
    fn group(b:&mut Vec<u8>,w:u32,h:u32){for v in [1,w,h,1f32.to_bits(),0,1f32.to_bits(),0,0,1f32.to_bits(),0,0]{push(b,v);}}
    #[test]
    fn direct_groups_require_v2_and_preserve_isolation_rules() {
        let mut bytes = base();
        group(&mut bytes, 8, 8);
        push(&mut bytes, 3);
        push(&mut bytes, 3);
        let frame = parse(&bytes).unwrap();
        assert!(matches!(&frame.commands[0], Command::Group(group) if !group.direct));
        bytes[24..28].copy_from_slice(&4u32.to_le_bytes());
        assert!(parse(&bytes).is_err());
        bytes[4..8].copy_from_slice(&2u32.to_le_bytes());
        let frame = parse(&bytes).unwrap();
        assert!(matches!(&frame.commands[0], Command::Group(group) if group.direct));
        bytes[40..44].copy_from_slice(&1f32.to_le_bytes());
        assert!(parse(&bytes).is_err());
        bytes[40..44].copy_from_slice(&0f32.to_le_bytes());
        bytes[36..40].copy_from_slice(&0.5f32.to_le_bytes());
        assert!(parse(&bytes).is_err());
    }
    #[test] fn empty_frame_and_truncations(){
        let mut b=base();push(&mut b,3);let f=parse(&b).unwrap();assert_eq!(f.id,7);assert_eq!((f.width,f.height),(800,600));
        for n in 0..b.len(){assert!(parse(&b[..n]).is_err());}
        push(&mut b,3);assert!(parse(&b).is_err());
    }
    #[test] fn rejects_depth_and_budget(){
        let mut b=base();for _ in 0..18 {group(&mut b,1,1);}for _ in 0..19{push(&mut b,3);}assert!(parse(&b).is_err());
        let mut b=base();group(&mut b,8192,8192);push(&mut b,3);push(&mut b,3);assert_eq!(parse(&b).err(),Some(-33));
    }
    #[test] fn rejects_nonfinite_and_unknown(){
        let mut b=base();group(&mut b,10,10);b[36..40].copy_from_slice(&f32::NAN.to_le_bytes());push(&mut b,3);push(&mut b,3);assert!(parse(&b).is_err());
        let mut b=base();push(&mut b,99);assert!(parse(&b).is_err());
    }
}
