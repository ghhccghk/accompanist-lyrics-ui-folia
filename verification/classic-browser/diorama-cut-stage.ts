// Original complete TEXT layer + verbatim extracted camera operators.
// Controlled new-song edge/local media clock, not mounted React/RAF orchestration.
import * as THREE from 'three';
import {originalTarget} from 'virtual:diorama-camera-target';
import {originalFlight} from 'virtual:diorama-camera-flight';
import {getDioramaTextPlacement} from '../../.work/folia-major/src/components/visualizer/diorama/cameraPath';
import {pickTransitionOffset} from '../../.work/folia-major/src/components/visualizer/diorama/dioramaTransition';
import {createSequencerState,appendSegment} from '../../.work/folia-major/src/components/visualizer/diorama/dioramaSequencer';
import {resolveDioramaMountedIndices,buildDioramaVisibleLines,splitDioramaLyricUnits,layoutDioramaActiveUnits} from '../../.work/folia-major/src/components/visualizer/diorama/dioramaSceneLayout';
import {rasterDioramaLine} from '../../.work/folia-major/src/components/visualizer/diorama/dioramaTextRaster';
import {resolveFrameFitScale,resolveTextLife} from '../../.work/folia-major/src/components/visualizer/diorama/dioramaSceneUnits';
import {buildLineGraphemeTimeline} from '../../.work/folia-major/src/utils/lyrics/graphemeTiming';
import {updateDioramaNeighborLines,updateDioramaActiveUnits} from '../../.work/folia-major/src/components/visualizer/diorama/dioramaSceneFrame';
import latin from '../assets/FoliaParityLatin-Bold.ttf?url';
const family='Diorama Cut Stage',face=new FontFace(family,`url(${latin})`,{weight:'700'});
await face.load();document.fonts.add(face);
const makeLines=(n:number,label:string)=>Array.from({length:n},(_,i)=>({fullText:`Synthetic ${label} line ${i}`,
 startTime:(700+i*1400)/1000,endTime:(1500+i*1400)/1000,words:[]}));
