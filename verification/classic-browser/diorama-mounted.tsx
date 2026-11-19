// Original components remain intact. Only props, frame clock/RAF scheduling and
// passive Fiber/ref inspection are supplied here. No source envelope injection.
import React from 'react';
import {createRoot,extend,advance,flushSync,_roots} from '@react-three/fiber';
import * as THREE from 'three';
import {motionValue} from 'framer-motion';
import {originalTarget} from 'virtual:diorama-camera-target';
import {originalFlight} from 'virtual:diorama-camera-flight';
import CameraRig from '../../.work/folia-major/src/components/visualizer/diorama/CameraRig';
import DioramaScene from '../../.work/folia-major/src/components/visualizer/diorama/DioramaScene';
import {createSequencerState,appendSegment} from '../../.work/folia-major/src/components/visualizer/diorama/dioramaSequencer';
import {pickTransitionOffset} from '../../.work/folia-major/src/components/visualizer/diorama/dioramaTransition';
import {getDioramaShot,getDioramaTextPlacement,resolveDioramaMotionParams} from '../../.work/folia-major/src/components/visualizer/diorama/cameraPath';
import fontUrl from '../assets/FoliaParityLatin-Bold.ttf?url';
const family='Diorama Mounted',face=new FontFace(family,`url(${fontUrl})`,{weight:'700'});
await face.load();document.fonts.add(face);await document.fonts.ready;
const make=(n:number,label:string)=>Array.from({length:n},(_,i)=>({fullText:`Synthetic ${label} line ${i}`,
 startTime:(700+i*1400)/1000,endTime:(1500+i*1400)/1000,words:[]}));
const baseline = new URLSearchParams(location.search).has('androidBaseline');
const baselineLine = (a: string, b: string, startTime: number, middle: number, endTime: number) => ({
 fullText: a + b, startTime, endTime,
 words: [{text: a, startTime, endTime: middle}, {text: b, startTime: middle, endTime}],
});
const oldLines = baseline ? [baselineLine('Amber ', 'echo', .7, 1.3, 1.9),
 baselineLine('Silver ', 'light', 3.2, 3.65, 4.1)] : make(8, 'outgoing');
const lines = make(3, 'incoming'), oldSeed = baseline ? 'Diorama' : 'Outgoing mirror', seed = 'Incoming mirror';
const seq=createSequencerState();let segment=appendSegment(seq,{seed:oldSeed,lines:oldLines,round:0,placementOrigin:{x:0,y:0,z:0}});
const theme={name:'Mounted source',fontFamily:family,fontWeight:700,fontStyle:'sans',animationIntensity:'normal',
 primaryColor:'#dfe5ec',secondaryColor:baseline?'#adb9c8':'#758daa',accentColor:baseline?'#ffffff':'#f17b4d',backgroundColor:'#162435',wordColors:[]};
const motion=resolveDioramaMotionParams(undefined,'normal'),media=motionValue(0),zero=motionValue(0);
const bands=Object.fromEntries(['bass','lowMid','mid','vocal','treble'].map(k=>[k,zero]));
const widthRef={current:0},canvas=document.querySelector('canvas')!,root=createRoot(canvas);
extend(THREE);
await root.configure({frameloop:'never',dpr:1,size:{width:innerWidth,height:600,top:0,left:0},
 camera:{position:[0,.6,9],fov:55},gl:{alpha:true,antialias:true,preserveDrawingBuffer:true}});
