// Actual original single-segment window, raster/layout/material functions.
// Frozen camera isolates composition. No original CameraRig/React/fog/background.
import * as THREE from 'three';
import {buildDioramaPath,getDioramaTextPlacement,composeLocal} from '../../.work/folia-major/src/components/visualizer/diorama/cameraPath';
import {frameQuaternion} from '../../.work/folia-major/src/components/visualizer/diorama/dioramaSceneUnits';
import {buildLineGraphemeTimeline} from '../../.work/folia-major/src/utils/lyrics/graphemeTiming';
import {resolveDioramaMountedIndices,buildDioramaVisibleLines,splitDioramaLyricUnits,layoutDioramaActiveUnits} from '../../.work/folia-major/src/components/visualizer/diorama/dioramaSceneLayout';
import {createSequencerState,appendSegment} from '../../.work/folia-major/src/components/visualizer/diorama/dioramaSequencer';
import {rasterDioramaLine} from '../../.work/folia-major/src/components/visualizer/diorama/dioramaTextRaster';
import {updateDioramaNeighborLines,updateDioramaActiveUnits} from '../../.work/folia-major/src/components/visualizer/diorama/dioramaSceneFrame';
import {resolveFrameFitScale,resolveTextLife} from '../../.work/folia-major/src/components/visualizer/diorama/dioramaSceneUnits';
import {FOG_NEAR,FOG_FAR} from '../../.work/folia-major/src/components/visualizer/diorama/dioramaSceneConstants';
import latin from '../assets/FoliaParityLatin-Bold.ttf?url';
import arabic from '../assets/FoliaParityBidi-Bold.ttf?url';
import cjk from '../assets/FoliaParityChatCJK-Bold.otf?url';

const name=new URLSearchParams(location.search).get('scene')??'latin';
const tokens={latin:['Synthetic','mirror','plane'],arabic:['مرحبا','بالعالم'],cjk:['星光','回响','向远方']}[name]!;
const file={arabic,cjk}[name]??latin;
const face=new FontFace('Diorama Window Probe',`url(${file})`,{weight:'700'});
await face.load();document.fonts.add(face);
const lines=Array.from({length:8},(_,i)=>({fullText:tokens.join(' '),startTime:(700+i*2400)/1000,endTime:(2300+i*2400)/1000,
 words:tokens.map((text,j)=>({text,startTime:(700+i*2400+Math.round(j*1600/tokens.length))/1000,
  endTime:(700+i*2400+Math.round((j+1)*1600/tokens.length))/1000}))}));
const seed='DioramaProbe',seq=createSequencerState();
appendSegment(seq,{seed,lines,round:0,placementOrigin:{x:0,y:0,z:0}});
const frames=buildDioramaPath(lines.length,seed);
const rasters=new Map(lines.map((l,i)=>[i,rasterDioramaLine(l.fullText,'Diorama Window Probe',700)]));
const layouts=lines.map(l=>layoutDioramaActiveUnits(l,splitDioramaLyricUnits(l,buildLineGraphemeTimeline(l)),
 '700 128px "Diorama Window Probe"')!);
const renderer=new THREE.WebGLRenderer({antialias:true,preserveDrawingBuffer:true});
renderer.setPixelRatio(1);renderer.setSize(innerWidth,600);renderer.setClearColor(0x162435,1);document.body.append(renderer.domElement);
const world=new THREE.Scene(),camera=new THREE.PerspectiveCamera(55,innerWidth/600,.1,1000);
const primary=new THREE.Color('#dfe5ec'),secondary=new THREE.Color('#758daa'),accent=new THREE.Color('#f17b4d');
const damped={primary,secondary,accent,bg:new THREE.Color('#162435')};
const fogEnabled=new URLSearchParams(location.search).has('fog');
if(fogEnabled)world.fog=new THREE.Fog(damped.bg.getHex(),FOG_NEAR,FOG_FAR);
// Actual installed Three direct-screen uniform conversion and smoothstep.
// These supplement, never replace, the WebGL text-stage images.
const encoded=(c:THREE.Color)=>c.getRGB(new THREE.Color(),renderer.outputColorSpace);
const fogControls=fogEnabled?[primary,secondary,accent].flatMap((c,index)=>{
 const a=encoded(c),b=encoded(world.fog!.color);
 return [...Array.from({length:401},(_,i)=>i/10),12-1e-9,12,12+1e-9,30-1e-9,30,30+1e-9].map(depth=>{
  const factor=THREE.MathUtils.smoothstep(depth,FOG_NEAR,FOG_FAR);
  return {index,depth,factor,body:a.toArray(),fog:b.toArray(),result:a.clone().lerp(b,factor).toArray()};
 });
}):[];
const vec=(v:any)=>[v.x,v.y,v.z];
const windows=Array.from({length:9},(_,total)=>Array.from({length:Math.max(total,1)},(_,current)=>({
 total,current,indices:resolveDioramaMountedIndices(current,null,total)}))).flat();
