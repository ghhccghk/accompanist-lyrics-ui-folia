// Original same-corridor restart branch + continuous original finite operator.
import fs from 'node:fs';import path from 'node:path';import{createHash}from'node:crypto';
import{createRequire}from'node:module';import{createServer,transformWithOxc}from'vite';
import{root,revision,checkReference,referenceMotionVersions}from'./reference.mjs';
import{cameraTargetPlugin}from'./diorama-camera-source.mjs';
import{cameraFollowPlugin}from'./diorama-follow-source.mjs';
import{cameraFlightPlugin}from'./diorama-flight-source.mjs';
checkReference();const folder=path.join(root,'verification/diorama-fixtures/loop-v1');
if(fs.existsSync(folder))throw Error('Preserve original loop fixture; refusing overwrite');
const relative='src/components/visualizer/diorama/VisualizerDiorama.tsx';
const source=fs.readFileSync(path.join(root,'.work/folia-major',relative),'utf8'),fragments=[];
function extract(start,end){
 const a=source.indexOf(start),b=source.indexOf(end,a);if(a<0||b<0)throw Error('Missing original loop block');
 const code=source.slice(a,b);fragments.push({file:relative,startLine:source.slice(0,a).split('\n').length,
  endLine:source.slice(0,b).split('\n').length,sha256:createHash('sha256').update(code).digest('hex')});return code;
}
const constant=extract('const LOOP_RESTART_MAX_INDEX','\n');
const edges=extract('    const isNewSong =','    // Clamp to the ACTIVE segment');
const prefix='/@fs/'+path.join(root,'.work/folia-major/src').replaceAll('\\','/');
const wrapper=`import{activeSegment,appendSegment,updateActiveSegmentLines}from'${prefix}/components/visualizer/diorama/dioramaSequencer.ts';
import{getFrame}from'${prefix}/components/visualizer/diorama/cameraPath.ts';
import{pickTransitionOffset}from'${prefix}/components/visualizer/diorama/dioramaTransition.ts';
${constant}
export function loopEdge(seq,input){
 const outgoingSeg=activeSegment(seq),gatedSeed=outgoingSeg.seed,effectiveLines=outgoingSeg.lines;
 const outgoingLocal=input.previousIndex,outgoingGlobal=outgoingSeg.globalStart+outgoingLocal,effectiveLineIndex=input.index;
 const lastLocalIndexRef={current:input.previousIndex},lastActiveKeyRef={current:outgoingSeg.key},hasEverActiveRef={current:true};
 const transitionEpochRef={current:input.epoch};let transition=null;
 const setTransition=value=>transition=value,console={log:()=>{}};
 ${edges}
 return{isNewSong,isLoopRestart,startingTransition,transition,epoch:transitionEpochRef.current,
  key:activeSeg.key,origin:activeSeg.placementOrigin,globalStart:activeSeg.globalStart,
  segments:seq.segments.length,lastIndex:lastLocalIndexRef.current};
}`;
const plugin={name:'observe-original-loop-edge',resolveId:id=>id==='virtual:diorama-loop-edge'?'\0diorama-loop-edge.ts':null,
 load:async id=>id==='\0diorama-loop-edge.ts'?await transformWithOxc(wrapper,'loop-edge.ts'):null};
const require=createRequire(import.meta.url);
const server=await createServer({configFile:false,root:path.join(root,'verification/classic-browser'),
 plugins:[cameraTargetPlugin(),cameraFollowPlugin(),cameraFlightPlugin(),plugin],
 resolve:{alias:{three:path.join(path.dirname(require.resolve('three')),'three.module.js')}},
 optimizeDeps:{noDiscovery:true,entries:[]},cacheDir:path.join(root,'.work/diorama-loop-numeric'),
 server:{middlewareMode:true,fs:{allow:[root]}}});
