// Pinned original physical options/transforms + real locked Motion dispatcher.
import fs from'node:fs';import path from'node:path';import{createHash}from'node:crypto';import{createRequire}from'node:module';import{pathToFileURL}from'node:url';
import{motionValue,spring}from'framer-motion';import{referenceModules,root,upstream,revision}from'./reference.mjs';
const require=createRequire(import.meta.url),mr=createRequire(require.resolve('framer-motion'));
const dist=path.dirname(path.dirname(mr.resolve('motion-dom')));
const{animateMotionValue}=await import(pathToFileURL(path.join(dist,'es/animation/interfaces/motion-value.mjs')));
const original=fs.readFileSync(path.join(upstream,'src/components/visualizer/pendolo/VisualizerPendolo.tsx'),'utf8');
const options=original.match(/const tickSpring = useSpring\(targetLineIndex, (\{[\s\S]*?\})\);/)?.[1];
const rotate=original.match(/value => -\(value - targetLineIndex\)[^\r\n]+/)?.[0].replace(/,$/,'');
const correction=original.match(/value => -value \* 0\.65/)?.[0];
if(!options||!rotate||!correction)throw Error('Pinned tick seam changed');
const config=(springSnappiness,motionProfile)=>new Function('springSnappiness','motionProfile',`return (${options})`)(springSnappiness,motionProfile);
const angle=(value,targetLineIndex,arc)=>new Function('targetLineIndex','angleStepRad',`return (${rotate})`)(targetLineIndex,arc*Math.PI/180/8)(value);
const correct=new Function(`return (${correction})`)();
const{server,load}=await referenceModules();
const out=path.join(root,'verification/pendolo-fixtures'),rows=['# intensity snappiness from to velocity elapsed stiffness damping mass runtimeValue rawValue rawVelocity rawDone runtimeDone'];
const tracks=['# scene intensity snappiness time current target value velocity wheel correction'],inputs={
 regular:[[1,2.5,2.8],[3.1,4,4],[5,6,6.4]],
 rapid:[[.7,.73,.73],[.78,.8,.8],[.83,.86,.86],[.87,.9,.9]],
 overlap:[[.7,3,3],[.78,.81,.81],[1.1,1.18,1.18],[1.22,2.7,2.7],[2,2.04,2.04]],
 tied:[[0,.4,.4],[0,.2,.2],[.4,.6,.6]],empty:[]};
let configs={};
try{
 const{resolvePendoloMotionProfile}=await load('components/visualizer/pendolo/pendoloMotionProfile.ts');
 const{resolvePendoloFallbackAnchorIndex}=await load('components/visualizer/pendolo/pendoloTimeline.ts');
 function response(from,to,velocity,c){
  return{at(t){
   // A dispatcher that has finished stays finished. Each arbitrary query must
   // use a fresh animation; reusing one would corrupt earlier retarget states.
   const gen=spring({...c,keyframes:[from,to],velocity});
   const animation=animateMotionValue('tick',motionValue(from),to,{...c,type:'spring',velocity,from,isSync:true,autoplay:false})(()=>{});animation.play();
   const r=animation.sample(Math.max(0,t)*1000),done=r.done||animation.state==='finished';
   const result={value:r.value,velocity:done?0:gen.velocity(Math.max(0,t)*1000),done};animation.stop();return result;
  },stop(){}};
 }
 for(const intensity of['calm','normal','chaotic'])for(const snap of[.5,1,2]){
  const c=config(snap,resolvePendoloMotionProfile(intensity));configs[`${intensity}-${snap}`]=c;
  for(const[from,to,velocity]of[[-1,0,0],[.5,1,0],[0,8,0],[9,-1,0],[.3,-.5,12],[-.8,1.5,-4]]){
   const runtime=response(from,to,velocity,c),gen=spring({...c,keyframes:[from,to],velocity});
   for(const t of[0,.0004,.0006,.0137,.025,.05,.075,.1,.15,.18,.2,.25,.3,.4,.5,.6,.8,1,1.5,3,10]){
    const a=runtime.at(t),raw=gen.next(t*1000);rows.push([intensity,snap,from,to,velocity,t,c.stiffness,c.damping,c.mass,a.value,raw.value,gen.velocity(t*1000),raw.done,a.done].join('\t'));
   }runtime.stop();
  }
  for(const[name,values]of Object.entries(inputs)){
   const lines=values.map(([startTime,endTime,renderEndTime])=>({startTime,endTime,renderHints:{renderEndTime}}));
   const boundaries=[...new Set(values.flatMap(l=>[l[0],l[2]]))].sort((a,b)=>a-b);
   const current=t=>lines.findLastIndex(l=>t>=l.startTime&&t<=l.renderHints.renderEndTime);
   let last=-1;
   // Query exact boundaries and their right limits through original fallback.
   const history=[];
   for(const t of boundaries){for(const after of[false,true]){const q=t+(after?1e-9:0),i=current(q);if(i>=0)last=i;
    history.push({t,after,last,target:resolvePendoloFallbackAnchorIndex(lines,i,Math.max(last,0),last>=0,q)});}}
   const anchor=t=>{const observed=history.filter(e=>e.t<t||e.t===t&&!e.after).at(-1)?.last??-1;return resolvePendoloFallbackAnchorIndex(lines,current(t),Math.max(observed,0),observed>=0,t)};
   const initial=anchor(0),segments=[];let target=initial;
   for(const t of[0,...boundaries.filter(x=>x>0)]){
    const next=history.filter(e=>e.t<=t).at(-1)?.target??initial;
    if(next===target)continue;const previous=segments.at(-1),s=previous?previous.response.at(t-previous.t):{value:initial,velocity:0};
    segments.push({t,response:response(s.value,next,s.velocity,c)});target=next;
   }
   const times=[...new Set([0,.0001,.5,1,2,3,4,5,7,10,...boundaries.flatMap(t=>[-.001,0,.0001,.0137,.05,.1,.25,.75].map(d=>t+d).filter(x=>x>=0))])].sort((a,b)=>a-b);
   for(const t of times){const seg=segments.filter(e=>e.t<=t).at(-1),s=seg?seg.response.at(t-seg.t):{value:initial,velocity:0},target=anchor(t),rotation=angle(s.value,target,100);
    tracks.push([name,intensity,snap,t,current(t),target,s.value,s.velocity,rotation,correct(rotation)].join('\t'));}
   for(const s of segments)s.response.stop();
  }
 }
 fs.writeFileSync(path.join(out,'tick-response.tsv'),rows.join('\n')+'\n');
 fs.writeFileSync(path.join(out,'tick-track.tsv'),tracks.join('\n')+'\n');
 fs.writeFileSync(path.join(out,'tick-inputs.json'),JSON.stringify(inputs,null,2)+'\n');
 fs.writeFileSync(path.join(out,'tick-reference.json'),JSON.stringify({revision,configs,source:'src/components/visualizer/pendolo/VisualizerPendolo.tsx',extracts:[options,rotate,correction].map(s=>({sha256:createHash('sha256').update(s).digest('hex'),characters:s.length})),
  responses:rows.length-1,trackQueries:tracks.length-1,scope:'Actual Motion dispatcher/generator, unchanged original options/profiles/rotation/fallback. Event history uses exact physical derivative on retarget; not mounted useSpring frame-velocity, DOM or device acceptance.'},null,2)+'\n');
 console.log(JSON.stringify({responses:rows.length-1,trackQueries:tracks.length-1}));
}finally{await server.close()}
