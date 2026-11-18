// Independent oracle: the installed original-lock Three MeshBasicMaterial
// shader writes the default framebuffer. No JavaScript copy of ACES is used.
import * as THREE from 'three';
import { FOG_NEAR, FOG_FAR } from '../../.work/folia-major/src/components/visualizer/diorama/dioramaSceneConstants';

const renderer=new THREE.WebGLRenderer({antialias:false,preserveDrawingBuffer:true});
renderer.setPixelRatio(1);renderer.setSize(16,16);renderer.setClearColor(0x162435,1);
renderer.outputColorSpace=THREE.SRGBColorSpace;renderer.toneMappingExposure=1;
document.body.append(renderer.domElement);
const scene=new THREE.Scene();scene.fog=new THREE.Fog(0x162435,FOG_NEAR,FOG_FAR);
const camera=new THREE.OrthographicCamera(-1,1,1,-1,.1,100);
const material=new THREE.MeshBasicMaterial({transparent:true,depthWrite:false});
const geometry=new THREE.PlaneGeometry(2,2),mesh=new THREE.Mesh(geometry,material);scene.add(mesh);
const colors=[
 [0,0,0],[.001,.001,.001],[.0031308,.0031308,.0031308],[.01,.01,.01],
 [.18,.18,.18],[.5,.5,.5],[1,1,1],[4,4,4],
 [1,0,0],[0,1,0],[0,0,1],[2,.2,.01],[.02,.4,1.7],
 ...[0xdfe5ec,0x758daa,0xf17b4d].map(hex=>new THREE.Color(hex).toArray())
];
const rows=[];
for(const mapping of ['None','AcesFilmic'])for(const color of colors)
 for(const alpha of [1,.63,.2])for(const additive of [false,true])for(const depth of [6,12,21,30,36]) {
  renderer.toneMapping=mapping==='None'?THREE.NoToneMapping:THREE.ACESFilmicToneMapping;
  material.color.setRGB(...color as [number,number,number]);material.opacity=alpha;
  material.blending=additive?THREE.AdditiveBlending:THREE.NormalBlending;
  mesh.position.z=-depth;renderer.render(scene,camera);
  const rgba=new Uint8Array(4),gl=renderer.getContext();gl.readPixels(8,8,1,1,gl.RGBA,gl.UNSIGNED_BYTE,rgba);
  if(gl.getError()!==gl.NO_ERROR)throw Error('Original framebuffer read failed');
  rows.push({mapping,color,alpha,additive,depth,rgba:Array.from(rgba)});
 }
const output={rows,three:THREE.REVISION,outputColorSpace:renderer.outputColorSpace,
 toneMappingExposure:renderer.toneMappingExposure,fog:[FOG_NEAR,FOG_FAR],context:renderer.getContext().getContextAttributes()};
geometry.dispose();material.dispose();renderer.dispose();renderer.forceContextLoss();
Object.assign(window,{toneMappingOracle:output});