const oldLines=makeLines(8,'outgoing'),lines=makeLines(3,'incoming'),oldSeed='Outgoing mirror',seed='Incoming mirror';
const seq=createSequencerState();let segment=appendSegment(seq,{seed:oldSeed,lines:oldLines,round:0,placementOrigin:{x:0,y:0,z:0}});
const oldSegment=segment,all=[...oldLines,...lines];
const unitSets=all.map(l=>splitDioramaLyricUnits(l,buildLineGraphemeTimeline(l)));
const layouts=all.map((l,i)=>layoutDioramaActiveUnits(l,unitSets[i],`700 128px "${family}"`)!);
const rasters=new Map(all.map((l,i)=>[i,rasterDioramaLine(l.fullText,family,700)]));
const renderer=new THREE.WebGLRenderer({antialias:true,preserveDrawingBuffer:true});
renderer.setPixelRatio(1);renderer.setSize(innerWidth,600);renderer.setClearColor(0x162435,1);document.body.append(renderer.domElement);
const world=new THREE.Scene(),camera=new THREE.PerspectiveCamera(55,innerWidth/600,.1,1000);
const primary=new THREE.Color('#dfe5ec'),secondary=new THREE.Color('#758daa'),accent=new THREE.Color('#f17b4d');
const damped={primary,secondary,accent,background:new THREE.Color('#162435')};
let previous:any,flightState:any,last:any;
const currentAt=(t:number)=>Math.max(0,segment.lines.findLastIndex(l=>l.startTime<=t));
function step(time:number,delta:number,epoch:number){
 const local=currentAt(time),index=segment.globalStart+local,frame=segment.frames[local];
 const placement=getDioramaTextPlacement(local,segment.seed),line=segment.lines[local];
 const trackWidth=layouts[index].lineWidth*resolveFrameFitScale(layouts[index].lineWidth,5.2,55,innerWidth/600)*placement.scale;
 const target=originalTarget({line,now:time,frame,placement,seed:segment.seed,index:local,shotKind:'orbit',trackWidth,aspect:innerWidth/600});
 last=originalFlight({line,now:time,wallTime:time,frame,placement,shotKind:'orbit',delta,aspect:innerWidth/600,
  target,previous,flightState,transitionEpoch:epoch,shotSeed:segment.seed});
 previous=last.state;flightState=last.flight;
 camera.position.fromArray(last.renderedPosition);camera.quaternion.fromArray(last.renderedOrientation);camera.updateMatrixWorld();
 return {...last,target};
}
Object.assign(window,{cutStageReady:true,cutInputs:{oldLines,lines,oldSeed,seed,outgoingTime:6.8,outgoing:4,epoch:1},
 stepOutgoing:(t:number,dt:number)=>step(t,dt,0),beginCut:()=>{
  // Exact source placement rule VisualizerDiorama:381–387. Edge supplied by host.
  const anchor=oldSegment.frames[4].position,off=pickTransitionOffset(seed,1);
  const origin={x:anchor.x+off.x,y:anchor.y+off.y,z:anchor.z+off.z};
  segment=appendSegment(seq,{seed,lines,round:0,placementOrigin:origin});flightState=undefined;
  return {previous,origin,globalStart:segment.globalStart};
 },stepIncoming:(t:number,dt:number)=>step(t,dt,1),drawIncoming:(time:number)=>{
 for(const child of [...world.children]){
  child.traverse((o:any)=>{o.geometry?.dispose();o.material?.dispose();});world.remove(child);
 }
 const current=segment.globalStart+currentAt(time),outgoing=time<3.2?4:null;
 const indices=resolveDioramaMountedIndices(current,outgoing,all.length);
 const visible=buildDioramaVisibleLines(seq,indices,current,outgoing,1);
 const meshes=new Map(),materials=new Map();
 for(const e of visible)if(e.index!==current){
  const r=rasters.get(e.index)!;
  const mat=new THREE.MeshBasicMaterial({map:r.texture,transparent:true,depthWrite:false});
  const mesh=new THREE.Mesh(new THREE.PlaneGeometry(r.canvasWidthPx*.62/r.fontPx,r.canvasHeightPx*.62/r.fontPx),mat);
  mesh.position.fromArray(e.position);mesh.quaternion.fromArray(e.quaternion);world.add(mesh);
  meshes.set(e.index,mesh);materials.set(e.index,mat);
 }
 updateDioramaNeighborLines({visibleLines:visible,globalIndex:current,transitionOutgoingIndex:outgoing,meshes,materials,rasters,
  camPos:camera.position,fov:55,aspect:innerWidth/600,lyricsFontScale:1,damped});
 const active=visible.find(e=>e.index===current)!,layout=layouts[current],units=unitSets[current];
 const placement=getDioramaTextPlacement(current-segment.globalStart,seed),group=new THREE.Group();
 group.position.fromArray(active.position);group.quaternion.fromArray(active.quaternion);
 group.scale.setScalar(resolveFrameFitScale(layout.lineWidth,5.2,55,innerWidth/600)*placement.scale);world.add(group);
 const planes:any={baseMats:[],glowMats:[],soulMats:[],glowMeshes:[],soulMeshes:[],unitGroups:[]};
 for(const u of layout.units){
  const ug=new THREE.Group();group.add(ug);planes.unitGroups.push(ug);
  for(const [kind,texture,z,order]of[['glow',u.raster.glowTexture,-.01,17],['soul',u.raster.baseTexture,-.005,18],['base',u.raster.baseTexture,0,19]]as const){
   const mat=new THREE.MeshBasicMaterial({map:texture,transparent:true,depthTest:false,depthWrite:false,
    blending:kind==='base'?THREE.NormalBlending:THREE.AdditiveBlending});
   const mesh=new THREE.Mesh(new THREE.PlaneGeometry(u.width,u.height),mat);mesh.position.set(u.centerX,0,z);mesh.renderOrder=order;ug.add(mesh);
   planes[kind+'Mats'].push(mat);if(kind!=='base')planes[kind+'Meshes'].push(mesh);
  }
 }
 const charge=(u:any,a:number,r:number)=>time<=u.startTime?0:(1-Math.exp(-a*(Math.min(time,u.endTime)-u.startTime)))*Math.exp(-r*Math.max(0,time-u.endTime));
 const life=resolveTextLife(group.position.distanceTo(camera.position));
 updateDioramaActiveUnits({activeLineUnits:units,planes,lightVals:new Float32Array(units.map(u=>charge(u,14,4.5))),
  soulVals:new Float32Array(units.map(u=>charge(u,12,2.2))),now:time,delta:0,life,breath:1,powerEnv:0,damped,
  keywordUnitColors:new Map(),glowIntensity:1,soulIntensity:1,soulActiveEnabled:true,gradientIntensity:1});
 renderer.render(world,camera);
 const forward=camera.getWorldDirection(new THREE.Vector3());
 return{current,outgoing,indices,life,lineWidths:layouts.map(l=>l.lineWidth),
  primary:primary.toArray(),secondary:secondary.toArray(),accent:accent.toArray(),
  renderedPosition:last.renderedPosition,renderedOrientation:last.renderedOrientation,
  neighbors:visible.filter(e=>e.index!==current).map(e=>({index:e.index,outgoing:e.isOutgoing,
   opacity:materials.get(e.index).opacity,fit:meshes.get(e.index).scale.x,color:materials.get(e.index).color.toArray(),
   center:meshes.get(e.index).position.toArray(),depth:meshes.get(e.index).position.clone().sub(camera.position).dot(forward)}))};
}});
