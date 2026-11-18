// Controlled frozen spring branch, extracted verbatim from the pinned source.
// Incoming state/target/duration/phase come ONLY from the preserved original trace.
import fs from 'node:fs';import path from 'node:path';import {createHash} from 'node:crypto';
import {createRequire} from 'node:module';
import {createServer,transformWithOxc} from 'vite';
import {root,upstream,revision,checkReference,referenceMotionVersions} from './reference.mjs';
checkReference();
const folder=path.join(root,'verification/fume-fixtures/frozen-clamp-v1');
if(fs.existsSync(folder))throw Error('Preserve existing frozen-clamp fixture; refusing overwrite');
const file=path.join(upstream,'src/components/visualizer/fume/fumeCameraStep.ts');
const source=fs.readFileSync(file,'utf8');const begin=source.indexOf('        const boostedCatchUpRate =');
const end=source.indexOf('\n    }\n\n    return { shouldShowOverview',begin);
if(begin<0||end<0)throw Error('Pinned spring source boundary missing');
const fragment=source.slice(begin,end),prefix='/@fs/'+path.join(upstream,'src/components/visualizer/fume').replaceAll('\\','/');
const code=`import {clamp,mix} from '${prefix}/fumeMath.ts';
import {CAMERA_SCALE_MIN,CAMERA_SCALE_MAX} from '${prefix}/fumeCamera.ts';
export function frozen(input){
const state=structuredClone(input.state),{dt,retargetBoost,target,cameraDistance}=input;
const [targetCameraX,targetCameraY,targetCameraScale]=target;
${fragment}
return state;
}`;
const server=await createServer({configFile:false,root:path.join(root,'verification/classic-browser'),
 resolve:{alias:{'@chenglou/pretext':createRequire(import.meta.url).resolve('@chenglou/pretext')}},
 plugins:[{name:'frozen-original-spring',resolveId:id=>id==='virtual:frozen-spring'?'\0frozen-spring.ts':null,
  load:async id=>id==='\0frozen-spring.ts'?await transformWithOxc(code,'frozen-spring.ts'):null}],
 optimizeDeps:{noDiscovery:true,entries:[]},cacheDir:path.join(root,'.work/fume-frozen-clamp'),
 server:{middlewareMode:true,fs:{allow:[root]}}});
