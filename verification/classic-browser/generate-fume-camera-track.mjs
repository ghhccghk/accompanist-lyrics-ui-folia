// Controlled media-time driver around the UNTOUCHED original camera operator.
// Adaptive sampling is the Kotlin platform adaptation, not claimed upstream behavior.
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
 const epochs=[[0,0],[1.5,2],[3,0],[4.5,4]];
 const epochAt=t=>epochs.findLast(e=>e[0]<=t)[1];
 const controlTimes=[];
 for(const b of blocks){const n=b.graphemes.length,d=Math.max(b.line.endTime-b.line.startTime,.12);controlTimes.push(b.line.startTime,b.line.endTime);
  const offsets=[...Array.from({length:n+1},(_,i)=>i),...b.renderLines.flatMap(r=>[r.start-.7,r.start+.7,r.end-.7,r.end+.7])].filter(p=>p>=0&&p<=n);
  if(n)for(const p of offsets)controlTimes.push(b.line.startTime+p/n*d);
 }
 const events=[...new Set([...epochs.map(e=>e[0]),...controlTimes,6.8,8].filter(t=>Number.isFinite(t)&&t>=0&&t<=8))].sort((a,b)=>a-b);
 const out=[`# Folia ${revision}; original operator + explicitly adapted media driver; NOT original RAF equivalence`,
  '# scenario\ttime\tstepped\tstatic\tnodes\tadaptivePose\tadaptiveRetarget\t60HzPose'];
 const flatten=s=>{const p=s.camera;return[p.x,p.y,p.velocityX,p.velocityY,p.focusX,p.focusY,p.scale,p.velocityScale,p.focusScale].join(',')};
 const rt=s=>{const t=s.retarget;return[t.sourceLineIndex,t.startedAt,t.duration,t.fromX,t.fromY,t.fromScale,t.bridgeMode,t.bridgeWaypointX,t.bridgeWaypointY,t.bridgeWaypointScale,t.bridgeWaypointPhase].join(',')};
 for(let scenario=0;scenario<4;scenario++){
  const stepped=!!(scenario&1),staticMode=!!(scenario&2),viewport={width:800,height:600},overview=resolveArticleOverviewCamera(article,viewport);
  const fresh=()=>{const state=createFumeCameraState();syncFumeCameraToArticle(state,article);return state};
  const step=(before,time,dt,lineIndex,showOverview)=>{const state=structuredClone(before);stepFumeCamera(state,{article,time,now:(time+9.25)*1000,dt,lineIndex,viewport,overviewCamera:overview,overviewStartTime:showOverview?6.8:Infinity,cameraSpeed:1,cameraTrackingMode:stepped?'stepped':'smooth',staticMode,animationIntensity:'normal'});return state;};
  const error=(a,b)=>Math.max(Math.abs(a.camera.x-b.camera.x)*Math.max(a.camera.scale,b.camera.scale,.001),Math.abs(a.camera.y-b.camera.y)*Math.max(a.camera.scale,b.camera.scale,.001),Math.abs(a.camera.scale-b.camera.scale)*800);
  const integrate=(initial,from,to,lineIndex,showOverview,accepted=()=>{})=>{
   let state=initial,time=from,h=Math.min(.05,to-from),attempts=0;
   while(time<to){if(++attempts>500000)throw Error('Nonconvergence');h=Math.min(h,to-time);if(!(h>0&&time+h>time))throw Error('Precision');
    const full=step(state,time+h,h,lineIndex,showOverview),middle=step(state,time+h*.5,h*.5,lineIndex,showOverview),half=step(middle,time+h,h*.5,lineIndex,showOverview),e=error(full,half);
    if(!Number.isFinite(e))throw Error('Nonfinite');
    if(e<=.02){time=h===to-time?to:time+h;state=half;accepted(time,state);h*=e<.02*.125?1.8:1;}else h*=Math.max(.15,.8*Math.sqrt(.02/e));
   }return state;
  };
  const nodes=[];let time=0,state=fresh();
  for(const end of events){if(end>time){state=integrate(state,time,end,epochAt(time),time>=6.8,(t,s)=>nodes.push({time:t,state:s}));time=end;}
   const source=time>=6.8?-2:epochAt(time);if(state.retarget.sourceLineIndex!==source)state=step(state,time,0,epochAt(time),time>=6.8);
   if(nodes.at(-1)?.time===time)nodes[nodes.length-1]={time,state};else nodes.push({time,state});
  }
  const sample=t=>{const node=nodes.findLast(n=>n.time<=t)??nodes[0];return t<=node.time?node.state:integrate(node.state,node.time,t,epochAt(node.time),node.time>=6.8)};
  let regular=fresh();
  for(let i=0;i<=480;i++){const t=i/60;regular=step(regular,t,i?1/60:0,epochAt(t),t>=6.8);
   if(i%6===0||[91,92,93,181,182,183,409,410,411].includes(i))out.push([scenario,t,stepped,staticMode,nodes.length,flatten(sample(t)),rt(sample(t)),flatten(regular)].join('\t'));
  }
 }
 fs.writeFileSync(path.join(folder,'camera-track.tsv'),out.join('\n')+'\n');console.log(JSON.stringify({revision,samples:out.length-2}));
}finally{await server.close();}
