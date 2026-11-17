// Isolated original three-layer raster/material stage. Analytic media charges injected, delta0; NOT original RAF.
import * as THREE from'three';
import {buildDioramaPath,getDioramaTextPlacement,composeLocal}from'../../.work/folia-major/src/components/visualizer/diorama/cameraPath';
import{frameQuaternion,resolveFrameFitScale}from'../../.work/folia-major/src/components/visualizer/diorama/dioramaSceneUnits';
import{buildLineGraphemeTimeline}from'../../.work/folia-major/src/utils/lyrics/graphemeTiming';
import{splitDioramaLyricUnits,layoutDioramaActiveUnits}from'../../.work/folia-major/src/components/visualizer/diorama/dioramaSceneLayout';
import{updateDioramaActiveUnits}from'../../.work/folia-major/src/components/visualizer/diorama/dioramaSceneFrame';
import latin from'../assets/FoliaParityLatin-Bold.ttf?url';import arabic from'../assets/FoliaParityBidi-Bold.ttf?url';import cjk from'../assets/FoliaParityChatCJK-Bold.otf?url';
const name=new URLSearchParams(location.search).get('scene')??'latin';
const tokens={latin:['Synthetic','mirror','plane'],arabic:['مرحبا','بالعالم'],cjk:['星光','回响','向远方']}[name]!;
const file={arabic,cjk}[name]??latin;const face=new FontFace('Diorama Active Probe',`url(${file})`,{weight:'700'});await face.load();document.fonts.add(face);
const text=tokens.join(' '),words=tokens.map((text,i)=>({text,startTime:.7+i*.8,endTime:1.5+i*.8}));
const line={fullText:text,startTime:.7,endTime:.7+tokens.length*.8,words};const units=splitDioramaLyricUnits(line,buildLineGraphemeTimeline(line));
const layout=layoutDioramaActiveUnits(line,units,'700 128px "Diorama Active Probe"')!;
const f=buildDioramaPath(12,'DioramaProbe')[3],p=getDioramaTextPlacement(3,'DioramaProbe',1),origin=composeLocal(f,p.offsetR,p.offsetU,0);
const group=new THREE.Group();group.position.set(origin.x,origin.y,origin.z);group.quaternion.set(...frameQuaternion(f,p.roll,p.yaw));
group.scale.setScalar(resolveFrameFitScale(layout.lineWidth,5.2,55,innerWidth/600)*p.scale);
const planes={baseMats:[],glowMats:[],soulMats:[],glowMeshes:[],soulMeshes:[],unitGroups:[]};
for(const u of layout.units){
 const ug=new THREE.Group();group.add(ug);planes.unitGroups.push(ug);
 for(const [kind,texture,z,order]of[['glow',u.raster.glowTexture,-.01,17],['soul',u.raster.baseTexture,-.005,18],['base',u.raster.baseTexture,0,19]]as const){
  const material=new THREE.MeshBasicMaterial({map:texture,transparent:true,depthTest:false,depthWrite:false,
   blending:kind==='base'?THREE.NormalBlending:THREE.AdditiveBlending});
  const mesh=new THREE.Mesh(new THREE.PlaneGeometry(u.width,u.height),material);mesh.position.set(u.centerX,0,z);mesh.renderOrder=order;ug.add(mesh);
  planes[kind+'Mats'].push(material);if(kind!=='base')planes[kind+'Meshes'].push(mesh);
 }
}
const renderer=new THREE.WebGLRenderer({antialias:true,preserveDrawingBuffer:true});renderer.setPixelRatio(1);renderer.setSize(innerWidth,600);renderer.setClearColor(0x162435,1);document.body.append(renderer.domElement);
const world=new THREE.Scene();world.add(group);const camera=new THREE.PerspectiveCamera(55,innerWidth/600,.1,1000);
const eye=composeLocal(f,p.offsetR+1.7,p.offsetU+.8,-5.2),look=composeLocal(f,p.offsetR+p.lookR,p.offsetU,0);camera.position.set(eye.x,eye.y,eye.z);camera.lookAt(look.x,look.y,look.z);camera.updateMatrixWorld();
const primary=new THREE.Color('#dfe5ec'),accent=new THREE.Color('#f17b4d');
Object.assign(window,{ready:true,targetStage:{line,layout,frame:f,placement:p,camera,group},rasters:()=>layout.units.map(u=>({base:u.raster.baseTexture.image.toDataURL(),halo:u.raster.glowTexture.image.toDataURL()})),drawLayers:(time:number,active:boolean)=>{
 const charge=(u:any,a:number,r:number)=>time<=u.startTime?0:(1-Math.exp(-a*(Math.min(time,u.endTime)-u.startTime)))*Math.exp(-r*Math.max(0,time-u.endTime));
 updateDioramaActiveUnits({activeLineUnits:units,planes,lightVals:new Float32Array(units.map(u=>charge(u,14,4.5))),soulVals:new Float32Array(units.map(u=>charge(u,12,2.2))),now:time,delta:0,
  life:1,breath:1,powerEnv:0,damped:{primary,accent,secondary:primary,background:primary},keywordUnitColors:new Map(),glowIntensity:1,soulIntensity:1,soulActiveEnabled:active,gradientIntensity:1});
 renderer.render(world,camera);return{text,words,eye,look,lineWidth:layout.lineWidth,primary:primary.toArray(),accent:accent.toArray(),
  units:units.map((u,i)=>({...u,centerX:layout.units[i].centerX,width:layout.units[i].width,height:layout.units[i].height,fill:planes.baseMats[i].color.toArray(),opacity:planes.baseMats[i].opacity,glowOpacity:planes.glowMats[i].opacity,glowVisible:planes.glowMeshes[i].visible,soulOpacity:planes.soulMats[i].opacity,soulVisible:planes.soulMeshes[i].visible,soulLift:planes.soulMeshes[i].position.y,soulScale:planes.soulMeshes[i].scale.x}))};
}});
