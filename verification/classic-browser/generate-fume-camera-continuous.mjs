// Continuous response calibration, using the UNTOUCHED original operator as target/trajectory oracle.
// The modified equation is explicitly an adaptation, not original RAF equivalence.
// dt=1 target inversion here is a test-only observation seam; native uses a pure resolved intent.
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
 const events=[...new Set([...epochs.map(e=>e[0]),...controlTimes,...controlTimes.map(t=>t-1/120),...controlTimes.map(t=>t-1/60),6.8,8].filter(t=>Number.isFinite(t)&&t>=0&&t<=8))].sort((a,b)=>a-b);
 const pose=s=>{const p=s.camera;return[p.x,p.y,p.velocityX,p.velocityY,p.focusX,p.focusY,p.scale,p.velocityScale,p.focusScale]};
 const setPose=(s,p)=>{const q=structuredClone(s);Object.assign(q.camera,{x:p[0],y:p[1],velocityX:p[2],velocityY:p[3],focusX:p[4],focusY:p[5],scale:p[6],velocityScale:p[7],focusScale:p[8]});return q;};
 const H=1/60,clamp=(x,a,b)=>Math.max(a,Math.min(b,x)),mix=(a,b,t)=>a+(b-a)*t;
 function logSpring(k,d,r){
  const a=Math.exp(-r*H),b00=1-H*H*k,b01=H*(1-H*d),b10=-H*k,b11=1-H*d;
  const det=b00*b11-b01*b10,tr=b00+b11,disc=tr*tr-4*det;
  let m00,m01,m10,m11;
  if(disc<0){const radius=Math.sqrt(det),cs=tr/(2*radius),theta=Math.acos(cs),f=theta/Math.sin(theta),log=Math.log(radius);
   m00=log+f*(b00/radius-cs);m01=f*b01/radius;m10=f*b10/radius;m11=log+f*(b11/radius-cs);
  }else{const s=Math.sqrt(disc),p=(tr+s)/2,q=(tr-s)/2,f=s<1e-8?1/p:(Math.log(p)-Math.log(q))/s;
   m00=Math.log(q)+f*(b00-q);m01=f*b01;m10=f*b10;m11=Math.log(q)+f*(b11-q);
  }
  const u0=H*H*k*a,u1=H*k*a,ln=Math.log(a),z0=(m00-ln)*u0+m01*u1,z1=m10*u0+(m11-ln)*u1;
  const divisor=(b00-a)*(b11-a)-b01*b10;
  const w0=((b11-a)*z0-b01*z1)/divisor,w1=(-b10*z0+(b00-a)*z1)/divisor;
  return[m00/H,m01/H,w0/H,m10/H,m11/H,w1/H,-r];
 }
 const reports=[],output=[`# Folia ${revision}; adapted continuous media response vs original frame-dependent operator`,"# scenario\trate\ttime\tstepped\tstatic\tnodes\tcontinuousPose\toriginalPose"];
 for(let scenario=0;scenario<4;scenario++){
  const stepped=!!(scenario&1),staticMode=!!(scenario&2),viewport={width:800,height:600},overview=resolveArticleOverviewCamera(article,viewport);
  const fresh=()=>{const state=createFumeCameraState();syncFumeCameraToArticle(state,article);return state};
  const step=(before,time,dt,lineIndex,showOverview)=>{const state=structuredClone(before);stepFumeCamera(state,{article,time,now:(time+9.25)*1000,dt,lineIndex,viewport,overviewCamera:overview,overviewStartTime:showOverview?6.8:Infinity,cameraSpeed:1,cameraTrackingMode:stepped?'stepped':'smooth',staticMode,animationIntensity:'normal'});return state;};
  function info(s,t,index,show){
   const phase=clamp((t-s.retarget.startedAt)/Math.max(s.retarget.duration,.001),0,1),boost=1-(1-(1-phase)**3);
   const p=step(s,t,1,index,show).camera,bridge=s.retarget.bridgeMode!=='none'&&phase<1;
   if(bridge)return{bridge,phase,q:[p.focusX,p.focusY,p.focusScale],rate:s.retarget.bridgeMode==='overview'?mix(12.5,22,1-phase):mix(10.5,17.5,1-phase)};
   const rate=mix(11.2,clamp(4.8/Math.max(s.retarget.duration,.05),20,54),boost),rs=mix(5.4,12.8,boost),a=1-Math.exp(-rate),as=1-Math.exp(-rs);
   const q=[s.camera.focusX+(p.focusX-s.camera.focusX)/a,s.camera.focusY+(p.focusY-s.camera.focusY)/a,s.camera.focusScale+(p.focusScale-s.camera.focusScale)/as];
   const k=mix(208,clamp(15.8/Math.max(s.retarget.duration*s.retarget.duration,.0064),260,780),boost),d=mix(24,clamp(Math.sqrt(k)*1.36,24,40),boost);
   return{bridge,phase,q,limit:mix(1320,clamp(Math.hypot(q[0]-s.camera.x,q[1]-s.camera.y)/Math.max(s.retarget.duration*.28,.028),2600,8800),boost),mat:logSpring(k,d,rate),smat:logSpring(mix(54,108,boost),mix(13.5,21,boost),rs)};
  }
  const lead=H/2;
  function derivative(s,t,index,show){
   let i=info(s,t+lead,index,show);const later=info(s,t+H,index,show);if(i.bridge&&!later.bridge)i=later;if(i.bridge)i={...i,q:info(s,t+1/i.rate-H/Math.expm1(i.rate*H),index,show).q};const p=pose(s),v=Array(9).fill(0);
   if(i.bridge){v[0]=(i.q[0]-p[0])*i.rate;v[1]=(i.q[1]-p[1])*i.rate;v[6]=(i.q[2]-p[6])*i.rate;v[2]=Math.log(.72)/H*p[2];v[3]=Math.log(.72)/H*p[3];v[7]=Math.log(.68)/H*p[7];}
   else for(const[x,velocity,focus,target,m]of[[0,2,4,i.q[0],i.mat],[1,3,5,i.q[1],i.mat],[6,7,8,i.q[2],i.smat]]){
    v[x]=m[0]*(p[x]-target)+m[1]*p[velocity]+m[2]*(p[focus]-target);
    v[velocity]=m[3]*(p[x]-target)+m[4]*p[velocity]+m[5]*(p[focus]-target);v[focus]=m[6]*(p[focus]-target);const limit=velocity===7?1.6:i.limit;if(p[velocity]>=limit&&v[velocity]>0){v[velocity]=0;v[x]=limit;}else if(p[velocity]<=-limit&&v[velocity]<0){v[velocity]=0;v[x]=-limit;}
   }
   return v;
  }
  function rk(s,t,h,index,show){
   const p=pose(s),k1=derivative(s,t,index,show),k2=derivative(setPose(s,p.map((v,i)=>v+h*k1[i]/2)),t+h/2,index,show),
    k3=derivative(setPose(s,p.map((v,i)=>v+h*k2[i]/2)),t+h/2,index,show),k4=derivative(setPose(s,p.map((v,i)=>v+h*k3[i])),t+h,index,show);
   const next=setPose(s,p.map((v,i)=>v+h/6*(k1[i]+2*k2[i]+2*k3[i]+k4[i])));
   next.camera.scale=clamp(next.camera.scale,.22,2.24);next.camera.velocityScale=clamp(next.camera.velocityScale,-1.6,1.6);
   let i=info(next,t+h+lead,index,show);const later=info(next,t+h+H,index,show);if(i.bridge&&!later.bridge)i=later;if(!i.bridge){next.camera.velocityX=clamp(next.camera.velocityX,-i.limit,i.limit);next.camera.velocityY=clamp(next.camera.velocityY,-i.limit,i.limit);}if(i.bridge){next.camera.focusX=i.q[0];next.camera.focusY=i.q[1];next.camera.focusScale=i.q[2];}
   return next;
  }
  const err=(a,b)=>Math.max(Math.abs(a.camera.x-b.camera.x)*Math.max(a.camera.scale,b.camera.scale,.001),Math.abs(a.camera.y-b.camera.y)*Math.max(a.camera.scale,b.camera.scale,.001),Math.abs(a.camera.scale-b.camera.scale)*800);
  let nodes=0;
  function integrate(s,from,to,index,show,accepted=()=>{}){let time=from,h=Math.min(.05,to-from),attempts=0;
   while(time<to){if(++attempts>100000)throw Error('Nonconverge');h=Math.min(h,to-time);if(s.retarget.bridgeMode!=='none'){for(const delta of [H,H/2]){const edge=s.retarget.startedAt+s.retarget.duration-delta;if(edge>time&&edge<time+h)h=edge-time;}}const full=rk(s,time,h,index,show),middle=rk(s,time,h/2,index,show),half=rk(middle,time+h/2,h/2,index,show),e=err(full,half);
    if(!Number.isFinite(e))throw Error('nonfinite');
    if(e<.0000001){time=h===to-time?to:time+h;s=half;nodes++;accepted(time,s);h*=e<.000000005?1.8:1;}else h*=Math.max(.15,.8*(.0000001/e)**.2);
   }return s;
  }
  const prepared=[];let s=fresh(),time=0;
  for(const end of events){if(end>time){s=integrate(s,time,end,epochAt(time),time>=6.8,(time,state)=>prepared.push({time,state}));time=end;}
   if(s.retarget.sourceLineIndex!==(time>=6.8?-2:epochAt(time)))s=step(s,time,0,epochAt(time),time>=6.8);
   prepared.push({time,state:s});
  }
  const preparedNodes=nodes;
  const sample=t=>{const n=prepared.findLast(n=>n.time<=t)??prepared[0];return t<=n.time?n.state:integrate(n.state,n.time,t,epochAt(n.time),n.time>=6.8)};
  for(const rate of [30,60,144,'irregular']){
   let regular=fresh(),time=0,frame=0,dist=0,scaleError=0,screen=0;
   const record=()=>{
    const s=sample(time),d=Math.hypot(s.camera.x-regular.camera.x,s.camera.y-regular.camera.y);
    dist=Math.max(dist,d);scaleError=Math.max(scaleError,Math.abs(s.camera.scale-regular.camera.scale));screen=Math.max(screen,d*Math.max(s.camera.scale,regular.camera.scale));
    output.push([scenario,rate,time,stepped,staticMode,preparedNodes,pose(s).join(','),pose(regular).join(',')].join('\t'));
   };
   regular=step(regular,0,0,epochAt(0),false);record();
   while(time<8){const dt=Math.min(rate==='irregular'?[.007,.029,.011,.019,.033][frame%5]:1/rate,8-time);
    time=rate==='irregular'?time+dt:Math.min((frame+1)/rate,8);
    regular=step(regular,time,dt,epochAt(time),time>=6.8);record();frame++;
   }
   reports.push({scenario,rate,nodes:preparedNodes,maximumWorldDistance:dist,maximumScaleError:scaleError,maximumScreenDistance:screen});
  }
 }
 fs.writeFileSync(path.join(folder,'camera-continuous.tsv'),output.join('\n')+'\n');console.log(JSON.stringify(reports,null,2));
}finally{await server.close();}
