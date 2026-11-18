// Actual pinned text raster/layout/material functions + verbatim CameraRig flight.
// Single incoming line, identical explicit incoming refs. No outgoing corridor,
// React mount, background or audio. Analytic media charge injection is disclosed.
import * as THREE from 'three';
import {originalTarget} from 'virtual:diorama-camera-target';
import {originalFlight} from 'virtual:diorama-camera-flight';
import {buildDioramaPath,getDioramaTextPlacement,composeLocal,translateFrames} from '../../.work/folia-major/src/components/visualizer/diorama/cameraPath';
import {pickTransitionOffset} from '../../.work/folia-major/src/components/visualizer/diorama/dioramaTransition';
import {frameQuaternion,resolveFrameFitScale,resolveTextLife} from '../../.work/folia-major/src/components/visualizer/diorama/dioramaSceneUnits';
import {buildLineGraphemeTimeline} from '../../.work/folia-major/src/utils/lyrics/graphemeTiming';
import {splitDioramaLyricUnits,layoutDioramaActiveUnits} from '../../.work/folia-major/src/components/visualizer/diorama/dioramaSceneLayout';
import {updateDioramaActiveUnits} from '../../.work/folia-major/src/components/visualizer/diorama/dioramaSceneFrame';
import latin from '../assets/FoliaParityLatin-Bold.ttf?url';
import arabic from '../assets/FoliaParityBidi-Bold.ttf?url';
import cjk from '../assets/FoliaParityChatCJK-Bold.otf?url';

const name=new URLSearchParams(location.search).get('scene')??'latin';
const tokens={latin:['Synthetic','mirror','plane'],arabic:['مرحبا','بالعالم'],cjk:['星光','回响','向远方']}[name]!;
const file={arabic,cjk}[name]??latin;
const face=new FontFace('Diorama Flight Probe',`url(${file})`,{weight:'700'});
await face.load();document.fonts.add(face);
const text=tokens.join(' '),words=tokens.map((text,i)=>({text,startTime:.7+i*.8,endTime:1.5+i*.8}));
const line={fullText:text,startTime:.7,endTime:.7+tokens.length*.8,words};
const units=splitDioramaLyricUnits(line,buildLineGraphemeTimeline(line));
const layout=layoutDioramaActiveUnits(line,units,'700 128px "Diorama Flight Probe"')!;
const seed='Incoming mirror',epoch=1,origin=pickTransitionOffset(seed,epoch);
const frame=translateFrames(buildDioramaPath(1,seed),origin)[0];
const placement=getDioramaTextPlacement(0,seed),center=composeLocal(frame,placement.offsetR,placement.offsetU,0);
const group=new THREE.Group();group.position.set(center.x,center.y,center.z);
group.quaternion.set(...frameQuaternion(frame,placement.roll,placement.yaw));
group.scale.setScalar(resolveFrameFitScale(layout.lineWidth,5.2,55,innerWidth/600)*placement.scale);
const planes:any={baseMats:[],glowMats:[],soulMats:[],glowMeshes:[],soulMeshes:[],unitGroups:[]};
for(const u of layout.units){
 const ug=new THREE.Group();group.add(ug);planes.unitGroups.push(ug);
 for(const [kind,texture,z,order] of [['glow',u.raster.glowTexture,-.01,17],['soul',u.raster.baseTexture,-.005,18],['base',u.raster.baseTexture,0,19]] as const){
  const mat=new THREE.MeshBasicMaterial({map:texture,transparent:true,depthTest:false,depthWrite:false,
   blending:kind==='base'?THREE.NormalBlending:THREE.AdditiveBlending});
  const mesh=new THREE.Mesh(new THREE.PlaneGeometry(u.width,u.height),mat);
  mesh.position.set(u.centerX,0,z);mesh.renderOrder=order;ug.add(mesh);
  planes[kind+'Mats'].push(mat);if(kind!=='base')planes[kind+'Meshes'].push(mesh);
 }
}
const renderer=new THREE.WebGLRenderer({antialias:true,preserveDrawingBuffer:true});
renderer.setPixelRatio(1);renderer.setSize(innerWidth,600);renderer.setClearColor(0x162435,1);
document.body.append(renderer.domElement);
const world=new THREE.Scene();world.add(group);
const camera=new THREE.PerspectiveCamera(55,innerWidth/600,.1,1000);
const primary=new THREE.Color('#dfe5ec'),accent=new THREE.Color('#f17b4d');
let previous:any,flightState:any,last:any;
Object.assign(window,{flightReady:true,stepFlight:(time:number,delta:number)=>{
 const target=originalTarget({line,now:time,frame,placement,seed,index:0,shotKind:'orbit',
  trackWidth:layout.lineWidth*group.scale.x,aspect:innerWidth/600});
 const result=originalFlight({line,now:time,wallTime:time,frame,placement,shotKind:'orbit',
  delta,aspect:innerWidth/600,target,previous,flightState,transitionEpoch:epoch,shotSeed:seed});
 previous=result.state;flightState=result.flight;last=result;
 camera.position.fromArray(result.renderedPosition);camera.quaternion.fromArray(result.renderedOrientation);
 camera.updateMatrixWorld();return {...result,target};
},drawFlight:(time:number)=>{
 const charge=(u:any,a:number,r:number)=>time<=u.startTime?0:
  (1-Math.exp(-a*(Math.min(time,u.endTime)-u.startTime)))*Math.exp(-r*Math.max(0,time-u.endTime));
 const life=resolveTextLife(camera.position.distanceTo(new THREE.Vector3(center.x,center.y,center.z)));
 updateDioramaActiveUnits({activeLineUnits:units,planes,
  lightVals:new Float32Array(units.map(u=>charge(u,14,4.5))),soulVals:new Float32Array(units.map(u=>charge(u,12,2.2))),
  now:time,delta:0,life,breath:1,powerEnv:0,damped:{primary,accent,secondary:primary,background:primary},
  keywordUnitColors:new Map(),glowIntensity:1,soulIntensity:1,soulActiveEnabled:true,gradientIntensity:1});
 renderer.render(world,camera);
 return {text,words,origin,lineWidth:layout.lineWidth,primary:primary.toArray(),accent:accent.toArray(),life,
  renderedPosition:last.renderedPosition,renderedOrientation:last.renderedOrientation};
}});
