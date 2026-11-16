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
 const out=[`# original stepFumeCamera ${revision}; synthetic geometry; dt explicitly recorded; NOT media seek model`,
 '# scenario\ttime\tfloatTimeSeconds\tdt\tindex\twidth\theight\tspeed\tstepped\tstatic\tintensity\toverviewStart\tpose(x,y,vx,vy,fx,fy,scale,vs,fs)\tretarget(source,start,duration,fromX,fromY,fromScale,mode,waypointX,waypointY,waypointScale,phase)\toverview\trestore'];
 let id=0;
 for(const hz of[30,60,144,0])for(const stepped of[false,true])for(const staticMode of[false,true]){
  const viewport={width:stepped?380:800,height:600},state=createFumeCameraState();syncFumeCameraToArticle(state,article);
  const speed=[.55,1,1.85][id%3],intensity=['calm','normal','chaotic'][id%3];let time=0,index=0;
  const overview=resolveArticleOverviewCamera(article,viewport);
  while(time<8){
   const dt=hz?1/hz:[1/30,1/60,1/144,.05][index%4];time+=dt;
   const lineIndex=time<1.5?0:time<3?2:time<4.5?0:4;
   const r=stepFumeCamera(state,{article,time,now:(time+9.25)*1000,dt,lineIndex,viewport,overviewCamera:overview,overviewStartTime:6.8,cameraSpeed:speed,cameraTrackingMode:stepped?'stepped':'smooth',staticMode,animationIntensity:intensity});
   const p=state.camera,t=state.retarget;
   out.push([id,time,time+9.25,dt,lineIndex,viewport.width,viewport.height,speed,stepped,staticMode,intensity,6.8,
    [p.x,p.y,p.velocityX,p.velocityY,p.focusX,p.focusY,p.scale,p.velocityScale,p.focusScale].join(','),
    [t.sourceLineIndex,t.startedAt,t.duration,t.fromX,t.fromY,t.fromScale,t.bridgeMode,t.bridgeWaypointX,t.bridgeWaypointY,t.bridgeWaypointScale,t.bridgeWaypointPhase].join(','),r.shouldShowOverview,r.overviewTextRestoreProgress].join('\t'));index++;
  }id++;
 }
 fs.writeFileSync(path.join(folder,'camera-step.tsv'),out.join('\n')+'\n');console.log(JSON.stringify({revision,scenarios:id,steps:out.length-2}));
}finally{await server.close();}
