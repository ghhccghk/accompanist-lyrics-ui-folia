import fs from 'node:fs';import path from 'node:path';import{createHash,createHash as hash}from'node:crypto';
import{createRequire}from'node:module';import{createServer}from'vite';
import{root,revision,checkReference,referenceMotionVersions}from'./reference.mjs';
import{cameraTargetPlugin,sourceFragments}from'./diorama-camera-source.mjs';
import{cameraFollowPlugin,followSourceFragments}from'./diorama-follow-source.mjs';
import{cameraFlightPlugin,flightSourceFragments}from'./diorama-flight-source.mjs';
checkReference();const folder=path.join(root,'verification/diorama-fixtures/flight-v1');
if(fs.existsSync(folder))throw Error('Preserve original flight fixture; refusing overwrite');
const require=createRequire(import.meta.url);
const server=await createServer({configFile:false,root:path.join(root,'verification/classic-browser'),
 plugins:[cameraTargetPlugin(),cameraFollowPlugin(),cameraFlightPlugin()],
 resolve:{alias:{three:path.join(path.dirname(require.resolve('three')),'three.module.js')}},
 optimizeDeps:{noDiscovery:true,entries:[]},cacheDir:path.join(root,'.work/diorama-flight-numeric'),
 server:{middlewareMode:true,fs:{allow:[root]}}});
try{
 const{originalTarget}=await server.ssrLoadModule('virtual:diorama-camera-target');
 const{originalFollow}=await server.ssrLoadModule('virtual:diorama-camera-follow');
 const{originalFlight}=await server.ssrLoadModule('virtual:diorama-camera-flight');
 const cam=await server.ssrLoadModule(path.join(root,'.work/folia-major/src/components/visualizer/diorama/cameraPath.ts'));
 const tr=await server.ssrLoadModule(path.join(root,'.work/folia-major/src/components/visualizer/diorama/dioramaTransition.ts'));
 const line={fullText:'Synthetic mirror plane',startTime:.7,endTime:3.1,
  words:['Synthetic','mirror','plane'].map((text,i)=>({text,startTime:.7+i*.8,endTime:1.5+i*.8}))};
 const outgoingSeed='DioramaProbe',outPath=cam.buildDioramaPath(6,outgoingSeed);
 const oldFrame=outPath[3],oldPlacement=cam.getDioramaTextPlacement(3,outgoingSeed);
 const flat=s=>[...s.position,...s.velocity,...s.look,...s.lookVelocity,s.alignment,
  ...(s.alignmentOrientation??[0,0,0,1]),...(s.orientation??[0,0,0,1]),s.alignmentOrientation?1:0,s.orientation?1:0];
 const records=[];
 for(const seed of ['Incoming mirror','Incoming 🎵'])for(const width of [380,800])for(const epoch of [1,2,7]){
  let previous;
  for(let i=0;i<40;i++){
   const time=1+i*.03,target=originalTarget({line,now:time,frame:oldFrame,placement:oldPlacement,seed:outgoingSeed,index:3,
    shotKind:'orbit',trackWidth:12,aspect:width/600});
   previous=originalFollow({line,now:time,frame:oldFrame,placement:oldPlacement,shotKind:'orbit',delta:.03,
    aspect:width/600,target,previous}).state;
  }
  const off=tr.pickTransitionOffset(seed,epoch),origin={x:oldFrame.position.x+off.x,y:oldFrame.position.y+off.y,z:oldFrame.position.z+off.z};
  const frames=cam.translateFrames(cam.buildDioramaPath(3,seed),origin);
  const firstPlacement=cam.getDioramaTextPlacement(0,seed),firstTarget=originalTarget({line,now:0,frame:frames[0],placement:firstPlacement,
   seed,index:0,shotKind:'orbit',trackWidth:12,aspect:width/600});
  const initial=originalFlight({line,now:0,frame:frames[0],placement:firstPlacement,shotKind:'orbit',delta:0,aspect:width/600,
   target:firstTarget,previous,transitionEpoch:epoch,shotSeed:seed});
  for(const now of [0,.01,.7,1.2,1.6,2.4,3.199,3.2,3.201,4])for(const delta of [0,1/60,1/144,.043]){
   const index=now<1.9?0:1,frame=frames[index],placement=cam.getDioramaTextPlacement(index,seed);
   const target=originalTarget({line,now,frame,placement,seed,index,shotKind:'orbit',trackWidth:12,aspect:width/600});
   const result=originalFlight({line,now,frame,placement,shotKind:'orbit',delta,aspect:width/600,target,previous,
    flightState:initial.flight,transitionEpoch:epoch,shotSeed:seed});
   records.push([Buffer.from(seed).toString('base64'),width,epoch,now,delta,index,...Object.values(origin),
    ...flat(previous),...Object.values(firstTarget.eye),...Object.values(target.eye),...Object.values(target.look),
    ...flat(result.state),...result.renderedPosition,...result.renderedOrientation,+result.flying,+result.snapped,
    result.followSmooth,result.arcMag,result.sweepMag,result.bank,result.offAxis,...result.targetOrientation]);
  }
 }
 fs.mkdirSync(folder,{recursive:true});
 fs.writeFileSync(path.join(folder,'finite.tsv'),['# seed64 width epoch now dt index origin3 previous23 initialTargetEye3 target6 result23 renderedEye3 renderedQuaternion4 flying snapped smooth arc sweep bank offAxis targetQuaternion4',
  ...records.map(r=>r.join('\t'))].join('\n')+'\n');
 const digest=file=>createHash('sha256').update(fs.readFileSync(path.join(root,file))).digest('hex');
 fs.writeFileSync(path.join(folder,'conditions.json'),JSON.stringify({revision,motion:referenceMotionVersions(),three:'0.185.1',
  sourceFragments:[...sourceFragments,...followSourceFragments,...flightSourceFragments],
  sources:['.work/folia-major/src/components/visualizer/diorama/dioramaTransition.ts','verification/classic-browser/pnpm-lock.yaml',
   'verification/classic-browser/diorama-flight-source.mjs','verification/classic-browser/generate-diorama-flight.mjs'].map(file=>({file,sha256:digest(file)})),
  cases:records.length,outgoingSeed,line,scope:'Actual extracted original flight + normal follower. Incoming references from original normal follower; original seeded origin/flight capture. Independent equal-state controls, not a mounted component or a continuous trajectory. Media-domain production clock is a separate adaptation.'},null,2)+'\n');
 console.log(JSON.stringify({cases:records.length}));
}finally{await server.close()}