let current=0,outgoing:number|null=null,epoch=0,lastRender='';
function render(){
 const key=`${current}/${outgoing}/${epoch}`;if(key===lastRender)return;
 lastRender=key;
 flushSync(()=>root.render(<>
  <CameraRig currentTime={media} sequencer={seq} globalIndex={current} activeLineWidthRef={widthRef} motion={motion} transitionEpoch={epoch}/>
  <DioramaScene theme={theme} sequencer={seq} globalIndex={current} transitionOutgoingIndex={outgoing}
   currentTime={media} activeLineWidthRef={widthRef} audioPower={zero} audioBands={bands} motion={motion}
   showLyrics={true} showParticles={false} backgroundParticleCircumference={1} backgroundParticleRadial={1}
   geometryVisibility={{enabled:false,mode:'clouds'}} particleDensity={0} particleScale={1}
   particleGlowEnabled={false} particleGlowIntensity={0} lyricsFontScale={1} glowIntensity={1}
   soulIntensity={1} soulActiveEnabled={true} gradientIntensity={1} keywordColoringEnabled={false}/>
 </>));
}
function fiberOf(type:any){
 const find=(f:any):any=>!f?null:f.type===type?f:find(f.child)||find(f.sibling);
 const f=find(_roots.get(canvas)!.fiber.current);if(!f)throw Error(`Missing original ${type.name} Fiber`);return f;
}
function hooks(type:any){const a:any[]=[];for(let h=fiberOf(type).memoizedState;h;h=h.next)a.push(h.memoizedState);return a;}
function refs(type:any){return hooks(type).filter(v=>v&&Object.hasOwn(v,'current')).map(v=>v.current);}
function rig(){
 const r=refs(CameraRig);const [flight,position,velocity,look,lookVelocity,alignment,aq,ai,q,qi]=r;
 return{state:{position:position.toArray(),velocity:velocity.toArray(),look:look.toArray(),lookVelocity:lookVelocity.toArray(),
  alignment,alignmentOrientation:ai?aq.toArray():null,orientation:qi?q.toArray():null},
  flight:{...flight,perp:flight.perp.toArray()}};
}
function sceneInfo(){
 const r=refs(DioramaScene),meshes=r[0],materials=r[1],planes=r.find(v=>Array.isArray(v?.baseMats)),light=r[6],soul=r[7];
 const cache=r.find(v=>v?.rasters instanceof Map),values=hooks(DioramaScene).map(v=>Array.isArray(v)?v[0]:v);
 const layout=values.find(v=>Array.isArray(v?.units)&&typeof v?.lineWidth==='number');
 return{rasterIndices:[...cache.rasters.keys()].sort((a,b)=>a-b),
  neighbors:[...meshes.entries()].map(([index,mesh]:any)=>({index,position:mesh.position.toArray(),scale:mesh.scale.toArray(),
   opacity:materials.get(index).opacity,color:materials.get(index).color.toArray()})).sort((a,b)=>a.index-b.index),
  activeUnits:layout?.units.length,activeWidth:layout?.lineWidth,light:light?Array.from(light):[],soul:soul?Array.from(soul):[],
  unitMaterials:planes.baseMats.map((m:any,i:number)=>({color:m.color.toArray(),opacity:m.opacity,
   glowOpacity:planes.glowMats[i].opacity,soulOpacity:planes.soulMats[i].opacity}))};
}
function read(){const s=_roots.get(canvas)!.store.getState();return{...rig(),...sceneInfo(),current,outgoing,epoch,
 widthAfter:widthRef.current,renderedPosition:s.camera.position.toArray(),renderedOrientation:s.camera.quaternion.toArray(),
 renderer:{toneMapping:s.gl.toneMapping,outputColorSpace:s.gl.outputColorSpace},fog:s.scene.fog?{
  near:s.scene.fog.near,far:s.scene.fog.far,color:s.scene.fog.color.toArray()}:null};}
