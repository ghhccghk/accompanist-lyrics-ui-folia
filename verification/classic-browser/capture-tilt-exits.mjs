import fs from 'node:fs';
import path from 'node:path';
import {chromium} from 'playwright';
import {checkReference,root,revision} from './reference.mjs';
checkReference();
const output=path.join(root,'build/verification/tilt-exits-browser');fs.mkdirSync(output,{recursive:true});
const records=[`# actual original ${revision}; scene=wrap; width=380; parent AnimatePresence exit`,
 '# elapsedMs\tpresent\topacity'];
const browser=await chromium.launch({headless:true});
try {
 const page=await browser.newPage({viewport:{width:380,height:600},deviceScaleFactor:1});
 await page.addInitScript(()=>{delete Element.prototype.animate});
 const epoch=new Date('2026-01-01T00:00:00Z');await page.clock.install({time:epoch});await page.clock.pauseAt(epoch);
 await page.goto('http://127.0.0.1:4173/?manual&theme=tilt&scene=wrap&split=0&tilt=0');
 await page.waitForFunction(()=>window.ready);
 const leave=await page.evaluate(()=>window.lineRenderEnds[0]);
 const captures=[0,50,120,160,299,300,310,400,449,450,451];
 const ticks=[...new Set([...Array.from({length:Math.ceil((leave+451)/10)},(_,i)=>i*10),leave,...captures.map(t=>leave+t)])].sort((a,b)=>a-b);
 let previous=0;
 for(const time of ticks) {
  if(time>previous)await page.clock.runFor(time-previous);
  await page.evaluate(t=>window.step(t),time);previous=time;
  const elapsed=time-leave;
  if(!captures.includes(elapsed))continue;
  const state=await page.locator('div.whitespace-nowrap').evaluateAll(els=>els.length?{
   present:true,opacity:window.motionValues(els[0].parentElement).opacity??1
  }:{present:false,opacity:0});
  records.push([elapsed,state.present,state.opacity].join('\t'));
  await page.screenshot({path:path.join(output,`${elapsed}ms.png`)});
 }
 fs.writeFileSync(path.join(root,'verification/tilt-fixtures/mounted-exits.tsv'),records.join('\n')+'\n');
 fs.writeFileSync(path.join(output,'conditions.json'),JSON.stringify({revision,leave,width:380,height:600,dpr:1,browser:browser.version(),captures},null,2));
 console.log(records.join('\n'));
}finally{await browser.close()}
