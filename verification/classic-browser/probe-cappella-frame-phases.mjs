// Same original schedule/previous commit; observe DOM flow separately from
// media commit, RAF projection and native animation transport. No wrappers,
// setters, prototype changes or synthetic snapshot writes.
import fs from 'node:fs';import path from 'node:path';import {createHash}from'node:crypto';
import {chromium}from'playwright';import{startReferenceServer}from'./serve.mjs';
import{root,revision,checkReference,referenceMotionVersions}from'./reference.mjs';
checkReference();
const output=path.resolve(root,process.argv[2]??'verification/cappella-projection/frame-phases-v2.json');
if(fs.existsSync(output))throw Error('Preserve original phase trace; refusing overwrite');
const server=await startReferenceServer(4191);let browser;
try {
 browser=await chromium.launch({headless:true});const runs=[];
 for(const instrumented of [false,true]){
  const page=await browser.newPage({viewport:{width:380,height:480},deviceScaleFactor:1});const errors=[];
  page.on('pageerror',e=>errors.push(e.message));await page.goto('http://127.0.0.1:4191/cappella-playback.html?scene=latin&timeline=rapid&intensity=chaotic');
  await page.waitForFunction(()=>window.ready);const lines=await page.evaluate(()=>window.stageInputs);
  const probes=[...new Set([0,.7,.72,.78,.8,.847,.86,.9,.94,1,1.02,1.1,1.2,1.3,1.5,1.8,2,2.2,2.5,3,3.5,4,
   ...lines.flatMap(l=>[l.startTime,l.renderHints.renderEndTime,l.renderHints.renderEndTime+.01])])].sort((a,b)=>a-b).filter(t=>t<=1.1);
  await page.evaluate(()=>window.mountStage(0));await page.evaluate(()=>{for(let i=0;i<90;i++)window.advanceSourceFrame()});
  const base=await page.evaluate(()=>window.testNow);
  const ticks=[...new Set([...probes,...Array.from({length:241},(_,i)=>i/60)])].filter(t=>t<=1.1).sort((a,b)=>a-b);
  await page.evaluate(instrumented=>{
   window.phaseTrace=[];
   window.observeFramePhase=(tag,t,previousTime)=>{
    if(!instrumented||t<.99||t>1.04)return;
    const copy=v=>v===undefined?null:JSON.parse(JSON.stringify(v));
    const box=b=>b?{layout:copy(b.layoutBox),measured:copy(b.measuredBox),latest:copy(b.latestValues)}:null;
    const rect=e=>{const r=e.getBoundingClientRect();return [r.x,r.y,r.width,r.height]};
    const rows=[...document.querySelector('.max-w-4xl').children].map(row=>{
     const start=row[Object.keys(row).find(k=>k.startsWith('__reactFiber'))];let p,id;
     for(let f=start;f;f=f.return)if(f.memoizedProps?.message){id=f.memoizedProps.message.id;break}
     search:for(let f=start;f;f=f.return){let h=f.memoizedState;while(h){for(const c of [h.memoizedState,h.memoizedState?.current,...(Array.isArray(h.memoizedState)?h.memoizedState:[])])if(c?.projection){p=c.projection;break search}h=h.next}}
     const group=row.firstElementChild,frame=group.children[1];
     return{id,flowTop:row.offsetTop,flowHeight:row.offsetHeight,rowRect:rect(row),rowTransform:row.style.transform,
      groupMargin:group.style.marginTop,groupTransform:group.style.transform,
      frameInlineHeight:frame.style.height,frameComputedHeight:getComputedStyle(frame).height,
      projection:p?{snapshot:box(p.snapshot),layout:box(p.layout),progress:p.animationProgress,latest:copy(p.latestValues),
       targetDelta:copy(p.targetDelta),projectionDelta:copy(p.projectionDelta)}:null};
    });
    window.phaseTrace.push({tag,media:t,previousMedia:previousTime,clock:window.testNow,rows});
   };
  },instrumented);
  const states={};let previousTime=null;
  for(const t of ticks){
   await page.evaluate(({t,base,previousTime})=>{
    window.observeFramePhase('before-media',t,previousTime);
    window.testNow=base+t*1000;window.advanceStage(t);
    window.observeFramePhase('after-media-before-raf',t,previousTime);
    window.advanceSourceFrame(0);window.observeFramePhase('after-raf-before-native',t,previousTime);
    window.driveNativeAnimations();window.observeFramePhase('after-native',t,previousTime);
   },{t,base,previousTime});
   // React's local commit can land AFTER the evaluated RAF task, before the
   // next media update. Observe that phase too; do not call the inside-task
   // after-native values the final committed layout.
   await page.evaluate(({t,previousTime})=>window.observeFramePhase('after-browser-task',t,previousTime),{t,previousTime});
   previousTime=t;
   if(probes.includes(t)){await page.evaluate(t=>window.anchorStage(t),t);states[String(t)]=await page.evaluate(()=>window.readStage())}
  }
  if(errors.length)throw Error(errors.join('\n'));runs.push({instrumented,states,trace:await page.evaluate(()=>window.phaseTrace)});await page.close();
 }
 const baseline=JSON.parse(fs.readFileSync(path.join(root,'build/verification/cappella-presence-browser/states.json'),'utf8'));
 const originalGeometryMatches=Object.entries(runs[0].states).every(([time,value])=>{
  const original=Object.entries(baseline).find(([file,s])=>file.startsWith('rapid-chaotic-')&&s.time===Number(time))?.[1];
  return original&&JSON.stringify(original.geometry)===JSON.stringify(value);
 });
 const instrumentedGeometryMatches=JSON.stringify(runs[0].states)===JSON.stringify(runs[1].states);
 const hash=p=>createHash('sha256').update(fs.readFileSync(path.join(root,p))).digest('hex');
 fs.writeFileSync(output,JSON.stringify({revision,browser:browser.version(),motion:referenceMotionVersions(),viewport:[380,480],DPR:1,
  hashes:['.work/folia-major/src/components/visualizer/cappella/CappellaMessageRow.tsx','verification/classic-browser/cappella-stage.tsx',
   'verification/classic-browser/cappella-playback.html','verification/classic-browser/pnpm-lock.yaml','verification/assets/FoliaParityLatin-Bold.ttf'].map(file=>({file,sha256:hash(file)})),
  scope:'Same rapid/chaotic prefix to1.1s and exact previous commits as original pressure/boundary probe. Read-only phase observation can force DOM layout; both original and observer geometry are compared. No source/model/DOM/RAF/projection implementation is replaced. No screenshot or production repair.',
  originalGeometryMatches,instrumentedGeometryMatches,runs},null,2)+'\n');
 console.log(JSON.stringify({originalGeometryMatches,instrumentedGeometryMatches,phases:runs[1].trace.length}));
 if(!originalGeometryMatches||!instrumentedGeometryMatches)throw Error('Original or observed geometry changed; reject phase trace');
}finally{await browser?.close();await server.close()}
