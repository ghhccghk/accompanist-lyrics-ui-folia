// Bounded original two-commit trace; no source/projection formulas are replaced.
import fs from 'node:fs';
import path from 'node:path';
import {createHash} from 'node:crypto';
import {chromium} from 'playwright';
import {startReferenceServer} from './serve.mjs';
import {checkReference,root,revision,referenceMotionVersions} from './reference.mjs';
checkReference();
const output=path.resolve(root,process.argv[2]??'verification/cappella-projection/commit-boundary-v1.json');
if(fs.existsSync(output))throw Error('Preserve previous trace; do not overwrite');
const server=await startReferenceServer(4185);
let browser;
try {
 browser=await chromium.launch({headless:true});
 const runs=[];
 for(const instrumented of [false,true]){
  const page=await browser.newPage({viewport:{width:380,height:480},deviceScaleFactor:1});
  await page.goto('http://127.0.0.1:4185/cappella-playback.html?scene=latin&timeline=rapid&intensity=chaotic');
  await page.waitForFunction(()=>window.ready);
  const lines=await page.evaluate(()=>window.stageInputs);
  const probes=[...new Set([0,.7,.72,.78,.8,.847,.86,.9,.94,1,1.02,1.1,1.2,1.3,1.5,1.8,2,2.2,2.5,3,3.5,4,
   ...lines.flatMap(l=>[l.startTime,l.renderHints.renderEndTime,l.renderHints.renderEndTime+.01])])].sort((a,b)=>a-b).filter(t=>t<=1.1);
  await page.evaluate(()=>window.mountStage(0));
  await page.evaluate(()=>{for(let i=0;i<90;i++)window.advanceSourceFrame()});
  const base=await page.evaluate(()=>window.testNow);
  const ticks=[...new Set([...probes,...Array.from({length:241},(_,i)=>i/60)])].filter(t=>t<=1.1).sort((a,b)=>a-b);
  await page.evaluate(()=>{window.boundaryTrace=[];window.boundaryNodes=new WeakSet()});
  const observe=()=>page.evaluate(()=>{
   for(const row of document.querySelector('.max-w-4xl').children){
    let fiber=row[Object.keys(row).find(k=>k.startsWith('__reactFiber'))],p,id;
    for(let f=fiber;f;f=f.return)if(f.memoizedProps?.message){id=f.memoizedProps.message.id;break}
    if(id!=='line-0.94-3')continue;
    search:while(fiber){let h=fiber.memoizedState;while(h){for(const c of [h.memoizedState,h.memoizedState?.current,...(Array.isArray(h.memoizedState)?h.memoizedState:[])])if(c?.projection){p=c.projection;break search}h=h.next}fiber=fiber.return}
    if(!p||window.boundaryNodes.has(p))continue;
    window.boundaryNodes.add(p);
    const copy=v=>v===undefined?null:JSON.parse(JSON.stringify(v));
    const box=b=>b?{layout:copy(b.layoutBox),measured:copy(b.measuredBox),latest:copy(b.latestValues)}:null;
    const dom=()=>[...document.querySelector('.max-w-4xl').children].map(r=>{
     const g=r.firstElementChild,f=g.children[1];
     return {text:r.textContent,top:r.offsetTop,height:r.offsetHeight,computedHeight:getComputedStyle(r).height,
      rowTransform:r.style.transform,groupMargin:g.style.marginTop,groupTransform:g.style.transform,
      frameInlineHeight:f.style.height,frameComputedHeight:getComputedStyle(f).height};
    });
    const record=(tag,event)=>{
     if(window.traceTime<.99||window.traceTime>1.04)return;
     window.boundaryTrace.push({tag,time:window.traceTime,id,snapshot:box(p.snapshot),layout:box(p.layout),
      targetDelta:copy(p.targetDelta),progress:p.animationProgress,latest:copy(p.latestValues),
      event:event?{layout:copy(event.layout),snapshot:box(event.snapshot),delta:copy(event.delta)}:null,dom:dom()});
    };
    for(const name of ['willUpdate','updateLayout']){
     const original=p[name];if(typeof original!=='function')throw Error('Missing original projection method '+name);
     p[name]=function(...args){record(name+'.before');const result=Reflect.apply(original,this,args);record(name+'.after');return result};
    }
    p.addEventListener('didUpdate',event=>record('didUpdate',event));
   }
  });
  if(instrumented)await observe();
  const states={};
  for(const t of ticks){
   await page.evaluate(({t,base})=>{window.traceTime=t;window.testNow=base+t*1000;window.advanceStage(t);window.advanceSourceFrame(0);window.driveNativeAnimations()},{t,base});
   if(instrumented)await observe();
   if(probes.includes(t)){
    await page.evaluate(t=>window.anchorStage(t),t);
    states[String(t)]=await page.evaluate(()=>window.readStage());
   }
  }
  runs.push({instrumented,states,trace:await page.evaluate(()=>window.boundaryTrace)});
  await page.close();
 }
 const original=JSON.parse(fs.readFileSync(path.join(root,'build/verification/cappella-presence-browser/states.json'),'utf8'));
 const originalGeometryMatches=Object.entries(runs[0].states).every(([time,geometry])=>{
  const source=Object.entries(original).find(([file,s])=>file.startsWith('rapid-chaotic-')&&s.time===Number(time))?.[1];
  return source&&JSON.stringify(source.geometry)===JSON.stringify(geometry);
 });
 const instrumentedGeometryMatches=JSON.stringify(runs[0].states)===JSON.stringify(runs[1].states);
 const sha=file=>createHash('sha256').update(fs.readFileSync(path.join(root,file))).digest('hex');
 fs.writeFileSync(output,JSON.stringify({revision,motion:referenceMotionVersions(),browser:browser.version(),viewport:[380,480],DPR:1,
  sourceSha256:sha('.work/folia-major/src/components/visualizer/cappella/CappellaMessageRow.tsx'),
  lockSha256:sha('verification/classic-browser/pnpm-lock.yaml'),fontSha256:sha('verification/assets/FoliaParityLatin-Bold.ttf'),
  scope:'Exact original rapid/chaotic capture schedule prefix to1.1s. Separate control and instrumented original runs. Wrappers only read before/after original willUpdate/updateLayout, delegate with unchanged this/args, plus passive didUpdate. Extra DOM reads can force layout; geometry comparisons explicitly assess observer effects.',
  originalGeometryMatches,instrumentedGeometryMatches,runs},null,2)+'\n');
 console.log(JSON.stringify({originalGeometryMatches,instrumentedGeometryMatches,trace:runs[1].trace.length}));
 if(!originalGeometryMatches||!instrumentedGeometryMatches)throw Error('Original or instrumented geometry changed; trace is not accepted');
}finally{await browser?.close();await server.close()}