render();
Object.assign(window,{mountedReady:true,mountedInputs:{oldLines,lines,oldSeed,seed,motion},
 stepMounted:(time:number,wall:number)=>{
  media.set(time);current=segment.globalStart+Math.max(0,segment.lines.findLastIndex(l=>l.startTime<=time));
  if(epoch)outgoing=time<3.2?4:null;render();
  // Original cache callback/React updates run before the controlled R3F frame;
  // callbacks created this turn remain queued until the NEXT frame.
  flushSync(()=>{const q=[...(window as any).queuedRafs.values()];(window as any).queuedRafs.clear();for(const cb of q)cb(wall*1000);});
  const before=widthRef.current,state=_roots.get(canvas)!.store.getState(),previous=rig(),dt=wall-state.clock.elapsedTime;
  const local=current-segment.globalStart,line=segment.lines[local],frame=segment.frames[local];
  const placement=getDioramaTextPlacement(local,segment.seed,motion.weaveScale);
  const shot=getDioramaShot(local,segment.lines,segment.seed,motion.subMode);
  // Independent observation of the extraction seam; NEVER supplies poses,
  // targets or material values to either original mounted component.
  const target=originalTarget({line,now:time,frame,placement,seed:segment.seed,index:local,shotKind:shot,
   trackWidth:before,aspect:innerWidth/600,moveScale:motion.moveScale,driftScale:motion.driftScale});
  const control=originalFlight({line,now:time,wallTime:wall,frame,placement,shotKind:shot,delta:dt,
   aspect:innerWidth/600,target,previous:previous.state,flightState:previous.flight,transitionEpoch:epoch,shotSeed:segment.seed});
  advance(wall,false,state);const observed=read();
  const flat=(v:any)=>[...v.state.position,...v.state.velocity,...v.state.look,...v.state.lookVelocity,v.state.alignment,
   ...v.state.alignmentOrientation,...v.state.orientation,...v.renderedPosition,...v.renderedOrientation];
  const a=flat(observed),b=flat(control),controlPeak=Math.max(...a.map((v,i)=>Math.abs(v-b[i])));
  if(!Number.isFinite(controlPeak)||controlPeak>1e-9)throw Error(`Mounted/extracted control t${time} peak${controlPeak}`);
  return{time,wall,delta:dt,widthBefore:before,shot,controlPeak,...observed};
 },beginMountedCut:()=>{
  const previous=read(),anchor=segment.frames[4].position,off=pickTransitionOffset(seed,1);
  segment=appendSegment(seq,{seed,lines,round:0,placementOrigin:{x:anchor.x+off.x,y:anchor.y+off.y,z:anchor.z+off.z}});
  epoch=1;return previous;
 },readMounted:read,
 // Passive export of the ACTUAL mounted neighbour cache. No raster function
 // calls, context writes, source ref changes or additional animation turns.
 readMountedLineRasters:()=>{
  const cache=refs(DioramaScene).find(v=>v?.rasters instanceof Map);
  return [...cache.rasters.entries()].sort((a,b)=>a[0]-b[0]).map(([index,raster]:any)=>{
   const canvas=raster.texture.image as HTMLCanvasElement,ctx=canvas.getContext('2d')!;
   const text=[...oldLines,...lines][index].fullText,metrics=ctx.measureText(text);
   return{index,text,fontPx:raster.fontPx,advancePx:raster.advancePx,
    width:raster.canvasWidthPx,height:raster.canvasHeightPx,font:ctx.font,
    baseline:ctx.textBaseline,align:ctx.textAlign,
    metrics:{width:metrics.width,actualBoundingBoxLeft:metrics.actualBoundingBoxLeft,
     actualBoundingBoxRight:metrics.actualBoundingBoxRight,actualBoundingBoxAscent:metrics.actualBoundingBoxAscent,
     actualBoundingBoxDescent:metrics.actualBoundingBoxDescent},png:canvas.toDataURL('image/png')};
  });
 },
 // Passive actual active-unit textures/materials/geometry. No raster or matrix
 // updates, hidden envelope injection or additional animation callback.
 readMountedActivePlanes:()=>{
  const state=_roots.get(canvas)!.store.getState();
  const planes=refs(DioramaScene).find(v=>Array.isArray(v?.baseMats));
  return planes.unitGroups.flatMap((group:any,unit:number)=>group.children.filter((mesh:any)=>mesh.isMesh).map((mesh:any)=>{
   const mat=mesh.material,map=mat.map,geometry=mesh.geometry;
   return{unit,order:mesh.renderOrder,visible:mesh.visible,width:map.image.width,height:map.image.height,
    worldMatrix:mesh.matrixWorld.toArray(),planeParameters:geometry.parameters,
    color:mat.color.toArray(),opacity:mat.opacity,blending:mat.blending,premultipliedAlpha:mat.premultipliedAlpha,
    depthTest:mat.depthTest,depthWrite:mat.depthWrite,side:mat.side,toneMapped:mat.toneMapped,
    texture:{version:map.version,sourceVersion:map.source.version,minFilter:map.minFilter,magFilter:map.magFilter,
     anisotropy:map.anisotropy,colorSpace:map.colorSpace,premultiplyAlpha:map.premultiplyAlpha,flipY:map.flipY},
    png:map.image.toDataURL('image/png')};
  }));
 },
 // Diagnostic clone render of existing actual active meshes. Original refs,
 // geometry/material/texture objects and time stay unchanged. Null restores.
 renderMountedActiveLayer:(kind:'base'|'glow'|'soul'|'all'|'neighbors'|null)=>{
  const state=_roots.get(canvas)!.store.getState();
  if(kind===null){state.gl.render(state.scene,state.camera);return;}
  const planes=refs(DioramaScene).find(v=>Array.isArray(v?.baseMats));
  const scene=new THREE.Scene();scene.background=state.scene.background;scene.fog=state.scene.fog;
  const meshes=kind==='neighbors'?[...refs(DioramaScene)[0].values()]:
   planes.unitGroups.flatMap((group:any)=>group.children.filter((m:any)=>m.isMesh));
  for(const mesh of meshes){
   if(kind!=='all'&&kind!=='neighbors'&&mesh.renderOrder!==({glow:17,soul:18,base:19}[kind]))continue;
   const clone=mesh.clone(false);clone.matrixAutoUpdate=false;clone.matrix.copy(mesh.matrixWorld);scene.add(clone);
  }
  try{state.gl.render(scene,state.camera);}finally{scene.clear();}
 },
 // Passive actual GPU inputs; do not update any matrix or render a new frame.
 // Both Float32 geometry corners and ideal plane parameters are retained so
 // buffer quantization cannot be mistaken for a camera/placement discrepancy.
 readMountedNeighborGeometry:(index:number)=>{
  const state=_roots.get(canvas)!.store.getState(),mesh=refs(DioramaScene)[0].get(index);
  if(!mesh)throw Error(`Missing actual mounted neighbour ${index}`);
  const geometry=mesh.geometry,position=geometry.getAttribute('position'),uv=geometry.getAttribute('uv');
  const mat=mesh.material,map=mat.map,camera=state.camera;
  const vertices=Array.from({length:position.count},(_,i)=>{
   const local=new THREE.Vector3().fromBufferAttribute(position,i),world=local.clone().applyMatrix4(mesh.matrixWorld);
   const view=world.clone().applyMatrix4(camera.matrixWorldInverse);
   const clip=new THREE.Vector4(view.x,view.y,view.z,1).applyMatrix4(camera.projectionMatrix);
   return{local:local.toArray(),uv:[uv.getX(i),uv.getY(i)],world:world.toArray(),view:view.toArray(),
    clip:clip.toArray(),pixel:[(clip.x/clip.w+1)*innerWidth/2,(1-clip.y/clip.w)*600/2],depth:-view.z};
  });
  return{index,vertices,indices:Array.from(geometry.index.array),planeParameters:geometry.parameters,
   worldMatrix:mesh.matrixWorld.toArray(),viewMatrix:camera.matrixWorldInverse.toArray(),
   projectionMatrix:camera.projectionMatrix.toArray(),
   material:{opacity:mat.opacity,color:mat.color.toArray(),transparent:mat.transparent,
    blending:mat.blending,premultipliedAlpha:mat.premultipliedAlpha,alphaTest:mat.alphaTest,
    depthTest:mat.depthTest,depthWrite:mat.depthWrite,side:mat.side,toneMapped:mat.toneMapped},
   texture:{width:map.image.width,height:map.image.height,minFilter:map.minFilter,magFilter:map.magFilter,
    anisotropy:map.anisotropy,generateMipmaps:map.generateMipmaps,flipY:map.flipY,
    premultiplyAlpha:map.premultiplyAlpha,colorSpace:map.colorSpace,wrapS:map.wrapS,wrapT:map.wrapT},
   renderer:{maxAnisotropy:state.gl.capabilities.getMaxAnisotropy(),samples:state.gl.getContext().getParameter(state.gl.getContext().SAMPLES)}};
 },
 // Read the ACTUAL uploaded/generated mip texels without rendering or changing
 // a texture. Restore READ framebuffer/read-buffer exactly; DRAW is untouched.
 readMountedNeighborMips:(index:number,levels:number[])=>{
  const state=_roots.get(canvas)!.store.getState(),mesh=refs(DioramaScene)[0].get(index);
  if(!mesh)throw Error(`Missing actual mounted neighbour ${index}`);
  const texture=mesh.material.map,gl=state.gl.getContext() as WebGL2RenderingContext;
  const handle=state.gl.properties.get(texture).__webglTexture;
  if(!handle||!gl.READ_FRAMEBUFFER)throw Error('Expected already uploaded WebGL2 texture');
  if(gl.getParameter(gl.PIXEL_PACK_BUFFER_BINDING))throw Error('Unexpected pixel-pack buffer');
  const before={read:gl.getParameter(gl.READ_FRAMEBUFFER_BINDING),draw:gl.getParameter(gl.DRAW_FRAMEBUFFER_BINDING),
   buffer:gl.getParameter(gl.READ_BUFFER),version:texture.version,sourceVersion:texture.source.version};
  const fbo=gl.createFramebuffer();
  try {
   gl.bindFramebuffer(gl.READ_FRAMEBUFFER,fbo);gl.readBuffer(gl.COLOR_ATTACHMENT0);
   const records=levels.map(level=>{
    const width=Math.max(1,texture.image.width>>level),height=Math.max(1,texture.image.height>>level);
    gl.framebufferTexture2D(gl.READ_FRAMEBUFFER,gl.COLOR_ATTACHMENT0,gl.TEXTURE_2D,handle,level);
    const status=gl.checkFramebufferStatus(gl.READ_FRAMEBUFFER);
    if(status!==gl.FRAMEBUFFER_COMPLETE)throw Error(`Mip framebuffer incomplete level${level}: ${status}`);
    const pixels=new Uint8Array(width*height*4);gl.readPixels(0,0,width,height,gl.RGBA,gl.UNSIGNED_BYTE,pixels);
    const error=gl.getError();if(error!==gl.NO_ERROR)throw Error(`Mip read error${error}`);
    const data=new Uint8ClampedArray(pixels.length);
    for(let y=0;y<height;y++)data.set(pixels.subarray(y*width*4,(y+1)*width*4),(height-1-y)*width*4);
    // PNG canvas storage may round tiny-alpha straight RGB. Retain exact raw
    // GPU bytes separately; PNGs are ONLY labelled visual aids.
    const canvas=document.createElement('canvas');canvas.width=width;canvas.height=height;
    canvas.getContext('2d')!.putImageData(new ImageData(data,width,height),0,0);
    return{level,width,height,rgba:Array.from(data),png:canvas.toDataURL('image/png')};
   });
   return{records,textureVersion:before.version,sourceVersion:before.sourceVersion};
  }finally {
   gl.bindFramebuffer(gl.READ_FRAMEBUFFER,before.read);gl.readBuffer(before.buffer);gl.deleteFramebuffer(fbo);
   if(gl.getParameter(gl.DRAW_FRAMEBUFFER_BINDING)!==before.draw||texture.version!==before.version||
      texture.source.version!==before.sourceVersion)throw Error('Mip read altered original binding/texture versions');
  }
 },
 // Labelled diagnostic render only: clone an ACTUAL mounted neighbour mesh,
 // retaining its matrix/geometry/texture/material and the mounted camera/fog.
 // Original objects/refs stay unchanged; null restores the original whole scene.
 renderMountedNeighborLayer:(index:number|null,sampling?:'linear-no-mip'|'trilinear-1')=>{
  const state=_roots.get(canvas)!.store.getState();
  if(index===null){state.gl.render(state.scene,state.camera);return;}
  const mesh=refs(DioramaScene)[0].get(index);
  if(!mesh)throw Error(`Missing actual mounted neighbour ${index}`);
  mesh.updateWorldMatrix(true,false);
  const clone=mesh.clone(),material=mesh.material.clone();
  let texture:THREE.Texture|undefined;
  if(sampling){
   texture=material.map.clone();
   // Texture.clone shares Source; detach it BEFORE needsUpdate to keep the
   // real original texture/source versions and GPU cache completely untouched.
   texture.source=new THREE.Source(material.map.image);texture.anisotropy=1;
   texture.minFilter=sampling==='linear-no-mip'?THREE.LinearFilter:THREE.LinearMipmapLinearFilter;
   texture.generateMipmaps=sampling!=='linear-no-mip';texture.needsUpdate=true;material.map=texture;
  }
  clone.material=material;clone.matrix.copy(mesh.matrixWorld);clone.matrixAutoUpdate=false;
  const scene=new THREE.Scene();scene.fog=state.scene.fog?.clone()??null;scene.add(clone);
  try{state.gl.render(scene,state.camera);}finally{material.dispose();texture?.dispose();scene.remove(clone);}
 },
 unmountMounted:async()=>{root.unmount();await new Promise(r=>setTimeout(r,600));return{roots:_roots.size,queuedRafs:(window as any).queuedRafs.size}}});
