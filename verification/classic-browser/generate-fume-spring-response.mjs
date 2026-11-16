// Constant-coefficient probes of the UNTOUCHED original camera operator.
// Calibrates a continuous response, not a fixed-FPS native playback driver.
import fs from 'node:fs';import path from 'node:path';
import {referenceModules,root,revision}from'./reference.mjs';
const{server,load}=await referenceModules();
try{
 const {createFumeCameraState,syncFumeCameraToArticle,stepFumeCamera}=await load('components/visualizer/fume/fumeCameraStep.ts');
 const {resolveArticleOverviewCamera}=await load('components/visualizer/fume/fumeCamera.ts');
 const {buildWordRangesFromWords}=await load('components/visualizer/fume/fumeReveal.ts');
 const{getLineRenderEndTime}=await load('utils/lyrics/renderHints.ts');
 const folder=path.join(root,'verification/fume-fixtures');
 const rows=n=>fs.readFileSync(path.join(folder,n+'.tsv'),'utf8').split(/\r?\n/).filter(s=>s&&!s.startsWith('#')).map(s=>s.split('\t'));
 const geom=rows('camera-rows');
 const blocks=rows('camera-blocks').map(f=>{
  const line={fullText:Buffer.from(f[1],'base64').toString('utf8'),startTime:+f[2],endTime:+f[3],words:[]};
  const gs=[...line.fullText],rs=geom.filter(r=>+r[0]===+f[0]);
  return {sourceLineIndex:+f[0],line,x:+f[4],y:+f[5],width:+f[6],height:+f[7],lineHeight:+f[8],graphemes:gs,wordRanges:buildWordRangesFromWords(line,gs),
   prepared:{segments:rs.map(r=>gs.slice(+r[1],+r[2]).join('')),widths:rs.map(r=>+r[5]),breakableFitAdvances:rs.map(r=>{const o=r[6].split(',').map(Number);return o.slice(1).map((v,i)=>v-o[i]);})},
   segmentMetas:rs.map(r=>({graphemeStart:+r[1],graphemeEnd:+r[2],graphemeCount:+r[2]-+r[1]})),
   renderLines:rs.map(r=>({start:+r[1],end:+r[2],left:+r[3],top:+r[4],width:+r[5],glyphOffsets:r[6].split(',').slice(0,-1).map(Number),graphemes:gs.slice(+r[1],+r[2])}))};
 });
 const article={width:2500,height:4600,blocks,chronologicalBlocks:blocks,blockBySourceLineIndex:new Map(blocks.map(b=>[b.sourceLineIndex,b])),lastChronologicalRenderEndTime:getLineRenderEndTime(blocks.at(-1).line)};
 const output=[`# Folia ${revision}; unmodified step with target and coefficients held constant, not a playback driver`,
 '# duration\tphase\tsteps\th\tstrength\tdamping\tpositionRate\tscaleStrength\tscaleDamping\tscaleRate\ttarget\tinitial\texpected'];
 const h=1/60,target=[400,500,1.1],initial=[401,498,3,-4,400.4,499.6,1.105,.01,1.102];
 for(const duration of [.03,.055,.13,.3,1.2])for(const phase of [0,.15,.4,.75,1]){
  const boost=(1-phase)**3,mix=(a,b,q)=>a+(b-a)*q,clamp=(x,a,b)=>Math.max(a,Math.min(b,x));
  const strength=mix(208,clamp(15.8/Math.max(duration*duration,.0064),260,780),boost),damping=mix(24,clamp(Math.sqrt(strength)*1.36,24,40),boost);
  const rates=[strength,damping,mix(11.2,clamp(4.8/Math.max(duration,.05),20,54),boost),mix(54,108,boost),mix(13.5,21,boost),mix(5.4,12.8,boost)];
  const state=createFumeCameraState();syncFumeCameraToArticle(state,article);
  Object.assign(state.camera,{x:initial[0],y:initial[1],velocityX:initial[2],velocityY:initial[3],focusX:initial[4],focusY:initial[5],scale:initial[6],velocityScale:initial[7],focusScale:initial[8]});
  Object.assign(state.retarget,{sourceLineIndex:-2,startedAt:10-phase*duration,duration,bridgeMode:'none'});
  for(let step=1;step<=120;step++){
   stepFumeCamera(state,{article,time:10,now:10000,dt:h,lineIndex:-1,viewport:{width:800,height:600},overviewCamera:{x:target[0],y:target[1],scale:target[2]},overviewStartTime:0,cameraSpeed:1,cameraTrackingMode:'smooth',staticMode:true,animationIntensity:'normal'});
   if([1,2,17,60,120].includes(step)){const p=state.camera;output.push([duration,phase,step,h,...rates,target.join(','),initial.join(','),[p.x,p.y,p.velocityX,p.velocityY,p.focusX,p.focusY,p.scale,p.velocityScale,p.focusScale].join(',')].join('\t'));}
  }
 }
 fs.writeFileSync(path.join(folder,'spring-response.tsv'),output.join('\n')+'\n');console.log(JSON.stringify({revision,samples:output.length-2}));
}finally{await server.close();}
