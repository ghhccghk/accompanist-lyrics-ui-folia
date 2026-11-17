import fs from 'node:fs';
import {chromium} from 'playwright';
import {checkReference,root,revision,referenceMotionVersions} from './reference.mjs';
import {startReferenceServer} from './serve.mjs';
import path from 'node:path';
checkReference();
const timeline=process.argv[2]??'overlap',intensity=process.argv[3]??'chaotic';
if(!['rapid','overlap'].includes(timeline)||!['calm','normal','chaotic'].includes(intensity))throw Error('Unknown pressure configuration');
const server=await startReferenceServer(4183),browser=await chromium.launch({headless:true});
try {
 const page=await browser.newPage({viewport:{width:380,height:480}});
 await page.goto(`http://127.0.0.1:4183/cappella-playback.html?scene=latin&timeline=${timeline}&intensity=${intensity}`);
 await page.waitForFunction(()=>window.ready);await page.evaluate(()=>window.mountStage(0));
 await page.evaluate(()=>{for(let i=0;i<90;i++)window.advanceSourceFrame()});
 const base=await page.evaluate(()=>window.testNow),lines=await page.evaluate(()=>window.stageInputs);
 const probes=[...new Set([0,.7,.72,.78,.8,.847,.86,.9,.94,1,1.02,1.1,1.2,1.3,1.5,1.8,2,2.2,2.5,3,3.5,4,...lines.flatMap(l=>[l.startTime,l.renderHints.renderEndTime,l.renderHints.renderEndTime+.01])])].sort((a,b)=>a-b);
 const ticks=[...new Set([...probes,...Array.from({length:241},(_,i)=>i/60)])].filter(t=>t<=2.2).sort((a,b)=>a-b);
 const observe=()=>page.evaluate(()=>{
  window.commitTrace??=[];window.projectionNodes??=new WeakSet();
  for(const row of document.querySelector('.max-w-4xl').children){
   let fiber=row[Object.keys(row).find(k=>k.startsWith('__reactFiber'))],p,message;
   for(let f=fiber;f;f=f.return)if(f.memoizedProps?.message){message=f.memoizedProps.message.id;break;}
   search:while(fiber){let h=fiber.memoizedState;while(h){for(const c of[h.memoizedState,h.memoizedState?.current,...(Array.isArray(h.memoizedState)?h.memoizedState:[])])if(c?.projection){p=c.projection;break search}h=h.next}fiber=fiber.return}
   if(!p||window.projectionNodes.has(p))continue;window.projectionNodes.add(p);
   p.addEventListener('didUpdate',event=>window.commitTrace.push(JSON.parse(JSON.stringify({time:window.traceTime,message,id:p.id,layout:event.layout.y,snapshot:event.snapshot.layoutBox.y,delta:event.delta.y.translate,progress:p.animationProgress,offset:p.targetDelta?.y.translate,latest:p.latestValues,
    offsetTop:row.offsetTop,offsetHeight:row.offsetHeight,computedHeight:getComputedStyle(row).height,position:getComputedStyle(row).position}))));
  }
 });
 await observe();
 for(const t of ticks){await page.evaluate(({t,base})=>{window.traceTime=t;window.testNow=base+t*1000;window.advanceStage(t);window.advanceSourceFrame(0);window.driveNativeAnimations()},{t,base});await observe();if(probes.includes(t))await page.evaluate(t=>window.anchorStage(t),t)}
 const result=await page.evaluate(()=>({events:window.commitTrace,geometry:window.readStage()}));
 const metadata={revision,motion:referenceMotionVersions(),browser:browser.version(),timeline,intensity,scope:'Passive original projection didUpdate callbacks; no source logic changed. Same pressure clock/probes; trace limited to2.2s.'};
 fs.writeFileSync(path.join(root,`build/verification/cappella-presence-browser/layout-commits-${timeline}-${intensity}.json`),JSON.stringify({...metadata,...result},null,2));
 console.log(JSON.stringify({events:result.events.length,timeline,intensity}));
}finally{await browser.close();await server.close()}
