// Original pinned Folia configuration + actual locked Motion runtime, not a rewritten spring oracle.
import fs from'node:fs';import path from'node:path';import{createRequire}from'node:module';import{pathToFileURL}from'node:url';
import{motionValue,spring}from'framer-motion';import{referenceModules,root,revision}from'./reference.mjs';
const require=createRequire(import.meta.url),mr=createRequire(require.resolve('framer-motion'));
const dist=path.dirname(path.dirname(mr.resolve('motion-dom')));
const{animateMotionValue}=await import(pathToFileURL(path.join(dist,'es/animation/interfaces/motion-value.mjs')));
const{server,load}=await referenceModules();
try{
 const{getCappellaIntensityConfig}=await load('components/visualizer/cappella/cappellaIntensity.ts');
 const rows=['# intensity kind from to velocity elapsedSeconds actualRuntimeValue rawGeneratorValue rawGeneratorVelocity rawDone'];
 const config={};
 for(const intensity of['calm','normal','chaotic']){
  const m=getCappellaIntensityConfig(intensity).motion;config[intensity]=m;
  const specs=[['scale',m.activeScale,m.passedScale,0,m.avatarSpring],['marginTop',0,28,0,m.avatarSpring],
   ['marginTop',27,-15,72,m.avatarSpring],['scale',1.04,m.activeScale,-.9,m.avatarSpring],
   ['opacity',0,1,0,{duration:m.rowEnterDuration,ease:'easeOut'}],
   ['y',m.rowEnterY,0,0,{duration:m.rowEnterDuration,ease:'easeOut'}],
   ['scale',m.rowEnterScale,1,0,{duration:m.rowEnterDuration,ease:'easeOut'}],
   ['opacity',1,0,0,{duration:m.rowExitDuration,ease:'easeIn'}],
   ['y',0,m.rowExitY,0,{duration:m.rowExitDuration,ease:'easeIn'}],
   ['scale',1,m.rowExitScale,0,{duration:m.rowExitDuration,ease:'easeIn'}],
   ['width',72,395,0,{duration:.2,ease:'easeOut'}],
   ['height',150,64,0,{duration:.2,ease:'easeOut'}],
   ['opacity',0,.62,0,{duration:.18,ease:'easeOut'}],
   ['y',4,0,0,{duration:.18,ease:'easeOut'}]];
  for(const[key,from,to,velocity,transition]of specs)for(const t of[0,.0004,.0006,.0137,.025,.05,.075,.1,.15,.18,.2,.22,.25,.28,.3,.32,.35,.38,.4,.45,.5,.55,.6,.7,1,1.5,10]){
   const physical=transition.stiffness!==undefined;
   const animation=animateMotionValue(key,motionValue(from),to,{...transition,...(physical?{type:'spring'}:{}),velocity,from,isSync:true,autoplay:false})(()=>{});
   animation.play();const value=animation.sample(t*1000).value;animation.stop();
   const gen=physical?spring({...transition,keyframes:[from,to],velocity}):null;
   const raw=gen?.next(t*1000),v=gen?.velocity(t*1000);
   const kind=physical?'spring':`${key==='width'||key==='height'?'resize':transition.ease==='easeIn'?'exit':transition.duration===.18?'timestamp':'enter'}-${key}`;
   rows.push([intensity,kind,from,to,velocity,t,value,raw?.value??'NA',v??'NA',raw?.done??'NA'].join('\t'));
  }
 }
 const out=path.join(root,'verification/cappella-fixtures');
 fs.writeFileSync(path.join(out,'response.tsv'),rows.join('\n')+'\n');
 fs.writeFileSync(path.join(out,'response-config.json'),JSON.stringify({revision,motionRuntime:'motion-dom13.5.1 / framer-motion13.4.3',scope:'Actual Motion animateMotionValue runtime responses with pinned original Cappella motion config. Individual tween/spring only; not mounted layout/interrupt history acceptance.',config},null,2)+'\n');
 console.log(JSON.stringify({observations:rows.length-1}));
}finally{await server.close()}
