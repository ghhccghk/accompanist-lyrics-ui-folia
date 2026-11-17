// Passive source state exposure only; same original operators/geometry as scene capture.
import fs from'node:fs';import path from'node:path';import{chromium}from'playwright';
import{checkReference,root,revision}from'./reference.mjs';checkReference();
const browser=await chromium.launch({headless:true});
try{
 const result={revision,viewport:[800,600],rate:60,scope:'Original fixture operator, no retarget modification or delay fitting',scenes:[]};
 for(const stepped of[false,true]){
  const page=await browser.newPage({viewport:{width:800,height:600}});
  await page.goto(`http://127.0.0.1:4173/fume-scene.html?glow=0&stepped=${stepped}`);await page.waitForFunction(()=>window.ready);
  const setup=await page.evaluate(()=>window.fumeScene),frames=[];
  for(const frame of[167,168,169,179,180,181,185,189,579,580,581,582,583,584,585,600]){
   const t=frame/60;frames.push({t,...await page.evaluate(t=>window.drawAt(t),t)});
  }
  result.scenes.push({stepped,overviewStart:setup.overviewStart,frames});await page.close();
 }
 const out=path.join(root,'verification/fume-fixtures/scene-boundaries.json');fs.writeFileSync(out,JSON.stringify(result,null,2)+'\n');
 console.log(JSON.stringify(result.scenes.map(s=>({stepped:s.stepped,overviewStart:s.overviewStart,
  retargets:s.frames.filter((r,i,a)=>i===0||JSON.stringify(r.retarget)!==JSON.stringify(a[i-1].retarget)).map(r=>({t:r.t,...r.retarget}))})),null,2));
}finally{await browser.close()}
