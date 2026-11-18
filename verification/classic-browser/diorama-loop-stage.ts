// Complete text-only original stage: source camera blocks, plain window,
// complete source rasters and active three layers. Not a mounted React scene.
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
const face=new FontFace('Diorama Loop Stage',`url(${latin})`,{weight:'700'});await face.load();document.fonts.add(face);
const lines=Array.from({length:3},(_,i)=>({fullText:`Synthetic loop line ${i}`,startTime:(700+i*1400)/1000,
 endTime:(1500+i*1400)/1000,words:[]}));
const seed='Incoming mirror',seq=createSequencerState(),origin=pickTransitionOffset(seed,2);
const segment=appendSegment(seq,{seed,lines,round:0,placementOrigin:origin});
const unitSets=lines.map(l=>splitDioramaLyricUnits(l,buildLineGraphemeTimeline(l)));
const layouts=lines.map((l,i)=>layoutDioramaActiveUnits(l,unitSets[i],'700 128px "Diorama Loop Stage"')!);
const rasters=new Map(lines.map((l,i)=>[i,rasterDioramaLine(l.fullText,'Diorama Loop Stage',700)]));
const renderer=new THREE.WebGLRenderer({antialias:true,preserveDrawingBuffer:true});
renderer.setPixelRatio(1);renderer.setSize(innerWidth,600);renderer.setClearColor(0x162435,1);document.body.append(renderer.domElement);
const world=new THREE.Scene(),camera=new THREE.PerspectiveCamera(55,innerWidth/600,.1,1000);
const primary=new THREE.Color('#dfe5ec'),secondary=new THREE.Color('#758daa'),accent=new THREE.Color('#f17b4d');
const damped={primary,secondary,accent,background:new THREE.Color('#162435')};
let previous:any,flightState:any,last:any;
const currentAt=(t:number)=>Math.max(0,lines.findLastIndex(l=>l.startTime<=t));
function step(time:number,delta:number,epoch:number){
 const current=currentAt(time),frame=segment.frames[current],placement=getDioramaTextPlacement(current,seed);
 const trackWidth=layouts[current].lineWidth*resolveFrameFitScale(layouts[current].lineWidth,5.2,55,innerWidth/600)*placement.scale;
 const target=originalTarget({line:lines[current],now:time,frame,placement,seed,index:current,shotKind:'orbit',trackWidth,aspect:innerWidth/600});
 last=originalFlight({line:lines[current],now:time,wallTime:time,frame,placement,shotKind:'orbit',delta,aspect:innerWidth/600,
  target,previous,flightState,transitionEpoch:epoch,shotSeed:seed});
 previous=last.state;flightState=last.flight;
 camera.position.fromArray(last.renderedPosition);camera.quaternion.fromArray(last.renderedOrientation);camera.updateMatrixWorld();
 return {...last,target};
}
Object.assign(window,{loopStageReady:true,loopInputs:{lines,seed,origin,epoch:3,outgoing:2},
 stepOutgoing:(t:number,dt:number)=>step(t,dt,2),beginLoop:()=>{flightState=undefined;return previous;},
 stepLoop:(t:number,dt:number)=>step(t,dt,3),drawLoop:(time:number)=>{
 for(const child of [...world.children]){
  child.traverse((o:any)=>{o.geometry?.dispose();o.material?.dispose();});world.remove(child);
 }
 const current=currentAt(time),outgoing=time<3.2?2:null;
 const indices=resolveDioramaMountedIndices(current,outgoing,lines.length);
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
 const placement=getDioramaTextPlacement(current,seed),group=new THREE.Group();
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
 return{current,outgoing,indices,life,lineWidths:layouts.map(l=>l.lineWidth),
  primary:primary.toArray(),secondary:secondary.toArray(),accent:accent.toArray(),
  renderedPosition:last.renderedPosition,renderedOrientation:last.renderedOrientation,
  neighbors:visible.filter(e=>e.index!==current).map(e=>({index:e.index,outgoing:e.isOutgoing,
   opacity:materials.get(e.index).opacity,fit:meshes.get(e.index).scale.x,color:materials.get(e.index).color.toArray()}))};
}});
