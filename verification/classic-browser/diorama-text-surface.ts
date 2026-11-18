// Actual locked Three material/shader/WebGL blend state, transparent canvas.
// No JS copy of a native compositor supplies expected bytes.
import * as THREE from 'three';
const renderer=new THREE.WebGLRenderer({alpha:true,antialias:false,preserveDrawingBuffer:true});
renderer.setPixelRatio(1);renderer.setSize(192,192);renderer.setClearColor(0,0);
renderer.outputColorSpace=THREE.SRGBColorSpace;renderer.toneMapping=THREE.NoToneMapping;
document.body.append(renderer.domElement);
const scene=new THREE.Scene(),camera=new THREE.OrthographicCamera(0,192,192,0,.1,100);
const geometry=new THREE.PlaneGeometry(16,16),materials:THREE.MeshBasicMaterial[]=[];
const palette=[new THREE.Color(0xdfe5ec).toArray(),new THREE.Color(0xf17b4d).toArray(),[0,0,0]];
const rows=[];
for(const color of palette)for(const glow of [0,.015,.035,.1])for(const soul of [0,.06,.8])for(const base of [0,.44,1]){
 const i=rows.length,x=(i%12)*16+8,y=192-(Math.floor(i/12)*16+8);
 for(const [order,opacity,additive] of [[17,glow,true],[18,soul,true],[19,base,false]] as const){
  if(opacity===0)continue;
  const material=new THREE.MeshBasicMaterial({transparent:true,depthWrite:false,depthTest:false,opacity,
   blending:additive?THREE.AdditiveBlending:THREE.NormalBlending});
  material.color.setRGB(...color as [number,number,number]);materials.push(material);
  const mesh=new THREE.Mesh(geometry,material);mesh.position.set(x,y,-6);mesh.renderOrder=order;scene.add(mesh);
 }
 rows.push({color,glow,soul,base});
}
renderer.render(scene,camera);
const gl=renderer.getContext(),raw=new Uint8Array(192*192*4);gl.readPixels(0,0,192,192,gl.RGBA,gl.UNSIGNED_BYTE,raw);
if(gl.getError()!==gl.NO_ERROR)throw Error('Source transparent framebuffer read failed');
const observed=rows.map((row,i)=>({...row,rgba:Array.from(raw.slice(((191-(Math.floor(i/12)*16+8))*192+(i%12)*16+8)*4,
 ((191-(Math.floor(i/12)*16+8))*192+(i%12)*16+8)*4+4))}));
Object.assign(window,{surfaceOracle:{rows:observed,context:gl.getContextAttributes(),
 clearColor:Array.from(gl.getParameter(gl.COLOR_CLEAR_VALUE)),three:THREE.REVISION},
 releaseSurfaceOracle:()=>{for(const m of materials)m.dispose();geometry.dispose();scene.clear();renderer.dispose();renderer.forceContextLoss();return{materials:materials.length,geometries:1}}});
