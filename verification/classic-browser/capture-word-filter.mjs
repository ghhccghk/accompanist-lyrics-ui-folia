// Mounted original GlowWord filter values, including active->passed interruption.
import fs from 'node:fs';
import path from 'node:path';
import {chromium} from 'playwright';
import {checkReference,root,revision} from './reference.mjs';
checkReference();
const browser=await chromium.launch({headless:true});
const out=[`# actual mounted original ${revision}; body filter blur CSS px`,
 '# scene\tms\tword\tblur'];
try {
 for(const scene of ['wrap','tight']) {
  const page=await browser.newPage({viewport:{width:800,height:600},deviceScaleFactor:1});
  await page.addInitScript(()=>{delete Element.prototype.animate});
  const epoch=new Date('2026-01-01T00:00:00Z');await page.clock.install({time:epoch});await page.clock.pauseAt(epoch);
  await page.goto(`http://127.0.0.1:4173/?manual&theme=partita&scene=${scene}&intensity=normal`);
  await page.waitForFunction(()=>window.ready);
  const events=await page.evaluate(()=>window.eventTimes);
  const checks=Array.from({length:33},(_,i)=>700+i*25);
  const ticks=[...new Set([...Array.from({length:151},(_,i)=>i*10),...events.filter(t=>t<=1500),...checks])].sort((a,b)=>a-b);
  for(const t of ticks) {
   await page.evaluate(t=>window.step(t),t);
   if(!checks.includes(t))continue;
   const filters=await page.locator('.inline-flex > .inline-block').evaluateAll(els=>els.map(el=>window.motionValues(el.children[1]).filter));
   filters.forEach((filter,i)=>{
    if(filter!=='none'&&!/^blur\([-\d.]+px\)$/.test(filter))throw Error(`Unexpected filter ${filter}`);
    out.push([scene,t,i,filter==='none'?0:parseFloat(filter.slice(5))].join('\t'));
   });
  }
  await page.close();
 }
 fs.writeFileSync(path.join(root,'verification/partita-fixtures/word-filter.tsv'),out.join('\n')+'\n');
 console.log(`Saved ${out.length-2} actual body filter values`);
}finally{await browser.close()}
