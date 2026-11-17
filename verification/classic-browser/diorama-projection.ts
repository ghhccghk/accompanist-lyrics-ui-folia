// Isolated frozen plane, not a reconstructed VisualizerDiorama or CameraRig.
import * as THREE from 'three';
import { buildDioramaPath,getDioramaTextPlacement,composeLocal } from '../../.work/folia-major/src/components/visualizer/diorama/cameraPath';
import { frameQuaternion,resolveFrameFitScale } from '../../.work/folia-major/src/components/visualizer/diorama/dioramaSceneUnits';
import { rasterDioramaLine } from '../../.work/folia-major/src/components/visualizer/diorama/dioramaTextRaster';
import fontUrl from '../assets/FoliaParityLatin-Bold.ttf?url';
const font=new FontFace('Diorama Probe',`url(${fontUrl})`,{weight:'700'});await font.load();document.fonts.add(font);
const width=innerWidth,height=innerHeight;const f=buildDioramaPath(12,'DioramaProbe')[3];const p=getDioramaTextPlacement(3,'DioramaProbe',1);
const origin=composeLocal(f,p.offsetR,p.offsetU,0);const raster=rasterDioramaLine('Synthetic mirror plane','"Diorama Probe"',700);
const scale=resolveFrameFitScale(raster.advancePx*(.62/raster.fontPx),5.2,55,width/height)*p.scale;
const mesh=new THREE.Mesh(new THREE.PlaneGeometry(raster.canvasWidthPx*.62/raster.fontPx,raster.canvasHeightPx*.62/raster.fontPx),
 new THREE.MeshBasicMaterial({map:raster.texture,transparent:true,depthWrite:false,color:0xffffff}));
mesh.position.set(origin.x,origin.y,origin.z);mesh.quaternion.set(...frameQuaternion(f,p.roll,p.yaw));mesh.scale.setScalar(scale);
const renderer=new THREE.WebGLRenderer({antialias:true,preserveDrawingBuffer:true});renderer.setPixelRatio(1);renderer.setSize(width,height);
renderer.setClearColor(0x162435,1);document.body.append(renderer.domElement);const scene=new THREE.Scene();scene.add(mesh);
const camera=new THREE.PerspectiveCamera(55,width/height,.1,1000);
Object.assign(window,{rasterPng:(raster.texture.image as HTMLCanvasElement).toDataURL('image/png'),
 drawProbe:(right:number,up:number)=>{
  const eye=composeLocal(f,p.offsetR+right,p.offsetU+up,-5.2),look=composeLocal(f,p.offsetR+p.lookR,p.offsetU,0);
  camera.position.set(eye.x,eye.y,eye.z);camera.lookAt(look.x,look.y,look.z);camera.updateMatrixWorld();renderer.render(scene,camera);
  const axes=[new THREE.Vector3(1,0,0),new THREE.Vector3(0,1,0)].map(v=>v.applyQuaternion(mesh.quaternion).multiplyScalar(scale*.62/raster.fontPx));
  const gl=renderer.getContext();const ext=gl.getExtension('WEBGL_debug_renderer_info');
  return {eye,look,origin,x:axes[0],y:axes[1],rasterWidth:raster.canvasWidthPx,rasterHeight:raster.canvasHeightPx,
    renderer:ext?gl.getParameter(ext.UNMASKED_RENDERER_WEBGL):gl.getParameter(gl.RENDERER)};
 },ready:true});
