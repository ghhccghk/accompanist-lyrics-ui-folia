import fs from'node:fs';import path from'node:path';import{referenceModules,root,revision}from'./reference.mjs';
const{server,load}=await referenceModules();
try{
 const{resolvePendoloMotionProfile:profile,resolvePendoloChorusPresentation:resolve}=await load('components/visualizer/pendolo/pendoloMotionProfile.ts');
 const rows=['# intensity chorus focal font active mix haloOpacity haloScale glow duration sigma1 sigma2 padding'];
 for(const level of['calm','normal','chaotic'])for(const chorus of[false,true])for(const focal of[false,true])for(const font of[12,22,28,37.5,72]){
  const p=resolve(chorus,focal,profile(level));
  rows.push([level,chorus,focal,font,p.isActive,p.accentMix,p.haloOpacity,p.haloScale,p.glowMultiplier,p.transitionDuration,
   Math.round(font*.3*p.glowMultiplier),Math.round(font*.62*p.glowMultiplier),Math.ceil(font*.7*p.glowMultiplier+10)].join('\t'));
 }
 fs.writeFileSync(path.join(root,'verification/pendolo-fixtures/chorus.tsv'),rows.join('\n')+'\n');
 fs.writeFileSync(path.join(root,'verification/pendolo-fixtures/chorus-reference.json'),JSON.stringify({revision,cases:rows.length-1,
 scope:'Unchanged original chorus profile plus literal filter formulas from PendoloActiveLyricSweep. Not halo motion or pixel acceptance.'},null,2)+'\n');
 console.log(JSON.stringify({cases:rows.length-1}));
}finally{await server.close()}