try {
 const {frozen}=await server.ssrLoadModule('virtual:frozen-spring');
 const trace=JSON.parse(fs.readFileSync(path.join(root,'verification/fume-fixtures/handoff-v1/source-trace.json'),'utf8'));
 const steps=fs.readFileSync(path.join(root,'verification/fume-fixtures/handoff-v1/steps.tsv'),'utf8').split('\n').filter(l=>l&&!l.startsWith('#')).map(l=>l.split('\t'));
 const pose=p=>[p.x,p.y,p.velocityX,p.velocityY,p.focusX,p.focusY,p.scale,p.velocityScale,p.focusScale];
 const from=p=>({x:p[0],y:p[1],velocityX:p[2],velocityY:p[3],focusX:p[4],focusY:p[5],scale:p[6],velocityScale:p[7],focusScale:p[8]});
 const rows=[],partitions=[],controls=[],skipped=[];
 for(const c of trace.cases){
  const record=c.runs.find(r=>r.rate==='mounted60').records.find(r=>r.trace[0].branch==='spring');
  if(!record){skipped.push({case:c.id,reason:'No actual spring in preserved250ms window. No synthetic continuation substituted.'});continue}
  const original=steps.find(r=>r[0]===c.id&&r[1]==='mounted60'&&+r[3]===record.time);
  const before=original[5].split(',').map(Number),expected=original[6].split(',').map(Number);
  const intent=record.trace[0],spring=record.trace.find(t=>t.kind==='springXY'),scale=record.trace.find(t=>t.kind==='springScale');
  const target=intent.target,boost=(1-intent.phase)**3,duration=c.seed.retarget.duration;
  const input={state:{camera:from(before),retarget:{duration}},target,retargetBoost:boost,cameraDistance:Math.hypot(target[0]-before[0],target[1]-before[1]),dt:record.dt};
  const result=pose(frozen(input).camera),peak=Math.max(...result.map((v,i)=>Math.abs(v-expected[i])));
  if(peak>1e-9)throw Error('Extracted spring does not reproduce actual original first spring '+c.id+' '+peak);
  controls.push({case:c.id,from:record.from,time:record.time,dt:record.dt,actualSourcePeak:peak,
   originalIncoming:before,target,duration,phase:intent.phase,strength:spring.strength,damping:spring.damping,limit:spring.limit,
   scaleStrength:scale.strength,scaleDamping:13.5+(21-13.5)*boost,scope:'Freeze target/phase/duration/distance; same original spring source block, no future target or branch.'});
  for(const dt of [record.dt,1/144,1/30])for(const [axis,vi,pi,fi,ti,strength,damping,rate,cap]of[
   ['x',2,0,4,0,spring.strength,spring.damping,11.2,spring.limit],
   ['y',3,1,5,1,spring.strength,spring.damping,11.2,spring.limit],
   ['scale',7,6,8,2,scale.strength,13.5,5.4,1.6]]){
   if(boost!==0)throw Error('First-spring control requires explicit boost rate export');
   const focus=before[fi]+(target[ti]-before[fi])*(1-Math.exp(-dt*rate));
   for(const sign of [-1,1]){
    const threshold=(sign*cap-(focus-before[pi])*strength*dt)/(1-damping*dt);
    for(const delta of [-.1,-.001,-1e-8,0,1e-8,.001,.1]){
     const state=structuredClone(input.state);state.camera=from(before);const p=pose(state.camera);p[vi]=threshold+delta*cap;state.camera=from(p);
     const after=pose(frozen({...input,state,dt}).camera);
     rows.push([c.id,axis,sign,delta,dt,duration,boost,cap,threshold,...target,...p,...after]);
    }
   }
  }
  for(const n of [1,2,4,8,16]){
   let state=structuredClone(input.state);
   for(let i=0;i<n;i++)state=frozen({...input,state,dt:record.dt/n});
   partitions.push([c.id,n,record.dt,...pose(state.camera)]);
  }
 }
 fs.mkdirSync(folder,{recursive:true});
 fs.writeFileSync(path.join(folder,'sweep.tsv'),['# case axis sign delta dt duration boost cap threshold target3 before9 after9',...rows.map(r=>r.join('\t'))].join('\n')+'\n');
 fs.writeFileSync(path.join(folder,'partitions.tsv'),['# case partitions totalDt after9',...partitions.map(r=>r.join('\t'))].join('\n')+'\n');
 const hash=p=>createHash('sha256').update(fs.readFileSync(path.join(root,p))).digest('hex');
 fs.writeFileSync(path.join(folder,'conditions.json'),JSON.stringify({revision,motion:referenceMotionVersions(),
  incomingSha256:hash('verification/fume-fixtures/handoff-v1/source-trace.json'),stepsSha256:hash('verification/fume-fixtures/handoff-v1/steps.tsv'),
  sourceSha256:createHash('sha256').update(source).digest('hex'),fragment:{startLine:source.slice(0,begin).split('\n').length,
   endLine:source.slice(0,end).split('\n').length,sha256:createHash('sha256').update(fragment).digest('hex')},controls,skipped,
  cases:rows.length,scope:'CONTROLLED frozen original spring block from actual original first-spring incoming state. Target/phase/duration/distance fixed; some algebraic velocity sweeps deliberately exceed normally admissible velocities. No mounted/whole-song visual repair, no source fixture or expected threshold changed.'},null,2)+'\n');
 console.log(JSON.stringify({controls:controls.length,sweeps:rows.length,partitions:partitions.length,skipped}));
}finally{await server.close()}