Object.assign(window,{neighborsReady:true,neighborInputs:{lines,seed,windows,primary:primary.toArray(),secondary:secondary.toArray(),accent:accent.toArray(),
 ...(fogEnabled?{fog:{near:FOG_NEAR,far:FOG_FAR,encoded:encoded(world.fog!.color).toArray()},fogControls,threeRevision:THREE.REVISION}:{})},
 drawNeighbors:(current:number,distance:number,time:number)=>{
 for(const child of [...world.children]) {
  child.traverse((o:any)=>{o.geometry?.dispose();if(o.material)o.material.dispose();});world.remove(child);
 }
 const indices=resolveDioramaMountedIndices(current,null,lines.length);
 const visible=buildDioramaVisibleLines(seq,indices,current,null,1);
 const f=frames[current],p=getDioramaTextPlacement(current,seed,1);
 const eye=composeLocal(f,p.offsetR+1.7,p.offsetU+.8,-distance),look=composeLocal(f,p.offsetR+p.lookR,p.offsetU,0);
 camera.position.set(eye.x,eye.y,eye.z);camera.lookAt(look.x,look.y,look.z);camera.updateMatrixWorld();
 const meshes=new Map(),materials=new Map();
 for(const entry of visible)if(entry.index!==current){
  const r=rasters.get(entry.index)!;
  const mat=new THREE.MeshBasicMaterial({map:r.texture,transparent:true,depthWrite:false});
  const mesh=new THREE.Mesh(new THREE.PlaneGeometry(r.canvasWidthPx*.62/r.fontPx,r.canvasHeightPx*.62/r.fontPx),mat);
  mesh.position.fromArray(entry.position);mesh.quaternion.fromArray(entry.quaternion);world.add(mesh);
  meshes.set(entry.index,mesh);materials.set(entry.index,mat);
 }
 updateDioramaNeighborLines({visibleLines:visible,globalIndex:current,transitionOutgoingIndex:null,meshes,materials,rasters,
  camPos:camera.position,fov:55,aspect:innerWidth/600,lyricsFontScale:1,damped});
 const neighbors=visible.filter(e=>e.index!==current).map(e=>({index:e.index,center:e.position,
  fit:meshes.get(e.index).scale.x,opacity:materials.get(e.index).opacity,color:materials.get(e.index).color.toArray(),
  depth:-new THREE.Vector3(...e.position).applyMatrix4(camera.matrixWorldInverse).z,
  advance:rasters.get(e.index)!.advancePx,fontPx:rasters.get(e.index)!.fontPx}));
 const group=new THREE.Group(),center=composeLocal(f,p.offsetR,p.offsetU,0),layout=layouts[current];
 group.position.set(center.x,center.y,center.z);group.quaternion.set(...frameQuaternion(f,p.roll,p.yaw));
 group.scale.setScalar(resolveFrameFitScale(layout.lineWidth,5.2,55,innerWidth/600)*p.scale);world.add(group);
 const units=splitDioramaLyricUnits(lines[current],buildLineGraphemeTimeline(lines[current]));
 const planes:any={baseMats:[],glowMats:[],soulMats:[],glowMeshes:[],soulMeshes:[],unitGroups:[]};
 for(const u of layout.units)for(const [kind,texture,z,order]of [
  ['glow',u.raster.glowTexture,-.01,17],['soul',u.raster.baseTexture,-.005,18],['base',u.raster.baseTexture,0,19]]as const){
  const mat=new THREE.MeshBasicMaterial({map:texture,transparent:true,depthTest:false,depthWrite:false,
   blending:kind==='base'?THREE.NormalBlending:THREE.AdditiveBlending});
  const mesh=new THREE.Mesh(new THREE.PlaneGeometry(u.width,u.height),mat);mesh.position.set(u.centerX,0,z);mesh.renderOrder=order;group.add(mesh);
  planes[kind+'Mats'].push(mat);if(kind!=='base')planes[kind+'Meshes'].push(mesh);
 }
 const charge=(u:any,a:number,r:number)=>time<=u.startTime?0:(1-Math.exp(-a*(Math.min(time,u.endTime)-u.startTime)))*Math.exp(-r*Math.max(0,time-u.endTime));
 const life=resolveTextLife(group.position.distanceTo(camera.position));
 updateDioramaActiveUnits({activeLineUnits:units,planes,lightVals:new Float32Array(units.map(u=>charge(u,14,4.5))),
  soulVals:new Float32Array(units.map(u=>charge(u,12,2.2))),now:time,delta:0,life,breath:1,powerEnv:0,damped,
  keywordUnitColors:new Map(),glowIntensity:1,soulIntensity:1,soulActiveEnabled:true,gradientIntensity:1});
 renderer.render(world,camera);
 return {current,distance,time,indices,eye:vec(camera.position),look:vec(look),neighbors,activeLife:life,
  drawOrder:[...neighbors].sort((a,b)=>b.depth-a.depth||a.index-b.index).map(n=>n.index)};
 }});
