// Original pure functions and actual locked Three.js0.185.1; no source rewrites.
import fs from 'node:fs';import path from 'node:path';import * as THREE from 'three';
import {referenceModules,root,revision} from './reference.mjs';
const {server,load}=await referenceModules();
try{
 const c=await load('components/visualizer/diorama/cameraPath.ts');
 const u=await load('components/visualizer/diorama/dioramaSceneUnits.ts');
 const rows=[`# original Folia ${revision}; Three.js0.185.1; frozen explicit view NOT CameraRig playback`,
  '# kind\tseedB64\tindex\tweave\tviewportWidth\targuments\texpected'];
 const emit=(kind,seed,index,weave,width,args,expected)=>rows.push([kind,Buffer.from(seed).toString('base64'),index,weave,width,args.join(','),expected.join(',')].join('\t'));
 const vec=v=>[v.x,v.y,v.z];
 for(const seed of['DioramaProbe','合成镜台','A😀B','']){
  const frames=c.buildDioramaPath(12,seed);
  for(const [index,f] of frames.entries()){
   emit('frame',seed,index,1,800,[],[...vec(f.position),...vec(f.forward),...vec(f.right),...vec(f.up)]);
   for(const weave of[0,.5,1,1.35]){
    const p=c.getDioramaTextPlacement(index,seed,weave);const origin=c.composeLocal(f,p.offsetR,p.offsetU,0);
    emit('placement',seed,index,weave,800,[],Object.values(p));
    const q=new THREE.Quaternion(...u.frameQuaternion(f,p.roll,p.yaw));
    for(const xy of[[1,0],[0,1],[1.3,-.7]])emit('axis',seed,index,weave,800,xy,new THREE.Vector3(...xy,0).applyQuaternion(q).toArray());
    for(const width of[380,800,801]){
     const eye=c.composeLocal(f,p.offsetR+1.7,p.offsetU+.8,-5.2);
     const look=c.composeLocal(f,p.offsetR+p.lookR,p.offsetU,0);
     const camera=new THREE.PerspectiveCamera(55,width/600,.1,1000);camera.position.set(...vec(eye));camera.lookAt(new THREE.Vector3(...vec(look)));camera.updateMatrixWorld();
     for(const xy of[[-1.4,-.4],[1.4,-.4],[-1.4,.4],[1.4,.4],[0,0]]){
      const world=new THREE.Vector3(...xy,0).applyQuaternion(q).add(new THREE.Vector3(...vec(origin)));
      const ndc=world.clone().project(camera);const depth=-world.clone().applyMatrix4(camera.matrixWorldInverse).z;
      emit('project',seed,index,weave,width,[...vec(eye),...vec(look),...xy],[(ndc.x+1)*width/2,(1-ndc.y)*600/2,depth]);
     }
    }
   }
  }
 }
 for(const width of[0,1,4,20,100])for(const aspect of[380/600,800/600])emit('fit','',0,0,800,[width,5.2,55,aspect],[u.resolveFrameFitScale(width,5.2,55,aspect)]);
 for(const d of[0,.8999,.9,1,1.5,2,12,30,32,36,39.9,40,100])emit('life','',0,0,800,[d],[u.resolveTextLife(d)]);
 const out=path.join(root,'verification/diorama-fixtures');fs.mkdirSync(out,{recursive:true});
 fs.writeFileSync(path.join(out,'projection.tsv'),rows.join('\n')+'\n');console.log(JSON.stringify({rows:rows.length-2}));
}finally{await server.close();}
