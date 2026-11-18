// Original useSpring's actual attachFollow options/generator, not the old
// animateMotionValue dispatcher oracle. Numerical root probes are not RAF ticks.
import fs from 'node:fs';
import path from 'node:path';
import {createHash} from 'node:crypto';
import {createRequire} from 'node:module';
import {pathToFileURL} from 'node:url';
import {motionValue} from 'framer-motion';
import {root,upstream,revision,referenceModules,referenceMotionVersions} from './reference.mjs';
const output=path.join(root,'verification/pendolo-fixtures/follower-v1');
if(fs.existsSync(output))throw Error('Preserve independent follower oracle; refusing overwrite');
const require=createRequire(import.meta.url),mr=createRequire(require.resolve('framer-motion'));
const dist=path.dirname(path.dirname(mr.resolve('motion-dom')));
const {attachFollow}=await import(pathToFileURL(path.join(dist,'es/value/follow-value.mjs')));
const originalPath=path.join(upstream,'src/components/visualizer/pendolo/VisualizerPendolo.tsx');
const original=fs.readFileSync(originalPath,'utf8');
const options=original.match(/const tickSpring = useSpring\(targetLineIndex, (\{[\s\S]*?\})\);/)?.[1];
if(!options)throw Error('Original options seam changed');
const config=(springSnappiness,motionProfile)=>new Function('springSnappiness','motionProfile',`return (${options})`)(springSnappiness,motionProfile);
const {server,load}=await referenceModules();
const responseRows=['# intensity snap from to initialVelocity time value velocity done'];
const stopRows=['# intensity snap from to initialVelocity stop coarseStop restDelta restSpeed'];
const trackRows=['# scene intensity snap time target value velocity wheel correction'];
const driverRows=['# intensity fps time target value velocity wheel restDelta restSpeed elapsedMs'];
const inputs=JSON.parse(fs.readFileSync(path.join(root,'verification/pendolo-fixtures/tick-inputs.json'),'utf8'));
inputs.mounted=Array.from({length:8},(_,i)=>[.7+i*4,2.8+i*4,2.8+i*4]);
let maxRootRefinement=0;
try {
 const {resolvePendoloMotionProfile}=await load('components/visualizer/pendolo/pendoloMotionProfile.ts');
 const {resolvePendoloFallbackAnchorIndex}=await load('components/visualizer/pendolo/pendoloTimeline.ts');
 function follower(from,to,velocity,c){
  const value=motionValue(from);let tick,now=0;
  const remove=attachFollow(value,from,{...c,velocity,driver:cb=>{tick=cb;return {start(){},stop(){},now:()=>now}}});
  value.set(to);const animation=value.animation;
  if(!animation)throw Error('Expected original follower');
  const raw=t=>{const state=animation.generator.next(t*1000);return {value:state.value,velocity:animation.generator.velocity(t*1000),done:state.done}};
  function firstRest(step){
   if(raw(0).done)return 0;
   let lower=0,upper=step;
   while(!raw(upper).done){lower=upper;upper+=step;if(upper>20)throw Error('Unbounded original rest search')}
   for(let i=0;i<48;i++){const mid=(lower+upper)/2;if(raw(mid).done)upper=mid;else lower=mid}
   return upper;
  }
  const stop=firstRest(.0001),coarseStop=firstRest(.0002);
  maxRootRefinement=Math.max(maxRootRefinement,Math.abs(stop-coarseStop));
  return {raw,stop,coarseStop,options:animation.options,remove,
   at(t){return t>=stop?{value:to,velocity:0}:raw(t)}};
 }
 for(const intensity of ['calm','normal','chaotic'])for(const snap of [.5,1,2]){
  const c=config(snap,resolvePendoloMotionProfile(intensity));
  for(const [from,to,v] of [[-1,0,0],[.5,1,0],[0,8,0],[9,-1,0],[.3,-.5,12],[-.8,1.5,-4]]){
   const response=follower(from,to,v,c);
   stopRows.push([intensity,snap,from,to,v,response.stop,response.coarseStop,response.options.restDelta,response.options.restSpeed].join('\t'));
   for(const t of [0,.0001,.0137,.025,.05,.1,.2,.3,.4,.5,.6,.8,1,2,10,response.stop-1e-6,response.stop,response.stop+1e-6]){
    const r=response.raw(t);responseRows.push([intensity,snap,from,to,v,t,r.value,r.velocity,r.done].join('\t'));
   }
   response.remove();
  }
  for(const [scene,values]of Object.entries(inputs)){
   const lines=values.map(([startTime,endTime,renderEndTime])=>({startTime,endTime,renderHints:{renderEndTime}}));
   const boundaries=[...new Set(values.flatMap(l=>[l[0],l[2]]))].sort((a,b)=>a-b);
   const current=(t,after=false)=>lines.findLastIndex(l=>t>=l.startTime&&(t<l.renderHints.renderEndTime||(!after&&t===l.renderHints.renderEndTime)));
   let last=-1;const history=[];
   for(const t of boundaries)for(const after of [false,true]){
    const index=current(t,after);if(index>=0)last=index;
    const target=after&&index<0&&last>=0&&t===lines.at(-1)?.renderHints.renderEndTime?lines.length:
     resolvePendoloFallbackAnchorIndex(lines,index,Math.max(last,0),last>=0,t);
    history.push({t,after,last,target});
   }
   const anchor=t=>{const past=history.filter(e=>e.t<t||e.t===t&&!e.after).at(-1);return resolvePendoloFallbackAnchorIndex(lines,current(t),Math.max(past?.last??-1,0),(past?.last??-1)>=0,t)};
   const initial=anchor(0),segments=[];let target=initial;
   for(const t of [0,...boundaries.filter(x=>x>0)]){
    const next=history.filter(e=>e.t<=t).at(-1)?.target??initial;if(next===target)continue;
    const old=segments.at(-1),s=old?old.response.at(t-old.t):{value:initial,velocity:0};
    segments.push({t,response:follower(s.value,next,s.velocity,c)});target=next;
   }
   const times=[...new Set([0,.5,1,2,3,4,5,7,10,...boundaries.flatMap(t=>[-.001,0,.0001,.0137,.05,.1,.25,.75].map(d=>t+d).filter(t=>t>=0))])].sort((a,b)=>a-b);
   for(const t of times){const segment=segments.filter(e=>e.t<=t).at(-1),s=segment?segment.response.at(t-segment.t):{value:initial,velocity:0};const target=anchor(t),wheel=-(s.value-target)*100/8;
    trackRows.push([scene,intensity,snap,t,target,s.value,s.velocity,wheel,-wheel*.65].join('\t'));
   }
   for(const s of segments)s.response.remove();
  }
 }
 // Same mounted source target schedule, sampled by the REAL original follower.
 // Frame differences are observations, never a proposed fixed playback cadence.
 for(const intensity of ['calm','normal','chaotic'])for(const fps of [30,60,144]){
  const c=config(2,resolvePendoloMotionProfile(intensity));let now=0,tick;
  const value=motionValue(1),remove=attachFollow(value,1,{...c,driver:cb=>{tick=cb;return {start(){},stop(){},now:()=>now}}});
  const times=[...new Set([6,6.8,7,7.3,...Array.from({length:Math.ceil(1.3*fps)+1},(_,i)=>6+i/fps)].filter(t=>t<=7.3))].sort((a,b)=>a-b);let previous=1;
  for(const t of times){now=t*1000;const target=t>6.8?1.5:1;if(target!==previous){value.set(target);previous=target}if(tick)tick(now);
   if(t===6.8||t===7||t===7.3){const a=value.animation;driverRows.push([intensity,fps,t,target,value.get(),a?.getGeneratorVelocity()??0,-(value.get()-target)*100/8,a?.options.restDelta??.001,a?.options.restSpeed??.01,a?.currentTime??0].join('\t'))}
  }remove();
 }
 const hash=p=>createHash('sha256').update(fs.readFileSync(p)).digest('hex');
 fs.mkdirSync(output,{recursive:true});
 for(const [name,rows]of [['response',responseRows],['stops',stopRows],['track',trackRows],['driver',driverRows]])fs.writeFileSync(path.join(output,name+'.tsv'),rows.join('\n')+'\n');
 fs.writeFileSync(path.join(output,'conditions.json'),JSON.stringify({revision,motion:referenceMotionVersions(),
  sources:[originalPath,path.join(dist,'es/value/follow-value.mjs'),path.join(dist,'es/animation/FollowAnimation.mjs'),path.join(dist,'es/animation/generators/spring.mjs')].map(p=>({file:path.relative(root,p).replaceAll('\\','/'),sha256:hash(p)})),
  hostSha256:hash(new URL(import.meta.url)),lockSha256:hash(path.join(root,'verification/classic-browser/pnpm-lock.yaml')),inputs,maxRootRefinement,
  scope:'Actual attachFollow supplies restDelta/restSpeed and original raw generator. First-rest root is independent numerical media-time adaptation (100/200 microsecond isolation, 48 bisections), NOT source RAF equality. Existing dispatcher and mounted fixtures remain unchanged. Driver observes source schedule differences, no UI screenshots.'},null,2)+'\n');
 console.log(JSON.stringify({responses:responseRows.length-1,stops:stopRows.length-1,tracks:trackRows.length-1,maxRootRefinement}));
}finally{await server.close()}
