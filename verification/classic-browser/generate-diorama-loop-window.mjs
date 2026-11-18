import fs from 'node:fs';import path from 'node:path';import{createHash}from'node:crypto';
import{root,upstream,revision,referenceModules,referenceMotionVersions}from'./reference.mjs';
const folder=path.join(root,'verification/diorama-fixtures/loop-window-v1');
if(fs.existsSync(folder))throw Error('Preserve original loop-window fixture; refusing overwrite');
const{server,load}=await referenceModules();
try{
 const camera=await load('components/visualizer/diorama/cameraPath.ts');
 const seqmod=await load('components/visualizer/diorama/dioramaSequencer.ts');
 const layout=await load('components/visualizer/diorama/dioramaSceneLayout.ts');
 const material=await load('components/visualizer/diorama/dioramaSceneFrame.ts');
 const THREE=await server.ssrLoadModule('three');
 const windows=[],rows=[];const seed='DioramaLoopWindow',origin={x:3,y:2,z:-6};
 for(const total of [1,8,12]){
  const seq=seqmod.createSequencerState();
  seqmod.appendSegment(seq,{seed,round:0,placementOrigin:origin,
   lines:Array.from({length:total},(_,i)=>({fullText:`Synthetic window ${i}`,startTime:i,endTime:i+.8,words:[]}))});
  for(const current of [...new Set([0,Math.floor(total/2),total-1])])
   for(const outgoing of [...new Set([null,0,Math.floor(total/2),total-1])]){
    const indices=layout.resolveDioramaMountedIndices(current,outgoing,total);
    const entries=layout.buildDioramaVisibleLines(seq,indices,current,outgoing,1);
    const key=`${total}-${current}-${outgoing??'none'}`;
    windows.push([key,total,current,outgoing??-1,indices.join(','),entries.map(e=>+e.isOutgoing).join(',')]);
    for(const width of[380,800])for(const eye of[[0,.6,9],[10,4,-30]]){
     const meshes=new Map(),materials=new Map(),rasters=new Map();
     for(const e of entries){
      const mesh=new THREE.Mesh();mesh.position.fromArray(e.position);meshes.set(e.index,mesh);
      materials.set(e.index,new THREE.MeshBasicMaterial());rasters.set(e.index,{advancePx:300+e.index*37,fontPx:128});
     }
     const primary=new THREE.Color(.74,.78,.84),secondary=new THREE.Color(.23,.31,.42);
     const cam=new THREE.PerspectiveCamera(55,width/600,.1,1000);cam.position.fromArray(eye);cam.lookAt(0,0,-10);cam.updateMatrixWorld();
     material.updateDioramaNeighborLines({visibleLines:entries,globalIndex:current,transitionOutgoingIndex:outgoing,
      meshes,materials,rasters,camPos:cam.position,fov:55,aspect:width/600,lyricsFontScale:1,
      damped:{primary,secondary,accent:primary,background:secondary}});
     const forward=cam.getWorldDirection(new THREE.Vector3());
     for(const e of entries)if(e.index!==current){
      const m=meshes.get(e.index),mat=materials.get(e.index),depth=m.position.clone().sub(cam.position).dot(forward);
      rows.push([key,width,...eye,e.index,+e.isOutgoing,m.scale.x,mat.opacity,...mat.color.toArray(),...m.position.toArray(),depth]);
     }
    }
   }
 }
 fs.mkdirSync(folder,{recursive:true});
 fs.writeFileSync(path.join(folder,'windows.tsv'),['# key total current outgoing(-1=null) sortedIndices outgoingFlags',...windows.map(r=>r.join('\t'))].join('\n')+'\n');
 fs.writeFileSync(path.join(folder,'materials.tsv'),['# key width eye3 index outgoing fit opacity linearColor3 center3 viewDepth',...rows.map(r=>r.join('\t'))].join('\n')+'\n');
 const sources=['dioramaSceneLayout.ts','dioramaSceneFrame.ts','dioramaSceneConstants.ts','dioramaSequencer.ts','cameraPath.ts'];
 const hash=p=>createHash('sha256').update(fs.readFileSync(p)).digest('hex');
 fs.writeFileSync(path.join(folder,'conditions.json'),JSON.stringify({revision,motion:referenceMotionVersions(),seed,origin,
  windows:windows.length,materialRows:rows.length,sourceHashes:sources.map(file=>({file,
   sha256:hash(path.join(upstream,'src/components/visualizer/diorama',file))})),
  harnessSha256:hash(new URL(import.meta.url)),lockSha256:hash(path.join(root,'verification/classic-browser/pnpm-lock.yaml')),
  scope:'Actual original mounted/visible/neighbor-material functions with original Three objects. Single-segment loop windows, null/overlap/endpoints. Fixed synthetic advance and camera controls; no raster/React mount/full source scene or device proof.'},null,2)+'\n');
 console.log(JSON.stringify({windows:windows.length,materials:rows.length}));
}finally{await server.close()}