try{
 const{originalTarget}=await server.ssrLoadModule('virtual:diorama-camera-target');
 const{originalFollow}=await server.ssrLoadModule('virtual:diorama-camera-follow');
 const{originalFlight}=await server.ssrLoadModule('virtual:diorama-camera-flight');
 const{loopEdge}=await server.ssrLoadModule('virtual:diorama-loop-edge');
 const cam=await server.ssrLoadModule(path.join(root,'.work/folia-major/src/components/visualizer/diorama/cameraPath.ts'));
 const tr=await server.ssrLoadModule(path.join(root,'.work/folia-major/src/components/visualizer/diorama/dioramaTransition.ts'));
 const seqmod=await server.ssrLoadModule(path.join(root,'.work/folia-major/src/components/visualizer/diorama/dioramaSequencer.ts'));
 const lines=[0,1,2].map(i=>({fullText:`Synthetic loop line ${i}`,startTime:.7+i*1.4,endTime:1.5+i*1.4,words:[]}));
 const flat=s=>[...s.position,...s.velocity,...s.look,...s.lookVelocity,s.alignment,...s.alignmentOrientation,...s.orientation];
 const times=[0,.7,1.6,2.099,2.1,3.2,3.5,4.5],records=[],scenarios=[];
 for(const seed of ['DioramaProbe','Incoming mirror'])for(const width of[380,800]){
  const origin=seed==='DioramaProbe'?{x:0,y:0,z:0}:tr.pickTransitionOffset(seed,2);
  const seq=seqmod.createSequencerState(),segment=seqmod.appendSegment(seq,{seed,lines,round:0,placementOrigin:origin});
  const originalFrames=segment.frames,epoch=2;
  const edge=loopEdge(seq,{previousIndex:2,index:0,epoch});
  if(!edge.startingTransition||edge.segments!==1||segment.frames!==originalFrames)throw Error('Unexpected original loop branch');
  const at=t=>{
   const index=Math.max(0,lines.findLastIndex(l=>l.startTime<=t)),frame=segment.frames[index],line=lines[index];
   const placement=cam.getDioramaTextPlacement(index,seed);
   return {line,now:t,wallTime:t,frame,placement,seed,index,shotKind:'orbit',trackWidth:12,aspect:width/600,
    transitionEpoch:edge.epoch,shotSeed:seed};
  };
  let previous;
  for(let i=0;i<=270;i++){
   const now=i/60,input=at(now),target=originalTarget(input);
   previous=originalFollow({...input,target,delta:i===0?0:1/60,previous}).state;
  }
  const incoming=previous;let flightState,last=0;
  const ticks=[...new Set([...times,...Array.from({length:271},(_,i)=>i/60)])].sort((a,b)=>a-b);
  for(const now of ticks){
   const input=at(now),target=originalTarget(input),delta=now-last;
   const value=originalFlight({...input,target,delta,previous,flightState});last=now;
   previous=value.state;flightState=value.flight;
   if(times.includes(now))records.push([Buffer.from(seed).toString('base64'),width,edge.epoch,now,...Object.values(origin),
    ...flat(incoming),...flat(value.state),...value.renderedPosition,...value.renderedOrientation,+value.flying,+value.snapped]);
  }
  scenarios.push({seed,width,edge,originalFramesRetained:segment.frames===originalFrames,nextGlobalStart:seq.nextGlobalStart});
 }
 fs.mkdirSync(folder,{recursive:true});
 fs.writeFileSync(path.join(folder,'finite.tsv'),['# seed64 width epoch time origin3 incoming21 result21 renderedEye3 renderedQuaternion4 flying snapped',...records.map(r=>r.join('\t'))].join('\n')+'\n');
 const hash=file=>createHash('sha256').update(fs.readFileSync(path.join(root,file))).digest('hex');
 fs.writeFileSync(path.join(folder,'conditions.json'),JSON.stringify({revision,motion:referenceMotionVersions(),
  sourceFragments:fragments,lines,times,scenarios,records:records.length,
  harnessSha256:hash('verification/classic-browser/generate-diorama-loop.mjs'),
  scope:'Original VisualizerDiorama restart branch with actual sequencer and camera blocks. Same corridor/frames; original outgoing 60Hz refs and continuous finite loop trajectory. Fixed synthetic 12-world-unit track width. Not mounted React/full text raster or production display-scheduled camera.'},null,2)+'\n');
 console.log(JSON.stringify({records:records.length,scenarios:scenarios.length}));
}finally{await server.close()}
