import fs from'node:fs';import path from'node:path';import{chromium}from'playwright';import{root,revision,checkReference}from'./reference.mjs';checkReference();
const browser=await chromium.launch({headless:true});
try{
 const page=await browser.newPage({viewport:{width:380,height:600}});
 await page.goto('http://127.0.0.1:4173/pendolo-chorus-stage.html?scene=latin&intensity=normal&dense=1');
 await page.waitForFunction(()=>window.ready);for(let i=0;i<10;i++)await page.evaluate(()=>window.advanceSourceFrame());
 const base=await page.evaluate(()=>window.testNow),probes=[0,.7,.72,.75,.8,.9,1.1],events=[];
 const times=[...new Set([...probes,...Array.from({length:67},(_,i)=>i/60)])].sort((a,b)=>a-b);
 for(const time of times){
  const event=await page.evaluate(({time,base})=>{window.testNow=base+time*1000;const before=window.stageModel?.items.find(i=>i.isActive)?.index??-1;
   const state=window.advanceWheel(time);window.advanceSourceFrame(0);window.driveNativeAnimations();
   return{...state,before,time};},{time,base});
  if(event.current!==event.before)events.push(event);
 }
 const actual=await page.evaluate(()=>{const wheel=document.querySelector('#pendolo-reference').firstElementChild;
  return[...wheel.children].flatMap((row,i)=>{const p=row.firstElementChild.firstElementChild.firstElementChild;
   const halo=p.children.length===2?p.firstElementChild:null;return halo?[{index:window.stageModel.items[i].index,
    opacity:getComputedStyle(halo).opacity,animations:halo.getAnimations().map(a=>({currentTime:a.currentTime,timing:a.effect.getTiming(),keyframes:a.effect.getKeyframes()}))}]:[];});});
 const inputs=await page.evaluate(()=>window.chorusInputs);
 fs.writeFileSync(path.join(root,'verification/pendolo-fixtures/chorus-source-events.json'),JSON.stringify({revision,
  scope:'Actual source current-line changes under the SAME60Hz plus exact probes as primary matrix; no forced phase correction.',
  semanticStarts:inputs.lines.map(l=>l.startTime),observedChanges:events,at1p1:actual},null,2)+'\n');
 console.log(JSON.stringify({events,at1p1:actual}));
}finally{await browser.close()}
