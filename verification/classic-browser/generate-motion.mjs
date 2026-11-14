import fs from 'node:fs';
import path from 'node:path';
import { createRequire } from 'node:module';
import { pathToFileURL } from 'node:url';
import { motionValue } from 'framer-motion';
import { referenceModules, revision, root } from './reference.mjs';
const require = createRequire(import.meta.url);
const motionRequire = createRequire(require.resolve('framer-motion'));
const motionDist = path.dirname(path.dirname(motionRequire.resolve('motion-dom')));
const {getDefaultTransition} = await import(pathToFileURL(path.join(motionDist,'es/animation/utils/default-transitions.mjs')));
const {animateMotionValue} = await import(pathToFileURL(path.join(motionDist,'es/animation/interfaces/motion-value.mjs')));
const {server,load} = await referenceModules();
function sample(from,to,key,transition,us) {
  const specified=transition && Object.keys(transition).length>0;
  const options=specified ? transition : getDefaultTransition(key,{keyframes:[from,to]});
  // Use the actual dispatcher, not only the low-level generator: unspecified tween
  // easing starts as easeOut here, whereas keyframes() alone defaults to easeInOut.
  const animation=animateMotionValue(key,motionValue(from),to,{...options,isSync:true,from,autoplay:false})(()=>{});
  const value=animation.sample(Math.max(0,us/1000)).value;
  animation.stop();
  return value;
}
try {
  const {buildGlowWordLayoutVariants,glowWordBodyVariants}=await load('components/visualizer/glowWordVariants.ts');
  const {getGlowWordActiveEndTime,getGlowWordDisplayDuration,getGlowWordLineContainerMotion}=await load('components/visualizer/glowWordTiming.ts');
  const config={x:12.3,y:-4.2,scale:1.24,rotate:2.1,passedRotate:-7.3};
  const layout=buildGlowWordLayoutVariants('normal',true);
  const rows=[`# pinned Folia ${revision}; actual variants + pinned Motion generators`,
    '# mode\tstartUs\tendUs\tlineEnterUs\trenderEndUs\ttimeUs\tx\ty\tscale\trotation\talpha\tmix\tblur'];
  const lineRows=[`# pinned Folia ${revision}; actual line motion + Motion default transitions`,
    '# mode\telapsedUs\texitElapsedUs\talpha\tscale\tblur'];
  for(const mode of ['normal','fast','instant']) {
    const profile={wordRevealMode:mode,wordLookahead:mode==='normal'?.15:mode==='fast'?.08:.03,
      lineRenderEndTime:mode==='normal'?2.14:.9,lineTransitionMode:mode==='normal'?'normal':mode==='fast'?'fast':'none'};
    const word={startTime:mode==='normal'?1.3:.75,endTime:mode==='normal'?1.9:.8};
    const begin=Math.max(.7,word.startTime-profile.wordLookahead)*1e6,end=getGlowWordActiveEndTime(word,profile)*1e6;
    const duration=getGlowWordDisplayDuration(word,profile);
    const custom={config,baseColor:'#dfe5ec',activeColor:'#ffffff',duration,wordRevealMode:mode};
    const waiting=layout.waiting(custom),active=layout.active(custom),passed=layout.passed(custom);
    const bodyActive=glowWordBodyVariants.active(custom),bodyPassed=glowWordBodyVariants.passed(custom);
    const transition=(v,key)=>v.transition[key] || Object.fromEntries(Object.entries(v.transition).filter(([k,v])=>typeof v!=='object'));
    for(const delta of [-50000,0,25000,50000,100000,150000,200000,300000,500000,750000,850000,950000,1100000,1500000]) {
      const time=begin+delta,elapsed=Math.min(Math.max(0,delta),end-begin),exit=Math.max(0,time-end);
      const result=['x','y','scale','rotate','opacity'].map(key=>{
        const v=sample(waiting[key],active[key],key,transition(active,key),elapsed);
        return sample(v,passed[key],key,transition(passed,key),exit);
      });
      const color=sample(0,1,'color',bodyActive.transition.color,elapsed)*(1-sample(0,1,'color',bodyPassed.transition.color,exit));
      const blur=sample(10,0,'filter',bodyActive.transition.filter,elapsed)*(1-sample(0,1,'filter',bodyPassed.transition.filter,exit));
      rows.push([mode,Math.round(word.startTime*1e6),Math.round(word.endTime*1e6),700000,Math.round(profile.lineRenderEndTime*1e6),Math.round(time),...result,color,blur].join('\t'));
    }
    const motion=getGlowWordLineContainerMotion(profile);
    for(const elapsed of [0,25000,50000,100000,150000,200000,300000,600000]) {
      for(const exit of [0,25000,75000,150000,300000]) {
        const result=['opacity','scale','filter'].map(key=>{
          const initial=key==='filter'?parseFloat(motion.initial.filter.slice(5)):motion.initial[key];
          const target=key==='filter'?0:motion.animate[key];
          const v=sample(initial,target,key,motion.animate.transition,elapsed);
          const end=key==='filter'?parseFloat(motion.exit.filter.slice(5)):motion.exit[key];
          return sample(v,end,key,motion.exit.transition,exit);
        });
        lineRows.push([mode,elapsed,exit,...result].join('\t'));
      }
    }
  }
  fs.writeFileSync(path.join(root,'verification/classic-fixtures/motion.tsv'),rows.join('\n')+'\n');
  fs.writeFileSync(path.join(root,'verification/classic-fixtures/line-motion.tsv'),lineRows.join('\n')+'\n');
} finally {await server.close();}
