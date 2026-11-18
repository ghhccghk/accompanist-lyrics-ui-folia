// Real locked Three GPU FrontSide and homogeneous near/far clipping.
// Shared ORIGINAL synthetic raster isolates clipping, not own shaping or a theme.
import * as THREE from 'three';
import raster from '../diorama-fixtures/plane-raster.png?url';
const texture=await new THREE.TextureLoader().loadAsync(raster);
texture.colorSpace=THREE.SRGBColorSpace;
const width=texture.image.width,height=texture.image.height,unit=2/width;
const mesh=new THREE.Mesh(new THREE.PlaneGeometry(width*unit,height*unit),
 new THREE.MeshBasicMaterial({map:texture,transparent:true,depthWrite:false,color:0xffffff}));
const renderer=new THREE.WebGLRenderer({antialias:true,preserveDrawingBuffer:true});
renderer.setPixelRatio(1);renderer.setSize(innerWidth,600);renderer.setClearColor(0x162435,1);document.body.append(renderer.domElement);
const scene=new THREE.Scene();scene.add(mesh);
const camera=new THREE.PerspectiveCamera(55,innerWidth/600,.5,2);camera.position.set(0,0,0);camera.lookAt(0,0,-1);camera.updateMatrixWorld();
Object.assign(window,{clipReady:true,drawClip:(distance:number,angle:number)=>{
 mesh.position.set(0,0,-distance);mesh.rotation.set(0,angle,0);mesh.updateMatrixWorld();renderer.render(scene,camera);
 const x=new THREE.Vector3(unit,0,0).applyQuaternion(mesh.quaternion),y=new THREE.Vector3(0,unit,0).applyQuaternion(mesh.quaternion);
 const origin=mesh.position.clone().addScaledVector(x,-width/2).addScaledVector(y,height/2);
 const points=[[0,0],[width,0],[width,height],[0,height]].map(([u,v])=>{
  const world=origin.clone().addScaledVector(x,u).addScaledVector(y,-v);
  const projected=world.clone().project(camera);
  return {u,v,depth:-world.z,ndc:projected.toArray()};
 });
 return {origin:origin.toArray(),x:x.toArray(),y:y.negate().toArray(),width,height,near:.5,far:2,points};
}});
