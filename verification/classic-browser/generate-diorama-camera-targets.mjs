import fs from'node:fs';import path from'node:path';import{createServer}from'vite';
import{root,revision,checkReference}from'./reference.mjs';import{cameraTargetPlugin,sourceFragments}from'./diorama-camera-source.mjs';
checkReference();const server=await createServer({configFile:false,root:path.join(root,'verification/classic-browser'),plugins:[cameraTargetPlugin()],optimizeDeps:{noDiscovery:true,entries:[]},
 cacheDir:path.join(root,'.work/diorama-camera-numeric-vite'),server:{middlewareMode:true,fs:{allow:[root]}}});
try{
 const {originalTarget}=await server.ssrLoadModule('virtual:diorama-camera-target');
 const cam=await server.ssrLoadModule(path.join(root,'.work/folia-major/src/components/visualizer/diorama/cameraPath.ts'));
 const cases=[],values=[];
 const shots=['pushIn','pullBack','orbit','track','crane','hold','swell','spiral','pendulum','flyby','arc','float','glide'];
 const inputLines=[
 {fullText:'Synthetic mirror plane',startTime:.7,endTime:3.1,words:['Synthetic','mirror','plane'].map((text,i)=>({text,startTime:.7+i*.8,endTime:1.5+i*.8}))},
 {fullText:'مرحبا بالعالم',startTime:.7,endTime:2.3,words:['مرحبا','بالعالم'].map((text,i)=>({text,startTime:.7+i*.8,endTime:1.5+i*.8}))},
 {fullText:'星光 回响 向远方',startTime:.7,endTime:3.1,words:['星光','回响','向远方'].map((text,i)=>({text,startTime:.7+i*.8,endTime:1.5+i*.8}))},
 {fullText:'Micro',startTime:1,endTime:1.02,words:[]},
 {fullText:'Last end',startTime:1,endTime:1.17,words:[{text:'Last',startTime:1,endTime:4},{text:'end',startTime:1.1,endTime:1.17}]},
 {fullText:'',startTime:1,endTime:1,words:[]}];
 const seed='DioramaProbe';
 for(const [li,line]of inputLines.entries())for(const shotKind of shots)for(const now of[0,.7,1,1.02,1.1,1.5,2.3,3.1,3.45,6.42,8.92,11.42])for(const width of[380,800,801]){
 const index=li+3,frame=cam.buildDioramaPath(12,seed)[index],placement=cam.getDioramaTextPlacement(index,seed,1);
 const trackWidth=li%2?12:4,moveScale=li===3?0:li===4?1.35:1,driftScale=li===3?0:.62;
 const input={line,now,frame,placement,seed,index,shotKind,trackWidth,aspect:width/600,moveScale,driftScale};
 const out=originalTarget(input);cases.push({li,shotKind,now,width,trackWidth,moveScale,driftScale});
 values.push([...Object.values(out.eye),...Object.values(out.look),out.progress,out.wordProgress,out.settle,out.truck,out.renderEnd]);
 }
 const folder=path.join(root,'verification/diorama-fixtures');
 fs.writeFileSync(path.join(folder,'camera-target-inputs.tsv'),['# lineIndex shot now width trackWidth moveScale driftScale',...cases.map(c=>Object.values(c).join('\t'))].join('\n')+'\n');
 fs.writeFileSync(path.join(folder,'camera-target-values.tsv'),['# eyeXYZ lookXYZ progress wordProgress settle truck renderEnd',...values.map(r=>r.join('\t'))].join('\n')+'\n');
 fs.writeFileSync(path.join(folder,'camera-target-conditions.json'),JSON.stringify({revision,cases:cases.length,seed,shots,inputLines,sourceFragments,
 scope:'Exact private original progress/target blocks exported without modification. No SmoothDamp/quaternion/alignment/transition. No source renderHint override; raw last-word semantics retained.'},null,2)+'\n');
 console.log(JSON.stringify({cases:cases.length}));
}finally{await server.close();}
