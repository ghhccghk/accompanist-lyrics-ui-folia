// Actual installed Three shader, original Folia fog constants, original shared
// synthetic raster. Solid-white controls isolate fog algebra from glyph edges.
import * as THREE from 'three';
import {FOG_NEAR,FOG_FAR} from '../../.work/folia-major/src/components/visualizer/diorama/dioramaSceneConstants';
import raster from '../diorama-fixtures/plane-raster.png?url';
const glyph=await new THREE.TextureLoader().loadAsync(raster);glyph.colorSpace=THREE.SRGBColorSpace;
const width=glyph.image.width,height=glyph.image.height;
const canvas=document.createElement('canvas');canvas.width=width;canvas.height=height;
const ctx=canvas.getContext('2d')!;ctx.fillStyle='white';ctx.fillRect(0,0,width,height);
const solid=new THREE.CanvasTexture(canvas);solid.colorSpace=THREE.SRGBColorSpace;
const renderer=new THREE.WebGLRenderer({antialias:true,preserveDrawingBuffer:true});
renderer.setPixelRatio(1);renderer.setSize(innerWidth,600);renderer.setClearColor(0x162435,1);document.body.append(renderer.domElement);
const scene=new THREE.Scene();scene.fog=new THREE.Fog(0x162435,FOG_NEAR,FOG_FAR);
const camera=new THREE.PerspectiveCamera(55,innerWidth/600,.1,1000);camera.lookAt(0,0,-1);camera.updateMatrixWorld();
const unit=32/width,material=new THREE.MeshBasicMaterial({map:glyph,color:0xdfe5ec,opacity:.63,transparent:true,depthWrite:false});
const mesh=new THREE.Mesh(new THREE.PlaneGeometry(width*unit,height*unit),material);scene.add(mesh);
const cases=[{name:'before',depth:6,angle:0,x:0},{name:'near-cross',depth:12,angle:1.1,x:0},
 {name:'middle',depth:21,angle:0,x:0},{name:'whole-band',depth:21,angle:1.3,x:0},
 {name:'off-axis',depth:21,angle:.65,x:9},{name:'far-cross',depth:30,angle:1.1,x:0},
 {name:'beyond',depth:36,angle:0,x:0}];
Object.assign(window,{fogPlaneReady:true,fogPlaneCases:cases,drawFogPlane:(name:string,mask:string,additive:boolean)=>{
 const c=cases.find(c=>c.name===name)!;
 material.map=mask==='solid'?solid:glyph;material.blending=additive?THREE.AdditiveBlending:THREE.NormalBlending;
 mesh.position.set(c.x,0,-c.depth);mesh.rotation.set(0,c.angle,.12);mesh.updateMatrixWorld();renderer.render(scene,camera);
 const x=new THREE.Vector3(unit,0,0).applyQuaternion(mesh.quaternion),y=new THREE.Vector3(0,unit,0).applyQuaternion(mesh.quaternion);
 const origin=mesh.position.clone().addScaledVector(x,-width/2).addScaledVector(y,height/2);
 return {name,mask,additive,alpha:material.opacity,width,height,origin:origin.toArray(),x:x.toArray(),y:y.negate().toArray(),
  center:mesh.position.toArray(),angle:c.angle,near:FOG_NEAR,far:FOG_FAR};
}});
