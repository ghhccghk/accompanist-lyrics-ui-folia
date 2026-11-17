// Actual original neighbour-line raster/path/fit with real Three projection.
// Isolated frozen text plane, NOT the original CameraRig or full Diorama scene.
import * as THREE from 'three';
import {buildDioramaPath,getDioramaTextPlacement,composeLocal}from'../../.work/folia-major/src/components/visualizer/diorama/cameraPath';
import {frameQuaternion,resolveFrameFitScale}from'../../.work/folia-major/src/components/visualizer/diorama/dioramaSceneUnits';
import {rasterDioramaLine}from'../../.work/folia-major/src/components/visualizer/diorama/dioramaTextRaster';
import latin from'../assets/FoliaParityLatin-Bold.ttf?url';import arabic from'../assets/FoliaParityBidi-Bold.ttf?url';import cjk from'../assets/FoliaParityChatCJK-Bold.otf?url';
const name=new URLSearchParams(location.search).get('scene')??'latin';
const text={latin:'Synthetic mirror plane',arabic:'مرحبا بالعالم',cjk:'星光回响 向远方',long:'Synthetic mirror plane '.repeat(8)}[name]!;
const file={arabic,cjk}[name]??latin;const face=new FontFace('Diorama Text Probe',`url(${file})`,{weight:'700'});await face.load();document.fonts.add(face);
const width=innerWidth,height=innerHeight,f=buildDioramaPath(12,'DioramaProbe')[3],p=getDioramaTextPlacement(3,'DioramaProbe',1);
const origin=composeLocal(f,p.offsetR,p.offsetU,0);const raster=rasterDioramaLine(text,'"Diorama Text Probe"',700);
const scale=resolveFrameFitScale(raster.advancePx*(.62/raster.fontPx),5.2,55,width/height)*p.scale;
const mesh=new THREE.Mesh(new THREE.PlaneGeometry(raster.canvasWidthPx*.62/raster.fontPx,raster.canvasHeightPx*.62/raster.fontPx),
 new THREE.MeshBasicMaterial({map:raster.texture,transparent:true,depthWrite:false,color:0xffffff}));
mesh.position.set(origin.x,origin.y,origin.z);mesh.quaternion.set(...frameQuaternion(f,p.roll,p.yaw));mesh.scale.setScalar(scale);
const metricCanvas=document.createElement('canvas'),metricContext=metricCanvas.getContext('2d')!;
metricContext.font=`700 ${raster.fontPx}px "Diorama Text Probe"`;metricContext.textBaseline='alphabetic';
const alphabetic=metricContext.measureText(text);metricContext.textBaseline='middle';const middle=metricContext.measureText(text);
const baselineMetrics={alphabeticAscent:alphabetic.actualBoundingBoxAscent,middleAscent:middle.actualBoundingBoxAscent,
 baselineShift:alphabetic.actualBoundingBoxAscent-middle.actualBoundingBoxAscent,fontAscent:alphabetic.fontBoundingBoxAscent,fontDescent:alphabetic.fontBoundingBoxDescent};
const renderer=new THREE.WebGLRenderer({antialias:true,preserveDrawingBuffer:true});renderer.setPixelRatio(1);renderer.setSize(width,height);renderer.setClearColor(0x162435,1);document.body.append(renderer.domElement);
const world=new THREE.Scene();world.add(mesh);const camera=new THREE.PerspectiveCamera(55,width/height,.1,1000);
Object.assign(window,{ready:true,drawTextProbe:(right:number,up:number)=>{
 const eye=composeLocal(f,p.offsetR+right,p.offsetU+up,-5.2),look=composeLocal(f,p.offsetR+p.lookR,p.offsetU,0);
 camera.position.set(eye.x,eye.y,eye.z);camera.lookAt(look.x,look.y,look.z);camera.updateMatrixWorld();renderer.render(world,camera);
 const gl=renderer.getContext(),ext=gl.getExtension('WEBGL_debug_renderer_info');
 return{text,eye,look,baselineMetrics,advancePx:raster.advancePx,fontPx:raster.fontPx,rasterWidth:raster.canvasWidthPx,rasterHeight:raster.canvasHeightPx,
 renderer:ext?gl.getParameter(ext.UNMASKED_RENDERER_WEBGL):gl.getParameter(gl.RENDERER)};
}});
